package com.fittrack.app.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WorkoutExercise(
    val id: String,
    val name: String,
    val category: String, // Cardio, Abs, Full Body, Legs, Upper
    val defaultDurationSeconds: Int = 30,
    val caloriesPerMinute: Double = 9.0,
    val instructions: String = "",
    val targetMuscles: String = ""
)

data class WorkoutPlan(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val level: String, // Cơ bản, Trung bình, Nâng cao
    val estimatedMinutes: Int,
    val estimatedCalories: Int,
    val restBetweenSeconds: Int = 15,
    val exercises: List<WorkoutExercise>
)

data class WorkoutSession(
    val id: Long = 0,
    val planTitle: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int,
    val caloriesBurned: Double,
    val completedExercisesCount: Int,
    val note: String = ""
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getDurationFormatted(): String {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

object DefaultWorkoutData {
    val allExercises = listOf(
        WorkoutExercise(
            id = "ex_jumping_jacks",
            name = "Jumping Jacks (Nhảy dang tay chân)",
            category = "Cardio",
            defaultDurationSeconds = 30,
            caloriesPerMinute = 10.0,
            instructions = "Đứng thẳng, hai chân khép lại. Nhảy bật lên đồng thời mở rộng hai chân và đưa hai tay lên trên đầu. Nhảy về tư thế chuẩn bị.",
            targetMuscles = "Toàn thân, Bắp chân, Vai"
        ),
        WorkoutExercise(
            id = "ex_burpees",
            name = "Burpees (Bật nhảy hít đất)",
            category = "Toàn thân",
            defaultDurationSeconds = 30,
            caloriesPerMinute = 14.0,
            instructions = "Bắt đầu từ tư thế đứng, ngồi xổm chống hai tay xuống đất, bật hai chân về sau thành tư thế hít đất, hít đất 1 cái, bật chân về trước rồi nhảy vươn cao tay.",
            targetMuscles = "Ngực, Bụng, Đùi, Vai, Cardio"
        ),
        WorkoutExercise(
            id = "ex_mountain_climbers",
            name = "Mountain Climbers (Leo núi tại chỗ)",
            category = "Bụng",
            defaultDurationSeconds = 30,
            caloriesPerMinute = 11.0,
            instructions = "Chống hai tay xuống sàn như tư thế Plank cao. Lần lượt co từng đầu gối hướng về phía ngực nhanh và dứt khoát như đang leo núi.",
            targetMuscles = "Cơ bụng, Vai, Đùi trước"
        ),
        WorkoutExercise(
            id = "ex_high_knees",
            name = "High Knees (Chạy nâng cao đùi)",
            category = "Cardio",
            defaultDurationSeconds = 30,
            caloriesPerMinute = 12.0,
            instructions = "Chạy tại chỗ với đùi nâng cao vuông góc 90 độ với thân người. Đánh tay nhịp nhàng theo chuyển động của chân.",
            targetMuscles = "Cơ đùi, Cơ mông, Tim mạch"
        ),
        WorkoutExercise(
            id = "ex_squats",
            name = "Squats (Ngồi xổm định hình)",
            category = "Chân",
            defaultDurationSeconds = 40,
            caloriesPerMinute = 8.5,
            instructions = "Hai chân rộng bằng vai, mũi chân hơi xoay ra ngoài. Từ từ hạ mông ra sau như đang ngồi vào ghế, giữ lưng thẳng và đùi song song với mặt sàn.",
            targetMuscles = "Cơ đùi trước, Cơ mông, Bắp chân"
        ),
        WorkoutExercise(
            id = "ex_pushups",
            name = "Push-ups (Chống đẩy / Hít đất)",
            category = "Ngực & Tay",
            defaultDurationSeconds = 30,
            caloriesPerMinute = 9.0,
            instructions = "Hai tay chống rộng hơn vai, thân người tạo thành một đường thẳng từ đầu đến gót chân. Hạ ngực sát sàn rồi dùng lực đẩy người lên.",
            targetMuscles = "Ngực, Cơ tam đầu (tay sau), Vai"
        ),
        WorkoutExercise(
            id = "ex_plank",
            name = "Plank (Giữ thắt cơ bụng)",
            category = "Bụng",
            defaultDurationSeconds = 45,
            caloriesPerMinute = 6.0,
            instructions = "Chống bằng cẳng tay, cùi chỏ vuông góc dưới vai. Giữ người thành một đường thẳng, siết chặt cơ bụng và cơ mông, không võng lưng.",
            targetMuscles = "Toàn bộ cơ bụng, Lưng dưới, Lõi thân"
        ),
        WorkoutExercise(
            id = "ex_bicycle_crunches",
            name = "Bicycle Crunches (Gập bụng đạp xe)",
            category = "Bụng",
            defaultDurationSeconds = 35,
            caloriesPerMinute = 9.0,
            instructions = "Nằm ngửa, hai tay chạm nhẹ sau gáy. Nhấc chân và vai lên khỏi sàn. Đưa cùi chỏ phải chạm đầu gối trái trong khi duỗi thẳng chân phải, sau đó đổi bên liên tục.",
            targetMuscles = "Cơ bụng trên, Cơ liên sườn"
        ),
        WorkoutExercise(
            id = "ex_lunges",
            name = "Lunges (Chùng chân)",
            category = "Chân",
            defaultDurationSeconds = 35,
            caloriesPerMinute = 8.0,
            instructions = "Bước một chân dài về phía trước, hạ thấp hông sao cho cả hai đầu gối gập thành góc khoảng 90 độ. Đẩy người trở về vị trí cũ và đổi chân.",
            targetMuscles = "Đùi trước, Cơ mông, Gân kheo"
        ),
        WorkoutExercise(
            id = "ex_russian_twist",
            name = "Russian Twists (Xoay người vặn eo)",
            category = "Bụng",
            defaultDurationSeconds = 30,
            caloriesPerMinute = 8.0,
            instructions = "Ngồi trên sàn gập đầu gối, nghiêng người ra sau khoảng 45 độ, hai chân hơi nhấc khỏi sàn. Xoay thân trên sang trái rồi sang phải chạm sàn.",
            targetMuscles = "Cơ liên sườn (eo), Cơ bụng"
        )
    )

    val plans = listOf(
        WorkoutPlan(
            id = "plan_hiit_fat_burn",
            title = "HIIT Đốt Mỡ Thần Tốc",
            subtitle = "Cardio cường độ cao giảm mỡ toàn thân",
            description = "Tổ hợp các bài tập HIIT kích hoạt tối đa nhịp tim, đốt cháy calo mạnh mẽ ngay cả sau khi buổi tập kết thúc (hiệu ứng Afterburn EPOC).",
            level = "Trung bình",
            estimatedMinutes = 15,
            estimatedCalories = 180,
            restBetweenSeconds = 15,
            exercises = listOf(
                allExercises[0], // Jumping Jacks
                allExercises[4], // Squats
                allExercises[3], // High Knees
                allExercises[2], // Mountain Climbers
                allExercises[6], // Plank
                allExercises[1]  // Burpees
            )
        ),
        WorkoutPlan(
            id = "plan_flat_abs",
            title = "Bụng Phẳng & Eo Thon",
            subtitle = "Tập trung săn chắc cơ lõi và đánh tan mỡ bụng",
            description = "Chuyên biệt cho vùng bụng và eo, siết chặt các khối cơ bụng ngang và cơ liên sườn giúp eo thon gọn và bụng phẳng lì.",
            level = "Trung bình",
            estimatedMinutes = 12,
            estimatedCalories = 120,
            restBetweenSeconds = 15,
            exercises = listOf(
                allExercises[6], // Plank
                allExercises[7], // Bicycle Crunches
                allExercises[2], // Mountain Climbers
                allExercises[9], // Russian Twists
                allExercises[6]  // Plank kết thúc
            )
        ),
        WorkoutPlan(
            id = "plan_beginner_burn",
            title = "Khởi Động Giảm Cân Dễ Dàng",
            subtitle = "Dành cho người mới bắt đầu làm quen",
            description = "Các động tác êm dịu, không gây sốc cho khớp xương nhưng vẫn đảm bảo kích thích lưu thông máu và tiêu hao năng lượng thừa.",
            level = "Cơ bản",
            estimatedMinutes = 10,
            estimatedCalories = 90,
            restBetweenSeconds = 20,
            exercises = listOf(
                allExercises[0], // Jumping Jacks
                allExercises[4], // Squats
                allExercises[8], // Lunges
                allExercises[6]  // Plank
            )
        ),
        WorkoutPlan(
            id = "plan_fullbody_power",
            title = "Sức Bền & Đốt Mỡ Toàn Thân",
            subtitle = "Tăng cơ giảm mỡ chuẩn vóc dáng",
            description = "Kết hợp bài tập sức mạnh cơ bắp và cardio để bảo toàn khối cơ trong quá trình ăn kiêng thâm hụt calo.",
            level = "Nâng cao",
            estimatedMinutes = 20,
            estimatedCalories = 240,
            restBetweenSeconds = 15,
            exercises = listOf(
                allExercises[0], // Jumping Jacks
                allExercises[5], // Push-ups
                allExercises[4], // Squats
                allExercises[1], // Burpees
                allExercises[8], // Lunges
                allExercises[7], // Bicycle Crunches
                allExercises[6]  // Plank
            )
        )
    )
}
