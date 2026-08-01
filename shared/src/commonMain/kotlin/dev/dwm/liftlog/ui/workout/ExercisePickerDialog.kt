package dev.dwm.liftlog.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.dwm.liftlog.data.db.AppDatabase
import dev.dwm.liftlog.data.db.Exercise
import dev.dwm.liftlog.domain.Kit
import dev.dwm.liftlog.domain.fitsKit
import dev.dwm.liftlog.ui.collectAsStateList
import dev.dwm.liftlog.ui.demoVideoUrl
import dev.dwm.liftlog.ui.openUrl
import dev.dwm.liftlog.ui.components.FullScreenDialog

@Composable
fun ExercisePickerDialog(
    db: AppDatabase,
    onDismiss: () -> Unit,
    onPick: (Exercise) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var equipFilter by remember { mutableStateOf<String?>(null) }
    var myKit by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { myKit = db.settingDao().get("equipmentKit") == "home" }
    val results by remember(query) { db.exerciseDao().search(query) }.collectAsStateList()
    // "Bodyweight" seed data uses two spellings; match either
    val byEquip = when (equipFilter) {
        null -> results
        "body" -> results.filter { "body" in it.equipment.lowercase() || "none" in it.equipment.lowercase() }
        else -> results.filter { equipFilter!! in it.equipment.lowercase() }
    }
    val filtered = if (myKit) byEquip.filter { fitsKit(it, Kit.HOME) } else byEquip

    FullScreenDialog("Add Exercise", onDismiss) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search exercises") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(
                    "All" to null, "Barbell" to "barbell", "Dumbbell" to "dumbbell",
                    "Kettlebell" to "kettlebell", "Bodyweight" to "body", "TRX" to "trx",
                    "Machine" to "machine", "Cable" to "cable",
                ).forEach { (label, key) ->
                    FilterChip(
                        selected = equipFilter == key,
                        onClick = { equipFilter = key },
                        label = { Text(label) },
                    )
                }
                // "My kit" = what a dumbbell/kettlebell home gym can actually do
                FilterChip(selected = myKit, onClick = { myKit = !myKit }, label = { Text("My kit") })
            }
            LazyColumn(Modifier.weight(1f)) {
                items(filtered, key = { it.id }) { exercise ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(exercise) }.padding(vertical = 8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        dev.dwm.liftlog.ui.components.ExerciseImage(exercise.name, Modifier.size(48.dp))
                        Column(Modifier.weight(1f)) {
                            Text(exercise.name)
                            Text(
                                listOf(exercise.category, exercise.muscles)
                                    .filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { openUrl(demoVideoUrl(exercise.name)) }) { Text("▶ Demo") }
                    }
                }
            }
        }
    }
}
