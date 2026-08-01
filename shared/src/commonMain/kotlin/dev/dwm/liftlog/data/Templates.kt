package dev.dwm.liftlog.data

import dev.dwm.liftlog.data.db.AppDatabase
import dev.dwm.liftlog.data.db.Program
import dev.dwm.liftlog.data.db.ProgramDay
import dev.dwm.liftlog.data.db.ProgramExercise
import dev.dwm.liftlog.data.db.Rules
import dev.dwm.liftlog.domain.SchemeSet
import dev.dwm.liftlog.domain.encodeScheme

data class TemplateExercise(
    val name: String,
    val rule: String,
    val sets: Int = 3,
    val repsMin: Int = 5,
    val repsMax: Int = 5,
    val incrementKg: Double = 2.5,
    val weeks: List<List<SchemeSet>>? = null,
    val amrapBump: Boolean = false,
    val defaultTm: Double = 60.0,
    val defaultWeight: Double = 20.0,
)

data class TemplateDay(val name: String, val exercises: List<TemplateExercise>)
data class Template(val name: String, val days: List<TemplateDay>, val group: String = "Barbell strength")

private fun w(vararg sets: SchemeSet) = sets.toList()
private fun s(pct: Double, reps: Int, amrap: Boolean = false) = SchemeSet(pct, reps, amrap)

// 5/3/1 4-week wave, main lift
private val wave531 = listOf(
    w(s(0.65, 5), s(0.75, 5), s(0.85, 5, amrap = true)),
    w(s(0.70, 3), s(0.80, 3), s(0.90, 3, amrap = true)),
    w(s(0.75, 5), s(0.85, 3), s(0.95, 1, amrap = true)),
    w(s(0.40, 5), s(0.50, 5), s(0.60, 5)), // deload
)

private fun bbb(pct: Double = 0.5) = List(4) { List(5) { s(pct, 10) } }

private fun main531(name: String, inc: Double) = TemplateExercise(
    name, Rules.TM_PCT, weeks = wave531, incrementKg = inc, defaultTm = if (inc > 2.5) 100.0 else 60.0,
)

private fun bbb531(name: String) = TemplateExercise(name, Rules.TM_PCT, weeks = bbb(), incrementKg = 0.0)

// nSuns T1 (9 sets, AMRAP on set 3) and T2 (8 sets)
private val nsunsT1 = listOf(
    w(
        s(0.75, 5), s(0.85, 3), s(0.95, 1, amrap = true), s(0.90, 3), s(0.85, 3),
        s(0.80, 3), s(0.75, 5), s(0.70, 5), s(0.65, 5, amrap = true),
    )
)
private val nsunsT2 = listOf(
    w(s(0.50, 6), s(0.60, 5), s(0.70, 3), s(0.70, 5), s(0.70, 7), s(0.60, 4), s(0.60, 6), s(0.50, 8))
)

private fun t1(name: String, inc: Double) =
    TemplateExercise(name, Rules.TM_PCT, weeks = nsunsT1, incrementKg = inc, amrapBump = true, defaultTm = 80.0)

private fun t2(name: String) =
    TemplateExercise(name, Rules.TM_PCT, weeks = nsunsT2, incrementKg = 0.0, defaultTm = 60.0)

private fun linear(name: String, sets: Int, reps: Int, inc: Double = 2.5, weight: Double = 40.0) =
    TemplateExercise(name, Rules.LINEAR, sets = sets, repsMin = reps, repsMax = reps, incrementKg = inc, defaultWeight = weight)

private fun double(name: String, sets: Int, repsMin: Int, repsMax: Int, weight: Double = 20.0) =
    TemplateExercise(name, Rules.DOUBLE, sets = sets, repsMin = repsMin, repsMax = repsMax, defaultWeight = weight)

