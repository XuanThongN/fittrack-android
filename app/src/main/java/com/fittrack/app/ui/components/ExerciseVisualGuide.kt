package com.fittrack.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fittrack.app.data.model.WorkoutExercise
import com.fittrack.app.ui.theme.AmberWarning
import com.fittrack.app.ui.theme.CardSurface
import com.fittrack.app.ui.theme.CyanRun
import com.fittrack.app.ui.theme.DarkBackground
import com.fittrack.app.ui.theme.EmeraldLight
import com.fittrack.app.ui.theme.EmeraldPrimary
import com.fittrack.app.ui.theme.OrangeBurn
import com.fittrack.app.ui.theme.TextMuted
import com.fittrack.app.ui.theme.TextPrimary
import com.fittrack.app.ui.theme.TextSecondary

@Composable
fun ExerciseVisualGuide(
    exercise: WorkoutExercise,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Minh Họa", "Kỹ Thuật", "Hít Thở", "Lỗi Sai")

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Exercise Title & Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Nhóm cơ: ${exercise.targetMuscles}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EmeraldLight
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(OrangeBurn.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "~${exercise.caloriesPerMinute} kcal/p",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = OrangeBurn
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkBackground,
                contentColor = EmeraldPrimary,
                edgePadding = 0.dp,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) EmeraldPrimary else TextMuted
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTab) {
                0 -> {
                    // TAB 0: ANIMATED FIGURE ILLUSTRATION
                    AnimatedExerciseCanvas(exerciseId = exercise.id)
                }
                1 -> {
                    // TAB 1: STEP-BY-STEP TECHNIQUE
                    StepByStepTechnique(exerciseId = exercise.id, defaultInstructions = exercise.instructions)
                }
                2 -> {
                    // TAB 2: BREATHING GUIDE
                    BreathingGuide(exerciseId = exercise.id)
                }
                3 -> {
                    // TAB 3: COMMON MISTAKES
                    MistakesGuide(exerciseId = exercise.id)
                }
            }
        }
    }
}

