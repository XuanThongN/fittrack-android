package com.fittrack.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.app.data.model.UserProfile
import com.fittrack.app.ui.theme.CardSurface
import com.fittrack.app.ui.theme.DarkBackground
import com.fittrack.app.ui.theme.EmeraldLight
import com.fittrack.app.ui.theme.EmeraldPrimary
import com.fittrack.app.ui.theme.OrangeBurn
import com.fittrack.app.ui.theme.TextMuted
import com.fittrack.app.ui.theme.TextPrimary
import com.fittrack.app.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun BmiGaugeCard(
    currentWeightKg: Double,
    profile: UserProfile,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bmi = profile.calculateBmi(currentWeightKg)
    val bmiCat = profile.getBmiCategory(bmi)

    // Calculate Ideal Weight Range for Height (WHO Asian BMI 18.5 - 22.9)
    val heightM = profile.heightCm / 100.0
    val minIdealWeight = 18.5 * (heightM * heightM)
    val maxIdealWeight = 22.9 * (heightM * heightM)

    val weightDiff = currentWeightKg - maxIdealWeight
    val dailyWaterLitres = (currentWeightKg * 0.035).coerceAtLeast(1.5)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Height, Weight & Quick Edit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Chỉ Số Thể Trạng & BMI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.12f))
                        .clickable { onEditProfile() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Chỉnh sửa",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sửa chiều cao/cân",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stat numbers: Height, Current Weight, BMI Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Chiều cao", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        text = "${profile.heightCm.toInt()} cm",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Cân hiện tại", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        text = String.format(Locale.US, "%.1f kg", currentWeightKg),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "BMI Phân loại", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        text = "$bmi",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(bmiCat.colorHex)
                    )
                    Text(
                        text = bmiCat.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(bmiCat.colorHex)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BMI SPECTRUM GAUGE SLIDER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val barH = 14f
                    val barY = 8f

                    // Spectrum segments: Underweight (15-18.5), Normal (18.5-23), Overweight (23-25), Obese (25-35)
                    // Total range from 15 to 35 = 20 units
                    val underPct = (18.5f - 15f) / 20f
                    val normPct = (23f - 18.5f) / 20f
                    val overPct = (25f - 23f) / 20f
                    val obesePct = (35f - 25f) / 20f

                    var curX = 0f

                    // Underweight (Blue)
                    drawRoundRect(
                        Color(0xFF38BDF8),
                        Offset(curX, barY),
                        Size(w * underPct, barH),
                        CornerRadius(6f, 6f)
                    )
                    curX += w * underPct

                    // Normal (Emerald)
                    drawRect(
                        EmeraldPrimary,
                        Offset(curX, barY),
                        Size(w * normPct, barH)
                    )
                    curX += w * normPct

                    // Overweight (Amber)
                    drawRect(
                        Color(0xFFF59E0B),
                        Offset(curX, barY),
                        Size(w * overPct, barH)
                    )
                    curX += w * overPct

                    // Obese (Red)
                    drawRoundRect(
                        Color(0xFFEF4444),
                        Offset(curX, barY),
                        Size(w * obesePct, barH),
                        CornerRadius(6f, 6f)
                    )

                    // Indicator needle for user's BMI
                    val bmiClamped = bmi.toFloat().coerceIn(15f, 35f)
                    val needlePct = (bmiClamped - 15f) / 20f
                    val needleX = (w * needlePct).coerceIn(10f, w - 10f)

                    // Draw needle triangle
                    val needlePath = Path().apply {
                        moveTo(needleX, barY - 4f)
                        lineTo(needleX - 8f, barY - 14f)
                        lineTo(needleX + 8f, barY - 14f)
                        close()
                    }
                    drawPath(needlePath, color = Color.WHITE)

                    // Needle circle base
                    drawCircle(Color.WHITE, 4f, Offset(needleX, barY + barH / 2f))
                }
            }

            // Legend labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Thiếu cân (<18.5)", fontSize = 10.sp, color = Color(0xFF38BDF8))
                Text(text = "Lý tưởng (18.5-23)", fontSize = 10.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                Text(text = "Thừa cân (23-25)", fontSize = 10.sp, color = Color(0xFFF59E0B))
                Text(text = "Béo phì (>25)", fontSize = 10.sp, color = Color(0xFFEF4444))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // IDEAL WEIGHT & RECOMMENDATION BADGES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "🎯 Cân nặng lý tưởng", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f - %.1f kg", minIdealWeight, maxIdealWeight),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                        if (weightDiff > 0.5) {
                            Text(
                                text = String.format(Locale.US, "Nên giảm %.1f kg", weightDiff),
                                fontSize = 11.sp,
                                color = OrangeBurn
                            )
                        } else {
                            Text(
                                text = "Đang ở mốc cân đẹp!",
                                fontSize = 11.sp,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Nhu cầu nước", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f Lít / ngày", dailyWaterLitres),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        Text(
                            text = "Hỗ trợ thải độc & mỡ",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
