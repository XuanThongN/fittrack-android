package com.fittrack.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import com.fittrack.app.util.StreetChaseEngine
import java.util.Locale

@Composable
fun ZombieRadarMap(
    pathPoints: List<Pair<Double, Double>>,
    currentLocation: Pair<Double, Double>?,
    currentBearing: Float = 0f,
    zombieState: ZombieChaseState,
    runnerSpeedKmh: Double,
    isRunning: Boolean = false,
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
            .height(230.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(20.dp)
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
                val refPt = currentLocation ?: (if (pathPoints.isNotEmpty()) pathPoints.last() else Pair(10.7769, 106.7009))
                val meterScale = (maxRadius / 150f) // pixels per meter

                fun toRadarOffset(pt: Pair<Double, Double>): Offset {
                    val dxMeters = ((pt.second - refPt.second) * 111320.0 * kotlin.math.cos(Math.toRadians(refPt.first))).toFloat()
                    val dyMeters = -((pt.first - refPt.first) * 110540.0).toFloat()
                    val zX = (cx + dxMeters * meterScale).coerceIn(12f, w - 12f)
                    val zY = (cy + dyMeters * meterScale).coerceIn(12f, h - 12f)
                    return Offset(zX, zY)
                }

                if (pathPoints.size >= 2) {
                    val routePath = Path()
                    pathPoints.forEachIndexed { index, pt ->
                        val ptOffset = toRadarOffset(pt)
                        if (index == 0) {
                            routePath.moveTo(ptOffset.x, ptOffset.y)
                            drawCircle(EmeraldPrimary, 7f, ptOffset)
                        } else {
                            routePath.lineTo(ptOffset.x, ptOffset.y)
                        }
                    }

                    drawPath(
                        path = routePath,
                        color = CyanRun,
                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                    )
                }

                // 5. Runner Marker (Center of Radar with Pulse & Heading Arrow)
                val runnerPulseRad = 16f + (pulseAlpha * 8f)
                drawCircle(
                    color = CyanLight.copy(alpha = 0.3f),
                    radius = runnerPulseRad,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color.White,
                    radius = 9f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = CyanRun,
                    radius = 5.5f,
                    center = Offset(cx, cy)
                )

                // Direction pointer arrow
                val arrowAngleRad = Math.toRadians((currentBearing.toDouble() - 90.0))
                val arrowTip = Offset(
                    (cx + kotlin.math.cos(arrowAngleRad) * 18f).toFloat(),
                    (cy + kotlin.math.sin(arrowAngleRad) * 18f).toFloat()
                )
                drawLine(
                    color = CyanRun,
                    start = Offset(cx, cy),
                    end = arrowTip,
                    strokeWidth = 3.5f,
                    cap = StrokeCap.Round
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

                // 6. REAL STREET ZOMBIE HORDE RENDERING
                if (zombieState.isEnabled) {
                    val distM = zombieState.distanceMeters.coerceAtLeast(1.0)

                    val leadChase = StreetChaseEngine.getPointAlongPathBackwards(
                        pathPoints = pathPoints,
                        currentLocation = currentLocation,
                        currentBearing = currentBearing,
                        distanceBackMeters = distM,
                        lateralOffsetMeters = 0.0
                    )
                    val flank1Chase = StreetChaseEngine.getPointAlongPathBackwards(
                        pathPoints = pathPoints,
                        currentLocation = currentLocation,
                        currentBearing = currentBearing,
                        distanceBackMeters = distM + 6.0,
                        lateralOffsetMeters = 1.8
                    )
                    val flank2Chase = StreetChaseEngine.getPointAlongPathBackwards(
                        pathPoints = pathPoints,
                        currentLocation = currentLocation,
                        currentBearing = currentBearing,
                        distanceBackMeters = distM + 12.0,
                        lateralOffsetMeters = -1.8
                    )

                    val zLead = toRadarOffset(leadChase.position)
                    val zFlank1 = toRadarOffset(flank1Chase.position)
                    val zFlank2 = toRadarOffset(flank2Chase.position)

                    val zColor = when (zombieState.alertLevel) {
                        ZombieAlertLevel.DANGER, ZombieAlertLevel.CAUGHT -> DangerRed
                        ZombieAlertLevel.WARNING -> OrangeBurn
                        ZombieAlertLevel.SAFE -> Color(0xFFFACC15)
                    }

                    // Connecting danger threat line from runner to lead zombie
                    drawLine(
                        color = zColor.copy(alpha = 0.6f),
                        start = Offset(cx, cy),
                        end = zLead,
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )

                    // Draw Zombies in formation along the real route
                    listOf(
                        Triple(zLead, DangerRed, 18f),
                        Triple(zFlank1, OrangeBurn, 14f),
                        Triple(zFlank2, Color(0xFFA855F7), 14f)
                    ).forEach { (offset, color, rad) ->
                        drawCircle(
                            color = color.copy(alpha = pulseAlpha * 0.45f),
                            radius = rad + (pulseAlpha * 6f),
                            center = offset
                        )
                        drawCircle(
                            color = Color(0xFF450A0A),
                            radius = rad * 0.6f,
                            center = offset
                        )
                        drawCircle(
                            color = color,
                            radius = rad * 0.35f,
                            center = offset
                        )
                    }

                    // Zombie distance label text
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor(if (zombieState.alertLevel == ZombieAlertLevel.DANGER) "#EF4444" else "#F97316")
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                            isFakeBoldText = true
                        }
                        drawText("🧟 ${zombieState.distanceMeters.toInt()}m", zLead.x, (zLead.y + 26f).coerceAtMost(h - 10f), paint)
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
                                text = "RADAR TÁC CHIẾN",
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
                                Text(text = "GPS Khóa", fontSize = 10.sp, color = EmeraldPrimary)
                            } else {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "(Đang dò...)", fontSize = 10.sp, color = TextMuted)
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