@Composable
fun AnimatedExerciseCanvas(exerciseId: String) {
    val transition = rememberInfiniteTransition(label = "exercise_anim")
    val animProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val boneStroke = Stroke(width = 8f, cap = StrokeCap.Round)
            val headRadius = 14f

            when (exerciseId) {
                "ex_squats" -> {
                    // Squats animation: Hip sinks down, knees bend
                    val hipY = cy + (animProgress * 32f)
                    val kneeY = cy + 30f + (animProgress * 15f)
                    val kneeSpread = 28f + (animProgress * 14f)

                    // Head
                    drawCircle(EmeraldPrimary, headRadius, Offset(cx, hipY - 60f))
                    // Torso
                    drawLine(EmeraldPrimary, Offset(cx, hipY - 45f), Offset(cx, hipY), strokeWidth = 10f, cap = StrokeCap.Round)
                    // Arms (reaching forward for balance)
                    val armEndY = hipY - 20f + (animProgress * 10f)
                    drawLine(Color(0xFF38BDF8), Offset(cx, hipY - 40f), Offset(cx + 42f, armEndY), strokeWidth = 7f, cap = StrokeCap.Round)
                    // Left leg
                    drawLine(EmeraldLight, Offset(cx, hipY), Offset(cx - kneeSpread, kneeY), strokeWidth = 8f, cap = StrokeCap.Round)
                    drawLine(EmeraldLight, Offset(cx - kneeSpread, kneeY), Offset(cx - 30f, cy + 70f), strokeWidth = 8f, cap = StrokeCap.Round)
                    // Right leg
                    drawLine(EmeraldLight, Offset(cx, hipY), Offset(cx + kneeSpread, kneeY), strokeWidth = 8f, cap = StrokeCap.Round)
                    drawLine(EmeraldLight, Offset(cx + kneeSpread, kneeY), Offset(cx + 30f, cy + 70f), strokeWidth = 8f, cap = StrokeCap.Round)
                }

                "ex_jumping_jacks" -> {
                    // Jumping jacks: Arms swing from hips to overhead, feet spread wide
                    val armAngle = animProgress * 1.6f // 0 = down, 1.6 = up
                    val legSpread = 20f + (animProgress * 35f)
                    val hopY = cy - (animProgress * 15f)

                    // Head
                    drawCircle(EmeraldPrimary, headRadius, Offset(cx, hopY - 55f))
                    // Torso
                    drawLine(EmeraldPrimary, Offset(cx, hopY - 40f), Offset(cx, hopY), strokeWidth = 10f, cap = StrokeCap.Round)
                    // Left Arm
                    val lHandX = cx - (kotlin.math.cos(armAngle) * 45f)
                    val lHandY = hopY - 35f - (kotlin.math.sin(armAngle) * 45f)
                    drawLine(Color(0xFF38BDF8), Offset(cx, hopY - 35f), Offset(lHandX, lHandY), strokeWidth = 7f, cap = StrokeCap.Round)
                    // Right Arm
                    val rHandX = cx + (kotlin.math.cos(armAngle) * 45f)
                    val rHandY = hopY - 35f - (kotlin.math.sin(armAngle) * 45f)
                    drawLine(Color(0xFF38BDF8), Offset(cx, hopY - 35f), Offset(rHandX, rHandY), strokeWidth = 7f, cap = StrokeCap.Round)
                    // Legs
                    drawLine(EmeraldLight, Offset(cx, hopY), Offset(cx - legSpread, hopY + 65f), strokeWidth = 8f, cap = StrokeCap.Round)
                    drawLine(EmeraldLight, Offset(cx, hopY), Offset(cx + legSpread, hopY + 65f), strokeWidth = 8f, cap = StrokeCap.Round)
                }

                "ex_pushups" -> {
                    // Pushups: Horizontal plank, chest drops down and pushes up
                    val dropY = cy + (animProgress * 22f)
                    // Head
                    drawCircle(EmeraldPrimary, headRadius, Offset(cx + 55f, dropY - 10f))
                    // Body (Straight line from heels to shoulders)
                    drawLine(EmeraldPrimary, Offset(cx - 55f, cy + 25f), Offset(cx + 45f, dropY), strokeWidth = 10f, cap = StrokeCap.Round)
                    // Arms (Hands on floor, elbows bend)
                    val elbowY = dropY + 12f
                    drawLine(Color(0xFF38BDF8), Offset(cx + 40f, dropY), Offset(cx + 30f, elbowY), strokeWidth = 7f, cap = StrokeCap.Round)
                    drawLine(Color(0xFF38BDF8), Offset(cx + 30f, elbowY), Offset(cx + 38f, cy + 30f), strokeWidth = 7f, cap = StrokeCap.Round)
                    // Floor indicator line
                    drawLine(Color(0xFF334155), Offset(cx - 70f, cy + 34f), Offset(cx + 70f, cy + 34f), strokeWidth = 4f)
                }

                "ex_plank" -> {
                    // Plank: Solid isometric hold with glowing core energy
                    val glowAlpha = 0.3f + (animProgress * 0.4f)
                    drawCircle(OrangeBurn.copy(alpha = glowAlpha), 35f, Offset(cx, cy))
                    // Head
                    drawCircle(EmeraldPrimary, headRadius, Offset(cx + 55f, cy - 12f))
                    // Body (Rigid straight spine)
                    drawLine(EmeraldPrimary, Offset(cx - 60f, cy + 18f), Offset(cx + 45f, cy - 8f), strokeWidth = 11f, cap = StrokeCap.Round)
                    // Forearms resting flat on floor
                    drawLine(Color(0xFF38BDF8), Offset(cx + 42f, cy - 5f), Offset(cx + 40f, cy + 20f), strokeWidth = 8f, cap = StrokeCap.Round)
                    drawLine(Color(0xFF38BDF8), Offset(cx + 40f, cy + 20f), Offset(cx + 58f, cy + 20f), strokeWidth = 8f, cap = StrokeCap.Round)
                    // Floor line
                    drawLine(Color(0xFF334155), Offset(cx - 70f, cy + 22f), Offset(cx + 70f, cy + 22f), strokeWidth = 4f)
                }

                else -> {
                    // High Knees / Burpees / General Athletic Running Animation
                    val kneeLift = animProgress * 30f
                    // Head
                    drawCircle(EmeraldPrimary, headRadius, Offset(cx, cy - 50f))
                    // Torso
                    drawLine(EmeraldPrimary, Offset(cx, cy - 35f), Offset(cx, cy + 5f), strokeWidth = 10f, cap = StrokeCap.Round)
                    // High left knee
                    drawLine(EmeraldLight, Offset(cx, cy + 5f), Offset(cx - 24f, cy - kneeLift + 10f), strokeWidth = 8f, cap = StrokeCap.Round)
                    drawLine(EmeraldLight, Offset(cx - 24f, cy - kneeLift + 10f), Offset(cx - 20f, cy + 45f), strokeWidth = 8f, cap = StrokeCap.Round)
                    // Supporting right leg
                    drawLine(EmeraldLight, Offset(cx, cy + 5f), Offset(cx + 20f, cy + 65f), strokeWidth = 8f, cap = StrokeCap.Round)
                    // Runner arms
                    drawLine(Color(0xFF38BDF8), Offset(cx, cy - 30f), Offset(cx + 28f, cy - 10f), strokeWidth = 7f, cap = StrokeCap.Round)
                    drawLine(Color(0xFF38BDF8), Offset(cx, cy - 30f), Offset(cx - 28f, cy - 40f), strokeWidth = 7f, cap = StrokeCap.Round)
                }
            }
        }

        Text(
            text = "⚡ Động tác đang mô phỏng nhịp chuẩn",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}

