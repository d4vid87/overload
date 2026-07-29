package dev.dwm.liftlog.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Raw since/upsert access per table for the sync engine. LWW checks happen in SyncEngine. */
@Dao
interface SyncDao {
    @Query("SELECT * FROM Exercise WHERE updatedAt > :since")
    suspend fun exercisesSince(since: Long): List<Exercise>

    @Query("SELECT * FROM Workout WHERE updatedAt > :since")
    suspend fun workoutsSince(since: Long): List<Workout>

    @Query("SELECT * FROM WorkoutSet WHERE updatedAt > :since")
    suspend fun setsSince(since: Long): List<WorkoutSet>

    @Query("SELECT * FROM Program WHERE updatedAt > :since")
    suspend fun programsSince(since: Long): List<Program>

    @Query("SELECT * FROM ProgramDay WHERE updatedAt > :since")
    suspend fun programDaysSince(since: Long): List<ProgramDay>

    @Query("SELECT * FROM ProgramExercise WHERE updatedAt > :since")
    suspend fun programExercisesSince(since: Long): List<ProgramExercise>

    @Query("SELECT * FROM Food WHERE updatedAt > :since")
    suspend fun foodsSince(since: Long): List<Food>

    @Query("SELECT * FROM FoodLog WHERE updatedAt > :since")
    suspend fun foodLogsSince(since: Long): List<FoodLog>

    @Query("SELECT * FROM WeightEntry WHERE updatedAt > :since")
    suspend fun weightsSince(since: Long): List<WeightEntry>

    @Query("SELECT * FROM GroceryItem WHERE updatedAt > :since")
    suspend fun groceriesSince(since: Long): List<GroceryItem>

    @Query("SELECT * FROM Routine WHERE updatedAt > :since")
    suspend fun routinesSince(since: Long): List<Routine>

    @Query("SELECT * FROM RoutineExercise WHERE updatedAt > :since")
    suspend fun routineExercisesSince(since: Long): List<RoutineExercise>

    @Query("SELECT updatedAt FROM Exercise WHERE id = :id")
    suspend fun exerciseUpdatedAt(id: String): Long?

    @Query("SELECT updatedAt FROM Workout WHERE id = :id")
    suspend fun workoutUpdatedAt(id: String): Long?

    @Query("SELECT updatedAt FROM WorkoutSet WHERE id = :id")
    suspend fun setUpdatedAt(id: String): Long?

    @Query("SELECT updatedAt FROM Program WHERE id = :id")
    suspend fun programUpdatedAt(id: String): Long?

    @Query("SELECT updatedAt FROM ProgramDay WHERE id = :id")
    suspend fun programDayUpdatedAt(id: String): Long?

    @Query("SELECT updatedAt FROM ProgramExercise WHERE id = :id")
    suspend fun programExerciseUpdatedAt(id: String): Long?

    @Query("SELECT updatedAt FROM Food WHERE id = :id")
    suspend fun foodUpdatedAt(id: String): Long?

    @Query("SELECT updatedAt FROM FoodLog WHERE id = :id")
    suspend fun foodLogUpdatedAt(id: String): Long?

    @Query("SELECT updatedAt FROM WeightEntry WHERE id = :id")
    suspend fun weightUpdatedAt(id: String): Long?

    @Query("SELECT updatedAt FROM GroceryItem WHERE id = :id")
    suspend fun groceryUpdatedAt(id: String): Long?

    @Query("SELECT updatedAt FROM Routine WHERE id = :id")
    suspend fun routineUpdatedAt(id: String): Long?

    @Query("SELECT updatedAt FROM RoutineExercise WHERE id = :id")
    suspend fun routineExerciseUpdatedAt(id: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGrocery(row: GroceryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutine(row: Routine)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutineExercise(row: RoutineExercise)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(row: Exercise)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkout(row: Workout)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSet(row: WorkoutSet)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgram(row: Program)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgramDay(row: ProgramDay)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgramExercise(row: ProgramExercise)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFood(row: Food)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFoodLog(row: FoodLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeight(row: WeightEntry)

    // Settings were left out of backup entirely, so a reinstall lost the AI key, macro targets,
    // goal and rest prefs. Local-only on purpose: not part of the sync payload, just export/import.
    @Query("SELECT * FROM Setting WHERE updatedAt > :since")
    suspend fun settingsSince(since: Long): List<Setting>

    @Query("SELECT updatedAt FROM Setting WHERE key = :key")
    suspend fun settingUpdatedAt(key: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSetting(row: Setting)
}
