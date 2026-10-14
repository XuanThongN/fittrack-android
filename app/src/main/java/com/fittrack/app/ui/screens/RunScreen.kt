package com.fittrack.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import com.fittrack.app.data.db.FitTrackRepository
import com.fittrack.app.data.model.RunSession
import com.fittrack.app.service.LocationTracker
import com.fittrack.app.ui.components.StatCard
import com.fittrack.app.ui.theme.CardSurface
import com.fittrack.app.ui.theme.CyanLight
import com.fittrack.app.ui.theme.CyanRun
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
    val profile by repository.userProfile.collectAsState()
    val weights by repository.weightEntries.collectAsState()
    val runs by repository.runSessions.collectAsState()

    val currentWeightKg = weights.firstOrNull()?.weightKg ?: profile.initialWeightKg

    var isRunning by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var manualDistanceMeters by remember { mutableDoubleStateOf(0.0) }

    val gpsDistanceMeters by locationTracker.distanceMeters.collectAsState()
    val totalDistanceMeters = if (gpsDistanceMeters > 0) gpsDistanceMeters else manualDistanceMeters
    val distanceKm = totalDistanceMeters / 1000.0

    // Calories: Weight (kg) * Distance (km) * 1.036
    val caloriesBurned = (currentWeightKg * distanceKm * 1.036).coerceAtLeast(0.0)

    // Average Pace: seconds per km
    val avgPaceSecPerKm = if (distanceKm > 0.05) (elapsedSeconds / distanceKm).toLong() else 0L

    var showManualDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (fineGranted) {
            locationTracker.startTracking()
        }
    }

    // RUNNING TIMER
    LaunchedEffect(isRunning, isPaused) {
        while (isRunning && !isPaused) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            locationTracker.stopTracking()
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
                    text = "Đo quãng đường GPS, tốc độ pace & calo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(CyanRun.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsRun,
                    contentDescription = null,
                    tint = CyanRun,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ACTIVE DASHBOARD CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // DISTANCE DISPLAY
                Text(
                    text = String.format(Locale.US, "%.2f", distanceKm),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CyanLight
                )
                Text(
                    text = "KILOMÉT (KM)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(20.dp))

                // STATS ROW (TIME, PACE, CALORIES)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
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
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
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
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = OrangeBurn, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Calo", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${caloriesBurned.toInt()} kcal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = OrangeBurn
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

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
                                locationTracker.reset()

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
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanRun)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = DarkBackground)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Bắt đầu chạy GPS", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showManualDialog = true },
                            modifier = Modifier.height(52.dp),
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
                                        notes = "Chạy bộ ngoài trời"
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

        Spacer(modifier = Modifier.height(20.dp))

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
                        .background(CyanRun.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (run.isGpsTracked) Icons.Default.LocationOn else Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = CyanRun,
                        modifier = Modifier.size(24.dp)
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
                    text = if (run.isGpsTracked) "GPS Run" else "Treadmill",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}
