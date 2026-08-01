package dev.dwm.liftlog.data

import dev.dwm.liftlog.data.db.AppDatabase
import dev.dwm.liftlog.data.db.Exercise
import dev.dwm.liftlog.data.db.Program
import dev.dwm.liftlog.data.db.ProgramExercise
import dev.dwm.liftlog.data.db.Workout
import dev.dwm.liftlog.data.db.WorkoutSet
import dev.dwm.liftlog.data.db.nowMillis
import dev.dwm.liftlog.domain.prescription
import dev.dwm.liftlog.domain.progress

suspend fun getOrCreateExercise(db: AppDatabase, name: String, category: String = ""): Exercise {
    db.exerciseDao().byName(name)?.let { return it }
    val exercise = Exercise(name = name, category = category, muscles = "", equipment = "", custom = true)
    db.exerciseDao().insert(exercise)
    return exercise
}

/** Create a Workout pre-filled with today's prescribed sets. Returns it, or null if program has no days. */
suspend fun startProgramWorkout(db: AppDatabase, program: Program): Workout? {
    val days = db.programDao().daysFor(program.id)
    if (days.isEmpty()) return null
    val day = days[program.currentDayIndex % days.size]
    val workout = Workout(name = "${program.name} — ${day.name}", startedAt = nowMillis(), programDayId = day.id)
    db.workoutDao().insertWorkout(workout)
    for (pe in db.programDao().exercisesForDay(day.id)) {
        prescription(pe).forEachIndexed { i, p ->
            db.workoutDao().insertSet(
                WorkoutSet(
                    workoutId = workout.id,
                    exerciseId = pe.exerciseId,
                    setIndex = i,
                    weightKg = p.weightKg,
                    reps = p.reps,
                    targetReps = p.reps,
                    amrap = p.amrap,
                )
            )
        }
    }
    return workout
}

/** Top of the routine rep range: hit it on every set and the weight goes up next time. */
const val ROUTINE_PROGRESS_REPS = 10
const val ROUTINE_INCREMENT_KG = 2.5

/** How to nudge the next session: [addKg] on top of each set's own weight, aiming at [targetReps]. */
data class RoutineBump(val addKg: Double, val targetReps: Int)

/**
 * Double progression for routines, which otherwise just copied last session forever: if every
 * completed set hit [ROUTINE_PROGRESS_REPS], add 2.5 kg and reset the target to 8; otherwise keep
 * the weights and ask for one more rep. The bump is per set, so warm-up ramps stay ramps.
 * Returns null when there is nothing to go on.
 */
fun routineSuggestion(previous: List<WorkoutSet>): RoutineBump? {
    val done = previous.filter { it.completed && it.reps > 0 }
    if (done.isEmpty()) return null
    return if (done.all { it.reps >= ROUTINE_PROGRESS_REPS }) {
        RoutineBump(ROUTINE_INCREMENT_KG, 8)
    } else {
        RoutineBump(0.0, done.minOf { it.reps } + 1)
    }
}

/**
 * Strong-style routine start: sets are pre-filled from the exercise's previous
 * performance (weight/reps as uncompleted suggestions); no previous → empty sets.
 * The prefill carries a progression target, so routines improve like programs do.
 */
suspend fun startRoutineWorkout(db: AppDatabase, routine: dev.dwm.liftlog.data.db.Routine): Workout {
    val workout = Workout(name = routine.name, startedAt = nowMillis())
    db.workoutDao().insertWorkout(workout)
    for (re in db.routineDao().exercisesFor(routine.id)) {
        val previous = db.workoutDao().previousSets(re.exerciseId, workout.id)
        val suggestion = routineSuggestion(previous)
        repeat(re.sets) { i ->
            val prev = previous.getOrNull(i)
            db.workoutDao().insertSet(
                WorkoutSet(
                    workoutId = workout.id,
                    exerciseId = re.exerciseId,
                    setIndex = i,
                    weightKg = (prev?.weightKg ?: 0.0) + (if (prev != null) suggestion?.addKg ?: 0.0 else 0.0),
                    reps = prev?.reps ?: 0,
                    targetReps = suggestion?.targetReps,
                )
            )
        }
    }
    return workout
}

/** After finishing a program workout: apply progression per exercise, advance the program day pointer. */
suspend fun applyProgression(db: AppDatabase, workout: Workout) {
    val dayId = workout.programDayId ?: return
    val day = db.programDao().dayById(dayId) ?: return
    val program = db.programDao().programById(day.programId) ?: return
    val setsByExercise = db.workoutDao().setsForWorkoutOnce(workout.id)
        .filter { it.completed }
        .groupBy { it.exerciseId }
    for (pe in db.programDao().exercisesForDay(dayId)) {
        val done = setsByExercise[pe.exerciseId] ?: continue
        db.programDao().updateProgramExercise(
            progress(pe, done).copy(updatedAt = nowMillis())
        )
    }
    val dayCount = db.programDao().daysFor(program.id).size
    db.programDao().updateProgram(
        program.copy(currentDayIndex = (program.currentDayIndex + 1) % dayCount, updatedAt = nowMillis())
    )
}
