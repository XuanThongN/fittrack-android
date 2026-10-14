package com.fittrack.app.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.fittrack.app.data.db.FitTrackRepository
import com.fittrack.app.data.model.RunSession
import com.fittrack.app.data.model.ZombieAlertLevel
import com.fittrack.app.data.model.ZombieChaseState
import com.fittrack.app.data.model.ZombieDifficulty
import com.fittrack.app.util.StreetChaseEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Global Singleton Manager for Running Workouts.
 * Preserves full session state across tab switches, screen rotation, backgrounding,
 * and lock screen, and synchronizes with RunningTrackerService for Android Ongoing Notifications.
 */
object RunningSessionManager {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var streetFetchJob: Job? = null

    var locationTracker: LocationTracker? = null
        private set
    var voiceCoach: FitTrackVoiceCoach? = null
        private set

    // Run Session State
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _manualDistanceMeters = MutableStateFlow(0.0)
    val manualDistanceMeters: StateFlow<Double> = _manualDistanceMeters.asStateFlow()

    private val _mapMode = MutableStateFlow("street")
    val mapMode: StateFlow<String> = _mapMode.asStateFlow()

    private val _zombieState = MutableStateFlow(ZombieChaseState(isEnabled = true, distanceMeters = 80.0))
    val zombieState: StateFlow<ZombieChaseState> = _zombieState.asStateFlow()

    private val _streetPrefix = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val streetPrefix: StateFlow<List<Pair<Double, Double>>> = _streetPrefix.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(true)
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    // Triggers for notification updates and UI event callbacks
    private val _sessionUpdateTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionUpdateTrigger: SharedFlow<Unit> = _sessionUpdateTrigger.asSharedFlow()

    private val _stopRequested = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val stopRequested: SharedFlow<Unit> = _stopRequested.asSharedFlow()

    private val _lastFinishedRun = MutableStateFlow<RunSession?>(null)
    val lastFinishedRun: StateFlow<RunSession?> = _lastFinishedRun.asStateFlow()

    private var lastZombieWarningSec = 0L
    private var lastAnnouncedKm = 0

    fun clearLastFinishedRun() {
        _lastFinishedRun.value = null
    }

    fun initTracker(context: Context) {
        val appContext = context.applicationContext
        if (locationTracker == null) {
            locationTracker = LocationTracker(appContext).apply {
                if (hasPermission()) {
                    startListening()
                }
            }
        }
        if (voiceCoach == null) {
            voiceCoach = FitTrackVoiceCoach(appContext).apply {
                isMuted = _isMuted.value
            }
        }
        fetchStreetPrefixIfAvailable()
    }

    fun startRun(context: Context, isZombieMode: Boolean = true, difficulty: ZombieDifficulty = ZombieDifficulty.RUNNER) {
        initTracker(context)
        val appContext = context.applicationContext

        _isRunning.value = true
        _isPaused.value = false
        _elapsedSeconds.value = 0L
        _manualDistanceMeters.value = 0.0
        lastZombieWarningSec = 0L
        lastAnnouncedKm = 0

        _zombieState.value = ZombieChaseState(
            isEnabled = isZombieMode,
            difficulty = difficulty,
            distanceMeters = 80.0,
            alertLevel = ZombieAlertLevel.SAFE
        )

        locationTracker?.reset()
        if (locationTracker?.hasPermission() == true) {
            locationTracker?.startTracking()
        }
        fetchStreetPrefixIfAvailable()

        // Start Android Foreground Service with live notification
        val serviceIntent = Intent(appContext, RunningTrackerService::class.java).apply {
            action = RunningTrackerService.ACTION_START
        }
        ContextCompat.startForegroundService(appContext, serviceIntent)

        if (isZombieMode) {
            voiceCoach?.speakZombieStart(difficulty.title)
        }

        // Trigger pre-fetch for real OpenStreetMap street path
        fetchStreetPrefixIfAvailable()

        // Launch persistent 1-second workout ticker loop
        timerJob?.cancel()
        timerJob = scope.launch {
            var prevDistance = getTotalDistanceMeters()

            while (isActive && _isRunning.value) {
                delay(1000L)

                if (!_isPaused.value) {
                    _elapsedSeconds.value++

                    val currentTotalDist = getTotalDistanceMeters()

                    // Voice Coach KM milestone announcement
                    val currentKm = (currentTotalDist / 1000.0).toInt()
                    if (currentKm > lastAnnouncedKm && currentKm > 0) {
                        lastAnnouncedKm = currentKm
                        val paceSec = if (currentTotalDist > 0) (_elapsedSeconds.value / (currentTotalDist / 1000.0)).toLong() else 0L
                        val repo = FitTrackRepository.getInstance(appContext)
                        val cal = (repo.getLatestWeight() * (currentTotalDist / 1000.0) * 1.036).toInt()
                        voiceCoach?.speakKmMilestone(currentKm, paceSec, cal)
                    }

                    // Periodic real street route refresh every 15s
                    if (_elapsedSeconds.value % 15L == 0L) {
                        fetchStreetPrefixIfAvailable()
                    }

                    val zState = _zombieState.value

                    if (zState.isEnabled) {
                        val runnerDistDelta = (currentTotalDist - prevDistance).coerceAtLeast(0.0)
                        prevDistance = currentTotalDist

                        val zombieSpeedMps = (zState.difficulty.speedKmh * 1000.0) / 3600.0
                        val netDistDelta = runnerDistDelta - zombieSpeedMps
                        val updatedDist = (zState.distanceMeters + netDistDelta).coerceIn(0.0, 150.0)

                        val newAlertLevel = when {
                            updatedDist <= 0.0 -> ZombieAlertLevel.CAUGHT
                            updatedDist < 30.0 -> ZombieAlertLevel.DANGER
                            updatedDist < 60.0 -> ZombieAlertLevel.WARNING
                            else -> ZombieAlertLevel.SAFE
                        }

                        if (newAlertLevel == ZombieAlertLevel.DANGER && _elapsedSeconds.value - lastZombieWarningSec > 6) {
                            triggerHaptic(appContext, 400)
                            voiceCoach?.speakZombieApproaching(updatedDist.toInt())
                            lastZombieWarningSec = _elapsedSeconds.value
                        } else if (newAlertLevel == ZombieAlertLevel.CAUGHT) {
                            triggerHaptic(appContext, 800)
                            voiceCoach?.speakZombieCaught()
                            _zombieState.value = zState.copy(
                                distanceMeters = 45.0,
                                alertLevel = ZombieAlertLevel.DANGER,
                                caughtCount = zState.caughtCount + 1
                            )
                        } else {
                            _zombieState.value = zState.copy(
                                distanceMeters = updatedDist,
                                alertLevel = newAlertLevel
                            )
                        }
                    } else {
                        prevDistance = currentTotalDist
                    }

                    _sessionUpdateTrigger.tryEmit(Unit)
                }
            }
        }
    }

