package dev.dwm.liftlog.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

// v6: per-exercise rest + supersets — additive, no data loss
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE RoutineExercise ADD COLUMN restSeconds INTEGER")
        connection.execSQL("ALTER TABLE RoutineExercise ADD COLUMN supersetGroup INTEGER")
    }
}

// v7: per-exercise lifting tempo — additive
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE RoutineExercise ADD COLUMN tempo TEXT")
    }
}

// v8: food thumbnail from Open Food Facts — additive
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE Food ADD COLUMN imageUrl TEXT")
    }
}

// v9: real serving sizes so foods don't all default to 100g — additive
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE Food ADD COLUMN servingGrams REAL")
        connection.execSQL("ALTER TABLE Food ADD COLUMN servingLabel TEXT")
    }
}

// v10: rest-day cardio logging — new table, existing data untouched
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """CREATE TABLE IF NOT EXISTS Cardio (
                 id TEXT NOT NULL PRIMARY KEY,
                 startedAt INTEGER NOT NULL,
                 minutes INTEGER NOT NULL,
                 kind TEXT NOT NULL,
                 updatedAt INTEGER NOT NULL,
                 deletedAt INTEGER
               )"""
        )
    }
}

@Database(
    entities = [
        Exercise::class, Workout::class, WorkoutSet::class,
        Program::class, ProgramDay::class, ProgramExercise::class,
        Food::class, FoodLog::class, WeightEntry::class, Setting::class,
        GroceryItem::class, Routine::class, RoutineExercise::class,
        Cardio::class,
    ],
    version = 10,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun programDao(): ProgramDao
    abstract fun foodDao(): FoodDao
    abstract fun foodLogDao(): FoodLogDao
    abstract fun weightDao(): WeightDao
    abstract fun settingDao(): SettingDao
    abstract fun groceryDao(): GroceryDao
    abstract fun routineDao(): RoutineDao
    abstract fun cardioDao(): CardioDao
    abstract fun syncDao(): SyncDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
