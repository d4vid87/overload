package dev.dwm.liftlog.data

import dev.dwm.liftlog.data.db.AppDatabase
import dev.dwm.liftlog.data.db.Setting
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
data class SyncPayload(
    val since: Long,
    val changes: Map<String, List<JsonElement>>,
)

@Serializable
data class SyncResponse(
    val now: Long,
    val changes: Map<String, List<JsonElement>>,
)

data class SyncResult(val pushed: Int, val pulled: Int)

/** Silent best-effort sync — fires on app start and workout finish; manual button stays for debugging. */
suspend fun autoSync(db: AppDatabase) {
    val url = db.settingDao().get("syncUrl")?.takeIf { it.isNotBlank() } ?: return
    val token = db.settingDao().get("syncToken")?.takeIf { it.isNotBlank() } ?: return
    runCatching { SyncEngine(db, httpClient()).sync(url, token) }
}

/**
 * LWW replication against the Netlify sync function.
 * Push local rows with updatedAt > lastSync; pull remote rows newer than lastSync;
 * apply each remote row only if it is newer than the local copy.
 */
class SyncEngine(private val db: AppDatabase, engineClient: HttpClient) {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = engineClient.config {
        install(ContentNegotiation) { json(this@SyncEngine.json) }
    }

    suspend fun sync(baseUrl: String, token: String): SyncResult {
        val since = db.settingDao().get("lastSyncAt")?.toLongOrNull() ?: 0L
        val s = db.syncDao()

        val local: Map<String, List<JsonElement>> = mapOf(
            "Exercise" to s.exercisesSince(since).map { json.encodeToJsonElement(dev.dwm.liftlog.data.db.Exercise.serializer(), it) },
            "Workout" to s.workoutsSince(since).map { json.encodeToJsonElement(dev.dwm.liftlog.data.db.Workout.serializer(), it) },
            "WorkoutSet" to s.setsSince(since).map { json.encodeToJsonElement(dev.dwm.liftlog.data.db.WorkoutSet.serializer(), it) },
            "Program" to s.programsSince(since).map { json.encodeToJsonElement(dev.dwm.liftlog.data.db.Program.serializer(), it) },
            "ProgramDay" to s.programDaysSince(since).map { json.encodeToJsonElement(dev.dwm.liftlog.data.db.ProgramDay.serializer(), it) },
            "ProgramExercise" to s.programExercisesSince(since).map { json.encodeToJsonElement(dev.dwm.liftlog.data.db.ProgramExercise.serializer(), it) },
            "Food" to s.foodsSince(since).map { json.encodeToJsonElement(dev.dwm.liftlog.data.db.Food.serializer(), it) },
            "FoodLog" to s.foodLogsSince(since).map { json.encodeToJsonElement(dev.dwm.liftlog.data.db.FoodLog.serializer(), it) },
            "WeightEntry" to s.weightsSince(since).map { json.encodeToJsonElement(dev.dwm.liftlog.data.db.WeightEntry.serializer(), it) },
            "GroceryItem" to s.groceriesSince(since).map { json.encodeToJsonElement(dev.dwm.liftlog.data.db.GroceryItem.serializer(), it) },
            "Routine" to s.routinesSince(since).map { json.encodeToJsonElement(dev.dwm.liftlog.data.db.Routine.serializer(), it) },
            "RoutineExercise" to s.routineExercisesSince(since).map { json.encodeToJsonElement(dev.dwm.liftlog.data.db.RoutineExercise.serializer(), it) },
        ).filterValues { it.isNotEmpty() }

        val response: SyncResponse = client.post("${baseUrl.trimEnd('/')}/api/sync") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(SyncPayload(since, local))
        }.body()

