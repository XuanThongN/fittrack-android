package com.fittrack.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fittrack.app.data.db.FitTrackRepository
import com.fittrack.app.data.model.WorkoutPlan
import com.fittrack.app.ui.screens.DashboardScreen
import com.fittrack.app.ui.screens.ProfileScreen
import com.fittrack.app.ui.screens.RunScreen
import com.fittrack.app.ui.screens.WeightScreen
import com.fittrack.app.ui.screens.WorkoutScreen
import com.fittrack.app.ui.screens.WorkoutSessionPlayerScreen
import com.fittrack.app.ui.theme.CardSurface
import com.fittrack.app.ui.theme.DarkBackground
import com.fittrack.app.ui.theme.EmeraldPrimary
import com.fittrack.app.ui.theme.TextMuted
import com.fittrack.app.ui.theme.TextPrimary
import com.fittrack.app.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class Screen(val title: String, val icon: ImageVector) {
    DASHBOARD("Tổng quan", Icons.Default.Dashboard),
    WEIGHT("Cân nặng", Icons.Default.MonitorWeight),
    WORKOUT("Workout", Icons.Default.FitnessCenter),
    RUN("Chạy bộ", Icons.Default.DirectionsRun),
    PROFILE("Hồ sơ", Icons.Default.Person)
}

@Composable
fun FitTrackApp(repository: FitTrackRepository) {
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    var activeWorkoutPlan by remember { mutableStateOf<WorkoutPlan?>(null) }
    var showAddWeightDialog by remember { mutableStateOf(false) }

    val weights by repository.weightEntries.collectAsState()
    val latestWeight = weights.firstOrNull()?.weightKg ?: 70.0

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
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkBackground,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
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

    // GLOBAL ADD WEIGHT DIALOG
    if (showAddWeightDialog) {
        var inputWeight by remember { mutableStateOf(String.format("%.1f", latestWeight).replace(',', '.')) }
        var inputNote by remember { mutableStateOf("") }
        var inputBodyFat by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddWeightDialog = false },
            containerColor = CardSurface,
            title = {
                Text(
                    text = "Ghi Nhận Cân Nặng",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Hãy ghi cân nặng vào cùng một thời điểm mỗi ngày (ví dụ: buổi sáng sau khi ngủ dậy) để có số liệu chính xác nhất:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = inputWeight,
                        onValueChange = { inputWeight = it },
                        label = { Text("Số cân (kg) *") },
                        placeholder = { Text("Ví dụ: 68.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputNote,
                        onValueChange = { inputNote = it },
                        label = { Text("Ghi chú (tùy chọn)") },
                        placeholder = { Text("Ví dụ: Buổi sáng trước khi ăn") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputBodyFat,
                        onValueChange = { inputBodyFat = it },
                        label = { Text("% Mỡ cơ thể (tùy chọn)") },
                        placeholder = { Text("Ví dụ: 19.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val w = inputWeight.toDoubleOrNull()
                        if (w != null && w > 20.0 && w < 300.0) {
                            val bf = inputBodyFat.toDoubleOrNull()
                            CoroutineScope(Dispatchers.IO).launch {
                                repository.addWeight(weightKg = w, note = inputNote, bodyFat = bf)
                            }
                            showAddWeightDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(text = "Lưu Cân Nặng", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWeightDialog = false }) {
                    Text(text = "Hủy", color = TextSecondary)
                }
            }
        )
    }
}
