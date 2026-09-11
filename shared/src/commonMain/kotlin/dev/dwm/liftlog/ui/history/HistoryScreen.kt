package dev.dwm.liftlog.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.dwm.liftlog.data.db.AppDatabase
import dev.dwm.liftlog.data.db.E1rmPoint
import dev.dwm.liftlog.data.db.Exercise
import dev.dwm.liftlog.data.db.Workout
import dev.dwm.liftlog.domain.e1rm
import dev.dwm.liftlog.domain.kgToLb
import dev.dwm.liftlog.domain.kgToLbDisplay
import dev.dwm.liftlog.ui.Palette
import dev.dwm.liftlog.ui.collectAsStateList
import dev.dwm.liftlog.ui.workout.clean
import dev.dwm.liftlog.ui.workout.formatDuration
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

@Composable
fun HistoryScreen(db: AppDatabase, modifier: Modifier = Modifier) {
    var tab by remember { mutableStateOf(0) }
    Column(modifier.fillMaxSize()) {
        dev.dwm.liftlog.ui.components.ScreenHeading("Progress", "Every session adds up.", Modifier.padding(20.dp))
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("History") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Calendar") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Progress") })
        }
        when (tab) {
            0 -> WorkoutList(db)
            1 -> WorkoutCalendar(db)
            2 -> ProgressCharts(db)
        }
    }
}

// ---------- History cards ----------

private data class ExerciseSummary(val name: String, val setCount: Int, val bestWeightKg: Double, val bestReps: Int)
private data class WorkoutStats(
    val volumeKg: Double,
    val prCount: Int,
    val exercises: List<ExerciseSummary>,
)

@Composable
private fun WorkoutList(db: AppDatabase) {
    val workouts by remember { db.workoutDao().history() }.collectAsStateList()
    val cardio by remember { db.cardioDao().all() }.collectAsStateList()
    // one timeline: lifting sessions and cardio, newest first
    val entries = remember(workouts, cardio) {
        (workouts.map { it.startedAt to (it as Any) } + cardio.map { it.startedAt to (it as Any) })
            .sortedByDescending { it.first }
    }
    LazyColumn(
        Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (entries.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.FitnessCenter, null, tint = Palette.Success)
                        Text("Your story starts here", style = MaterialTheme.typography.titleMedium)
                        Text("Complete a workout or log cardio in Train. Your sessions will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        items(
            entries,
            key = { (_, e) -> if (e is Workout) e.id else (e as dev.dwm.liftlog.data.db.Cardio).id },
        ) { (_, e) ->
            when (e) {
                is Workout -> WorkoutCard(db, e)
                is dev.dwm.liftlog.data.db.Cardio -> CardioCard(e)
            }
        }
    }
}

