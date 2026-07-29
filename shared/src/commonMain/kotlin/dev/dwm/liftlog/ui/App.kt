package dev.dwm.liftlog.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.dwm.liftlog.data.OpenFoodFacts
import dev.dwm.liftlog.data.db.AppDatabase
import dev.dwm.liftlog.data.db.nowMillis
import dev.dwm.liftlog.data.httpClient
import dev.dwm.liftlog.data.seed.seedExercisesIfEmpty
import dev.dwm.liftlog.ui.dashboard.DashboardScreen
import dev.dwm.liftlog.ui.history.HistoryScreen
import dev.dwm.liftlog.ui.nutrition.NutritionScreen
import dev.dwm.liftlog.ui.settings.SettingsScreen
import dev.dwm.liftlog.ui.workout.RestTimer
import dev.dwm.liftlog.ui.workout.WorkoutTab
import kotlinx.coroutines.delay

enum class Tab(val label: String, val icon: ImageVector) {
    Dashboard("Dashboard", Icons.Default.Dashboard),
    Workout("Workout", Icons.Default.FitnessCenter),
    Nutrition("Food", Icons.Default.Restaurant),
    History("History", Icons.Default.History),
    Settings("More", Icons.Default.Settings),
}

@Composable
fun App(
    db: AppDatabase,
    scanBarcode: (suspend () -> String?)? = null,
    saveExport: (suspend (String) -> String)? = null,
    takePhoto: (suspend () -> CapturedPhoto?)? = null,
    voiceInput: (suspend () -> String?)? = null,
    uiScale: Float? = null,
    onUiScale: ((Float) -> Unit)? = null,
) {
    val off = remember { OpenFoodFacts(httpClient()) }
    var tab by remember { mutableStateOf(Tab.Dashboard) }
    var workoutRefresh by remember { mutableIntStateOf(0) }
    var onboarded by remember { mutableStateOf<Boolean?>(null) }
    var pendingQuickStart by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        seedExercisesIfEmpty(db)
        onboarded = db.settingDao().get("onboarded") == "1"
        // a rest timer running when Android killed the app is resumed here, not lost
        val savedEnd = db.settingDao().get("restEndsAt")?.toLongOrNull()
        if (savedEnd != null) {
            RestTimer.restore(savedEnd, db.settingDao().get("restDurationMs")?.toLongOrNull() ?: 90_000L)
        }
        dev.dwm.liftlog.data.autoSync(db) // pull other devices' changes on open
    }
    LaunchedEffect(RestTimer.endsAt) {
        val e = RestTimer.endsAt
        db.settingDao().put(dev.dwm.liftlog.data.db.Setting("restEndsAt", e?.toString() ?: ""))
        if (e != null) {
            db.settingDao().put(dev.dwm.liftlog.data.db.Setting("restDurationMs", RestTimer.durationMs.toString()))
        }
    }
    // never let the screen lock while a workout or rest timer is live
    LaunchedEffect(dev.dwm.liftlog.ui.workout.WorkoutSession.active, RestTimer.endsAt) {
        keepScreenAwake(dev.dwm.liftlog.ui.workout.WorkoutSession.active || RestTimer.endsAt != null)
    }

    // coil image loader wired to ktor for exercise thumbnails
    coil3.compose.setSingletonImageLoaderFactory { ctx ->
        coil3.ImageLoader.Builder(ctx)
            .components { add(coil3.network.ktor3.KtorNetworkFetcherFactory()) }
            .build()
    }

    LiftLogTheme {
        RestTimerEngine()
        if (onboarded == false) {
            dev.dwm.liftlog.ui.onboarding.OnboardingWizard(db) { onboarded = true }
        }
        val bg = Brush.verticalGradient(
            listOf(Color(0xFF0D1524), MaterialTheme.colorScheme.background),
        )
        @Composable
        fun content(modifier: Modifier) {
            when (tab) {
                Tab.Dashboard -> DashboardScreen(
                    db, modifier,
                    onStartWorkout = {
                        pendingQuickStart = true
                        workoutRefresh++
                        tab = Tab.Workout
                    },
                ) {
                    if (it == Tab.Workout) workoutRefresh++
                    tab = it
                }
                Tab.Workout -> WorkoutTab(
                    db, modifier, refreshKey = workoutRefresh, voiceInput = voiceInput,
                    quickStart = pendingQuickStart,
                    onQuickStartConsumed = { pendingQuickStart = false },
                )
                Tab.Nutrition -> NutritionScreen(db, off, modifier, scanBarcode, takePhoto)
                Tab.History -> HistoryScreen(db, modifier)
                Tab.Settings -> SettingsScreen(db, modifier, saveExport, uiScale, onUiScale)
            }
        }

        BoxWithConstraints(Modifier.fillMaxSize().background(bg)) {
            if (maxWidth >= 840.dp) {
                // desktop / wide: side rail + centered column so cards don't stretch
                Row(Modifier.fillMaxSize()) {
                    NavigationRail(containerColor = Color.Transparent) {
                        Tab.entries.forEach { t ->
                            NavigationRailItem(
                                selected = tab == t,
                                onClick = {
                                    if (t == Tab.Workout) workoutRefresh++
                                    tab = t
                                },
                                icon = { Icon(t.icon, null) },
                                label = { Text(t.label) },
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                            content(Modifier.widthIn(max = 700.dp).fillMaxSize())
                        }
                        if (tab != Tab.Workout) GlobalRestBar()
                    }
                }
            } else {
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        Column {
                            if (tab != Tab.Workout) GlobalRestBar()
                            NavigationBar {
                                Tab.entries.forEach { t ->
                                    NavigationBarItem(
                                        selected = tab == t,
                                        onClick = {
                                            if (t == Tab.Workout) workoutRefresh++
                                            tab = t
                                        },
                                        icon = { Icon(t.icon, null) },
                                        label = { Text(t.label) },
                                    )
                                }
                            }
                        }
                    }
                ) { padding ->
                    content(Modifier.padding(padding))
                }
            }
        }
    }
}

