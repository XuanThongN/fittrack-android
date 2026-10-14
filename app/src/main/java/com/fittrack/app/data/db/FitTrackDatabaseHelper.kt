package com.fittrack.app.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.fittrack.app.data.model.Roadmap
import com.fittrack.app.data.model.RunSession
import com.fittrack.app.data.model.UserProfile
import com.fittrack.app.data.model.WeightEntry
import com.fittrack.app.data.model.WorkoutSession

class FitTrackDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "fittrack.db"
        const val DATABASE_VERSION = 2

        // Tables
        const val TABLE_PROFILE = "profile"
        const val TABLE_WEIGHT = "weight_entries"
        const val TABLE_WORKOUT = "workout_sessions"
        const val TABLE_RUN = "run_sessions"
        const val TABLE_ROADMAP = "roadmaps"

        // Roadmap columns
        const val COL_WAYPOINTS = "waypoints_json"
        const val COL_ROUTE_COORDS = "route_coords_json"

        // Profile columns
        const val COL_NAME = "name"
        const val COL_HEIGHT = "height_cm"
        const val COL_INITIAL_WEIGHT = "initial_weight_kg"
        const val COL_TARGET_WEIGHT = "target_weight_kg"
        const val COL_GENDER = "gender"
        const val COL_AGE = "age"
        const val COL_ACTIVITY_LEVEL = "activity_level"

        // Weight columns
        const val COL_ID = "id"
        const val COL_WEIGHT_KG = "weight_kg"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_NOTE = "note"
        const val COL_BODY_FAT = "body_fat"

        // Workout columns
        const val COL_PLAN_TITLE = "plan_title"
        const val COL_DURATION_SEC = "duration_seconds"
        const val COL_CALORIES = "calories_burned"
        const val COL_EXERCISES_COUNT = "exercises_count"

        // Run columns
        const val COL_DISTANCE_METERS = "distance_meters"
        const val COL_AVG_PACE_SEC = "avg_pace_seconds"
        const val COL_IS_GPS = "is_gps"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createProfileTable = """
            CREATE TABLE $TABLE_PROFILE (
                $COL_ID INTEGER PRIMARY KEY,
                $COL_NAME TEXT,
                $COL_HEIGHT REAL,
                $COL_INITIAL_WEIGHT REAL,
                $COL_TARGET_WEIGHT REAL,
                $COL_GENDER TEXT,
                $COL_AGE INTEGER,
                $COL_ACTIVITY_LEVEL TEXT
            )
        """.trimIndent()

        val createWeightTable = """
            CREATE TABLE $TABLE_WEIGHT (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_WEIGHT_KG REAL NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_NOTE TEXT,
                $COL_BODY_FAT REAL
            )
        """.trimIndent()

        val createWorkoutTable = """
            CREATE TABLE $TABLE_WORKOUT (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PLAN_TITLE TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_DURATION_SEC INTEGER NOT NULL,
                $COL_CALORIES REAL NOT NULL,
                $COL_EXERCISES_COUNT INTEGER NOT NULL,
                $COL_NOTE TEXT
            )
        """.trimIndent()

        val createRunTable = """
            CREATE TABLE $TABLE_RUN (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_DURATION_SEC INTEGER NOT NULL,
                $COL_DISTANCE_METERS REAL NOT NULL,
                $COL_CALORIES REAL NOT NULL,
                $COL_AVG_PACE_SEC INTEGER NOT NULL,
                $COL_IS_GPS INTEGER NOT NULL,
                $COL_NOTE TEXT
            )
        """.trimIndent()

        val createRoadmapTable = """
            CREATE TABLE $TABLE_ROADMAP (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT NOT NULL,
                $COL_DISTANCE_METERS REAL NOT NULL,
                $COL_DURATION_SEC INTEGER NOT NULL,
                $COL_WAYPOINTS TEXT NOT NULL,
                $COL_ROUTE_COORDS TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL
            )
        """.trimIndent()

        db.execSQL(createProfileTable)
        db.execSQL(createWeightTable)
        db.execSQL(createWorkoutTable)
        db.execSQL(createRunTable)
        db.execSQL(createRoadmapTable)

        // Seed initial default profile and weight
        val initialValues = ContentValues().apply {
            put(COL_ID, 1)
            put(COL_NAME, "Bạn")
            put(COL_HEIGHT, 170.0)
            put(COL_INITIAL_WEIGHT, 75.0)
            put(COL_TARGET_WEIGHT, 65.0)
            put(COL_GENDER, "male")
            put(COL_AGE, 26)
            put(COL_ACTIVITY_LEVEL, "moderate")
        }
        db.insert(TABLE_PROFILE, null, initialValues)

        // Seed initial weight entry
        val initialWeight = ContentValues().apply {
            put(COL_WEIGHT_KG, 75.0)
            put(COL_TIMESTAMP, System.currentTimeMillis() - 7 * 24 * 3600 * 1000L) // 7 days ago
            put(COL_NOTE, "Cân nặng bắt đầu hành trình")
        }
        db.insert(TABLE_WEIGHT, null, initialWeight)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            val createRoadmapTable = """
                CREATE TABLE IF NOT EXISTS $TABLE_ROADMAP (
                    $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_NAME TEXT NOT NULL,
                    $COL_DISTANCE_METERS REAL NOT NULL,
                    $COL_DURATION_SEC INTEGER NOT NULL,
                    $COL_WAYPOINTS TEXT NOT NULL,
                    $COL_ROUTE_COORDS TEXT NOT NULL,
                    $COL_TIMESTAMP INTEGER NOT NULL
                )
            """.trimIndent()
            db.execSQL(createRoadmapTable)
        }
    }

    // PROFILE OPERATIONS
    fun getProfile(): UserProfile {
        val db = readableDatabase
        val cursor = db.query(TABLE_PROFILE, null, "$COL_ID = ?", arrayOf("1"), null, null, null)
        var profile = UserProfile()
        if (cursor.moveToFirst()) {
            profile = UserProfile(
                name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)) ?: "Bạn",
                heightCm = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_HEIGHT)),
                initialWeightKg = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_INITIAL_WEIGHT)),
                targetWeightKg = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TARGET_WEIGHT)),
                gender = cursor.getString(cursor.getColumnIndexOrThrow(COL_GENDER)) ?: "male",
                age = cursor.getInt(cursor.getColumnIndexOrThrow(COL_AGE)),
                activityLevel = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACTIVITY_LEVEL)) ?: "moderate"
            )
        }
        cursor.close()
        return profile
    }

    fun saveProfile(profile: UserProfile) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NAME, profile.name)
            put(COL_HEIGHT, profile.heightCm)
            put(COL_INITIAL_WEIGHT, profile.initialWeightKg)
            put(COL_TARGET_WEIGHT, profile.targetWeightKg)
            put(COL_GENDER, profile.gender)
            put(COL_AGE, profile.age)
            put(COL_ACTIVITY_LEVEL, profile.activityLevel)
        }
        db.update(TABLE_PROFILE, values, "$COL_ID = ?", arrayOf("1"))
    }

    // WEIGHT OPERATIONS
    fun insertWeight(entry: WeightEntry): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_WEIGHT_KG, entry.weightKg)
            put(COL_TIMESTAMP, entry.timestamp)
            put(COL_NOTE, entry.note)
            if (entry.bodyFatPercentage != null) {
                put(COL_BODY_FAT, entry.bodyFatPercentage)
            } else {
                putNull(COL_BODY_FAT)
            }
        }
        return db.insert(TABLE_WEIGHT, null, values)
    }

    fun getAllWeights(): List<WeightEntry> {
        val list = mutableListOf<WeightEntry>()
        val db = readableDatabase
        val cursor = db.query(TABLE_WEIGHT, null, null, null, null, null, "$COL_TIMESTAMP DESC")
        if (cursor.moveToFirst()) {
            do {
                val bodyFat = if (!cursor.isNull(cursor.getColumnIndexOrThrow(COL_BODY_FAT))) {
                    cursor.getDouble(cursor.getColumnIndexOrThrow(COL_BODY_FAT))
                } else null

                list.add(
                    WeightEntry(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                        weightKg = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_WEIGHT_KG)),
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP)),
                        note = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTE)) ?: "",
                        bodyFatPercentage = bodyFat
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deleteWeight(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_WEIGHT, "$COL_ID = ?", arrayOf(id.toString()))
    }

    // WORKOUT OPERATIONS
    fun insertWorkout(session: WorkoutSession): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_PLAN_TITLE, session.planTitle)
            put(COL_TIMESTAMP, session.timestamp)
            put(COL_DURATION_SEC, session.durationSeconds)
            put(COL_CALORIES, session.caloriesBurned)
            put(COL_EXERCISES_COUNT, session.completedExercisesCount)
            put(COL_NOTE, session.note)
        }
        return db.insert(TABLE_WORKOUT, null, values)
    }

    fun getAllWorkouts(): List<WorkoutSession> {
        val list = mutableListOf<WorkoutSession>()
        val db = readableDatabase
        val cursor = db.query(TABLE_WORKOUT, null, null, null, null, null, "$COL_TIMESTAMP DESC")
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    WorkoutSession(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                        planTitle = cursor.getString(cursor.getColumnIndexOrThrow(COL_PLAN_TITLE)),
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP)),
                        durationSeconds = cursor.getInt(cursor.getColumnIndexOrThrow(COL_DURATION_SEC)),
                        caloriesBurned = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_CALORIES)),
                        completedExercisesCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_EXERCISES_COUNT)),
                        note = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTE)) ?: ""
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // RUN OPERATIONS
    fun insertRun(session: RunSession): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TIMESTAMP, session.startTime)
            put(COL_DURATION_SEC, session.durationSeconds)
            put(COL_DISTANCE_METERS, session.distanceMeters)
            put(COL_CALORIES, session.caloriesBurned)
            put(COL_AVG_PACE_SEC, session.averagePaceSecondsPerKm)
            put(COL_IS_GPS, if (session.isGpsTracked) 1 else 0)
            put(COL_NOTE, session.notes)
        }
        return db.insert(TABLE_RUN, null, values)
    }

    fun getAllRuns(): List<RunSession> {
        val list = mutableListOf<RunSession>()
        val db = readableDatabase
        val cursor = db.query(TABLE_RUN, null, null, null, null, null, "$COL_TIMESTAMP DESC")
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    RunSession(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                        startTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP)),
                        durationSeconds = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DURATION_SEC)),
                        distanceMeters = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_DISTANCE_METERS)),
                        caloriesBurned = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_CALORIES)),
                        averagePaceSecondsPerKm = cursor.getLong(cursor.getColumnIndexOrThrow(COL_AVG_PACE_SEC)),
                        isGpsTracked = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_GPS)) == 1,
                        notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTE)) ?: ""
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deleteRun(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_RUN, "$COL_ID = ?", arrayOf(id.toString()))
    }

    // ROADMAP OPERATIONS
    fun insertRoadmap(roadmap: Roadmap): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NAME, roadmap.name)
            put(COL_DISTANCE_METERS, roadmap.distanceMeters)
            put(COL_DURATION_SEC, roadmap.estimatedDurationSeconds)
            put(COL_WAYPOINTS, roadmap.waypointsToJson())
            put(COL_ROUTE_COORDS, roadmap.routeCoordinatesToJson())
            put(COL_TIMESTAMP, roadmap.createdAt)
        }
        return db.insert(TABLE_ROADMAP, null, values)
    }

    fun getAllRoadmaps(): List<Roadmap> {
        val list = mutableListOf<Roadmap>()
        val db = readableDatabase
        val cursor = db.query(TABLE_ROADMAP, null, null, null, null, null, "$COL_TIMESTAMP DESC")
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    Roadmap(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                        distanceMeters = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_DISTANCE_METERS)),
                        estimatedDurationSeconds = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DURATION_SEC)),
                        waypoints = Roadmap.parseCoordsJson(cursor.getString(cursor.getColumnIndexOrThrow(COL_WAYPOINTS))),
                        routeCoordinates = Roadmap.parseCoordsJson(cursor.getString(cursor.getColumnIndexOrThrow(COL_ROUTE_COORDS))),
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deleteRoadmap(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_ROADMAP, "$COL_ID = ?", arrayOf(id.toString()))
    }
}