val templates = listOf(
    Template(
        "5/3/1 BBB", listOf(
            TemplateDay("OHP", listOf(main531("Overhead Press", 2.5), bbb531("Overhead Press"), double("Chin-ups", 5, 5, 10))),
            TemplateDay("Deadlift", listOf(main531("Deadlift", 5.0), bbb531("Deadlift"), double("Hanging Leg Raise", 5, 10, 15))),
            TemplateDay("Bench", listOf(main531("Bench Press", 2.5), bbb531("Bench Press"), double("Bent Over Row", 5, 10, 12, 40.0))),
            TemplateDay("Squat", listOf(main531("Squat", 5.0), bbb531("Squat"), double("Leg Curl", 5, 10, 15))),
        )
    ),
    Template(
        "nSuns LP 4-day", listOf(
            TemplateDay("Bench + OHP", listOf(t1("Bench Press", 2.5), t2("Overhead Press"), double("Chin-ups", 3, 8, 12))),
            TemplateDay("Squat + Sumo DL", listOf(t1("Squat", 5.0), t2("Sumo Deadlift"), double("Leg Press", 3, 8, 12, 100.0))),
            TemplateDay("OHP + Incline", listOf(t1("Overhead Press", 2.5), t2("Incline Bench Press"), double("Lateral Raise", 3, 10, 15, 8.0))),
            TemplateDay("Deadlift + Front Squat", listOf(t1("Deadlift", 5.0), t2("Front Squat"), double("Bent Over Row", 3, 8, 12, 40.0))),
        )
    ),
    Template(
        "GZCLP 4-day", listOf(
            TemplateDay("A1", listOf(linear("Squat", 5, 3, 5.0, 60.0), linear("Bench Press", 3, 10, 2.5, 40.0), double("Lat Pulldown", 3, 15, 25, 30.0))),
            TemplateDay("A2", listOf(linear("Overhead Press", 5, 3, 2.5, 30.0), linear("Deadlift", 3, 10, 5.0, 60.0), double("Dumbbell Row", 3, 15, 25, 15.0))),
            TemplateDay("B1", listOf(linear("Bench Press", 5, 3, 2.5, 40.0), linear("Squat", 3, 10, 5.0, 60.0), double("Lat Pulldown", 3, 15, 25, 30.0))),
            TemplateDay("B2", listOf(linear("Deadlift", 5, 3, 5.0, 60.0), linear("Overhead Press", 3, 10, 2.5, 30.0), double("Dumbbell Row", 3, 15, 25, 15.0))),
        )
    ),
    Template(
        "PPL Linear", listOf(
            TemplateDay("Push", listOf(linear("Bench Press", 5, 5, 2.5, 40.0), double("Overhead Press", 3, 8, 12, 25.0), double("Triceps Pushdown", 3, 8, 12, 20.0), double("Lateral Raise", 3, 10, 15, 8.0))),
            TemplateDay("Pull", listOf(linear("Deadlift", 3, 5, 5.0, 60.0), double("Bent Over Row", 3, 8, 12, 40.0), double("Lat Pulldown", 3, 8, 12, 40.0), double("Biceps Curl", 3, 8, 12, 12.0))),
            TemplateDay("Legs", listOf(linear("Squat", 5, 5, 5.0, 50.0), double("Leg Press", 3, 8, 12, 100.0), double("Leg Curl", 3, 8, 12, 30.0), double("Standing Calf Raise", 3, 10, 15, 40.0))),
        ),
        group = "Hypertrophy",
    ),
    // ---- barbell strength ----
    Template(
        "StrongLifts 5×5", listOf(
            TemplateDay("A", listOf(linear("Squat", 5, 5, 2.5, 40.0), linear("Bench Press", 5, 5, 2.5, 30.0), linear("Bent Over Row", 5, 5, 2.5, 30.0))),
            TemplateDay("B", listOf(linear("Squat", 5, 5, 2.5, 40.0), linear("Overhead Press", 5, 5, 2.5, 25.0), linear("Deadlift", 1, 5, 5.0, 50.0))),
        )
    ),
    Template(
        "Starting Strength", listOf(
            TemplateDay("A", listOf(linear("Squat", 3, 5, 2.5, 40.0), linear("Bench Press", 3, 5, 2.5, 30.0), linear("Deadlift", 1, 5, 5.0, 50.0))),
            TemplateDay("B", listOf(linear("Squat", 3, 5, 2.5, 40.0), linear("Overhead Press", 3, 5, 2.5, 25.0), linear("Power Clean", 5, 3, 2.5, 30.0))),
        )
    ),
    Template(
        "Greyskull LP", listOf(
            TemplateDay("A", listOf(double("Overhead Press", 3, 5, 8, 25.0), double("Chin-ups", 3, 6, 10), linear("Squat", 3, 5, 2.5, 40.0))),
            TemplateDay("B", listOf(double("Bench Press", 3, 5, 8, 30.0), double("Bent Over Row", 3, 6, 10, 30.0), linear("Deadlift", 1, 5, 5.0, 50.0))),
        )
    ),
    Template(
        "Upper/Lower 4-day", listOf(
            TemplateDay("Upper A", listOf(linear("Bench Press", 4, 5, 2.5, 40.0), double("Bent Over Row", 4, 6, 10, 40.0), double("Overhead Press", 3, 8, 12, 25.0), double("Biceps Curl", 3, 8, 12, 12.0), double("Triceps Pushdown", 3, 8, 12, 20.0))),
            TemplateDay("Lower A", listOf(linear("Squat", 4, 5, 5.0, 50.0), double("Romanian Deadlift", 3, 8, 12, 50.0), double("Leg Press", 3, 10, 15, 100.0), double("Standing Calf Raise", 4, 10, 15, 40.0), double("Hanging Leg Raise", 3, 10, 15))),
            TemplateDay("Upper B", listOf(double("Incline Bench Press", 4, 6, 10, 35.0), double("Lat Pulldown", 4, 8, 12, 40.0), double("Lateral Raise", 3, 10, 15, 8.0), double("Face Pull", 3, 12, 15, 15.0), double("Skullcrusher", 3, 8, 12, 15.0))),
            TemplateDay("Lower B", listOf(linear("Deadlift", 3, 5, 5.0, 60.0), double("Front Squat", 3, 6, 10, 40.0), double("Leg Curl", 3, 10, 15, 30.0), double("Seated Calf Raise", 4, 12, 15, 30.0), double("Cable Crunch", 3, 10, 15, 25.0))),
        )
    ),
    Template(
        "Full Body 3-day", listOf(
            TemplateDay("A", listOf(linear("Squat", 3, 5, 2.5, 40.0), double("Bench Press", 3, 6, 10, 30.0), double("Bent Over Row", 3, 6, 10, 30.0), double("Plank", 3, 30, 60))),
            TemplateDay("B", listOf(linear("Deadlift", 2, 5, 5.0, 50.0), double("Overhead Press", 3, 6, 10, 25.0), double("Lat Pulldown", 3, 8, 12, 40.0), double("Biceps Curl", 3, 8, 12, 12.0))),
            TemplateDay("C", listOf(double("Front Squat", 3, 6, 10, 35.0), double("Incline Bench Press", 3, 8, 12, 30.0), double("Dumbbell Row", 3, 8, 12, 20.0), double("Standing Calf Raise", 3, 12, 15, 40.0))),
        )
    ),
    // ---- hypertrophy ----
    Template(
        "PHUL 4-day", listOf(
            TemplateDay("Upper Power", listOf(linear("Bench Press", 4, 5, 2.5, 40.0), double("Bent Over Row", 4, 5, 8, 40.0), double("Overhead Press", 3, 6, 10, 25.0), double("Biceps Curl", 3, 6, 10, 12.0), double("Skullcrusher", 3, 6, 10, 15.0))),
            TemplateDay("Lower Power", listOf(linear("Squat", 4, 5, 5.0, 50.0), linear("Deadlift", 3, 5, 5.0, 60.0), double("Leg Press", 4, 10, 15, 100.0), double("Standing Calf Raise", 4, 8, 12, 40.0))),
            TemplateDay("Upper Hypertrophy", listOf(double("Incline Bench Press", 4, 8, 12, 30.0), double("Cable Fly", 3, 10, 15, 15.0), double("Seated Cable Row", 4, 8, 12, 40.0), double("Lateral Raise", 3, 10, 15, 8.0), double("Hammer Curl", 3, 10, 15, 10.0), double("Triceps Pushdown", 3, 10, 15, 20.0))),
            TemplateDay("Lower Hypertrophy", listOf(double("Front Squat", 4, 8, 12, 40.0), double("Romanian Deadlift", 3, 10, 12, 50.0), double("Leg Curl", 4, 10, 15, 30.0), double("Leg Extension", 3, 10, 15, 30.0), double("Seated Calf Raise", 4, 12, 15, 30.0))),
        ),
        group = "Hypertrophy",
    ),
    Template(
        "PHAT 5-day", listOf(
            TemplateDay("Upper Power", listOf(double("Bent Over Row", 3, 3, 5, 50.0), double("Bench Press", 3, 3, 5, 45.0), double("Overhead Press", 3, 5, 8, 30.0), double("Chin-ups", 2, 6, 10), double("Skullcrusher", 3, 6, 10, 15.0))),
            TemplateDay("Lower Power", listOf(linear("Squat", 3, 5, 5.0, 60.0), double("Deadlift", 3, 3, 5, 70.0), double("Leg Press", 2, 8, 10, 120.0), double("Standing Calf Raise", 3, 6, 10, 50.0))),
            TemplateDay("Back & Shoulders", listOf(double("Bent Over Row", 3, 8, 12, 40.0), double("Lat Pulldown", 3, 8, 12, 40.0), double("Seated Cable Row", 3, 8, 12, 40.0), double("Lateral Raise", 3, 12, 20, 8.0), double("Face Pull", 3, 12, 20, 15.0))),
            TemplateDay("Legs", listOf(double("Squat", 3, 8, 12, 50.0), double("Romanian Deadlift", 3, 8, 12, 50.0), double("Leg Extension", 3, 12, 15, 30.0), double("Leg Curl", 3, 12, 15, 30.0), double("Seated Calf Raise", 3, 12, 20, 30.0))),
            TemplateDay("Chest & Arms", listOf(double("Bench Press", 3, 8, 12, 40.0), double("Incline Dumbbell Press", 3, 8, 12, 20.0), double("Cable Fly", 3, 12, 15, 15.0), double("Biceps Curl", 3, 8, 12, 12.0), double("Triceps Pushdown", 3, 8, 12, 20.0), double("Hammer Curl", 2, 12, 15, 10.0))),
        ),
        group = "Hypertrophy",
    ),
    Template(
        "Arnold Split 6-day", listOf(
            TemplateDay("Chest & Back A", listOf(double("Bench Press", 4, 8, 12, 40.0), double("Incline Bench Press", 3, 8, 12, 30.0), double("Chin-ups", 4, 6, 10), double("Bent Over Row", 3, 8, 12, 40.0))),
            TemplateDay("Shoulders & Arms A", listOf(double("Overhead Press", 4, 8, 12, 25.0), double("Lateral Raise", 3, 10, 15, 8.0), double("Biceps Curl", 3, 8, 12, 12.0), double("Skullcrusher", 3, 8, 12, 15.0))),
            TemplateDay("Legs A", listOf(double("Squat", 4, 8, 12, 50.0), double("Romanian Deadlift", 3, 8, 12, 50.0), double("Leg Extension", 3, 10, 15, 30.0), double("Standing Calf Raise", 4, 10, 15, 40.0))),
            TemplateDay("Chest & Back B", listOf(double("Incline Dumbbell Press", 4, 8, 12, 20.0), double("Cable Fly", 3, 10, 15, 15.0), double("Lat Pulldown", 4, 8, 12, 40.0), double("Seated Cable Row", 3, 8, 12, 40.0))),
            TemplateDay("Shoulders & Arms B", listOf(double("Arnold Press", 4, 8, 12, 15.0), double("Face Pull", 3, 12, 15, 15.0), double("Hammer Curl", 3, 10, 12, 10.0), double("Triceps Pushdown", 3, 10, 12, 20.0))),
            TemplateDay("Legs B", listOf(double("Front Squat", 4, 8, 12, 40.0), double("Leg Curl", 3, 10, 15, 30.0), double("Leg Press", 3, 10, 15, 100.0), double("Seated Calf Raise", 4, 12, 15, 30.0))),
        ),
        group = "Hypertrophy",
    ),
    Template(
        "Bro Split 5-day", listOf(
            TemplateDay("Chest", listOf(double("Bench Press", 4, 8, 12, 40.0), double("Incline Dumbbell Press", 3, 8, 12, 20.0), double("Cable Fly", 3, 10, 15, 15.0), double("Push-up", 2, 10, 20))),
            TemplateDay("Back", listOf(double("Deadlift", 3, 5, 8, 60.0), double("Lat Pulldown", 4, 8, 12, 40.0), double("Bent Over Row", 3, 8, 12, 40.0), double("Face Pull", 3, 12, 15, 15.0))),
            TemplateDay("Shoulders", listOf(double("Overhead Press", 4, 8, 12, 25.0), double("Lateral Raise", 4, 10, 15, 8.0), double("Rear Delt Fly", 3, 12, 15, 8.0), double("Shrug", 3, 10, 15, 30.0))),
            TemplateDay("Arms", listOf(double("Biceps Curl", 4, 8, 12, 12.0), double("Skullcrusher", 4, 8, 12, 15.0), double("Hammer Curl", 3, 10, 12, 10.0), double("Triceps Pushdown", 3, 10, 12, 20.0))),
            TemplateDay("Legs", listOf(double("Squat", 4, 8, 12, 50.0), double("Leg Press", 3, 10, 15, 100.0), double("Leg Curl", 3, 10, 15, 30.0), double("Standing Calf Raise", 4, 12, 15, 40.0))),
        ),
        group = "Hypertrophy",
    ),
    // ---- dumbbell-only ----
    Template(
        "Dumbbell PPL 3-day", listOf(
            TemplateDay("Push", listOf(double("Dumbbell Bench Press", 4, 8, 12, 20.0), double("Dumbbell Shoulder Press", 3, 8, 12, 15.0), double("Incline Dumbbell Press", 3, 8, 12, 17.5), double("Lateral Raise", 3, 10, 15, 8.0), double("Dumbbell Triceps Extension", 3, 10, 12, 10.0))),
            TemplateDay("Pull", listOf(double("Dumbbell Row", 4, 8, 12, 20.0), double("Dumbbell Pullover", 3, 10, 12, 15.0), double("Rear Delt Fly", 3, 12, 15, 8.0), double("Biceps Curl", 3, 8, 12, 12.0), double("Hammer Curl", 3, 10, 12, 10.0))),
            TemplateDay("Legs", listOf(double("Goblet Squat", 4, 8, 12, 20.0), double("Dumbbell Romanian Deadlift", 3, 8, 12, 20.0), double("Dumbbell Lunge", 3, 8, 12, 15.0), double("Dumbbell Calf Raise", 4, 12, 15, 20.0))),
        ),
        group = "Dumbbell",
    ),
    Template(
        "Dumbbell Upper/Lower 4-day", listOf(
            TemplateDay("Upper A", listOf(double("Dumbbell Bench Press", 4, 6, 10, 20.0), double("Dumbbell Row", 4, 6, 10, 20.0), double("Dumbbell Shoulder Press", 3, 8, 12, 15.0), double("Biceps Curl", 3, 8, 12, 12.0))),
            TemplateDay("Lower A", listOf(double("Goblet Squat", 4, 8, 12, 20.0), double("Dumbbell Romanian Deadlift", 4, 8, 12, 20.0), double("Dumbbell Lunge", 3, 8, 12, 15.0), double("Dumbbell Calf Raise", 4, 12, 15, 20.0))),
            TemplateDay("Upper B", listOf(double("Incline Dumbbell Press", 4, 8, 12, 17.5), double("Dumbbell Pullover", 3, 10, 12, 15.0), double("Lateral Raise", 3, 10, 15, 8.0), double("Dumbbell Triceps Extension", 3, 10, 12, 10.0))),
            TemplateDay("Lower B", listOf(double("Dumbbell Bulgarian Split Squat", 4, 8, 12, 12.5), double("Dumbbell Step-up", 3, 8, 12, 12.5), double("Dumbbell Swing", 3, 12, 15, 16.0), double("Weighted Sit-up", 3, 10, 15, 5.0))),
        ),
        group = "Dumbbell",
    ),
    Template(
        "Dumbbell Full Body 3-day", listOf(
            TemplateDay("A", listOf(double("Goblet Squat", 3, 8, 12, 20.0), double("Dumbbell Bench Press", 3, 8, 12, 20.0), double("Dumbbell Row", 3, 8, 12, 20.0), double("Plank", 3, 30, 60))),
            TemplateDay("B", listOf(double("Dumbbell Romanian Deadlift", 3, 8, 12, 20.0), double("Dumbbell Shoulder Press", 3, 8, 12, 15.0), double("Dumbbell Pullover", 3, 10, 12, 15.0), double("Biceps Curl", 3, 8, 12, 12.0))),
            TemplateDay("C", listOf(double("Dumbbell Lunge", 3, 8, 12, 15.0), double("Incline Dumbbell Press", 3, 8, 12, 17.5), double("Rear Delt Fly", 3, 12, 15, 8.0), double("Dumbbell Calf Raise", 3, 12, 15, 20.0))),
        ),
        group = "Dumbbell",
    ),
    // ---- kettlebell ----
    Template(
        "Simple & Sinister", listOf(
            TemplateDay("Daily", listOf(double("Kettlebell Swing", 10, 10, 10, 24.0), double("Turkish Get-Up", 10, 1, 1, 16.0))),
        ),
        group = "Kettlebell",
    ),
    Template(
        "Kettlebell Full Body 3-day", listOf(
            TemplateDay("A", listOf(double("Kettlebell Clean and Press", 5, 5, 8, 16.0), double("Goblet Squat", 4, 8, 12, 16.0), double("Kettlebell Swing", 4, 15, 20, 24.0))),
            TemplateDay("B", listOf(double("Kettlebell Row", 4, 8, 12, 16.0), double("Kettlebell Front Squat", 4, 6, 10, 16.0), double("Turkish Get-Up", 5, 1, 2, 16.0))),
            TemplateDay("C", listOf(double("Kettlebell Snatch", 5, 5, 8, 16.0), double("Kettlebell Lunge", 3, 8, 12, 12.0), double("Kettlebell Swing", 4, 15, 20, 24.0))),
        ),
        group = "Kettlebell",
    ),
    // ---- bodyweight ----
    Template(
        "Recommended Routine (BW)", listOf(
            TemplateDay("A", listOf(double("Pull-up", 3, 5, 8, 0.0), double("Dips", 3, 5, 8, 0.0), double("Bodyweight Squat", 3, 5, 8, 0.0), double("Inverted Row", 3, 5, 8, 0.0), double("Push-up", 3, 5, 8, 0.0), double("Hanging Leg Raise", 3, 5, 8, 0.0))),
            TemplateDay("B", listOf(double("Chin-ups", 3, 5, 8, 0.0), double("Pike Push-up", 3, 5, 8, 0.0), double("Pistol Squat", 3, 5, 8, 0.0), double("Inverted Row", 3, 5, 8, 0.0), double("Diamond Push-up", 3, 5, 8, 0.0), double("Plank", 3, 30, 60, 0.0))),
            TemplateDay("C", listOf(double("Pull-up", 3, 5, 8, 0.0), double("Dips", 3, 5, 8, 0.0), double("Glute Bridge", 3, 8, 12, 0.0), double("Bodyweight Row", 3, 5, 8, 0.0), double("Push-up", 3, 8, 12, 0.0), double("Hanging Leg Raise", 3, 5, 8, 0.0))),
        ),
        group = "Bodyweight",
    ),
    Template(
        "Calisthenics Upper/Lower", listOf(
            TemplateDay("Upper", listOf(double("Pull-up", 4, 5, 10, 0.0), double("Push-up", 4, 10, 20, 0.0), double("Dips", 3, 5, 10, 0.0), double("Inverted Row", 3, 8, 12, 0.0), double("Pike Push-up", 3, 5, 10, 0.0))),
            TemplateDay("Lower", listOf(double("Bodyweight Squat", 4, 15, 25, 0.0), double("Walking Lunge", 3, 10, 15, 0.0), double("Glute Bridge", 3, 12, 20, 0.0), double("Single Leg Calf Raise", 4, 12, 20, 0.0), double("Plank", 3, 30, 60, 0.0))),
        ),
        group = "Bodyweight",
    ),
    // ---- TRX ----
    Template(
        "TRX Full Body 3-day", listOf(
            TemplateDay("A", listOf(double("TRX Row", 4, 8, 12, 0.0), double("TRX Chest Press", 4, 8, 12, 0.0), double("TRX Squat", 3, 12, 15, 0.0), double("TRX Pike", 3, 8, 12, 0.0))),
            TemplateDay("B", listOf(double("TRX Y-Fly", 3, 10, 15, 0.0), double("TRX Atomic Push-up", 3, 6, 10, 0.0), double("TRX Hamstring Curl", 3, 10, 15, 0.0), double("TRX Biceps Curl", 3, 10, 12, 0.0))),
            TemplateDay("C", listOf(double("TRX Row", 4, 8, 12, 0.0), double("TRX Triceps Extension", 3, 10, 12, 0.0), double("TRX Pistol Squat", 3, 5, 8, 0.0), double("TRX Fallout", 3, 8, 12, 0.0))),
        ),
        group = "TRX",
    ),
)