/** Runs the countdown + expiry side effects exactly once app-wide; all timer UI is display-only. */
@Composable
private fun RestTimerEngine() {
    LaunchedEffect(RestTimer.endsAt) {
        val endsAt = RestTimer.endsAt ?: return@LaunchedEffect
        while (RestTimer.endsAt == endsAt && !RestTimer.over) {
            if (nowMillis() >= (RestTimer.endsAt ?: return@LaunchedEffect)) {
                RestTimer.expire()
                notifyRestOver()
                playAlarm()
                speak("Rest over. Go!")
                repeat(2) {
                    haptic(Haptic.Buzz)
                    delay(400)
                    playBeep()
                }
                // re-alert until the GO banner is tapped (max 3 extra)
                repeat(3) {
                    delay(5000)
                    if (!RestTimer.over) return@LaunchedEffect
                    playAlarm()
                    haptic(Haptic.Buzz)
                }
            }
            delay(200)
        }
    }
}

/** Slim rest countdown + GO banner, shown on non-workout tabs. Display-only. */
@Composable
fun GlobalRestBar() {
    val endsAt = RestTimer.endsAt ?: return
    var tick by remember { mutableLongStateOf(nowMillis()) }
    LaunchedEffect(endsAt) {
        while (true) {
            tick = nowMillis()
            delay(200)
        }
    }
    if (RestTimer.over) {
        val flash = rememberInfiniteTransition()
        val alpha by flash.animateFloat(
            0.55f, 1f,
            animationSpec = infiniteRepeatable(tween(300), RepeatMode.Reverse),
        )
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                .background(Palette.Success.copy(alpha = alpha), RoundedCornerShape(12.dp))
                .clickable { RestTimer.clear() }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("REST OVER — GO!  (tap)", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        return
    }
    val remaining = (endsAt - tick).coerceAtLeast(0)
    val secs = remaining / 1000
    val urgent = secs <= 10
    val barColor = if (urgent) Palette.Protein else Palette.Volt
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Rest ${secs / 60}:${(secs % 60).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = barColor,
            )
            Row {
                TextButton(onClick = { RestTimer.add(-15) }) { Text("−15") }
                TextButton(onClick = { RestTimer.add(15) }) { Text("+15") }
                TextButton(onClick = { RestTimer.clear() }) { Text("Skip") }
            }
        }
        LinearProgressIndicator(
            progress = { (remaining / RestTimer.durationMs.toFloat()).coerceIn(0f, 1f) },
            color = barColor,
            modifier = Modifier.fillMaxWidth().height(6.dp),
        )
    }
}
