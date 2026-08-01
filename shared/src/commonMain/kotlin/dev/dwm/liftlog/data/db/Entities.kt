package dev.dwm.liftlog.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun newId(): String = Uuid.random().toString()

fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

@kotlinx.serialization.Serializable
@Entity
data class Exercise(
    @PrimaryKey val id: String = newId(),
    val name: String,
    val category: String,
    val muscles: String,
    val equipment: String,
    val custom: Boolean = false,
    val updatedAt: Long = nowMillis(),
    val deletedAt: Long? = null,
)

@kotlinx.serialization.Serializable
@Entity
data class Workout(
    @PrimaryKey val id: String = newId(),
    val name: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val programDayId: String? = null,
    val updatedAt: Long = nowMillis(),
    val deletedAt: Long? = null,
)

@kotlinx.serialization.Serializable
@Entity(indices = [Index("workoutId"), Index("exerciseId")])
data class WorkoutSet(
    @PrimaryKey val id: String = newId(),
    val workoutId: String,
    val exerciseId: String,
    val setIndex: Int,
    val weightKg: Double,
    val reps: Int,
    val targetReps: Int? = null,
    val amrap: Boolean = false,
    val rpe: Double? = null,
    val completed: Boolean = false,
    val updatedAt: Long = nowMillis(),
    val deletedAt: Long? = null,
)

@kotlinx.serialization.Serializable
@Entity
data class Program(
    @PrimaryKey val id: String = newId(),
    val name: String,
    val currentDayIndex: Int = 0,
    val active: Boolean = false,
    val updatedAt: Long = nowMillis(),
    val deletedAt: Long? = null,
)

@kotlinx.serialization.Serializable
@Entity(indices = [Index("programId")])
data class ProgramDay(
    @PrimaryKey val id: String = newId(),
    val programId: String,
    val dayIndex: Int,
    val name: String,
    val updatedAt: Long = nowMillis(),
    val deletedAt: Long? = null,
)

/**
 * One exercise slot in a program day, including its progression state.
 *
 * rule:
 *  - LINEAR: fixed sets×reps at workingWeightKg; success (all sets hit targetReps)
 *    → +incrementKg; failLimit consecutive fails → deload 10%.
 *  - DOUBLE: sets at workingWeightKg, reps range repsMin..repsMax; all sets at
 *    repsMax → +incrementKg and reps reset to repsMin.
 *  - TM_PCT: percent-of-training-max scheme (JSON in schemeJson, one entry per set:
 *    {"pct":0.65,"reps":5,"amrap":false}); cycle of cycleLength workouts, then TM
 *    += incrementKg (nSuns-style: bump keyed to AMRAP reps when amrapBump=true).
 */
@kotlinx.serialization.Serializable
@Entity(indices = [Index("programDayId")])
data class ProgramExercise(
    @PrimaryKey val id: String = newId(),
    val programDayId: String,
    val exerciseId: String,
    val position: Int,
    val rule: String,
    val sets: Int = 3,
    val repsMin: Int = 5,
    val repsMax: Int = 5,
    val workingWeightKg: Double = 20.0,
    val tmKg: Double = 60.0,
    val schemeJson: String? = null,
    val cycleLength: Int = 1,
    val cyclePos: Int = 0,
    val incrementKg: Double = 2.5,
    val failCount: Int = 0,
    val failLimit: Int = 3,
    val amrapBump: Boolean = false,
    val updatedAt: Long = nowMillis(),
    val deletedAt: Long? = null,
)

/** Strong-style reusable workout template. Weights come from previous performance, not the routine. */
@kotlinx.serialization.Serializable
@Entity
data class Routine(
    @PrimaryKey val id: String = newId(),
    val name: String,
    val position: Int = 0,
    val updatedAt: Long = nowMillis(),
    val deletedAt: Long? = null,
)

@kotlinx.serialization.Serializable
@Entity(indices = [Index("routineId")])
data class RoutineExercise(
    @PrimaryKey val id: String = newId(),
    val routineId: String,
    val exerciseId: String,
    val position: Int,
    val sets: Int = 3,
    val restSeconds: Int? = null,     // null = global default
    val supersetGroup: Int? = null,   // exercises sharing a group alternate without rest
    val tempo: String? = null,        // "3-1-1-0" ecc-pause-con-pause seconds
    val updatedAt: Long = nowMillis(),
    val deletedAt: Long? = null,
)

/** Rest-day cardio session. Duration only — no distance/pace until something actually needs them. */
@kotlinx.serialization.Serializable
@Entity
data class Cardio(
    @PrimaryKey val id: String = newId(),
    val startedAt: Long,
    val minutes: Int,
    val kind: String = "walk",
    val updatedAt: Long = nowMillis(),
    val deletedAt: Long? = null,
)

val CARDIO_KINDS = listOf("walk", "run", "bike", "row", "other")

object Rules {
    const val LINEAR = "LINEAR"
    const val DOUBLE = "DOUBLE"
    const val TM_PCT = "TM_PCT"
}