@Composable
private fun CardioCard(cardio: dev.dwm.liftlog.data.db.Cardio) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "🏃 ${cardio.minutes} min ${cardio.kind}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                formatDate(cardio.startedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WorkoutCard(db: AppDatabase, workout: Workout) {
    var stats by remember { mutableStateOf<WorkoutStats?>(null) }
    LaunchedEffect(workout.id) {
        val sets = db.workoutDao().setsForWorkoutOnce(workout.id).filter { it.completed }
        var prs = 0
        val summaries = sets.groupBy { it.exerciseId }.map { (id, exSets) ->
            val name = db.exerciseDao().byId(id)?.name ?: "?"
            val best = exSets.maxByOrNull { e1rm(it.weightKg, it.reps) }
            val before = db.workoutDao().bestE1rmBefore(id, workout.startedAt) ?: 0.0
            prs += exSets.count { it.weightKg > 0 && e1rm(it.weightKg, it.reps) > before }
            ExerciseSummary(name, exSets.size, best?.weightKg ?: 0.0, best?.reps ?: 0)
        }
        stats = WorkoutStats(sets.sumOf { it.weightKg * it.reps }, prs, summaries)
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(workout.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                formatDate(workout.startedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                workout.finishedAt?.let {
                    StatChip(Icons.Default.Timer, formatDuration(it - workout.startedAt))
                }
                stats?.let { s ->
                    StatChip(Icons.Default.FitnessCenter, "${s.volumeKg.kgToLb().toInt()} lb")
                    if (s.prCount > 0) StatChip(Icons.Default.EmojiEvents, "${s.prCount} PRs", Palette.Pr)
                }
            }
            stats?.takeIf { it.exercises.isNotEmpty() }?.let { s ->
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text("Exercise", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Best Set", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                s.exercises.forEach { ex ->
                    Row(Modifier.fillMaxWidth()) {
                        Text("${ex.setCount} × ${ex.name}", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text("${ex.bestWeightKg.kgToLbDisplay().clean()} lb × ${ex.bestReps}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color = Color.Unspecified) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, Modifier.size(16.dp), tint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else tint)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

// ---------- Calendar ----------

@Composable
private fun WorkoutCalendar(db: AppDatabase) {
    val workouts by remember { db.workoutDao().history() }.collectAsStateList()
    val tz = TimeZone.currentSystemDefault()
    val workoutDays = remember(workouts) {
        workouts.map { Instant.fromEpochMilliseconds(it.startedAt).toLocalDateTime(tz).date }.toSet()
    }
    val today = remember { kotlinx.datetime.Clock.System.now().toLocalDateTime(tz).date }
    val months = remember(today) {
        (0..5).map { LocalDate(today.year, today.monthNumber, 1).minus(it, DateTimeUnit.MONTH) }
    }
    LazyColumn(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(months) { firstOfMonth -> MonthGrid(firstOfMonth, workoutDays, today) }
    }
}

@Composable
private fun MonthGrid(firstOfMonth: LocalDate, workoutDays: Set<LocalDate>, today: LocalDate) {
    val daysInMonth = firstOfMonth.plusMonthLength()
    val leadingBlanks = firstOfMonth.dayOfWeek.ordinal // Monday = 0
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "${firstOfMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${firstOfMonth.year}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val cells = List(leadingBlanks) { null } + (1..daysInMonth).toList()
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(Modifier.weight(1f).height(40.dp), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            val date = LocalDate(firstOfMonth.year, firstOfMonth.monthNumber, day)
                            val trained = date in workoutDays
                            Box(
                                Modifier.size(32.dp).background(
                                    when {
                                        trained -> Palette.Success.copy(alpha = 0.3f)
                                        date == today -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> Color.Transparent
                                    },
                                    CircleShape,
                                ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$day",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (trained) Palette.Success else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (trained || date == today) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
                repeat(7 - week.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

private fun LocalDate.plusMonthLength(): Int {
    val nextMonth = if (monthNumber == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, monthNumber + 1, 1)
    return nextMonth.minus(1, DateTimeUnit.DAY).dayOfMonth
}

// ---------- Progress charts ----------

@Composable
private fun ProgressCharts(db: AppDatabase) {
    val exercises by remember { db.workoutDao().loggedExercises() }.collectAsStateList()
    var selected by remember { mutableStateOf<Exercise?>(null) }
    var points by remember { mutableStateOf<List<E1rmPoint>>(emptyList()) }

    LaunchedEffect(selected) {
        points = selected?.let { db.workoutDao().e1rmHistory(it.id) } ?: emptyList()
    }

    LazyColumn(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        selected?.let { exercise ->
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${exercise.name} — est. 1RM", style = MaterialTheme.typography.titleMedium)
                        if (points.size < 2) {
                            Text("Need 2+ logged workouts for a chart.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            LineChart(points, Modifier.fillMaxWidth().height(180.dp).padding(top = 8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(formatDate(points.first().time), style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "best ${points.maxOf { it.e1rm }.kgToLbDisplay().clean()} lb",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(formatDate(points.last().time), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        items(exercises, key = { it.id }) { exercise ->
            Text(
                exercise.name,
                Modifier.fillMaxWidth().clickable { selected = exercise }.padding(vertical = 10.dp),
                color = if (exercise.id == selected?.id) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun LineChart(points: List<E1rmPoint>, modifier: Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val minT = points.first().time
        val maxT = points.last().time
        val minV = points.minOf { it.e1rm }
        val maxV = points.maxOf { it.e1rm }
        val spanT = (maxT - minT).coerceAtLeast(1)
        val spanV = (maxV - minV).coerceAtLeast(1.0)
        val offsets = points.map {
            Offset(
                x = (it.time - minT) / spanT.toFloat() * size.width,
                y = size.height - ((it.e1rm - minV) / spanV).toFloat() * size.height * 0.9f - size.height * 0.05f,
            )
        }
        offsets.zipWithNext { a, b ->
            drawLine(color, a, b, strokeWidth = 4f, cap = StrokeCap.Round)
        }
        offsets.forEach { drawCircle(color, radius = 7f, center = it) }
        drawCircle(Color.White, radius = 3f, center = offsets.last())
    }
}

private fun formatDate(epochMillis: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.date} ${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
}
