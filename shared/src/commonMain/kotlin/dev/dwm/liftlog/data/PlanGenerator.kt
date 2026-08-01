package dev.dwm.liftlog.data

import dev.dwm.liftlog.data.db.Rules
import dev.dwm.liftlog.domain.Kit

enum class Goal(val label: String) {
    GROWTH("Muscle growth"),
    STRENGTH("Strength"),
    FAT_LOSS("Fat loss"),
}

/**
 * One exercise slot. Candidates are ordered gym-first; the first one whose equipment the chosen kit
 * can do wins. Equipment is declared here rather than looked up, because most template exercise
 * names are created on demand with a blank equipment field.
 */
private data class Cand(val name: String, val tag: String)

private data class Slot(val candidates: List<Cand>)

private fun slot(vararg c: Pair<String, String>) = Slot(c.map { Cand(it.first, it.second) })

private val HOME_TAGS = setOf("dumbbell", "kettlebell", "bodyweight")

private fun defaultWeight(tag: String) = when (tag) {
    "barbell" -> 40.0
    "dumbbell" -> 15.0
    "kettlebell" -> 16.0
    "machine", "cable" -> 30.0
    else -> 0.0
}

private val push = listOf(
    slot("Bench Press" to "barbell", "Dumbbell Bench Press" to "dumbbell", "Push-up" to "bodyweight"),
    slot("Incline Bench Press" to "barbell", "Incline Dumbbell Press" to "dumbbell", "Pike Push-up" to "bodyweight"),
    slot("Overhead Press" to "barbell", "Dumbbell Shoulder Press" to "dumbbell", "Kettlebell Clean and Press" to "kettlebell"),
    slot("Lateral Raise" to "dumbbell"),
    slot("Triceps Pushdown" to "cable", "Dumbbell Triceps Extension" to "dumbbell", "Dips" to "bodyweight"),
)

private val pull = listOf(
    slot("Bent Over Row" to "barbell", "Dumbbell Row" to "dumbbell", "Inverted Row" to "bodyweight"),
    slot("Lat Pulldown" to "machine", "Pull-up" to "bodyweight"),
    slot("Seated Cable Row" to "cable", "Kettlebell Row" to "kettlebell", "Inverted Row" to "bodyweight"),
    slot("Face Pull" to "cable", "Rear Delt Fly" to "dumbbell"),
    slot("Biceps Curl" to "dumbbell", "Hammer Curl" to "dumbbell"),
)

private val legs = listOf(
    slot("Squat" to "barbell", "Goblet Squat" to "kettlebell", "Bodyweight Squat" to "bodyweight"),
    slot("Romanian Deadlift" to "barbell", "Dumbbell Romanian Deadlift" to "dumbbell", "Kettlebell Swing" to "kettlebell"),
    slot("Leg Press" to "machine", "Dumbbell Lunge" to "dumbbell", "Walking Lunge" to "bodyweight"),
    slot("Leg Curl" to "machine", "Dumbbell Bulgarian Split Squat" to "dumbbell", "Glute Bridge" to "bodyweight"),
    slot("Standing Calf Raise" to "machine", "Dumbbell Calf Raise" to "dumbbell", "Single Leg Calf Raise" to "bodyweight"),
)

private val upper = listOf(push[0], pull[0], push[2], pull[1], pull[4], push[4])
private val lower = legs + listOf(slot("Hanging Leg Raise" to "bodyweight", "Plank" to "bodyweight"))

private val fullA = listOf(legs[0], push[0], pull[0], push[2], slot("Plank" to "bodyweight"))
private val fullB = listOf(legs[1], push[1], pull[1], legs[2], pull[4])

private fun splitFor(days: Int): List<Pair<String, List<Slot>>> = when (days) {
    2 -> listOf("Full Body A" to fullA, "Full Body B" to fullB)
    3 -> listOf("Push" to push, "Pull" to pull, "Legs" to legs)
    4 -> listOf("Upper A" to upper, "Lower A" to lower, "Upper B" to upper, "Lower B" to lower)
    else -> listOf("Push" to push, "Pull" to pull, "Legs" to legs, "Upper" to upper, "Lower" to lower)
}

private fun repsFor(goal: Goal) = when (goal) {
    Goal.GROWTH -> 8 to 12
    Goal.STRENGTH -> 5 to 8
    Goal.FAT_LOSS -> 10 to 15
}

private fun setsFor(goal: Goal) = if (goal == Goal.STRENGTH) 4 else 3

/**
 * Rule-based plan builder — same shape as a built-in [Template], so it installs through the
 * existing [installTemplate] path. Deterministic and offline on purpose: no AI JSON to parse.
 */
fun generatePlan(goal: Goal, daysPerWeek: Int, kit: Kit): Template {
    val (repsMin, repsMax) = repsFor(goal)
    val sets = setsFor(goal)
    val days = splitFor(daysPerWeek.coerceIn(2, 5)).map { (dayName, slots) ->
        TemplateDay(
            dayName,
            slots.map { s ->
                val pick = s.candidates.firstOrNull { kit == Kit.ALL || it.tag in HOME_TAGS }
                    ?: s.candidates.last()
                TemplateExercise(
                    name = pick.name,
                    rule = Rules.DOUBLE,
                    sets = sets,
                    repsMin = repsMin,
                    repsMax = repsMax,
                    defaultWeight = defaultWeight(pick.tag),
                )
            },
        )
    }
    val kitLabel = if (kit == Kit.HOME) "Home" else "Gym"
    return Template("$kitLabel ${goal.label} ${daysPerWeek}d", days, group = "Generated")
}

/** Shown after generating — APS prescribes cardio on the off days, we log it in the Train tab. */
const val REST_DAY_ADVICE = "Rest days: 30–40 min low-intensity cardio — log it from the Train tab."
