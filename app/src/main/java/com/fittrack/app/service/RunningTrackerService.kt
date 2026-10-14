package com.fittrack.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.fittrack.app.MainActivity
import com.fittrack.app.R
import com.fittrack.app.data.model.ZombieAlertLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class RunningTrackerService : Service() {

    companion object {
        const val CHANNEL_ID = "fittrack_running_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        observeSessionState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> RunningSessionManager.pauseRun(this)
            ACTION_RESUME -> RunningSessionManager.resumeRun(this)
            ACTION_STOP -> RunningSessionManager.requestStop()
            else -> {
                val notification = buildNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }
        return START_STICKY
    }

    private fun observeSessionState() {
        serviceScope.launch {
            RunningSessionManager.sessionUpdateTrigger.collectLatest {
                updateNotification()
            }
        }
    }

    private fun updateNotification() {
        if (!RunningSessionManager.isRunning.value) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val isPaused = RunningSessionManager.isPaused.value
        val elapsedSec = RunningSessionManager.elapsedSeconds.value
        val distM = RunningSessionManager.getTotalDistanceMeters()
        val distKm = distM / 1000.0
        val zombie = RunningSessionManager.zombieState.value

        val timeStr = String.format(Locale.US, "%02d:%02d", elapsedSec / 60, elapsedSec % 60)
        val speedKmh = if (elapsedSec > 2 && distKm > 0.01) distKm / (elapsedSec / 3600.0) else 0.0

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "RUN")
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = Intent(this, RunningTrackerService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pauseResumePendingIntent = PendingIntent.getService(
            this, 1, pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (zombie.isEnabled) {
            when (zombie.alertLevel) {
                ZombieAlertLevel.DANGER -> "⚠️ NGUY HIỂM! Zombie còn ${zombie.distanceMeters.toInt()}m!"
                ZombieAlertLevel.CAUGHT -> "💀 BỊ BẮT! Bứt tốc chạy ngay!"
                ZombieAlertLevel.WARNING -> "🧟 Zombie đang đuổi: ${zombie.distanceMeters.toInt()}m"
                ZombieAlertLevel.SAFE -> "🛡️ Sinh Tồn Zombie: ${zombie.distanceMeters.toInt()}m"
            }
        } else {
            if (isPaused) "⏸️ Đang Tạm Dừng Chạy Bộ" else "🏃 Đang Chạy Bộ (FitTrack)"
        }

        val contentText = String.format(
            Locale.US,
            "%.2f km • %s • %.1f km/h",
            distKm, timeStr, speedKmh
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_run_notification)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(!isPaused)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (isPaused) "Tiếp tục" else "Tạm dừng",
                pauseResumePendingIntent
            )

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FitTrack Hoạt Động Chạy Bộ",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hiển thị tiến độ chạy bộ và cảnh báo Zombie theo thời gian thực"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "FitTrack::RunningTrackerWakeLock"
            )?.apply {
                acquire(4 * 3600 * 1000L) // 4 hours timeout max
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
