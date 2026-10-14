package com.fittrack.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.app.data.db.FitTrackRepository
import com.fittrack.app.data.model.WorkoutPlan
import com.fittrack.app.service.RunningSessionManager
import com.fittrack.app.ui.screens.DashboardScreen
import com.fittrack.app.ui.screens.ProfileScreen
import com.fittrack.app.ui.screens.RunScreen
import com.fittrack.app.ui.screens.WeightScreen
import com.fittrack.app.ui.screens.WorkoutScreen
import com.fittrack.app.ui.screens.WorkoutSessionPlayerScreen
import com.fittrack.app.ui.theme.CardSurface
import com.fittrack.app.ui.theme.DarkBackground
import com.fittrack.app.ui.theme.DangerRed
import com.fittrack.app.ui.theme.EmeraldPrimary
import com.fittrack.app.ui.theme.TextMuted
import com.fittrack.app.ui.theme.TextPrimary
import com.fittrack.app.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

enum class Screen(val title: String, val icon: ImageVector) {
    DASHBOARD("Tổng quan", Icons.Default.Dashboard),
    WEIGHT("Cân nặng", Icons.Default.MonitorWeight),
    WORKOUT("Workout", Icons.Default.FitnessCenter),
    RUN("Chạy bộ", Icons.Default.DirectionsRun),
    PROFILE("Hồ sơ", Icons.Default.Person)
}

@Composable
fun FitTrackApp(
    repository: FitTrackRepository,
    initialScreen: Screen = Screen.DASHBOARD
) {
    var currentScreen by remember { mutableStateOf(initialScreen) }
    var activeWorkoutPlan by remember { mutableStateOf<WorkoutPlan?>(null) }
    var showAddWeightDialog by remember { mutableStateOf(false) }

    val weights by repository.weightEntries.collectAsState()
    val latestWeight = weights.firstOrNull()?.weightKg ?: 70.0

    // Monitor global running session state
    val isRunActive by RunningSessionManager.isRunning.collectAsState()
    val runElapsedSec by RunningSessionManager.elapsedSeconds.collectAsState()

    // FULL SCREEN WORKOUT PLAYER OVERLAY
    if (activeWorkoutPlan != null) {
        WorkoutSessionPlayerScreen(
            plan = activeWorkoutPlan!!,
            repository = repository,
            onClose = { activeWorkoutPlan = null }
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = CardSurface,
                contentColor = TextPrimary,
                tonalElevation = 8.dp
            ) {
                Screen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    val isThisRunScreen = screen == Screen.RUN

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (isThisRunScreen && isRunActive) {
                                        Badge(
                                            containerColor = DangerRed,
                                            contentColor = Color.White
                                        ) {
                                            Text(text = "LIVE", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            }
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkBackground,
                            selectedTextColor = if (isThisRunScreen && isRunActive) DangerRed else EmeraldPrimary,
                            indicatorColor = if (isThisRunScreen && isRunActive) DangerRed else EmeraldPrimary,
                            unselectedIconColor = if (isThisRunScreen && isRunActive) DangerRed else TextMuted,
                            unselectedTextColor = if (isThisRunScreen && isRunActive) DangerRed else TextMuted
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Persistent Active Run Banner (Visible on other tabs so user never loses track!)
                if (isRunActive && currentScreen != Screen.RUN) {
                    val distKm = RunningSessionManager.getTotalDistanceMeters() / 1000.0
                    val timeStr = String.format(Locale.US, "%02d:%02d", runElapsedSec / 60, runElapsedSec % 60)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DangerRed.copy(alpha = 0.95f))
                            .clickable { currentScreen = Screen.RUN }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🏃", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Buổi chạy đang tiếp tục: ${String.format(Locale.US, "%.2f km", distKm)} • $timeStr",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Mở lại ➔",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (currentScreen) {
                        Screen.DASHBOARD -> DashboardScreen(
                            repository = repository,
                            onNavigateToWeight = { currentScreen = Screen.WEIGHT },
                            onNavigateToWorkout = { currentScreen = Screen.WORKOUT },
                            onNavigateToRun = { currentScreen = Screen.RUN },
                            onOpenAddWeightDialog = { showAddWeightDialog = true }
                        )
                        Screen.WEIGHT -> WeightScreen(
                            repository = repository,
                            onOpenAddWeightDialog = { showAddWeightDialog = true }
                        )
                        Screen.WORKOUT -> WorkoutScreen(
                            repository = repository,
                            onStartWorkout = { plan -> activeWorkoutPlan = plan }
                        )
                        Screen.RUN -> RunScreen(
                            repository = repository
                        )
                        Screen.PROFILE -> ProfileScreen(
                            repository = repository
                        )
                    }
                }
            }
        }
    }

    // GLOBAL ADD WEIGHT DIALOG
    if (showAddWeightDialog) {
        var weightInput by remember { mutableStateOf(latestWeight.toString()) }
        var noteInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddWeightDialog = false },
            title = {
                Text(
                    text = "Ghi Nhận Cân Nặng Hôm Nay",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Nhập số cân nặng mới nhất để cập nhật tiến độ và chỉ số BMI:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Cân nặng (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Ghi chú (Tùy chọn)") },
                        placeholder = { Text("Ví dụ: Đo lúc sáng sớm sau khi ngủ dậy") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val w = weightInput.toDoubleOrNull()
                        if (w != null && w in 30.0..300.0) {
                            CoroutineScope(Dispatchers.IO).launch {
                                repository.addWeight(
                                    weightKg = w,
                                    note = noteInput.ifBlank { "Cập nhật nhanh" }
                                )
                            }
                            showAddWeightDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Lưu Cân Nặng", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWeightDialog = false }) {
                    Text("Hủy", color = TextMuted)
                }
            }
        )
    }
}
