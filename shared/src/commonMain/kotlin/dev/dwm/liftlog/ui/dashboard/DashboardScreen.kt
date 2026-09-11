package dev.dwm.liftlog.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.dwm.liftlog.data.db.AppDatabase
import dev.dwm.liftlog.data.db.WeightEntry
import dev.dwm.liftlog.data.db.nowMillis
import dev.dwm.liftlog.domain.e1rm
import kotlinx.coroutines.flow.first
import dev.dwm.liftlog.domain.DayIntake
import dev.dwm.liftlog.domain.DayWeight
import dev.dwm.liftlog.domain.TdeeResult
import dev.dwm.liftlog.domain.computeTdee
import dev.dwm.liftlog.domain.kgToLbDisplay
import dev.dwm.liftlog.ui.Palette
import dev.dwm.liftlog.ui.Tab
import dev.dwm.liftlog.ui.components.TrainingHero
import dev.dwm.liftlog.ui.components.MacroTile
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import dev.dwm.liftlog.ui.nutrition.todayEpochDay
import dev.dwm.liftlog.ui.workout.clean
import dev.dwm.liftlog.ui.collectAsStateList

/** MFP-style dashboard: calories ring, macros, weight trend, quick "+" actions. */
@Composable
fun DashboardScreen(
    db: AppDatabase,
    modifier: Modifier = Modifier,
    onStartWorkout: (() -> Unit)? = null,
    onGoTo: (Tab) -> Unit,
) {
    val today = remember { todayEpochDay() }
    val logs by remember { db.foodLogDao().forDay(today) }.collectAsStateList()
    var kcal by remember { mutableStateOf(0.0) }
    var protein by remember { mutableStateOf(0.0) }
    var carbs by remember { mutableStateOf(0.0) }
    var fat by remember { mutableStateOf(0.0) }
    var tdee by remember { mutableStateOf<TdeeResult?>(null) }
    var targetKcal by remember { mutableStateOf(2000.0) }
    var proteinPct by remember { mutableStateOf(30.0) }
    var fatPct by remember { mutableStateOf(30.0) }
    var weights by remember { mutableStateOf<List<WeightEntry>>(emptyList()) }
    var intakes by remember { mutableStateOf<List<DayIntake>>(emptyList()) }
    var showRecovery by remember { mutableStateOf(false) }
    if (showRecovery) dev.dwm.liftlog.ui.workout.RecoveryScreen(db) { showRecovery = false }
    var workoutDays by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var streakWeeks by remember { mutableStateOf(0) }
    var insights by remember { mutableStateOf<List<String>>(emptyList()) }
    var recap by remember { mutableStateOf<WeekRecap?>(null) }
    var nextWorkoutName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(logs) {
        var k = 0.0; var p = 0.0; var c = 0.0; var f = 0.0
        for (log in logs) {
            val food = db.foodDao().byId(log.foodId) ?: continue
            k += log.grams * food.kcal / 100
            p += log.grams * food.protein / 100
            c += log.grams * food.carbs / 100
            f += log.grams * food.fat / 100
        }
        kcal = k; protein = p; carbs = c; fat = f

        val goal = db.settingDao().get("goalKgPerWeek")?.toDoubleOrNull() ?: 0.0
        val allWeights = db.weightDao().all()
        weights = allWeights
        intakes = db.foodLogDao().dailyKcals(today - 365).map { DayIntake(it.epochDay, it.kcal) }
        tdee = computeTdee(
            allWeights.map { DayWeight(it.epochDay, it.kg) },
            intakes.filter { it.epochDay >= today - 35 },
            goal,
        )
        targetKcal = tdee?.targetKcal ?: db.settingDao().get("targetKcal")?.toDoubleOrNull() ?: 2000.0
        proteinPct = db.settingDao().get("proteinPct")?.toDoubleOrNull() ?: 30.0
        fatPct = db.settingDao().get("fatPct")?.toDoubleOrNull() ?: 30.0
        // cache for the home-screen widget
        db.settingDao().put(dev.dwm.liftlog.data.db.Setting("lastTargetKcal", "$targetKcal"))

        // streak + week dots + recap + insights (all pure reads)
        val workouts = db.workoutDao().history().first()
        val dayMs = 86_400_000L
        fun workoutDay(startedAt: Long) = Instant.fromEpochMilliseconds(startedAt)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date.toEpochDays().toLong()
        workoutDays = workouts.map { workoutDay(it.startedAt) }.toSet()
        // consecutive weeks (ending this week) with ≥1 workout; weeks start Monday
        val thisWeek = (today + 3) / 7 // epochDay 0 = Thursday; +3 aligns Monday week boundaries
        val weeksTrained = workoutDays.map { (it + 3) / 7 }.toSet()
        var wk = thisWeek
        var streak = 0
        while (wk in weeksTrained) { streak++; wk-- }
        streakWeeks = streak

        // last calendar week recap, shown Mon-Tue
        val dow = ((today + 3) % 7).toInt() // 0 = Monday
        if (dow <= 1) {
            val lastWeekStart = today - dow - 7
            val lastWeek = workouts.filter { workoutDay(it.startedAt) in lastWeekStart until lastWeekStart + 7 }
            if (lastWeek.isNotEmpty() || intakes.any { it.epochDay in lastWeekStart until lastWeekStart + 7 }) {
                var volume = 0.0
                for (w in lastWeek) {
                    volume += db.workoutDao().setsForWorkoutOnce(w.id)
                        .filter { it.completed }.sumOf { it.weightKg * it.reps }
                }
                val weekKcals = intakes.filter { it.epochDay in lastWeekStart until lastWeekStart + 7 }
                val adherence = if (weekKcals.isEmpty() || targetKcal <= 0) null
                else (weekKcals.map { it.kcal }.average() / targetKcal * 100).toInt()
                recap = WeekRecap(lastWeek.size, volume, adherence)
            }
        }

        // insights: top-3 exercise e1RM deltas over 30 days + protein 7d avg + weight 7d delta
        val list = mutableListOf<String>()
        val since = nowMillis() - 30 * dayMs
        val recentSets = db.syncDao().setsSince(since).filter { it.completed && it.deletedAt == null && it.weightKg > 0 }
        recentSets.groupBy { it.exerciseId }
            .entries.sortedByDescending { it.value.size }.take(3)
            .forEach { (exId, exSets) ->
                val name = db.exerciseDao().byId(exId)?.name ?: return@forEach
                val bestNow = exSets.maxOf { e1rm(it.weightKg, it.reps) }
                val bestBefore = db.workoutDao().bestE1rmBefore(exId, since) ?: return@forEach
                val delta = (bestNow - bestBefore).kgToLbDisplay()
                if (delta > 0.4) list.add("$name e1RM up ${delta.clean()} lb this month")
                else if (delta < -0.4) list.add("$name e1RM down ${(-delta).clean()} lb this month")
            }
        val protein7 = db.foodLogDao().dailyMacros(today - 6).filter { it.protein > 0 }
        if (protein7.isNotEmpty()) {
            val avg = protein7.map { it.protein }.average().toInt()
            val target = (targetKcal * proteinPct / 100 / 4).toInt()
            list.add("7-day protein average ${avg}g vs ${target}g target")
        }
        val cardio7 = db.cardioDao().minutesSince(nowMillis() - 7 * dayMs)
        if (cardio7 > 0) list.add("Cardio $cardio7 min over 7 days")
        val w7 = weights.filter { it.epochDay >= today - 7 }
        if (w7.size >= 2) {
            val d = (w7.last().kg - w7.first().kg).kgToLbDisplay()
            list.add("Weight ${if (d >= 0) "+" else ""}${d.clean()} lb over 7 days")
        }
        insights = list

        // next workout for the START CTA (hidden once trained today)
        nextWorkoutName = if ((today) in workoutDays) null else run {
            val program = db.programDao().programs().first().firstOrNull()
            if (program != null) {
                val days = db.programDao().daysFor(program.id)
                days.getOrNull(program.currentDayIndex % days.size.coerceAtLeast(1))?.name ?: "Workout"
            } else {
                db.routineDao().routines().first().firstOrNull()?.name
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("OVERLOAD", style = MaterialTheme.typography.headlineMedium, letterSpacing = 1.sp)
                    val date = kotlinx.datetime.LocalDate.fromEpochDays(today.toInt())
                    Text(
                        "${date.dayOfWeek.name.take(3)} / ${date.month.name.take(3)} ${date.dayOfMonth}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
            }
            if (onStartWorkout != null) {
                TrainingHero(
                    title = "Build\nmomentum.",
                    subtitle = nextWorkoutName?.let { "Up next · $it" } ?: "One session stronger. Keep showing up.",
                    eyebrow = if (today in workoutDays) "Today's work. Done." else "Your next rep starts here",
                    action = if (nextWorkoutName != null) "Start session" else "Explore your training",
                    onClick = { if (nextWorkoutName != null) onStartWorkout() else onGoTo(Tab.Workout) },
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("THE LAST 7", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                    Text(if (streakWeeks > 0) "$streakWeeks week streak ↗" else "Every session counts", style = MaterialTheme.typography.labelSmall, color = Palette.Success)
                }
                WeekDots(workoutDays, intakes.map { it.epochDay }.toSet(), today)
            }
            recap?.let { WeeklyRecapCard(it) }
            Card(Modifier.fillMaxWidth()) {
              Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("DAILY FUEL", style = MaterialTheme.typography.headlineSmall)
                    TextButton(onClick = { onGoTo(Tab.Nutrition) }) { Text("Log food ↗") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${kotlin.math.abs((targetKcal - kcal).toInt())}", style = MaterialTheme.typography.displayMedium)
                            Text(if (kcal > targetKcal) "kcal over" else "kcal left", Modifier.padding(bottom = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${kcal.toInt()} eaten / ${targetKcal.toInt()} target", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val carbsPct = (100.0 - proteinPct - fatPct).coerceAtLeast(0.0)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MacroTile("Protein", protein, targetKcal * proteinPct / 100 / 4, Palette.Protein, Modifier.weight(1f))
                    MacroTile("Carbs", carbs, targetKcal * carbsPct / 100 / 4, Palette.Carbs, Modifier.weight(1f))
                    MacroTile("Fat", fat, targetKcal * fatPct / 100 / 9, Palette.Fat, Modifier.weight(1f))
                }
              }
            }
            if (insights.isNotEmpty()) InsightsCard(insights)
            WeightTrendCard(weights, today)
            ExpenditureCard(intakes, tdee, today)
            dev.dwm.liftlog.ui.workout.RecoveryCard(db, onOpen = { showRecovery = true })
        }
    }
}

data class WeekRecap(val workouts: Int, val volumeKg: Double, val adherencePct: Int?)

@Composable
private fun WeeklyRecapCard(r: WeekRecap) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Your Week", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Palette.Boost)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                RecapStat("${r.workouts}", "workouts")
                RecapStat("${(r.volumeKg * 2.20462).toInt()}", "lb volume")
                RecapStat(r.adherencePct?.let { "$it%" } ?: "—", "kcal adherence")
            }
        }
    }
}

