package com.fittrack.app.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.app.data.db.FitTrackRepository
import com.fittrack.app.data.model.WorkoutPlan
import com.fittrack.app.data.model.WorkoutSession
import com.fittrack.app.ui.theme.CardSurface
import com.fittrack.app.ui.theme.DarkBackground
import com.fittrack.app.ui.theme.EmeraldLight
import com.fittrack.app.ui.theme.EmeraldPrimary
import com.fittrack.app.ui.theme.OrangeBurn
import com.fittrack.app.ui.theme.TextMuted
import com.fittrack.app.ui.theme.TextPrimary
import com.fittrack.app.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class WorkoutState {
    EXERCISING,
    RESTING,
    COMPLETED
}

@Composable
fun WorkoutSessionPlayerScreen(
    plan: WorkoutPlan,
    repository: FitTrackRepository,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    var workoutState by remember { mutableStateOf(WorkoutState.EXERCISING) }
    var isPaused by remember { mutableStateOf(false) }

    val currentExercise = plan.exercises.getOrNull(currentExerciseIndex) ?: plan.exercises.first()
    var secondsRemaining by remember(currentExerciseIndex, workoutState) {
        mutableIntStateOf(
            if (workoutState == WorkoutState.EXERCISING) currentExercise.defaultDurationSeconds
            else plan.restBetweenSeconds
        )
    }

    var totalWorkoutSeconds by remember { mutableIntStateOf(0) }
    val totalExercises = plan.exercises.size

    fun triggerVibrate() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(200)
            }
        } catch (_: Exception) {}
    }

    // TIMER LOOP
    LaunchedEffect(isPaused, workoutState, currentExerciseIndex) {
        while (!isPaused && workoutState != WorkoutState.COMPLETED) {
            delay(1000L)
            totalWorkoutSeconds++
            if (secondsRemaining > 1) {
                secondsRemaining--
                if (secondsRemaining <= 3) {
                    triggerVibrate()
                }
            } else {
                // Time's up for current phase
                triggerVibrate()
                if (workoutState == WorkoutState.EXERCISING) {
                    if (currentExerciseIndex < totalExercises - 1) {
                        workoutState = WorkoutState.RESTING
                    } else {
                        // Finished all exercises
                        workoutState = WorkoutState.COMPLETED
                        // Save session to repository
                        val caloriesBurned = (totalWorkoutSeconds / 60.0) * 11.0 // ~11 kcal/min average
                        val session = WorkoutSession(
                            planTitle = plan.title,
                            timestamp = System.currentTimeMillis(),
                            durationSeconds = totalWorkoutSeconds,
                            caloriesBurned = caloriesBurned,
                            completedExercisesCount = totalExercises
                        )
                        CoroutineScope(Dispatchers.IO).launch {
                            repository.addWorkoutSession(session)
                        }
                    }
                } else if (workoutState == WorkoutState.RESTING) {
                    currentExerciseIndex++
                    workoutState = WorkoutState.EXERCISING
                }
            }
        }
    }

    if (workoutState == WorkoutState.COMPLETED) {
        // CELEBRATION SUMMARY
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🎉", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Xuất Sắc! Hoàn Thành!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Bạn vừa hoàn thành buổi tập ${plan.title}!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Thời gian", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            val mins = totalWorkoutSeconds / 60
                            val secs = totalWorkoutSeconds % 60
                            Text(
                                text = "${mins}p ${secs}s",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Calo tiêu hao", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            val cal = (totalWorkoutSeconds / 60.0) * 11.0
                            Text(
                                text = "${cal.toInt()} kcal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = OrangeBurn
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Động tác", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = "$totalExercises/$totalExercises",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = onClose,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text(
                            text = "Lưu & Trở Về",
                            color = DarkBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
        return
    }

    // ACTIVE SESSION SCREEN
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // TOP BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Đóng",
                    tint = TextPrimary
                )
            }

            Text(
                text = plan.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardSurface)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${currentExerciseIndex + 1}/$totalExercises",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // PROGRESS BAR
        val progress = (currentExerciseIndex.toFloat() / totalExercises).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = EmeraldPrimary,
            trackColor = CardSurface
        )

        Spacer(modifier = Modifier.height(24.dp))

        // CENTER EXERCISE OR REST CARD
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (workoutState == WorkoutState.RESTING) {
                val nextExercise = plan.exercises.getOrNull(currentExerciseIndex + 1)
                Text(
                    text = "NGHỈ NGƠI HỒI SỨC",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OrangeBurn
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Chuẩn bị bài tiếp theo: ${nextExercise?.name ?: ""}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = currentExercise.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tác động: ${currentExercise.targetMuscles}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EmeraldLight
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // CIRCULAR COUNTDOWN TIMER
            val maxSeconds = if (workoutState == WorkoutState.EXERCISING) currentExercise.defaultDurationSeconds else plan.restBetweenSeconds
            val ringProgress = (secondsRemaining.toFloat() / maxSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)
            val ringColor = if (workoutState == WorkoutState.EXERCISING) EmeraldPrimary else OrangeBurn

            Box(
                modifier = Modifier.size(230.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { ringProgress },
                    modifier = Modifier.size(230.dp),
                    color = ringColor,
                    strokeWidth = 14.dp,
                    trackColor = CardSurface
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$secondsRemaining",
                        fontSize = 68.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "giây",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            if (workoutState == WorkoutState.EXERCISING) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = currentExercise.instructions,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(14.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // CONTROLS AT BOTTOM
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PAUSE / RESUME BUTTON
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (isPaused) EmeraldPrimary else CardSurface)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = { isPaused = !isPaused }) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Tiếp tục" else "Tạm dừng",
                        tint = if (isPaused) DarkBackground else TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // SKIP BUTTON
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(CardSurface)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        if (workoutState == WorkoutState.RESTING) {
                            currentExerciseIndex++
                            workoutState = WorkoutState.EXERCISING
                        } else {
                            if (currentExerciseIndex < totalExercises - 1) {
                                currentExerciseIndex++
                            } else {
                                workoutState = WorkoutState.COMPLETED
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Bỏ qua",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
