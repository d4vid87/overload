package dev.dwm.liftlog.ui.workout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.dwm.liftlog.data.db.nowMillis
import dev.dwm.liftlog.ui.notifyRest

/** App-wide "workout in progress" flag — drives keep-screen-awake. */
object WorkoutSession {
    var active by mutableStateOf(false)
}

/** App-wide rest timer — lives outside any screen so it survives tab switches. */
object RestTimer {
    var endsAt by mutableStateOf<Long?>(null)
        private set
    var durationMs by mutableStateOf(90_000L)
        private set
    var over by mutableStateOf(false)
        private set

    fun start(seconds: Int) {
        durationMs = seconds.coerceAtLeast(1) * 1000L
        endsAt = nowMillis() + durationMs
        over = false
        notifyRest(endsAt)
    }

    fun add(seconds: Int) {
        val e = endsAt ?: return
        val newEnd = (e + seconds * 1000L).coerceAtLeast(nowMillis())
        durationMs = (durationMs + seconds * 1000L).coerceAtLeast(1000L)
        endsAt = newEnd
        over = false
        notifyRest(newEnd)
    }

    fun expire() {
        over = true
        notifyRest(null)
    }

    fun clear() {
        endsAt = null
        over = false
        notifyRest(null)
    }

    /**
     * Restore a timer that was running when the process died. Android kills this app in the
     * background (seen as LOW_MEMORY in exit-info), which used to silently drop the rest timer and
     * strand its ongoing notification. `endsAt` is an absolute wall-clock time, so a stale value is
     * simply already expired.
     */
    fun restore(savedEndsAt: Long, savedDurationMs: Long) {
        if (endsAt != null) return // a live timer always wins over a persisted one
        if (savedEndsAt <= nowMillis()) {
            notifyRest(null) // already elapsed while we were dead — just clear the stale notification
            return
        }
        durationMs = savedDurationMs.coerceAtLeast(1000L)
        endsAt = savedEndsAt
        over = false
        notifyRest(savedEndsAt)
    }
}
