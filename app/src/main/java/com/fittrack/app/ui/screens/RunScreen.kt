package com.fittrack.app.ui.screens

import android.Manifest
import android.os.Build
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.fittrack.app.data.model.ZombieDifficulty
import com.fittrack.app.service.RunningSessionManager
import com.fittrack.app.ui.components.RealStreetZombieMapView
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
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun RunScreen(
    repository: FitTrackRepository
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Initialize Tracker in Singleton Manager
    LaunchedEffect(Unit) {
        RunningSessionManager.initTracker(context)
    }

    val profile by repository.userProfile.collectAsState()
    val weights by repository.weightEntries.collectAsState()
    val runs by repository.runSessions.collectAsState()

    val currentWeightKg = weights.firstOrNull()?.weightKg ?: profile.initialWeightKg

    // Collect global persistent running session states
    val isRunning by RunningSessionManager.isRunning.collectAsState()
    val isPaused by RunningSessionManager.isPaused.collectAsState()
    val elapsedSeconds by RunningSessionManager.elapsedSeconds.collectAsState()
    val manualDistanceMeters by RunningSessionManager.manualDistanceMeters.collectAsState()
    val zombieState by RunningSessionManager.zombieState.collectAsState()
    val mapMode by RunningSessionManager.mapMode.collectAsState()
    val isMuted by RunningSessionManager.isMuted.collectAsState()
    val keepScreenOn by RunningSessionManager.keepScreenOn.collectAsState()
    val lastFinishedRun by RunningSessionManager.lastFinishedRun.collectAsState()
    var runToDelete by remember { mutableStateOf<RunSession?>(null) }

    val locationTracker = RunningSessionManager.locationTracker
    val gpsDistanceMeters by locationTracker?.distanceMeters?.collectAsState() ?: remember { mutableDoubleStateOf(0.0) }
    val pathPoints by locationTracker?.pathPoints?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val currentLocation by locationTracker?.currentLocation?.collectAsState() ?: remember { mutableStateOf(null) }
    val currentBearing by locationTracker?.currentBearing?.collectAsState() ?: remember { mutableFloatStateOf(0f) }

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

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            locationTracker?.startListening()
            if (isRunning) {
                locationTracker?.startTracking()
            }
        }
    }

    // Automatically check permissions on screen enter
    LaunchedEffect(locationTracker) {
        if (locationTracker?.hasPermission() == false) {
            val perms = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permissionLauncher.launch(perms.toTypedArray())
        } else {
            locationTracker?.startListening()
        }
    }

    // Handle Stop Action requested from Ongoing Notification
    LaunchedEffect(Unit) {
        RunningSessionManager.stopRequested.collect {
            if (isRunning) {
                coroutineScope.launch {
                    val notes = if (zombieState.isEnabled) "Chạy sinh tồn Zombie (${zombieState.caughtCount} lần bị bắt)" else "Chạy bộ ngoài trời"
                    RunningSessionManager.stopAndSaveRun(context, repository, currentWeightKg, notes)
                }
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

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
                    style = MaterialTheme.typography.bodySmall,
                    color = if (zombieState.isEnabled) DangerRed else TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Screen On & Lock Screen toggle button
                IconButton(onClick = { RunningSessionManager.toggleKeepScreenOn() }) {
                    Icon(
                        imageVector = if (keepScreenOn) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = "Màn hình khóa & Luôn sáng",
                        tint = if (keepScreenOn) EmeraldPrimary else TextMuted
                    )
                }

                // Quick Manual Input button
                IconButton(onClick = { showManualDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nhập tay",
                        tint = CyanRun
                    )
                }

                // Sound Mute/Unmute
                IconButton(onClick = { RunningSessionManager.toggleMute() }) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Âm thanh",
                        tint = if (isMuted) TextMuted else EmeraldPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Map Mode Switcher: Real Street vs Tactical Radar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardSurface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (mapMode == "street") CyanRun else Color.Transparent)
                    .clickable { RunningSessionManager.setMapMode("street") }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🗺️", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Đường Phố Thật",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (mapMode == "street") DarkBackground else TextPrimary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (mapMode == "radar") DangerRed else Color.Transparent)
                    .clickable { RunningSessionManager.setMapMode("radar") }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "📡", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Radar Tác Chiến",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (mapMode == "radar") Color.White else TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Display selected Map View (Compact 230dp)
        if (mapMode == "street") {
            RealStreetZombieMapView(
                pathPoints = pathPoints,
                currentLocation = currentLocation,
                currentBearing = currentBearing,
                zombieState = zombieState,
                isRunning = isRunning && !isPaused,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            ZombieRadarMap(
                pathPoints = pathPoints,
                currentLocation = currentLocation,
                currentBearing = currentBearing,
                zombieState = zombieState,
                runnerSpeedKmh = runnerSpeedKmh,
                isRunning = isRunning && !isPaused,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Lock Screen Display Status & Screen Always On Toggle
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { RunningSessionManager.toggleKeepScreenOn() }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (keepScreenOn) "📱" else "🔒",
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Xem Bản Đồ Trên Màn Hình Khóa",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (keepScreenOn) "Màn hình luôn sáng khi đang chạy" else "Bật màn hình là thấy ngay bản đồ (không cần mở khóa)",
                            fontSize = 11.sp,
                            color = if (keepScreenOn) EmeraldPrimary else TextSecondary
                        )
                    }
                }
                Switch(
                    checked = keepScreenOn,
                    onCheckedChange = { RunningSessionManager.setKeepScreenOn(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EmeraldPrimary,
                        checkedTrackColor = EmeraldPrimary.copy(alpha = 0.35f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ==========================================
        // PRIMARY ACTION BUTTON BAR (START / PAUSE / STOP)
        // PLACED RIGHT UNDER THE MAP - 100% VISIBLE & EASY TO TAP!
        // ==========================================
        if (!isRunning) {
            Button(
                onClick = {
                    if (locationTracker?.hasPermission() == true) {
                        RunningSessionManager.startRun(context, zombieState.isEnabled, zombieState.difficulty)
                    } else {
                        val perms = mutableListOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionLauncher.launch(perms.toTypedArray())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (zombieState.isEnabled) DangerRed else CyanRun
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (zombieState.isEnabled) Color.White else DarkBackground,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (zombieState.isEnabled) "CHẠY TRỐN ZOMBIE NGAY!" else "BẮT ĐẦU CHẠY GPS",
                    color = if (zombieState.isEnabled) Color.White else DarkBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (isPaused) {
                            RunningSessionManager.resumeRun(context)
                        } else {
                            RunningSessionManager.pauseRun(context)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
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
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val notes = if (zombieState.isEnabled) "Chạy sinh tồn Zombie (${zombieState.caughtCount} lần bị bắt)" else "Chạy bộ ngoài trời"
                            RunningSessionManager.stopAndSaveRun(context, repository, currentWeightKg, notes)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeBurn)
                ) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = null, tint = DarkBackground)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Kết thúc & Lưu",
                        color = DarkBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // METRICS DASHBOARD CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Distance Number
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.US, "%.2f", distanceKm),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CyanLight
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "KM",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stats Row (Time, Pace, Calories)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Time
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = CyanLight, modifier = Modifier.size(13.dp))
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

                    // Pace
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Pace", style = MaterialTheme.typography.labelSmall, color = TextMuted)
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

                    // Calories
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
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ZOMBIE CHASE SETTINGS CARD
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
                            RunningSessionManager.setZombieEnabled(checked)
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
                        ZombieAlertLevel.DANGER, ZombieAlertLevel.CAUGHT -> DangerRed
                        ZombieAlertLevel.WARNING -> OrangeBurn
                        ZombieAlertLevel.SAFE -> EmeraldPrimary
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
                                        RunningSessionManager.setZombieDifficulty(diff)
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

        Spacer(modifier = Modifier.height(14.dp))

        // RUN HISTORY LIST
        Text(
            text = "Lịch sử các buổi chạy (${runs.size})",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (runs.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có buổi chạy nào. Hãy nhấn 'BẮT ĐẦU CHẠY' để ghi nhận buổi chạy đầu tiên!",
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                runs.forEach { run ->
                    RunHistoryItem(
                        run = run,
                        onDelete = { runToDelete = run }
                    )
                }
            }
        }

        // Bottom space so nothing is clipped by navigation bar
        Spacer(modifier = Modifier.height(90.dp))
    }

    // Delete Run Confirmation Dialog
    if (runToDelete != null) {
        val target = runToDelete!!
        AlertDialog(
            onDismissRequest = { runToDelete = null },
            title = { Text("Xác Nhận Xóa Buổi Chạy", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "Bạn có chắc muốn xóa buổi chạy ngày ${target.getFormattedDate()} (${target.distanceKmFormatted()})?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteRun(target.id)
                            runToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Xóa", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { runToDelete = null }) {
                    Text("Hủy", color = TextMuted)
                }
            }
        )
    }

    // Celebratory Post-Run Summary Dialog
    if (lastFinishedRun != null) {
        PostRunSummaryDialog(
            run = lastFinishedRun!!,
            onDismiss = { RunningSessionManager.clearLastFinishedRun() }
        )
    }

    // Manual Run Add Dialog
    if (showManualDialog) {
        ManualRunDialog(
            onDismiss = { showManualDialog = false },
            onConfirm = { distKm, durMin ->
                val distMeters = distKm * 1000.0
                val durSeconds = (durMin * 60).toLong()
                val calories = (currentWeightKg * distKm * 1.036).coerceAtLeast(0.0)
                val pace = if (distKm > 0) (durSeconds / distKm).toLong() else 0L

                val manualSession = RunSession(
                    durationSeconds = durSeconds,
                    distanceMeters = distMeters,
                    caloriesBurned = calories,
                    averagePaceSecondsPerKm = pace,
                    isGpsTracked = false,
                    notes = "Nhập tay thủ công (Máy chạy bộ)"
                )
                coroutineScope.launch {
                    repository.addRunSession(manualSession)
                }
                showManualDialog = false
            }
        )
    }
}

@Composable
fun RunHistoryItem(
    run: RunSession,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(14.dp)
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
                        .background(if (run.notes.contains("Zombie")) DangerRed.copy(alpha = 0.2f) else CyanRun.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = if (run.notes.contains("Zombie")) DangerRed else CyanRun,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = run.getFormattedDate(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (run.notes.isNotBlank()) run.notes else if (run.isGpsTracked) "GPS Ngoài trời" else "Nhập tay",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = run.distanceKmFormatted(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyanLight
                    )
                    Text(
                        text = "${run.durationFormatted()} • ${run.caloriesFormatted()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa",
                        tint = TextMuted.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PostRunSummaryDialog(
    run: RunSession,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🎉", fontSize = 26.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Hoàn Thành Buổi Chạy!",
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        fontSize = 18.sp
                    )
                    Text(
                        text = run.getFormattedDate(),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Big Distance Pill
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkBackground),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = run.distanceKmFormatted(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CyanRun
                        )
                        Text(
                            text = "Tổng quãng đường",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }

                // Grid stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(text = "⏱️ Thời gian", fontSize = 11.sp, color = TextMuted)
                        Text(text = run.durationFormatted(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(text = "⚡ Pace TB", fontSize = 11.sp, color = TextMuted)
                        Text(text = run.averagePaceFormatted(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(text = "🔥 Calo", fontSize = 11.sp, color = TextMuted)
                        Text(text = run.caloriesFormatted(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = OrangeBurn)
                    }
                }

                if (run.notes.isNotBlank()) {
                    Text(
                        text = "📝 ${run.notes}",
                        fontSize = 12.sp,
                        color = if (run.notes.contains("Zombie")) DangerRed else TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanRun),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tuyệt Vời & Lưu Lại", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun ManualRunDialog(
    onDismiss: () -> Unit,
    onConfirm: (distKm: Double, durMin: Double) -> Unit
) {
    var distText by remember { mutableStateOf("") }
    var durText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Nhập Tay Buổi Chạy", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = distText,
                    onValueChange = { distText = it },
                    label = { Text("Quãng đường (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = durText,
                    onValueChange = { durText = it },
                    label = { Text("Thời gian (phút)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val d = distText.toDoubleOrNull() ?: 0.0
                    val t = durText.toDoubleOrNull() ?: 0.0
                    if (d > 0 && t > 0) {
                        onConfirm(d, t)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanRun)
            ) {
                Text("Lưu Buổi Chạy", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = TextMuted)
            }
        }
    )
}
