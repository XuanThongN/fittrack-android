package com.fittrack.app.data.db

import android.content.Context
import com.fittrack.app.data.model.RunSession
import com.fittrack.app.data.model.UserProfile
import com.fittrack.app.data.model.WeightEntry
import com.fittrack.app.data.model.WorkoutSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class FitTrackRepository(context: Context) {
    private val dbHelper = FitTrackDatabaseHelper(context.applicationContext)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _weightEntries = MutableStateFlow<List<WeightEntry>>(emptyList())
    val weightEntries: StateFlow<List<WeightEntry>> = _weightEntries.asStateFlow()

    private val _workoutSessions = MutableStateFlow<List<WorkoutSession>>(emptyList())
    val workoutSessions: StateFlow<List<WorkoutSession>> = _workoutSessions.asStateFlow()

    private val _runSessions = MutableStateFlow<List<RunSession>>(emptyList())
    val runSessions: StateFlow<List<RunSession>> = _runSessions.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        scope.launch {
            val profile = dbHelper.getProfile()
            val weights = dbHelper.getAllWeights()
            val workouts = dbHelper.getAllWorkouts()
            val runs = dbHelper.getAllRuns()

            _userProfile.value = profile
            _weightEntries.value = weights
            _workoutSessions.value = workouts
            _runSessions.value = runs
        }
    }

    suspend fun saveProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        dbHelper.saveProfile(profile)
        _userProfile.value = profile
    }

    suspend fun addWeight(weightKg: Double, note: String = "", bodyFat: Double? = null) = withContext(Dispatchers.IO) {
        val entry = WeightEntry(
            weightKg = weightKg,
            timestamp = System.currentTimeMillis(),
            note = note,
            bodyFatPercentage = bodyFat
        )
        dbHelper.insertWeight(entry)
        _weightEntries.value = dbHelper.getAllWeights()
    }

    suspend fun deleteWeight(id: Long) = withContext(Dispatchers.IO) {
        dbHelper.deleteWeight(id)
        _weightEntries.value = dbHelper.getAllWeights()
    }

    suspend fun addWorkoutSession(session: WorkoutSession) = withContext(Dispatchers.IO) {
        dbHelper.insertWorkout(session)
        _workoutSessions.value = dbHelper.getAllWorkouts()
    }

    suspend fun addRunSession(session: RunSession) = withContext(Dispatchers.IO) {
        dbHelper.insertRun(session)
        _runSessions.value = dbHelper.getAllRuns()
    }

    // STATISTICAL HELPERS
    fun getLatestWeight(): Double {
        return _weightEntries.value.firstOrNull()?.weightKg ?: _userProfile.value.initialWeightKg
    }

    fun getTodayCaloriesBurned(): Double {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val workoutCal = _workoutSessions.value
            .filter { it.timestamp >= todayStart }
            .sumOf { it.caloriesBurned }

        val runCal = _runSessions.value
            .filter { it.startTime >= todayStart }
            .sumOf { it.caloriesBurned }

        return workoutCal + runCal
    }

    fun getTotalKmRun(): Double {
        return _runSessions.value.sumOf { it.distanceKm }
    }

    fun getTotalWorkoutMinutes(): Int {
        return _workoutSessions.value.sumOf { it.durationSeconds } / 60
    }
}