@Composable
fun StepByStepTechnique(exerciseId: String, defaultInstructions: String) {
    val steps = when (exerciseId) {
        "ex_squats" -> listOf(
            "1. Đứng thẳng, hai chân mở rộng bằng vai, mũi bàn chân hơi hướng ra ngoài 15-30 độ.",
            "2. Hít sâu, siết chặt cơ bụng, từ từ đẩy hông ra sau và hạ đùi xuống giống như ngồi ghế.",
            "3. Hạ cho đến khi đùi song song với mặt sàn (hoặc sâu hơn nếu khớp gối linh hoạt).",
            "4. Giữ đầu gối thẳng hướng theo mũi chân, lưng giữ thẳng tự nhiên, không cong lưng.",
            "5. Nhấn gót chân xuống sàn và thở mạnh ra khi đẩy người đứng thẳng trở lại."
        )
        "ex_burpees" -> listOf(
            "1. Bắt đầu từ tư thế đứng thẳng hai chân mở rộng bằng hông.",
            "2. Ngồi xổm nhanh xuống và chống hai lòng bàn tay chắc chắn lên mặt sàn.",
            "3. Bật nhanh hai chân về sau tạo thành tư thế hít đất thẳng người.",
            "4. Hạ ngực chạm sát sàn (hoặc hít đất 1 cái), sau đó đẩy người lên.",
            "5. Bật hai chân trở về sát hai tay rồi bật nhảy vươn thẳng hai tay lên trời vỗ tay."
        )
        "ex_plank" -> listOf(
            "1. Nằm sấp, chống hai cẳng tay xuống sàn, cùi chỏ vuông góc 90 độ ngay dưới vai.",
            "2. Nhón hai mũi chân lên, nâng toàn bộ thân người lên khỏi mặt đất.",
            "3. Giữ cơ thể tạo thành một đường thẳng tắp từ đỉnh đầu đến gót chân.",
            "4. Siết chặt cơ bụng và cơ mông liên tục, mắt nhìn xuống sàn để giữ thẳng đốt sống cổ.",
            "5. Tuyệt đối không võng lưng dưới xuống hoặc nhô mông quá cao lên trời."
        )
        else -> listOf(
            "1. Chuẩn bị tư thế chuẩn xác theo hướng dẫn.",
            "2. Thực hiện nhịp nhàng, tập trung vào cảm nhận cơ bắp đích.",
            "3. Giữ nhịp độ ổn định trong suốt hiệp tập đếm ngược.",
            defaultInstructions
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.forEach { step ->
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
fun BreathingGuide(exerciseId: String) {
    val (inhale, exhale, tip) = when (exerciseId) {
        "ex_squats" -> Triple(
            "HÍT VÀO bằng mũi sâu và giữ chắc bụng khi hạ hông xuống.",
            "THỞ RA dứt khoát bằng miệng khi dùng lực nhấn gót chân đẩy người đứng dậy.",
            "Không nín thở quá lâu để tránh tụt huyết áp hoặc chóng mặt."
        )
        "ex_pushups" -> Triple(
            "HÍT VÀO khi từ từ hạ ngực xuống sát mặt sàn.",
            "THỞ RA khi dùng lực ngực và tay sau đẩy người thẳng lên vị trí cũ.",
            "Thở đều giúp cơ bắp nạp oxy dồi dào, tăng số rep hít đất."
        )
        "ex_plank" -> Triple(
            "HÍT THỞ ĐỀU: Hít sâu chậm qua mũi và thở êm qua miệng liên tục.",
            "Không được nín thở khi siết cơ bụng.",
            "Nếu cảm thấy hụt hơi, hãy điều chỉnh nhịp thở sâu 3 giây hít - 3 giây thở."
        )
        else -> Triple(
            "HÍT VÀO khi ở giai đoạn cơ thể thả lỏng / hạ xuống.",
            "THỞ RA khi dùng lực phát động cơ thể (bật nhảy, co gập cơ).",
            "Duy trì nhịp thở nhịp nhàng theo bước chân hoặc nhịp đếm."
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkBackground),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Air, contentDescription = null, tint = CyanRun, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Hít vào (Inhale)", fontWeight = FontWeight.Bold, color = CyanRun)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = inhale, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkBackground),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Air, contentDescription = null, tint = OrangeBurn, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Thở ra (Exhale)", fontWeight = FontWeight.Bold, color = OrangeBurn)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = exhale, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
        }

        Text(
            text = "💡 Mẹo: $tip",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun MistakesGuide(exerciseId: String) {
    val mistakes = when (exerciseId) {
        "ex_squats" -> listOf(
            "Đầu gối chụm vào trong (sập khớp gối) -> Khắc phục: Chủ động mở đầu gối hướng theo mũi chân.",
            "Nhấc gót chân lên khỏi sàn -> Khắc phục: Luôn dồn trọng tâm vào giữa bàn chân và gót chân.",
            "Cong gập lưng tôm khi xuống sâu -> Khắc phục: Mở rộng ngực, mắt nhìn thẳng phía trước."
        )
        "ex_plank" -> listOf(
            "Võng lưng dưới xuống -> Nguy hiểm gây đau cột sống lưng. Hãy siết chặt cơ mông và kéo rốn về phía cột sống.",
            "Nhô mông quá cao -> Làm giảm áp lực lên cơ bụng, giảm hiệu quả đốt mỡ.",
            "Ngửa cổ nhìn về trước -> Gây mỏi cổ. Hãy giữ mắt nhìn thẳng xuống sàn giữa hai bàn tay."
        )
        "ex_pushups" -> listOf(
            "Cùi chỏ bè ngang 90 độ sang hai bên -> Dễ gây chấn thương khớp vai. Hãy khép cùi chỏ tạo góc 45 độ hình mũi tên.",
            "Chỉ gật đầu thay vì hạ toàn bộ thân ngực.",
            "Võng bụng sà xuống đất trước khi ngực hạ."
        )
        else -> listOf(
            "Không khởi động kỹ khớp cổ chân và khớp gối trước khi tập.",
            "Tập quá nhanh mất kiểm soát tư thế chuẩn xác.",
            "Nín thở trong lúc dùng sức khiến tim đập quá nhanh."
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        mistakes.forEach { item ->
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AmberWarning,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
