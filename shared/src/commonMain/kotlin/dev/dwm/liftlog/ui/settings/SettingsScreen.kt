package dev.dwm.liftlog.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.dwm.liftlog.data.SyncEngine
import dev.dwm.liftlog.data.db.AppDatabase
import dev.dwm.liftlog.data.db.Setting
import dev.dwm.liftlog.data.httpClient
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Composable
fun SettingsScreen(
    db: AppDatabase,
    modifier: Modifier = Modifier,
    saveExport: (suspend (String) -> String)? = null,
    /** Picks a previously exported JSON file and returns its text. */
    loadImport: (suspend () -> String?)? = null,
    uiScale: Float? = null,
    onUiScale: ((Float) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    var syncUrl by remember { mutableStateOf("") }
    var syncToken by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var proteinPct by remember { mutableStateOf("30") }
    var fatPct by remember { mutableStateOf("30") }
    var aiEndpoint by remember { mutableStateOf("") }
    var aiModel by remember { mutableStateOf("") }
    var aiKey by remember { mutableStateOf("") }
    var offUser by remember { mutableStateOf("") }
    var offPass by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var restSeconds by remember { mutableStateOf("90") }
    var autoStartRest by remember { mutableStateOf(true) }
    var showRpe by remember { mutableStateOf(false) }
    val engine = remember { SyncEngine(db, httpClient()) }

    LaunchedEffect(Unit) {
        syncUrl = db.settingDao().get("syncUrl") ?: ""
        syncToken = db.settingDao().get("syncToken") ?: ""
        goal = db.settingDao().get("goalKgPerWeek")?.toDoubleOrNull()
            ?.let { dev.dwm.liftlog.domain.kgToLbStr(it) } ?: "0"
        proteinPct = db.settingDao().get("proteinPct") ?: "30"
        fatPct = db.settingDao().get("fatPct") ?: "30"
        aiEndpoint = db.settingDao().get("aiEndpoint") ?: ""
        aiModel = db.settingDao().get("aiModel") ?: ""
        aiKey = db.settingDao().get("aiApiKey") ?: ""
        offUser = db.settingDao().get("offUser") ?: ""
        offPass = db.settingDao().get("offPassword") ?: ""
        restSeconds = db.settingDao().get("restSeconds") ?: "90"
        autoStartRest = db.settingDao().get("autoStartRest") != "false"
        showRpe = db.settingDao().get("showRpe") == "true"
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uiScale != null && onUiScale != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("UI Scale", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${(uiScale * 100).toInt()}% — also Ctrl+= / Ctrl+− / Ctrl+0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = uiScale,
                        onValueChange = onUiScale,
                        valueRange = 0.75f..3f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Workout", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = restSeconds,
                    onValueChange = { restSeconds = it },
                    label = { Text("Default rest (seconds)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = autoStartRest, onCheckedChange = { autoStartRest = it })
                    Text("  Auto-start rest timer after each set")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = showRpe, onCheckedChange = { showRpe = it })
                    Text("  Show RPE column")
                }
                Button(onClick = {
                    scope.launch {
                        db.settingDao().put(Setting("restSeconds", restSeconds.trim().toIntOrNull()?.toString() ?: "90"))
                        db.settingDao().put(Setting("autoStartRest", "$autoStartRest"))
                        db.settingDao().put(Setting("showRpe", "$showRpe"))
                        status = "Workout settings saved"
                    }
                }) { Text("Save Workout Settings") }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Goal", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    label = { Text("Weight change lb/week (- cut, + bulk)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = {
                    scope.launch {
                        val kg = goal.trim().toDoubleOrNull()?.let { it * dev.dwm.liftlog.domain.KG_PER_LB } ?: 0.0
                        db.settingDao().put(Setting("goalKgPerWeek", kg.toString()))
                        status = "Goal saved"
                    }
                }) { Text("Save Goal") }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Macro targets (% of calories)", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(proteinPct, { proteinPct = it }, label = { Text("Protein %") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(fatPct, { fatPct = it }, label = { Text("Fat %") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text(
                    "Carbs = remainder (${(100 - (proteinPct.toIntOrNull() ?: 30) - (fatPct.toIntOrNull() ?: 30)).coerceAtLeast(0)}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = {
                    scope.launch {
                        db.settingDao().put(Setting("proteinPct", proteinPct.trim()))
                        db.settingDao().put(Setting("fatPct", fatPct.trim()))
                        status = "Macro targets saved"
                    }
                }) { Text("Save Macro Targets") }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cloud Sync", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = syncUrl,
                    onValueChange = { syncUrl = it },
                    label = { Text("Netlify site URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = syncToken,
                    onValueChange = { syncToken = it },
                    label = { Text("Sync token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = {
                    scope.launch {
                        db.settingDao().put(Setting("syncUrl", syncUrl.trim()))
                        db.settingDao().put(Setting("syncToken", syncToken.trim()))
                        status = "Syncing…"
                        status = runCatching {
                            val r = engine.sync(syncUrl.trim(), syncToken.trim())
                            "Synced: pushed ${r.pushed}, pulled ${r.pulled}"
                        }.getOrElse { "Sync failed: ${it.message}" }
                    }
                }, enabled = syncUrl.isNotBlank() && syncToken.isNotBlank()) { Text("Sync Now") }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("AI (photo + text food logging)", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Paste an OpenRouter API key and everything works — endpoint/model below are optional overrides (e.g. local Ollama).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(aiKey, { aiKey = it }, label = { Text("API key") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(aiEndpoint, { aiEndpoint = it }, label = { Text("Endpoint override (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // one-tap model presets; free-text override below still wins
                    listOf(
                        "Gemini 2.5 Flash" to "google/gemini-2.5-flash",
                        "Claude Haiku 4.5" to "anthropic/claude-haiku-4.5",
                        "GPT-4o" to "openai/gpt-4o",
                    ).forEach { (label, id) ->
                        androidx.compose.material3.FilterChip(
                            selected = aiModel.trim() == id || (aiModel.isBlank() && id == dev.dwm.liftlog.data.DEFAULT_AI_MODEL),
                            onClick = {
                                aiModel = id
                                scope.launch { db.settingDao().put(Setting("aiModel", id)) }
                            },
                            label = { Text(label) },
                        )
                    }
                }
                OutlinedTextField(aiModel, { aiModel = it }, label = { Text("Model override (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    scope.launch {
                        db.settingDao().put(Setting("aiEndpoint", aiEndpoint.trim()))
                        db.settingDao().put(Setting("aiModel", aiModel.trim()))
                        db.settingDao().put(Setting("aiApiKey", aiKey.trim()))
                        status = "AI settings saved"
                    }
                }) { Text("Save AI Settings") }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Open Food Facts account (optional, to contribute)", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(offUser, { offUser = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(offPass, { offPass = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    scope.launch {
                        db.settingDao().put(Setting("offUser", offUser.trim()))
                        db.settingDao().put(Setting("offPassword", offPass))
                        status = "OFF account saved"
                    }
                }) { Text("Save OFF Account") }
            }
        }
        if (saveExport != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Backup", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(onClick = {
                        scope.launch {
                            status = runCatching { "Exported to ${saveExport(exportJson(db))}" }
                                .getOrElse { "Export failed: ${it.message}" }
                        }
                    }) { Text("Export all data (JSON)") }
                    Text(
                        "The export contains your AI key and sync token — keep the file private.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (loadImport != null) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                status = runCatching {
                                    val text = loadImport() ?: return@runCatching "Import cancelled"
                                    val n = dev.dwm.liftlog.data.SyncEngine(db, dev.dwm.liftlog.data.httpClient())
                                        .importAll(text)
                                    "Imported $n rows — restart the app to see everything"
                                }.getOrElse { "Import failed: ${it.message}" }
                            }
                        }) { Text("Restore from backup (JSON)") }
                    }
                }
            }
        }
        TextButton(onClick = {
            scope.launch {
                db.settingDao().put(Setting("onboarded", "0"))
                status = "Setup wizard will run on next app start"
            }
        }) { Text("Run setup wizard again") }
        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodyMedium)
    }
}

