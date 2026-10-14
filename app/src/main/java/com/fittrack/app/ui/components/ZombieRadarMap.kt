package com.fittrack.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.app.data.model.ZombieAlertLevel
import com.fittrack.app.data.model.ZombieChaseState
import com.fittrack.app.ui.theme.CardSurface
import com.fittrack.app.ui.theme.CyanLight
import com.fittrack.app.ui.theme.CyanRun
import com.fittrack.app.ui.theme.DangerRed
import com.fittrack.app.ui.theme.DarkBackground
import com.fittrack.app.ui.theme.EmeraldPrimary
import com.fittrack.app.ui.theme.OrangeBurn
import com.fittrack.app.ui.theme.TextMuted
import com.fittrack.app.ui.theme.TextPrimary
import java.util.Locale

@Composable
fun ZombieRadarMap(
    pathPoints: List<Pair<Double, Double>>,
    currentLocation: Pair<Double, Double>?,
    zombieState: ZombieChaseState,
    runnerSpeedKmh: Double,
    modifier: Modifier = Modifier
) {
    // Infinite radar sweep rotation
    val transition = rememberInfiniteTransition(label = "radar_anim")
    val sweepAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    // Pulsing danger glow when zombie is within 30 meters
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f
                val maxRadius = (minOf(w, h) / 2f) - 20f

                // 1. Radar Background (Dark Sci-Fi Green/Black)
                drawRect(Color(0xFF09121D))

                // Danger perimeter alert glow if zombie is near
                if (zombieState.isEnabled && zombieState.alertLevel == ZombieAlertLevel.DANGER) {
                    drawRect(DangerRed.copy(alpha = pulseAlpha * 0.15f))
                }

                // 2. Concentric Radar Distance Rings (25m, 50m, 100m, 150m)
                val ringRadii = listOf(maxRadius * 0.25f, maxRadius * 0.5f, maxRadius * 0.75f, maxRadius)
                val ringLabels = listOf("25m", "50m", "100m", "150m")

                ringRadii.forEachIndexed { i, r ->
                    drawCircle(
                        color = Color(0xFF1E293B),
                        radius = r,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2f)
                    )
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#475569")
                            textSize = 22f
                            isAntiAlias = true
                        }
                        drawText(ringLabels[i], cx + 6f, cy - r + 18f, paint)
                    }
                }

                // Radar Crosshair Axis lines
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(cx, cy - maxRadius),
                    end = Offset(cx, cy + maxRadius),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(cx - maxRadius, cy),
                    end = Offset(cx + maxRadius, cy),
                    strokeWidth = 2f
                )

                // 3. Rotating Radar Sweep Beam
                val sweepRad = Math.toRadians(sweepAngle.toDouble())
                val sweepEnd = Offset(
                    (cx + kotlin.math.cos(sweepRad) * maxRadius).toFloat(),
                    (cy + kotlin.math.sin(sweepRad) * maxRadius).toFloat()
                )
                drawLine(
                    color = if (zombieState.isEnabled) DangerRed.copy(alpha = 0.5f) else CyanRun.copy(alpha = 0.5f),
                    start = Offset(cx, cy),
                    end = sweepEnd,
                    strokeWidth = 3f
                )

                // 4. Draw GPS Polyline Route
                if (pathPoints.size >= 2) {
                    // Normalize GPS coordinates to canvas scale around runner (last point)
                    val lastPt = pathPoints.last()
                    // 1 meter in latitude is approx 0.000009 degrees
                    val meterScale = (maxRadius / 150f) // pixels per meter

                    val routePath = Path()
                    pathPoints.forEachIndexed { index, pt ->
                        val dxMeters = ((pt.second - lastPt.second) * 111320.0 * kotlin.math.cos(Math.toRadians(lastPt.first))).toFloat()
                        val dyMeters = -((pt.first - lastPt.first) * 110540.0).toFloat()
                        val ptX = (cx + dxMeters * meterScale).coerceIn(10f, w - 10f)
                        val ptY = (cy + dyMeters * meterScale).coerceIn(10f, h - 10f)

                        if (index == 0) {
                            routePath.moveTo(ptX, ptY)
                            // Start Flag Circle
                            drawCircle(EmeraldPrimary, 7f, Offset(ptX, ptY))
                        } else {
                            routePath.lineTo(ptX, ptY)
                        }
                    }

                    drawPath(
                        path = routePath,
                        color = CyanRun,
                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                    )
                }

                // 5. Runner Marker (Center of Radar)
                drawCircle(
                    color = CyanLight.copy(alpha = 0.35f),
                    radius = 20f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = CyanRun,
                    radius = 5f,
                    center = Offset(cx, cy)
                )

                // Runner label
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        isFakeBoldText = true
                    }
                    drawText("🏃 BẠN", cx, cy - 22f, paint)
                }

                // 6. ZOMBIE HORDE RENDERING (If Zombie mode is active)
                if (zombieState.isEnabled) {
                    // Map distance (0 to 150m) to radar pixels behind runner (downwards)
                    val zombieDistClamped = zombieState.distanceMeters.toFloat().coerceIn(4f, 150f)
                    val zombieDistPx = (zombieDistClamped / 150f) * maxRadius
                    val zombieCenterY = cy + zombieDistPx

                    val zColor = when (zombieState.alertLevel) {
                        ZombieAlertLevel.DANGER, ZombieAlertLevel.CAUGHT -> DangerRed
                        ZombieAlertLevel.WARNING -> OrangeBurn
                        ZombieAlertLevel.SAFE -> Color(0xFFFACC15)
                    }

                    // Zombie horde positions (3 zombies chasing behind in formation)
                    val zombieOffsets = listOf(
                        Offset(cx, zombieCenterY),
                        Offset(cx - 24f, zombieCenterY + 12f),
                        Offset(cx + 24f, zombieCenterY + 14f)
                    )

                    // Draw connecting danger line from runner to zombie horde
                    drawLine(
                        color = zColor.copy(alpha = 0.6f),
                        start = Offset(cx, cy + 12f),
                        end = Offset(cx, zombieCenterY - 14f),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )

                    // Draw Zombies
                    zombieOffsets.forEach { zOffset ->
                        drawCircle(
                            color = zColor.copy(alpha = pulseAlpha * 0.4f),
                            radius = 16f,
                            center = zOffset
                        )
                        drawCircle(
                            color = Color(0xFF7F1D1D),
                            radius = 9f,
                            center = zOffset
                        )
                        drawCircle(
                            color = zColor,
                            radius = 5f,
                            center = zOffset
                        )
                    }

                    // Zombie label text
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor(if (zombieState.alertLevel == ZombieAlertLevel.DANGER) "#EF4444" else "#F97316")
                            textSize = 26f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                            isFakeBoldText = true
                        }
                        drawText("🧟 ĐÀN ZOMBIE: ${zombieState.distanceMeters.toInt()}m", cx, zombieCenterY + 34f, paint)
                    }
                }
            }

            // Top Status Overlay Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBackground.copy(alpha = 0.85f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "RADAR GPS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanLight
                            )
                            if (currentLocation != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(EmeraldPrimary)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "GPS Tốt", fontSize = 10.sp, color = EmeraldPrimary)
                            } else {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "(Đang định vị...)", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }

                    if (zombieState.isEnabled) {
                        val statusBadgeColor = when (zombieState.alertLevel) {
                            ZombieAlertLevel.DANGER -> DangerRed
                            ZombieAlertLevel.WARNING -> OrangeBurn
                            ZombieAlertLevel.SAFE -> EmeraldPrimary
                            ZombieAlertLevel.CAUGHT -> DangerRed
                        }

                        val statusText = when (zombieState.alertLevel) {
                            ZombieAlertLevel.DANGER -> "⚠️ NGUY HIỂM! BỨT TỐC!"
                            ZombieAlertLevel.WARNING -> "⚡ ZOMBIE ĐANG ĐUỔI!"
                            ZombieAlertLevel.SAFE -> "🛡️ KHOẢNG CÁCH AN TOÀN"
                            ZombieAlertLevel.CAUGHT -> "💀 BỊ BẮT! CHẠY NGAY!"
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusBadgeColor.copy(alpha = 0.25f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = statusBadgeColor
                            )
                        }
                    }
                }
            }

            // Bottom Speeds Comparison Overlay
            if (zombieState.isEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkBackground.copy(alpha = 0.9f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🏃 Tốc độ bạn: ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(
                                    text = String.format(Locale.US, "%.1f km/h", runnerSpeedKmh),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (runnerSpeedKmh >= zombieState.difficulty.speedKmh) EmeraldPrimary else DangerRed
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🧟 Zombie: ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(
                                    text = String.format(Locale.US, "%.1f km/h", zombieState.difficulty.speedKmh),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = OrangeBurn
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