@Composable
private fun RecapStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Calendar strip: today is highlighted; the marker records training or food logging. */
@Composable
private fun WeekDots(workoutDays: Set<Long>, loggedDays: Set<Long>, today: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ((today - 6)..today).forEach { day ->
            val trained = day in workoutDays
            val logged = day in loggedDays
            val current = day == today
            val ink = if (current) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            Column(
                Modifier.weight(1f).background(if (current) Palette.Success else MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)).padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    dashDayLetter(day),
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.copy(alpha = 0.65f),
                )
                Text("${kotlinx.datetime.LocalDate.fromEpochDays(day.toInt()).dayOfMonth}", style = MaterialTheme.typography.titleMedium, color = ink)
                Box(Modifier.size(4.dp).background(if (trained) ink else if (logged) Palette.Volt else Color.Transparent, androidx.compose.foundation.shape.CircleShape))
            }
        }
    }
}

private fun dashDayLetter(epochDay: Long): String =
    when ((epochDay + 3).mod(7L)) {
        0L -> "M"; 1L -> "T"; 2L -> "W"; 3L -> "T"; 4L -> "F"; 5L -> "S"; else -> "S"
    }

@Composable
private fun InsightsCard(insights: List<String>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            insights.forEach {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(6.dp).background(Palette.Volt, androidx.compose.foundation.shape.CircleShape))
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// range chips shared by the MacroFactor-style trend cards
private val ranges = listOf("1W" to 7L, "1M" to 30L, "3M" to 91L, "6M" to 182L, "1Y" to 365L, "All" to 100_000L)

@Composable
private fun RangeChips(selected: Long, onSelect: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ranges.forEach { (label, days) ->
            val on = days == selected
            Box(
                Modifier.weight(1f)
                    .background(
                        if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(16.dp),
                    )
                    .clickable { onSelect(days) }
                    .heightIn(min = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (on) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AvgDiffHeader(title: String, avg: String, diff: String, unit: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        Column {
            Text("Average", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(avg, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(unit, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column {
            Text("Difference", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(diff, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(unit, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WeightTrendCard(all: List<WeightEntry>, today: Long) {
    var range by remember { mutableStateOf(91L) }
    val entries = all.filter { it.epochDay >= today - range }
    // MacroFactor: faint scale-weight line + bold purple EMA trend
    val trend = remember(entries) {
        var ema = entries.firstOrNull()?.kg ?: 0.0
        entries.map { e -> ema += 0.25 * (e.kg - ema); DayWeight(e.epochDay, ema) }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AvgDiffHeader(
                "Weight Trend",
                avg = if (entries.isEmpty()) "—" else entries.map { it.kg }.average().kgToLbDisplay().clean(),
                diff = trend.let {
                    if (it.size < 2) "—" else {
                        val d = (it.last().kg - it.first().kg).kgToLbDisplay()
                        "${if (d > 0) "+" else ""}${d.clean()}"
                    }
                },
                unit = "lbs",
            )
            if (entries.size >= 2) {
                TrendChart(
                    Modifier.fillMaxWidth().height(140.dp),
                    series = listOf(
                        Series(entries.map { it.epochDay to it.kg.kgToLbDisplay() }, Palette.Trend.copy(alpha = 0.45f), dots = true),
                        Series(trend.map { it.epochDay to it.kg.kgToLbDisplay() }, Palette.Trend),
                    ),
                    unit = "lb",
                )
            } else {
                Text(
                    "Log weight on the Food tab to see your trend.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RangeChips(range) { range = it }
        }
    }
}

@Composable
private fun ExpenditureCard(all: List<DayIntake>, tdee: TdeeResult?, today: Long) {
    var range by remember { mutableStateOf(30L) }
    val entries = all.filter { it.epochDay >= today - range && it.kcal > 0 }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AvgDiffHeader(
                "Expenditure",
                avg = tdee?.tdeeKcal?.toInt()?.toString() ?: "—",
                diff = if (tdee == null || entries.isEmpty()) "—" else {
                    val d = (entries.map { it.kcal }.average() - tdee.tdeeKcal).toInt()
                    "${if (d > 0) "+" else ""}$d"
                },
                unit = "kcal",
            )
            if (entries.size >= 2) {
                TrendChart(
                    Modifier.fillMaxWidth().height(140.dp),
                    series = listOfNotNull(
                        Series(entries.map { it.epochDay to it.kcal }, Palette.Protein.copy(alpha = 0.6f), dots = true),
                        tdee?.let {
                            Series(
                                listOf(entries.first().epochDay to it.tdeeKcal, entries.last().epochDay to it.tdeeKcal),
                                Palette.Protein,
                            )
                        },
                    ),
                    unit = "kcal",
                )
            } else {
                Text(
                    "Log food for a few days to see expenditure vs intake.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RangeChips(range) { range = it }
        }
    }
}

private data class Series(val points: List<Pair<Long, Double>>, val color: Color, val dots: Boolean = false)

/** Bezier-smoothed lines, gradient fill under the last series, drag to scrub a value tooltip. */
@Composable
private fun TrendChart(modifier: Modifier, series: List<Series>, unit: String = "") {
    val pts = series.flatMap { it.points }
    if (pts.isEmpty()) return
    val minD = pts.minOf { it.first }
    val maxD = pts.maxOf { it.first }
    val minV = pts.minOf { it.second }
    val maxV = pts.maxOf { it.second }
    var scrub by remember(series) { mutableStateOf<Pair<Long, Double>?>(null) }

    Column {
        Text(
            scrub?.let { (d, v) ->
                "${kotlinx.datetime.LocalDate.fromEpochDays(d.toInt())}: ${v.clean()} $unit"
            } ?: " ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Canvas(
            modifier.pointerInput(series) {
                detectHorizontalDragGestures(
                    onDragEnd = { scrub = null },
                    onDragCancel = { scrub = null },
                ) { change, _ ->
                    val spanD = (maxD - minD).coerceAtLeast(1)
                    val day = minD + ((change.position.x / size.width) * spanD).toLong()
                    val primary = series.last().points
                    scrub = primary.minByOrNull { kotlin.math.abs(it.first - day) }
                }
            },
        ) {
            val spanD = (maxD - minD).coerceAtLeast(1)
            val spanV = (maxV - minV).coerceAtLeast(0.5)
            fun toOffset(p: Pair<Long, Double>) = Offset(
                x = (p.first - minD) / spanD.toFloat() * size.width,
                y = size.height - ((p.second - minV) / spanV).toFloat() * size.height * 0.85f - size.height * 0.075f,
            )
            series.forEachIndexed { idx, s ->
                val offsets = s.points.map(::toOffset)
                if (offsets.size < 2) return@forEachIndexed
                // midpoint quadratic smoothing
                val path = Path().apply {
                    moveTo(offsets.first().x, offsets.first().y)
                    for (i in 1 until offsets.size) {
                        val prev = offsets[i - 1]
                        val cur = offsets[i]
                        val mid = Offset((prev.x + cur.x) / 2, (prev.y + cur.y) / 2)
                        quadraticTo(prev.x, prev.y, mid.x, mid.y)
                    }
                    lineTo(offsets.last().x, offsets.last().y)
                }
                if (idx == series.lastIndex) {
                    val fill = Path().apply {
                        addPath(path)
                        lineTo(offsets.last().x, size.height)
                        lineTo(offsets.first().x, size.height)
                        close()
                    }
                    drawPath(
                        fill,
                        Brush.verticalGradient(listOf(s.color.copy(alpha = 0.25f), Color.Transparent)),
                    )
                }
                drawPath(path, s.color, style = Stroke(width = 4f, cap = StrokeCap.Round))
                if (s.dots) offsets.forEach { drawCircle(s.color, radius = 5f, center = it) }
            }
            scrub?.let { p ->
                val o = toOffset(p)
                drawLine(Color.White.copy(alpha = 0.4f), Offset(o.x, 0f), Offset(o.x, size.height), strokeWidth = 2f)
                drawCircle(Color.White, radius = 7f, center = o)
            }
        }
    }
}
