package dev.dwm.liftlog.data

import dev.dwm.liftlog.data.db.WorkoutSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private fun set(weight: Double, reps: Int, completed: Boolean = true) =
    WorkoutSet(workoutId = "w", exerciseId = "e", setIndex = 0, weightKg = weight, reps = reps, completed = completed)

class RoutineProgressionTest {
    @Test
    fun allSetsAtTopOfRangeAddWeight() {
        val s = routineSuggestion(listOf(set(20.0, 10), set(20.0, 11), set(20.0, 10)))
        assertEquals(RoutineBump(2.5, 8), s)
    }

    @Test
    fun shortOfTheRangeAddsOneRepAndKeepsTheWeights() {
        val s = routineSuggestion(listOf(set(20.0, 10), set(20.0, 8), set(20.0, 9)))
        assertEquals(RoutineBump(0.0, 9), s)
    }

    @Test
    fun noCompletedHistoryMeansNoSuggestion() {
        assertNull(routineSuggestion(emptyList()))
        assertNull(routineSuggestion(listOf(set(20.0, 10, completed = false))))
    }
}
