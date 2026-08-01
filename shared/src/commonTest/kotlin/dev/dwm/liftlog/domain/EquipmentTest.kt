package dev.dwm.liftlog.domain

import dev.dwm.liftlog.data.Goal
import dev.dwm.liftlog.data.db.Exercise
import dev.dwm.liftlog.data.generatePlan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun ex(name: String, equipment: String, category: String = "Chest", muscles: String = "") =
    Exercise(name = name, category = category, muscles = muscles, equipment = equipment)

class EquipmentTest {
    @Test
    fun bothSeedVocabulariesNormalize() {
        assertEquals(setOf("barbell"), equipmentTags("Barbell"))
        assertEquals(setOf("barbell"), equipmentTags("barbell"))
        assertEquals(setOf("kettlebell"), equipmentTags("kettlebells"))
        assertEquals(setOf("bodyweight"), equipmentTags("body only"))
        assertEquals(setOf("bodyweight"), equipmentTags("none (bodyweight exercise)"))
        assertEquals(setOf("other"), equipmentTags(""))
    }

    @Test
    fun homeKitKeepsDumbbellsAndKettlebells() {
        assertTrue(fitsKit(ex("Dumbbell Row", "dumbbell"), Kit.HOME))
        assertTrue(fitsKit(ex("Swing", "Kettlebell"), Kit.HOME))
        assertFalse(fitsKit(ex("Squat", "Barbell"), Kit.HOME))
        assertFalse(fitsKit(ex("Leg Press", "machine"), Kit.HOME))
        assertTrue(fitsKit(ex("Squat", "Barbell"), Kit.ALL))
    }

    @Test
    fun swapPrefersSameCategoryInKit() {
        val current = ex("Bench Press", "Barbell", "Chest", "pectorals, triceps")
        val all = listOf(
            ex("Dumbbell Bench Press", "dumbbell", "Chest", "pectorals, triceps"),
            ex("Leg Press", "machine", "Legs", "quadriceps"),
            ex("Barbell Bench Press", "barbell", "Chest", "pectorals"),
        )
        val picks = swapCandidates(current, all, Kit.HOME)
        assertEquals(listOf("Dumbbell Bench Press"), picks.map { it.name })
    }

    @Test
    fun exercisesWithoutMetadataStillFindAlternativesByName() {
        val current = ex("Pull-up", "", category = "", muscles = "")
        val all = listOf(
            ex("Band Assisted Pull-Up", "bands", "Back", "lats"),
            ex("Leg Press", "machine", "Legs", "quadriceps"),
        )
        assertEquals(listOf("Band Assisted Pull-Up"), swapCandidates(current, all, Kit.HOME).map { it.name })
    }

    @Test
    fun generatedPlansAreValidForEveryCombination() {
        for (goal in Goal.entries) for (days in 2..5) for (kit in Kit.entries) {
            val plan = generatePlan(goal, days, kit)
            assertEquals(days, plan.days.size, "$goal $days $kit")
            assertTrue(plan.days.all { it.exercises.isNotEmpty() })
            assertTrue(plan.days.all { d -> d.exercises.all { it.repsMin <= it.repsMax } })
        }
    }

    @Test
    fun homePlansAvoidBarbellsAndMachines() {
        val plan = generatePlan(Goal.GROWTH, 4, Kit.HOME)
        val names = plan.days.flatMap { it.exercises }.map { it.name }
        assertTrue(names.none { it == "Squat" || it == "Bench Press" || it == "Leg Press" }, names.toString())
    }
}
