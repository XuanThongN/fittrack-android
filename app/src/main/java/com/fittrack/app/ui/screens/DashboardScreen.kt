package com.fittrack.app.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.app.data.db.FitTrackRepository
import com.fittrack.app.ui.components.BmiGaugeCard
import com.fittrack.app.ui.components.StatCard
import com.fittrack.app.ui.components.TargetProgressCard
import com.fittrack.app.ui.components.WaterTrackerCard
import com.fittrack.app.ui.components.WeeklyCaloriesChart
import com.fittrack.app.ui.components.WeightTrendChart
import com.fittrack.app.ui.theme.CardSurface
import com.fittrack.app.ui.theme.CyanRun
import com.fittrack.app.ui.theme.DarkBackground
import com.fittrack.app.ui.theme.EmeraldPrimary
import com.fittrack.app.ui.theme.OrangeBurn
import com.fittrack.app.ui.theme.TextPrimary
import com.fittrack.app.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun DashboardScreen(
    repository: FitTrackRepository,
    onNavigateToWeight: () -> Unit,
    onNavigateToWorkout: () -> Unit,
    onNavigateToRun: () -> Unit,
    onOpenAddWeightDialog: () -> Unit
) {
    val profile by repository.userProfile.collectAsState()
    val weights by repository.weightEntries.collectAsState()
    val workouts by repository.workoutSessions.collectAsState()
    val runs by repository.runSessions.collectAsState()

    val currentWeight = weights.firstOrNull()?.weightKg ?: profile.initialWeightKg
    val todayCalories = repository.getTodayCaloriesBurned()
    val totalKm = repository.getTotalKmRun()
    val workoutMins = repository.getTotalWorkoutMinutes()

    var showQuickSetupDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // GREETING HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Xin chào ${profile.name}! 👋",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Lộ trình giảm cân & tập luyện hôm nay",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(CardSurface)
                    .clickable { onOpenAddWeightDialog() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Ghi cân nặng",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ADVANCED BMI & BODY METRICS GAUGE CARD
        BmiGaugeCard(
            currentWeightKg = currentWeight,
            profile = profile,
            onEditProfile = { showQuickSetupDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // TARGET PROGRESS CARD
        TargetProgressCard(
            currentWeightKg = currentWeight,
            profile = profile
        )

        Spacer(modifier = Modifier.height(16.dp))

        // QUICK STATS ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Calo đốt hôm nay",
                value = "${todayCalories.toInt()}",
                unit = "kcal",
                icon = Icons.Default.LocalFireDepartment,
                iconTint = OrangeBurn,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Chạy bộ",
                value = String.format(Locale.US, "%.1f", totalKm),
                unit = "km",
                icon = Icons.Default.DirectionsRun,
                iconTint = CyanRun,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Workout",
                value = "$workoutMins",
                unit = "phút",
                icon = Icons.Default.FitnessCenter,
                iconTint = EmeraldPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DAILY WATER TRACKER
        val goalWater = (currentWeight * 35).toInt().coerceAtLeast(1500)
        WaterTrackerCard(goalMl = goalWater)

        Spacer(modifier = Modifier.height(20.dp))

        // WEIGHT TREND CHART SECTION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Xu hướng cân nặng",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Xem chi tiết",
                style = MaterialTheme.typography.bodyMedium,
                color = EmeraldPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onNavigateToWeight() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            WeightTrendChart(
                weights = weights,
                targetWeightKg = profile.targetWeightKg
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // WEEKLY BURN SECTION
        Text(
            text = "Calo tiêu hao 7 ngày qua",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            WeeklyCaloriesChart(
                workouts = workouts,
                runs = runs
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // NUTRITION & CALORIE DEFICIT ADVICE
        val bmr = profile.calculateBmr(currentWeight)
        val tdee = profile.calculateTdee(currentWeight)
        val targetIn = profile.getWeightLossTargetCalories(currentWeight)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = OrangeBurn,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lời khuyên dinh dưỡng giảm mỡ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "• BMR (Năng lượng cơ bản duy trì sự sống): ${bmr.toInt()} kcal/ngày\n" +
                            "• TDEE (Tổng năng lượng tiêu thụ thực tế): ${tdee.toInt()} kcal/ngày\n" +
                            "• Nạp đề xuất: ~${targetIn.toInt()} kcal/ngày (thâm hụt 500 kcal/ngày để giảm đều 0.5kg mỡ/tuần an toàn).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // QUICK ACTION BUTTONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNavigateToWorkout,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = null, tint = DarkBackground)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Tập Workout", color = DarkBackground, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onNavigateToRun,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanRun)
            ) {
                Icon(imageVector = Icons.Default.DirectionsRun, contentDescription = null, tint = DarkBackground)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Chạy bộ", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // QUICK SETUP DIALOG FOR HEIGHT, CURRENT WEIGHT & TARGET WEIGHT
    if (showQuickSetupDialog) {
        var inputHeight by remember { mutableStateOf(profile.heightCm.toInt().toString()) }
        var inputWeight by remember { mutableStateOf(String.format(Locale.US, "%.1f", currentWeight)) }
        var inputTarget by remember { mutableStateOf(profile.targetWeightKg.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { showQuickSetupDialog = false },
            containerColor = CardSurface,
            title = {
                Text(
                    text = "Cập Nhật Chiều Cao & Cân Nặng",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Nhập chính xác để app tính toán BMI, BMR và lượng calo chuẩn nhất:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = inputHeight,
                        onValueChange = { inputHeight = it },
                        label = { Text("Chiều cao (cm) *") },
                        placeholder = { Text("Ví dụ: 170") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputWeight,
                        onValueChange = { inputWeight = it },
                        label = { Text("Cân nặng hiện tại (kg) *") },
                        placeholder = { Text("Ví dụ: 72.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputTarget,
                        onValueChange = { inputTarget = it },
                        label = { Text("Cân nặng mục tiêu (kg) *") },
                        placeholder = { Text("Ví dụ: 65") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val h = inputHeight.toDoubleOrNull() ?: profile.heightCm
                        val w = inputWeight.toDoubleOrNull() ?: currentWeight
                        val tgt = inputTarget.toDoubleOrNull() ?: profile.targetWeightKg

                        val updatedProfile = profile.copy(
                            heightCm = h,
                            targetWeightKg = tgt
                        )
                        CoroutineScope(Dispatchers.IO).launch {
                            repository.saveProfile(updatedProfile)
                            repository.addWeight(weightKg = w, note = "Cập nhật thể trạng")
                        }
                        showQuickSetupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(text = "Lưu Chỉ Số", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickSetupDialog = false }) {
                    Text(text = "Đóng", color = TextSecondary)
                }
            }
        )
    }
}
