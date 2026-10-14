package com.fittrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fittrack.app.data.db.FitTrackRepository
import com.fittrack.app.ui.FitTrackApp
import com.fittrack.app.ui.theme.FitTrackTheme

class MainActivity : ComponentActivity() {
    private lateinit var repository: FitTrackRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = FitTrackRepository(this)

        val target = intent?.getStringExtra("navigate_to")
        val initialScreen = if (target == "RUN") com.fittrack.app.ui.Screen.RUN else com.fittrack.app.ui.Screen.DASHBOARD

        setContent {
            FitTrackTheme {
                FitTrackApp(repository = repository, initialScreen = initialScreen)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