private val json = Json { prettyPrint = false }

private suspend fun exportJson(db: AppDatabase): String {
    val s = db.syncDao()
    fun <T> enc(serializer: kotlinx.serialization.KSerializer<T>, rows: List<T>): JsonElement =
        json.encodeToJsonElement(kotlinx.serialization.builtins.ListSerializer(serializer), rows)
    val obj = buildJsonObject {
        put("exportedAt", dev.dwm.liftlog.data.db.nowMillis())
        put("Exercise", enc(dev.dwm.liftlog.data.db.Exercise.serializer(), s.exercisesSince(0)))
        put("Workout", enc(dev.dwm.liftlog.data.db.Workout.serializer(), s.workoutsSince(0)))
        put("WorkoutSet", enc(dev.dwm.liftlog.data.db.WorkoutSet.serializer(), s.setsSince(0)))
        put("Program", enc(dev.dwm.liftlog.data.db.Program.serializer(), s.programsSince(0)))
        put("ProgramDay", enc(dev.dwm.liftlog.data.db.ProgramDay.serializer(), s.programDaysSince(0)))
        put("ProgramExercise", enc(dev.dwm.liftlog.data.db.ProgramExercise.serializer(), s.programExercisesSince(0)))
        put("Food", enc(dev.dwm.liftlog.data.db.Food.serializer(), s.foodsSince(0)))
        put("FoodLog", enc(dev.dwm.liftlog.data.db.FoodLog.serializer(), s.foodLogsSince(0)))
        put("WeightEntry", enc(dev.dwm.liftlog.data.db.WeightEntry.serializer(), s.weightsSince(0)))
        put("GroceryItem", enc(dev.dwm.liftlog.data.db.GroceryItem.serializer(), s.groceriesSince(0)))
        put("Routine", enc(dev.dwm.liftlog.data.db.Routine.serializer(), s.routinesSince(0)))
        put("RoutineExercise", enc(dev.dwm.liftlog.data.db.RoutineExercise.serializer(), s.routineExercisesSince(0)))
        // includes the AI key and sync token — this file is a full credential-bearing backup
        put("Setting", enc(dev.dwm.liftlog.data.db.Setting.serializer(), s.settingsSince(0)))
    }
    return json.encodeToString(JsonElement.serializer(), obj)
}