        var pulled = 0
        for ((table, rows) in response.changes) {
            for (el in rows) {
                pulled += if (applyRemote(table, el)) 1 else 0
            }
        }
        db.settingDao().put(Setting(key = "lastSyncAt", value = response.now.toString()))
        return SyncResult(pushed = local.values.sumOf { it.size }, pulled = pulled)
    }

    /**
     * Restore an "Export all data (JSON)" file. Rows go through the same last-write-wins path as a
     * sync pull, so importing is safe to run over an existing database: newer local rows survive.
     * Returns the number of rows actually written.
     *
     * Export was write-only before this — the backup file could not be loaded back, which made it
     * useless as a backup for a reinstall.
     */
    suspend fun importAll(jsonText: String): Int {
        val root = json.parseToJsonElement(jsonText) as? kotlinx.serialization.json.JsonObject
            ?: error("not a JSON object")
        var applied = 0
        for ((table, el) in root) {
            val rows = el as? kotlinx.serialization.json.JsonArray ?: continue // skips "exportedAt"
            for (row in rows) {
                if (runCatching { applyRemote(table, row) }.getOrDefault(false)) applied++
            }
        }
        return applied
    }

    private suspend fun applyRemote(table: String, el: JsonElement): Boolean {
        val s = db.syncDao()
        return when (table) {
            "Exercise" -> {
                val row = json.decodeFromJsonElement(dev.dwm.liftlog.data.db.Exercise.serializer(), el)
                if ((s.exerciseUpdatedAt(row.id) ?: -1) < row.updatedAt) { s.upsertExercise(row); true } else false
            }
            "Workout" -> {
                val row = json.decodeFromJsonElement(dev.dwm.liftlog.data.db.Workout.serializer(), el)
                if ((s.workoutUpdatedAt(row.id) ?: -1) < row.updatedAt) { s.upsertWorkout(row); true } else false
            }
            "WorkoutSet" -> {
                val row = json.decodeFromJsonElement(dev.dwm.liftlog.data.db.WorkoutSet.serializer(), el)
                if ((s.setUpdatedAt(row.id) ?: -1) < row.updatedAt) { s.upsertSet(row); true } else false
            }
            "Program" -> {
                val row = json.decodeFromJsonElement(dev.dwm.liftlog.data.db.Program.serializer(), el)
                if ((s.programUpdatedAt(row.id) ?: -1) < row.updatedAt) { s.upsertProgram(row); true } else false
            }
            "ProgramDay" -> {
                val row = json.decodeFromJsonElement(dev.dwm.liftlog.data.db.ProgramDay.serializer(), el)
                if ((s.programDayUpdatedAt(row.id) ?: -1) < row.updatedAt) { s.upsertProgramDay(row); true } else false
            }
            "ProgramExercise" -> {
                val row = json.decodeFromJsonElement(dev.dwm.liftlog.data.db.ProgramExercise.serializer(), el)
                if ((s.programExerciseUpdatedAt(row.id) ?: -1) < row.updatedAt) { s.upsertProgramExercise(row); true } else false
            }
            "Food" -> {
                val row = json.decodeFromJsonElement(dev.dwm.liftlog.data.db.Food.serializer(), el)
                if ((s.foodUpdatedAt(row.id) ?: -1) < row.updatedAt) { s.upsertFood(row); true } else false
            }
            "FoodLog" -> {
                val row = json.decodeFromJsonElement(dev.dwm.liftlog.data.db.FoodLog.serializer(), el)
                if ((s.foodLogUpdatedAt(row.id) ?: -1) < row.updatedAt) { s.upsertFoodLog(row); true } else false
            }
            "WeightEntry" -> {
                val row = json.decodeFromJsonElement(dev.dwm.liftlog.data.db.WeightEntry.serializer(), el)
                if ((s.weightUpdatedAt(row.id) ?: -1) < row.updatedAt) { s.upsertWeight(row); true } else false
            }
            "GroceryItem" -> {
                val row = json.decodeFromJsonElement(dev.dwm.liftlog.data.db.GroceryItem.serializer(), el)
                if ((s.groceryUpdatedAt(row.id) ?: -1) < row.updatedAt) { s.upsertGrocery(row); true } else false
            }
            "Routine" -> {
                val row = json.decodeFromJsonElement(dev.dwm.liftlog.data.db.Routine.serializer(), el)
                if ((s.routineUpdatedAt(row.id) ?: -1) < row.updatedAt) { s.upsertRoutine(row); true } else false
            }
            "RoutineExercise" -> {
                val row = json.decodeFromJsonElement(dev.dwm.liftlog.data.db.RoutineExercise.serializer(), el)
                if ((s.routineExerciseUpdatedAt(row.id) ?: -1) < row.updatedAt) { s.upsertRoutineExercise(row); true } else false
            }
            // reachable only from importAll — Setting is deliberately not in the sync payload
            "Setting" -> {
                val row = json.decodeFromJsonElement(Setting.serializer(), el)
                if ((s.settingUpdatedAt(row.key) ?: -1) < row.updatedAt) { s.upsertSetting(row); true } else false
            }
            else -> false
        }
    }
}