/** Instantiate a template into DB rows. Exercises resolved by name, created if missing. */
suspend fun installTemplate(db: AppDatabase, template: Template): Program {
    // days go in first: the programs Flow emits as soon as the Program row lands, and a card that
    // renders before its days exist shows "Day 1 of 1" until something else invalidates it
    val program = Program(name = template.name)
    template.days.forEachIndexed { dayIndex, day ->
        val programDay = ProgramDay(programId = program.id, dayIndex = dayIndex, name = day.name)
        db.programDao().insertDay(programDay)
        day.exercises.forEachIndexed { position, te ->
            val exercise = getOrCreateExercise(db, te.name)
            db.programDao().insertProgramExercise(
                ProgramExercise(
                    programDayId = programDay.id,
                    exerciseId = exercise.id,
                    position = position,
                    rule = te.rule,
                    sets = te.sets,
                    repsMin = te.repsMin,
                    repsMax = te.repsMax,
                    workingWeightKg = te.defaultWeight,
                    tmKg = te.defaultTm,
                    schemeJson = te.weeks?.let { encodeScheme(it) },
                    cycleLength = te.weeks?.size ?: 1,
                    incrementKg = te.incrementKg,
                    amrapBump = te.amrapBump,
                )
            )
        }
    }
    db.programDao().insertProgram(program)
    return program
}
