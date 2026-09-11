package dev.dwm.liftlog.ui.workout

import dev.dwm.liftlog.data.db.nowMillis
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class RestTimerTest {
    @AfterTest fun clearTimer() = RestTimer.clear()

    @Test fun restoresDeadlineWithoutReplacingLiveTimer() {
        val deadline = nowMillis() + 90_000
        RestTimer.restore(deadline, 90_000)
        assertEquals(deadline, RestTimer.endsAt)
        RestTimer.restore(deadline + 10_000, 100_000)
        assertEquals(deadline, RestTimer.endsAt)
    }

    @Test fun ignoresExpiredDeadlineAndClampsInvalidDuration() {
        RestTimer.restore(nowMillis() - 1000, 0)
        assertNull(RestTimer.endsAt)
        RestTimer.start(-10)
        assertEquals(1000L, RestTimer.durationMs)
    }

    @Test fun extendingExpiredTimerRestartsCountdown() {
        RestTimer.start(90)
        RestTimer.expire()
        RestTimer.add(15)
        assertFalse(RestTimer.over)
        assertEquals(105_000L, RestTimer.durationMs)
    }
}
