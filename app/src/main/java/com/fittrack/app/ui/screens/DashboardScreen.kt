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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.app.data.db.FitTrackRepository
import com.fittrack.app.ui.components.StatCard
import com.fittrack.app.ui.components.TargetProgressCard
import com.fittrack.app.ui.components.WeeklyCaloriesChart
import com.fittrack.app.ui.components.WeightTrendChart
import com.fittrack.app.ui.theme.CardSurface
import com.fittrack.app.ui.theme.CyanRun
import com.fittrack.app.ui.theme.DarkBackground
import com.fittrack.app.ui.theme.EmeraldPrimary
import com.fittrack.app.ui.theme.OrangeBurn
import com.fittrack.app.ui.theme.TextMuted
import com.fittrack.app.ui.theme.TextPrimary
import com.fittrack.app.ui.theme.TextSecondary
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
    val bmi = profile.calculateBmi(currentWeight)
    val bmiCategory = profile.getBmiCategory(bmi)
    val todayCalories = repository.getTodayCaloriesBurned()
    val totalKm = repository.getTotalKmRun()
    val workoutMins = repository.getTotalWorkoutMinutes()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // HEADER GREETING
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
                    text = "Hôm nay hãy cùng đốt mỡ và giảm cân nhé!",
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

        // WEIGHT & BMI OVERVIEW CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "CÂN NẶNG HIỆN TẠI",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(Locale.US, "%.1f", currentWeight),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = " kg",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    // BMI BADGE
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(bmiCategory.colorHex).copy(alpha = 0.2f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "BMI $bmi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(bmiCategory.colorHex)
                            )
                            Text(
                                text = bmiCategory.label,
                                fontSize = 11.sp,
                                color = Color(bmiCategory.colorHex)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // TARGET PROGRESS
                TargetProgressCard(
                    currentWeightKg = currentWeight,
                    profile = profile
                )
            }
        }

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

        // BMR & DEFICIT NUTRITION ADVICE
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
                    text = "• BMR (Năng lượng cơ bản khi ngủ nghỉ): ${bmr.toInt()} kcal/ngày\n" +
                            "• TDEE (Tổng năng lượng tiêu thụ thực tế): ${tdee.toInt()} kcal/ngày\n" +
                            "• Nạp đề xuất: ~${targetIn.toInt()} kcal/ngày (để giảm 0.5kg mỡ/tuần an toàn).",
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
}
