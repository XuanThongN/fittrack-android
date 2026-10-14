package com.fittrack.app.data.model

import java.util.Locale

data class UserProfile(
    val name: String = "Người dùng",
    val heightCm: Double = 170.0,
    val initialWeightKg: Double = 75.0,
    val targetWeightKg: Double = 65.0,
    val gender: String = "male", // "male" or "female"
    val age: Int = 26,
    val activityLevel: String = "moderate" // sedentary, light, moderate, active, very_active
) {
    fun calculateBmi(weightKg: Double): Double {
        if (heightCm <= 0.0) return 0.0
        val heightM = heightCm / 100.0
        val bmi = weightKg / (heightM * heightM)
        return String.format(Locale.US, "%.1f", bmi).toDoubleOrNull() ?: bmi
    }

    // Phân loại BMI theo chuẩn WHO cho người châu Á
    fun getBmiCategory(bmi: Double): BmiCategory {
        return when {
            bmi < 18.5 -> BmiCategory("Thiếu cân", 0xFF38BDF8) // Light blue
            bmi < 23.0 -> BmiCategory("Bình thường (Lý tưởng)", 0xFF10B981) // Emerald Green
            bmi < 25.0 -> BmiCategory("Thừa cân (Tiền béo phì)", 0xFFF59E0B) // Amber
            bmi < 30.0 -> BmiCategory("Béo phì độ I", 0xFFF97316) // Orange
            else -> BmiCategory("Béo phì độ II", 0xFFEF4444) // Red
        }
    }

    // Tính BMR (Mifflin-St Jeor)
    fun calculateBmr(weightKg: Double): Double {
        val base = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age)
        return if (gender.lowercase() == "female") {
            base - 161.0
        } else {
            base + 5.0
        }
    }

    // Tính TDEE theo mức độ vận động
    fun calculateTdee(weightKg: Double): Double {
        val bmr = calculateBmr(weightKg)
        val multiplier = when (activityLevel.lowercase()) {
            "sedentary" -> 1.2
            "light" -> 1.375
            "moderate" -> 1.55
            "active" -> 1.725
            "very_active" -> 1.9
            else -> 1.55
        }
        return bmr * multiplier
    }

    // Lượng calo khuyến nghị tiêu thụ để giảm cân 0.5kg/tuần (thâm hụt 500 kcal)
    fun getWeightLossTargetCalories(weightKg: Double): Double {
        val tdee = calculateTdee(weightKg)
        val target = tdee - 500.0
        // Không nên nạp dưới 1200 kcal/ngày để đảm bảo an toàn sức khoẻ
        return if (target < 1200.0) 1200.0 else target
    }

    // Tính % hoàn thành mục tiêu cân nặng
    fun getProgressPercentage(currentWeightKg: Double): Float {
        val totalToLose = initialWeightKg - targetWeightKg
        if (totalToLose <= 0.0) return 1.0f
        val lostSoFar = initialWeightKg - currentWeightKg
        val progress = (lostSoFar / totalToLose).toFloat()
        return progress.coerceIn(0.0f, 1.0f)
    }
}

data class BmiCategory(
    val label: String,
    val colorHex: Long
)
