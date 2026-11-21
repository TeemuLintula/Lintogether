package com.example.lintogether

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.lintogether.sub_functions.GymDay
import com.example.lintogether.sub_functions.GymExercise
import com.example.lintogether.sub_functions.WeightEntry
import com.example.lintogether.sub_functions.Note
import com.example.lintogether.sub_functions.Reflection
import com.example.lintogether.sub_functions.Goal
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksSync(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)
    @Delete
    suspend fun deleteTask(task: Task)
    @Update
    suspend fun updateTask(task: Task)

    // Gym Exercise queries
    @Query("SELECT * FROM gym_exercises ORDER BY position ASC")
    fun getAllGymExercises(): Flow<List<GymExercise>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGymExercise(exercise: GymExercise)
    @Delete
    suspend fun deleteGymExercise(exercise: GymExercise)
    @Update
    suspend fun updateGymExercise(exercise: GymExercise)

    // Gym Day queries
    @Query("SELECT * FROM gym_days ORDER BY position ASC")
    fun getAllGymDays(): Flow<List<GymDay>>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGymDay(gymDay: GymDay)
    @Delete
    suspend fun deleteGymDay(gymDay: GymDay)
    @Update
    suspend fun updateGymDay(gymDay: GymDay)

    // Note queries
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long
    @Delete
    suspend fun deleteNote(note: Note)
    @Update
    suspend fun updateNote(note: Note)

    // Reflection queries
    @Query("SELECT * FROM reflections ORDER BY timestamp DESC")
    fun getAllReflections(): Flow<List<Reflection>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReflection(reflection: Reflection): Long
    @Delete
    suspend fun deleteReflection(reflection: Reflection)

    // Goal queries
    @Query("SELECT * FROM goals ORDER BY timestamp DESC")
    fun getAllGoals(): Flow<List<Goal>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal): Long
    @Delete
    suspend fun deleteGoal(goal: Goal)
    @Update
    suspend fun updateGoal(goal: Goal)
}

class Converters {
    @TypeConverter
    fun fromWeekDaySet(value: Set<WeekDay>): String = value.joinToString(",") { it.name }
    @TypeConverter
    fun toWeekDaySet(value: String): Set<WeekDay> {
        if (value.isBlank()) return emptySet()
        return value.split(",").map { WeekDay.valueOf(it) }.toSet()
    }
    @TypeConverter
    fun fromStringSet(value: Set<String>): String = value.joinToString(",")
    @TypeConverter
    fun toStringSet(value: String): Set<String> {
        if (value.isBlank()) return emptySet()
        return value.split(",").toSet()
    }
    @TypeConverter
    fun fromIntSet(value: Set<Int>): String = value.joinToString(",")
    @TypeConverter
    fun toIntSet(value: String): Set<Int> {
        if (value.isBlank()) return emptySet()
        return value.split(",").map { it.toInt() }.toSet()
    }
    @TypeConverter
    fun fromRepeatUnit(value: RepeatUnit): String = value.name
    @TypeConverter
    fun toRepeatUnit(value: String): RepeatUnit = RepeatUnit.valueOf(value)

    @TypeConverter
    fun fromWeightHistory(value: List<WeightEntry>): String {
        return Gson().toJson(value)
    }
    @TypeConverter
    fun toWeightHistory(value: String): List<WeightEntry> {
        val listType = object : TypeToken<List<WeightEntry>>() {}.type
        return Gson().fromJson(value, listType) ?: emptyList()
    }
}

@Database(entities = [Task::class, GymExercise::class, GymDay::class, Note::class, Reflection::class, Goal::class], version = 22, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}

val MIGRATION_5_8 = object : Migration(5, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN skippedDates TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_6_8 = object : Migration(6, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN skippedDates TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE tasks ADD COLUMN skippedDates TEXT NOT NULL DEFAULT ''")
        } catch (_: Exception) {}
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN isRelapseTracker INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `gym_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `gymDay` TEXT NOT NULL, `completedDates` TEXT NOT NULL)")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `gym_days` (`name` TEXT PRIMARY KEY NOT NULL)")
    }
}

val MIGRATION_9_11 = object : Migration(9, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `gym_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `gymDay` TEXT NOT NULL, `completedDates` TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `gym_days` (`name` TEXT PRIMARY KEY NOT NULL)")
    }
}

val MIGRATION_8_11 = object : Migration(8, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN isRelapseTracker INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE TABLE IF NOT EXISTS `gym_exercises` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `gymDay` TEXT NOT NULL, `completedDates` TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `gym_days` (`name` TEXT PRIMARY KEY NOT NULL)")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE gym_exercises ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE gym_days ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE gym_exercises ADD COLUMN weight TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE gym_exercises ADD COLUMN weightMarker TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_12_14 = object : Migration(12, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE gym_exercises ADD COLUMN weight TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE gym_exercises ADD COLUMN weightMarker TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE gym_exercises ADD COLUMN weightHistory TEXT NOT NULL DEFAULT '[]'")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE gym_exercises ADD COLUMN weightHistory TEXT NOT NULL DEFAULT '[]'")
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `reflections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE reflections ADD COLUMN title TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `parentId` INTEGER, `timestamp` INTEGER NOT NULL)")
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE goals ADD COLUMN description TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE goals ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE goals ADD COLUMN x REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE goals ADD COLUMN y REAL NOT NULL DEFAULT 0.0")
    }
}

val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE goals ADD COLUMN deadline TEXT")
    }
}
