package com.fittrack.app.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.app.data.db.FitTrackRepository
import com.fittrack.app.data.model.RunSession
import com.fittrack.app.data.model.ZombieAlertLevel
import com.fittrack.app.data.model.ZombieChaseState
import com.fittrack.app.data.model.ZombieDifficulty
import com.fittrack.app.service.FitTrackVoiceCoach
import com.fittrack.app.service.LocationTracker
import com.fittrack.app.ui.components.ZombieRadarMap
import com.fittrack.app.ui.theme.CardSurface
import com.fittrack.app.ui.theme.CyanLight
import com.fittrack.app.ui.theme.CyanRun
import com.fittrack.app.ui.theme.DangerRed
import com.fittrack.app.ui.theme.DarkBackground
import com.fittrack.app.ui.theme.EmeraldPrimary
import com.fittrack.app.ui.theme.OrangeBurn
import com.fittrack.app.ui.theme.TextMuted
import com.fittrack.app.ui.theme.TextPrimary
import com.fittrack.app.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun RunScreen(
    repository: FitTrackRepository
) {
    val context = LocalContext.current
    val locationTracker = remember { LocationTracker(context) }
    val voiceCoach = remember { FitTrackVoiceCoach(context) }
    var isMuted by remember { mutableStateOf(false) }

    val profile by repository.userProfile.collectAsState()
    val weights by repository.weightEntries.collectAsState()
    val runs by repository.runSessions.collectAsState()

    val currentWeightKg = weights.firstOrNull()?.weightKg ?: profile.initialWeightKg

    var isRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var manualDistanceMeters by remember { mutableDoubleStateOf(0.0) }

    val gpsDistanceMeters by locationTracker.distanceMeters.collectAsState()
    val pathPoints by locationTracker.pathPoints.collectAsState()
    val currentLocation by locationTracker.currentLocation.collectAsState()

    val totalDistanceMeters = if (gpsDistanceMeters > 0) gpsDistanceMeters else manualDistanceMeters
    val distanceKm = totalDistanceMeters / 1000.0

    // Runner speed calculation
    val runnerSpeedKmh = if (elapsedSeconds > 2 && distanceKm > 0.01) {
        val hrs = elapsedSeconds / 3600.0
        distanceKm / hrs
    } else 0.0

    // Calories: Weight (kg) * Distance (km) * 1.036
    val caloriesBurned = (currentWeightKg * distanceKm * 1.036).coerceAtLeast(0.0)

    // Average Pace: seconds per km
    val avgPaceSecPerKm = if (distanceKm > 0.05) (elapsedSeconds / distanceKm).toLong() else 0L

    var showManualDialog by remember { mutableStateOf(false) }

    // ZOMBIE CHASE STATE
    var zombieState by remember { mutableStateOf(ZombieChaseState(isEnabled = true, distanceMeters = 80.0)) }
    var lastZombieWarningSec by remember { mutableLongStateOf(0L) }

    fun triggerHaptic(durationMs: Long = 200) {
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

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (fineGranted) {
            locationTracker.startTracking()
        }
    }

    // RUNNING & ZOMBIE CHASE LOOP (Runs every second)
    LaunchedEffect(isRunning, isPaused, zombieState.isEnabled) {
        var prevDistance = totalDistanceMeters

        while (isRunning && !isPaused) {
            delay(1000L)
            elapsedSeconds++

            if (zombieState.isEnabled) {
                val runnerDistDelta = (totalDistanceMeters - prevDistance).coerceAtLeast(0.0)
                prevDistance = totalDistanceMeters

                // Zombie speed in meters per second
                val zombieSpeedMps = (zombieState.difficulty.speedKmh * 1000.0) / 3600.0

                // If runner is slower than zombie, distance decreases; if faster, distance increases
                val netDistanceDelta = runnerDistDelta - zombieSpeedMps
                val updatedDist = (zombieState.distanceMeters + netDistanceDelta).coerceIn(0.0, 150.0)

                val newAlertLevel = when {
                    updatedDist <= 0.0 -> ZombieAlertLevel.CAUGHT
                    updatedDist < 30.0 -> ZombieAlertLevel.DANGER
                    updatedDist < 60.0 -> ZombieAlertLevel.WARNING
                    else -> ZombieAlertLevel.SAFE
                }

                // Audio & Vibrate Triggers based on Zombie closeness
                if (newAlertLevel == ZombieAlertLevel.DANGER && elapsedSeconds - lastZombieWarningSec > 6) {
                    triggerHaptic(400)
                    voiceCoach.speakZombieApproaching(updatedDist.toInt())
                    lastZombieWarningSec = elapsedSeconds
                } else if (newAlertLevel == ZombieAlertLevel.CAUGHT) {
                    triggerHaptic(800)
                    voiceCoach.speakZombieCaught()
                    // Push zombie back 45m so the player can sprint away!
                    zombieState = zombieState.copy(
                        distanceMeters = 45.0,
                        alertLevel = ZombieAlertLevel.DANGER,
                        caughtCount = zombieState.caughtCount + 1
                    )
                } else {
                    zombieState = zombieState.copy(
                        distanceMeters = updatedDist,
                        alertLevel = newAlertLevel
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            locationTracker.stopTracking()
            voiceCoach.destroy()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Theo Dõi Chạy Bộ",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (zombieState.isEnabled) "🧟 Chế độ: Sinh Tồn Zombie Rượt Đuổi" else "Đo quãng đường GPS, tốc độ & calo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (zombieState.isEnabled) DangerRed else TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    isMuted = !isMuted
                    voiceCoach.isMuted = isMuted
                }) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Âm thanh",
                        tint = if (isMuted) TextMuted else EmeraldPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (zombieState.isEnabled) DangerRed.copy(alpha = 0.2f) else CyanRun.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = if (zombieState.isEnabled) DangerRed else CyanRun,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 100% RELIABLE HIGH-TECH NATIVE RADAR GPS MAP (With Zombies chasing!)
        ZombieRadarMap(
            pathPoints = pathPoints,
            currentLocation = currentLocation,
            zombieState = zombieState,
            runnerSpeedKmh = runnerSpeedKmh,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ZOMBIE CHASE TOGGLE & DIFFICULTY CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (zombieState.isEnabled && zombieState.alertLevel == ZombieAlertLevel.DANGER)
                    DangerRed.copy(alpha = 0.18f) else CardSurface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🧟", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Chế Độ Zombie Rượt Đuổi",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (zombieState.isEnabled) DangerRed else TextPrimary
                            )
                            Text(
                                text = if (zombieState.isEnabled) "Khoảng cách: ${zombieState.distanceMeters.toInt()}m (Ép không được dừng lại!)"
                                else "Tạo động lực bứt tốc và không bỏ cuộc",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = zombieState.isEnabled,
                        onCheckedChange = { checked ->
                            zombieState = zombieState.copy(isEnabled = checked, distanceMeters = 80.0)
                            if (checked) {
                                voiceCoach.speakZombieStart(zombieState.difficulty.title)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DangerRed,
                            checkedTrackColor = DangerRed.copy(alpha = 0.3f)
                        )
                    )
                }

                // Zombie Distance Danger Progress Bar
                if (zombieState.isEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val distFraction = (zombieState.distanceMeters / 120.0).toFloat().coerceIn(0f, 1f)
                    val barColor = when (zombieState.alertLevel) {
                        ZombieAlertLevel.DANGER -> DangerRed
                        ZombieAlertLevel.WARNING -> OrangeBurn
                        ZombieAlertLevel.SAFE -> EmeraldPrimary
                        ZombieAlertLevel.CAUGHT -> DangerRed
                    }

                    LinearProgressIndicator(
                        progress = { distFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = barColor,
                        trackColor = DarkBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Difficulty selector pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ZombieDifficulty.values().forEach { diff ->
                            val isSelected = zombieState.difficulty == diff
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DangerRed else DarkBackground)
                                    .clickable {
                                        zombieState = zombieState.copy(difficulty = diff)
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${diff.speedKmh} km/h",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // METRICS DASHBOARD CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // DISTANCE DISPLAY
                Text(
                    text = String.format(Locale.US, "%.2f", distanceKm),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CyanLight
                )
                Text(
                    text = "KILOMÉT (KM)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                // STATS ROW (TIME, PACE, CALORIES)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Thời gian", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        val hrs = elapsedSeconds / 3600
                        val mins = (elapsedSeconds % 3600) / 60
                        val secs = elapsedSeconds % 60
                        val timeStr = if (hrs > 0) String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
                        else String.format(Locale.US, "%02d:%02d", mins, secs)
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Pace TB", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        val paceStr = if (avgPaceSecPerKm in 1..3600) {
                            val pm = avgPaceSecPerKm / 60
                            val ps = avgPaceSecPerKm % 60
                            String.format(Locale.US, "%d'%02d\"", pm, ps)
                        } else "--'--\""
                        Text(
                            text = paceStr,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = OrangeBurn, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Calo", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${caloriesBurned.toInt()} kcal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OrangeBurn
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // CONTROLS
                if (!isRunning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                isRunning = true
                                isPaused = false
                                elapsedSeconds = 0L
                                manualDistanceMeters = 0.0
                                zombieState = zombieState.copy(distanceMeters = 80.0)
                                locationTracker.reset()

                                if (zombieState.isEnabled) {
                                    voiceCoach.speakZombieStart(zombieState.difficulty.title)
                                }

                                if (locationTracker.hasPermission()) {
                                    locationTracker.startTracking()
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (zombieState.isEnabled) DangerRed else CyanRun
                            )
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (zombieState.isEnabled) "CHẠY TRỐN ZOMBIE!" else "Bắt đầu chạy GPS",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { showManualDialog = true },
                            modifier = Modifier.height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBackground)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = TextSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Nhập tay", color = TextSecondary)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { isPaused = !isPaused },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPaused) EmeraldPrimary else CardSurface
                            )
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                tint = if (isPaused) DarkBackground else TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPaused) "Tiếp tục" else "Tạm dừng",
                                color = if (isPaused) DarkBackground else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                isRunning = false
                                isPaused = false
                                locationTracker.stopTracking()

                                if (distanceKm > 0.05 || elapsedSeconds > 30) {
                                    val session = RunSession(
                                        startTime = System.currentTimeMillis(),
                                        durationSeconds = elapsedSeconds,
                                        distanceMeters = totalDistanceMeters,
                                        caloriesBurned = caloriesBurned,
                                        averagePaceSecondsPerKm = avgPaceSecPerKm,
                                        isGpsTracked = gpsDistanceMeters > 0,
                                        notes = if (zombieState.isEnabled) "Chạy sinh tồn Zombie (${zombieState.caughtCount} lần bị bắt)" else "Chạy bộ ngoài trời"
                                    )
                                    CoroutineScope(Dispatchers.IO).launch {
                                        repository.addRunSession(session)
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeBurn)
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = DarkBackground)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Kết thúc & Lưu", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // RUN HISTORY LIST
        Text(
            text = "Lịch sử các buổi chạy (${runs.size})",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(runs) { run ->
                RunHistoryItem(run = run)
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // MANUAL ENTRY DIALOG
    if (showManualDialog) {
        var inputKm by remember { mutableStateOf("") }
        var inputMinutes by remember { mutableStateOf("") }
        var inputNote by remember { mutableStateOf("Chạy máy phòng gym") }

        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            containerColor = CardSurface,
            title = {
                Text(
                    text = "Ghi Nhận Buổi Chạy Thủ Công",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Thích hợp khi chạy trên máy chạy bộ (Treadmill) hoặc không dùng GPS:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = inputKm,
                        onValueChange = { inputKm = it },
                        label = { Text("Quãng đường (km) *") },
                        placeholder = { Text("Ví dụ: 5.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputMinutes,
                        onValueChange = { inputMinutes = it },
                        label = { Text("Thời gian (phút) *") },
                        placeholder = { Text("Ví dụ: 30") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputNote,
                        onValueChange = { inputNote = it },
                        label = { Text("Ghi chú") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val km = inputKm.toDoubleOrNull() ?: 0.0
                        val mins = inputMinutes.toLongOrNull() ?: 0L
                        if (km > 0 && mins > 0) {
                            val durationSec = mins * 60
                            val pace = (durationSec / km).toLong()
                            val cal = currentWeightKg * km * 1.036
                            val session = RunSession(
                                startTime = System.currentTimeMillis(),
                                durationSeconds = durationSec,
                                distanceMeters = km * 1000.0,
                                caloriesBurned = cal,
                                averagePaceSecondsPerKm = pace,
                                isGpsTracked = false,
                                notes = inputNote
                            )
                            CoroutineScope(Dispatchers.IO).launch {
                                repository.addRunSession(session)
                            }
                            showManualDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanRun)
                ) {
                    Text("Lưu buổi chạy", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) {
                    Text("Hủy", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun RunHistoryItem(run: RunSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (run.notes.contains("Zombie")) DangerRed.copy(alpha = 0.15f) else CyanRun.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (run.notes.contains("Zombie")) "🧟" else "🏃",
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = run.distanceKmFormatted(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = run.durationFormatted(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    Text(
                        text = "${run.getFormattedDate()} • Pace: ${run.paceFormatted()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = run.caloriesFormatted(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OrangeBurn
                )
                Text(
                    text = if (run.notes.contains("Zombie")) "Zombie Run" else if (run.isGpsTracked) "GPS Run" else "Treadmill",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (run.notes.contains("Zombie")) DangerRed else TextMuted
                )
            }
        }
    }
}
