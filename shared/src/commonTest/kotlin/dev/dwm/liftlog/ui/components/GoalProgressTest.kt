package dev.dwm.liftlog.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class GoalProgressTest {
    @Test fun handlesUnsetGoalsAndInvalidNumbers() {
        assertEquals(0f, goalProgress(0.0, 0.0))
        assertEquals(0f, goalProgress(50.0, 0.0))
        assertEquals(0f, goalProgress(Double.NaN, 100.0))
        assertEquals(0f, goalProgress(50.0, Double.POSITIVE_INFINITY))
        assertEquals(0f, goalProgress(-10.0, 100.0))
        assertEquals(0.5f, goalProgress(50.0, 100.0))
        assertEquals(1f, goalProgress(150.0, 100.0))
    }
}
