package com.fittrack.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.fittrack.app.data.db.FitTrackRepository
import com.fittrack.app.service.RunningSessionManager
import com.fittrack.app.ui.FitTrackApp
import com.fittrack.app.ui.Screen
import com.fittrack.app.ui.theme.FitTrackTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var repository: FitTrackRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = FitTrackRepository(this)

        val target = intent?.getStringExtra("navigate_to")
        val initialScreen = if (target == "RUN") Screen.RUN else Screen.DASHBOARD

        // Enable viewing live map and tracking on lock screen
        setupLockScreenAndDisplayFlags()

        setContent {
            FitTrackTheme {
                FitTrackApp(repository = repository, initialScreen = initialScreen)
            }
        }
    }

    private fun setupLockScreenAndDisplayFlags() {
        // Show over lock screen when running
        lifecycleScope.launch {
            RunningSessionManager.isRunning.collectLatest { isRunning ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    setShowWhenLocked(isRunning)
                    setTurnScreenOn(isRunning)
                } else {
                    @Suppress("DEPRECATION")
                    if (isRunning) {
                        window.addFlags(
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        )
                    } else {
                        window.clearFlags(
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        )
                    }
                }
            }
        }

        // Keep screen on when requested by runner
        lifecycleScope.launch {
            combine(RunningSessionManager.isRunning, RunningSessionManager.keepScreenOn) { isRunning, keepOn ->
                isRunning && keepOn
            }.collectLatest { shouldKeepOn ->
                if (shouldKeepOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