    fun pauseRun(context: Context) {
        _isPaused.value = true
        _sessionUpdateTrigger.tryEmit(Unit)
    }

    fun resumeRun(context: Context) {
        _isPaused.value = false
        _sessionUpdateTrigger.tryEmit(Unit)
    }

    fun stopRun(context: Context) {
        val appContext = context.applicationContext
        timerJob?.cancel()
        timerJob = null

        _isRunning.value = false
        _isPaused.value = false

        locationTracker?.stopTracking()

        val serviceIntent = Intent(appContext, RunningTrackerService::class.java).apply {
            action = RunningTrackerService.ACTION_STOP
        }
        appContext.stopService(serviceIntent)
        _sessionUpdateTrigger.tryEmit(Unit)
    }

    suspend fun stopAndSaveRun(
        context: Context,
        repository: FitTrackRepository,
        weightKg: Double,
        notes: String = ""
    ): RunSession {
        val totalDistM = getTotalDistanceMeters()
        val duration = _elapsedSeconds.value
        val distKm = totalDistM / 1000.0
        val cal = (weightKg * distKm * 1.036).coerceAtLeast(0.0)
        val avgPace = if (distKm > 0.05) (duration / distKm).toLong() else 0L

        val session = RunSession(
            durationSeconds = duration,
            distanceMeters = totalDistM,
            caloriesBurned = cal,
            averagePaceSecondsPerKm = avgPace,
            isGpsTracked = (locationTracker?.distanceMeters?.value ?: 0.0) > 0,
            notes = notes
        )
        repository.addRunSession(session)
        _lastFinishedRun.value = session
        stopRun(context)
        return session
    }

    fun discardRun(context: Context) {
        stopRun(context)
    }

    fun requestStop(context: Context) {
        scope.launch {
            if (_isRunning.value) {
                val repo = FitTrackRepository.getInstance(context)
                val weight = repo.getLatestWeight()
                val z = _zombieState.value
                val notes = if (z.isEnabled) "Chạy sinh tồn Zombie (${z.caughtCount} lần bị bắt)" else "Chạy bộ ngoài trời"
                stopAndSaveRun(context, repo, weight, notes)
            }
        }
    }

    fun requestStop() {
        _stopRequested.tryEmit(Unit)
    }

    fun addManualDistance(meters: Double) {
        _manualDistanceMeters.value += meters
        _sessionUpdateTrigger.tryEmit(Unit)
    }

    fun setMapMode(mode: String) {
        _mapMode.value = mode
    }

    fun setZombieEnabled(enabled: Boolean) {
        _zombieState.value = _zombieState.value.copy(isEnabled = enabled)
    }

    fun setZombieDifficulty(difficulty: ZombieDifficulty) {
        _zombieState.value = _zombieState.value.copy(difficulty = difficulty)
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        voiceCoach?.isMuted = _isMuted.value
    }

    fun toggleKeepScreenOn() {
        _keepScreenOn.value = !_keepScreenOn.value
    }

    fun setKeepScreenOn(enabled: Boolean) {
        _keepScreenOn.value = enabled
    }

    fun getTotalDistanceMeters(): Double {
        val gps = locationTracker?.distanceMeters?.value ?: 0.0
        return if (gps > 0) gps else _manualDistanceMeters.value
    }

    fun fetchStreetPrefixIfAvailable() {
        val cur = locationTracker?.currentLocation?.value ?: return
        val bearing = locationTracker?.currentBearing?.value ?: 0f

        streetFetchJob?.cancel()
        streetFetchJob = scope.launch {
            val nodes = StreetChaseEngine.fetchRealStreetPathBehind(
                lat = cur.first,
                lng = cur.second,
                bearingDeg = bearing,
                distanceMeters = 120.0
            )
            if (nodes.isNotEmpty()) {
                _streetPrefix.value = nodes
            }
        }
    }

    private fun triggerHaptic(context: Context, durationMs: Long = 200) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }
}
