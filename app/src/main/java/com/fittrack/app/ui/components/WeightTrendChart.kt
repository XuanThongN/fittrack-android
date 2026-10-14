package com.fittrack.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.fittrack.app.data.model.WeightEntry
import com.fittrack.app.ui.theme.EmeraldLight
import com.fittrack.app.ui.theme.EmeraldPrimary
import com.fittrack.app.ui.theme.OrangeBurn
import com.fittrack.app.ui.theme.TextMuted
import java.util.Locale

@Composable
fun WeightTrendChart(
    weights: List<WeightEntry>,
    targetWeightKg: Double,
    modifier: Modifier = Modifier
) {
    if (weights.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Chưa có đủ dữ liệu cân nặng để vẽ biểu đồ",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
        return
    }

    // Sort chronologically (oldest to newest)
    val sorted = weights.sortedBy { it.timestamp }
    val displayPoints = if (sorted.size > 12) sorted.takeLast(12) else sorted

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val paddingBottom = 40f
            val paddingTop = 30f
            val paddingLeft = 40f
            val paddingRight = 40f

            val chartWidth = width - paddingLeft - paddingRight
            val chartHeight = height - paddingTop - paddingBottom

            val weightValues = displayPoints.map { it.weightKg } + listOf(targetWeightKg)
            val minWeight = (weightValues.minOrNull() ?: 50.0) - 1.5
            val maxWeight = (weightValues.maxOrNull() ?: 80.0) + 1.5
            val weightRange = (maxWeight - minWeight).coerceAtLeast(1.0)

            fun getY(w: Double): Float {
                val ratio = ((w - minWeight) / weightRange).toFloat()
                return paddingTop + chartHeight * (1f - ratio)
            }

            fun getX(index: Int): Float {
                return if (displayPoints.size <= 1) {
                    paddingLeft + chartWidth / 2f
                } else {
                    paddingLeft + (index.toFloat() / (displayPoints.size - 1)) * chartWidth
                }
            }

            // Draw target weight dashed line
            val targetY = getY(targetWeightKg)
            drawLine(
                color = OrangeBurn.copy(alpha = 0.7f),
                start = Offset(paddingLeft, targetY),
                end = Offset(width - paddingRight, targetY),
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
            )

            // Target text label
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#F97316")
                    textSize = 28f
                    isAntiAlias = true
                }
                drawText("Mục tiêu ${targetWeightKg}kg", width - paddingRight - 150f, targetY - 10f, paint)
            }

            if (displayPoints.size >= 2) {
                val linePath = Path()
                val fillPath = Path()

                displayPoints.forEachIndexed { i, pt ->
                    val x = getX(i)
                    val y = getY(pt.weightKg)
                    if (i == 0) {
                        linePath.moveTo(x, y)
                        fillPath.moveTo(x, paddingTop + chartHeight)
                        fillPath.lineTo(x, y)
                    } else {
                        // Smooth cubic bezier curve
                        val prevX = getX(i - 1)
                        val prevY = getY(displayPoints[i - 1].weightKg)
                        val midX = (prevX + x) / 2f
                        linePath.cubicTo(midX, prevY, midX, y, x, y)
                        fillPath.cubicTo(midX, prevY, midX, y, x, y)
                    }
                }

                val lastX = getX(displayPoints.size - 1)
                fillPath.lineTo(lastX, paddingTop + chartHeight)
                fillPath.close()

                // Draw gradient under curve
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            EmeraldPrimary.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        startY = paddingTop,
                        endY = paddingTop + chartHeight
                    )
                )

                // Draw line curve
                drawPath(
                    path = linePath,
                    color = EmeraldLight,
                    style = Stroke(width = 6f)
                )
            }

            // Draw point circles and values
            displayPoints.forEachIndexed { i, pt ->
                val x = getX(i)
                val y = getY(pt.weightKg)

                drawCircle(
                    color = DarkBackground,
                    radius = 9f,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = EmeraldPrimary,
                    radius = 6f,
                    center = Offset(x, y)
                )

                // Draw weight value text above point
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    drawText(String.format(Locale.US, "%.1f", pt.weightKg), x, y - 16f, paint)

                    // Draw date below point
                    val datePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#94A3B8")
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    drawText(pt.getFormattedDate().substring(0, 5), x, paddingTop + chartHeight + 28f, datePaint)
                }
            }
        }
    }
}
private val DarkBackground = Color(0xFF0F172A)
