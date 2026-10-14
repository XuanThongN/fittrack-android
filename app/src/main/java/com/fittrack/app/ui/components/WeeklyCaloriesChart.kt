package com.fittrack.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.fittrack.app.data.model.RunSession
import com.fittrack.app.data.model.WorkoutSession
import com.fittrack.app.ui.theme.OrangeBurn
import com.fittrack.app.ui.theme.OrangeLight
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DailyBurn(
    val dayLabel: String,
    val calories: Double
)

@Composable
fun WeeklyCaloriesChart(
    workouts: List<WorkoutSession>,
    runs: List<RunSession>,
    modifier: Modifier = Modifier
) {
    // Calculate last 7 days calories
    val daysData = mutableListOf<DailyBurn>()
    val cal = Calendar.getInstance()
    val dayFormat = SimpleDateFormat("EE", Locale("vi", "VN"))

    for (i in 6 downTo 0) {
        val targetDay = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -i)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = targetDay.timeInMillis
        val endMs = startMs + 24 * 3600 * 1000L

        val workoutCals = workouts.filter { it.timestamp in startMs until endMs }.sumOf { it.caloriesBurned }
        val runCals = runs.filter { it.startTime in startMs until endMs }.sumOf { it.caloriesBurned }
        val total = workoutCals + runCals

        val label = if (i == 0) "H.nay" else dayFormat.format(targetDay.time)
        daysData.add(DailyBurn(label, total))
    }

    val maxCal = (daysData.maxOfOrNull { it.calories } ?: 300.0).coerceAtLeast(300.0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val paddingBottom = 40f
            val paddingTop = 25f
            val chartHeight = height - paddingTop - paddingBottom

            val barCount = daysData.size
            val slotWidth = width / barCount
            val barWidth = slotWidth * 0.45f

            daysData.forEachIndexed { index, item ->
                val x = index * slotWidth + (slotWidth - barWidth) / 2f
                val ratio = (item.calories / maxCal).toFloat().coerceIn(0.04f, 1f)
                val barH = chartHeight * ratio
                val y = paddingTop + chartHeight - barH

                // Draw rounded bar
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(OrangeLight, OrangeBurn),
                        startY = y,
                        endY = paddingTop + chartHeight
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(10f, 10f)
                )

                // Draw calories on top if > 0
                if (item.calories > 0) {
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 22f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        drawText("${item.calories.toInt()}", x + barWidth / 2f, y - 8f, paint)
                    }
                }

                // Draw day label below
                drawContext.canvas.nativeCanvas.apply {
                    val labelPaint = android.graphics.Paint().apply {
                        color = if (item.dayLabel == "H.nay") android.graphics.Color.parseColor("#F97316") else android.graphics.Color.parseColor("#94A3B8")
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    drawText(item.dayLabel, x + barWidth / 2f, paddingTop + chartHeight + 30f, labelPaint)
                }
            }
        }
    }
}
