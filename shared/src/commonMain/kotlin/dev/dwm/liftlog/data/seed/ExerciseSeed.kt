package dev.dwm.liftlog.data.seed

import dev.dwm.liftlog.data.db.AppDatabase
import dev.dwm.liftlog.data.db.Exercise
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import liftlog.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@Serializable
private data class SeedExercise(
    val id: Long,
    val name: String,
    val category: String = "",
    val muscles: List<String> = emptyList(),
    val equipment: List<String> = emptyList(),
)

// bump when exercises.json grows: existing installs insert the new names on next launch
private const val SEED_VERSION = "3"

/**
 * Stable id derived from the exercise name, so every device and every reinstall seeds the SAME id.
 *
 * Seeded exercises used to get a fresh random uuid per install. Sync replicates by id, so each
 * reinstall pushed a complete new copy of the catalogue and pulled back all the previous ones —
 * one database had 12176 exercise rows for 1293 real exercises. Deterministic ids make the seed
 * idempotent across devices: a re-seed collides with the existing row instead of duplicating it.
 */
internal fun seedExerciseId(name: String): String {
    var hash = -0x340d631b7bdddcdbL // FNV-1a 64-bit offset basis
    for (ch in name.trim().lowercase()) {
        hash = hash xor ch.code.toLong()
        hash *= 0x100000001b3L
    }
    return "seed-" + hash.toULong().toString(16).padStart(16, '0')
}

@OptIn(ExperimentalResourceApi::class)
suspend fun seedExercisesIfEmpty(db: AppDatabase) {
    val dao = db.exerciseDao()
    if (db.settingDao().get("exerciseSeedVersion") == SEED_VERSION) return
    val bytes = Res.readBytes("files/exercises.json")
    val seed = Json { ignoreUnknownKeys = true }
        .decodeFromString<List<SeedExercise>>(bytes.decodeToString())
    val fresh = dao.count() == 0
    val toInsert = if (fresh) seed else seed.filter { dao.byName(it.name) == null }
    dao.insertAll(
        toInsert.map {
            Exercise(
                id = seedExerciseId(it.name),
                name = it.name,
                category = it.category,
                muscles = it.muscles.joinToString(", "),
                equipment = it.equipment.joinToString(", "),
            )
        }
    )
    db.settingDao().put(dev.dwm.liftlog.data.db.Setting("exerciseSeedVersion", SEED_VERSION))
}
