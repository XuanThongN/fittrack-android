package com.fittrack.app.data.model

enum class ZombieDifficulty(
    val title: String,
    val speedKmh: Double,
    val description: String
) {
    WALKER("Zombie Chậm (Đi bộ nhanh)", 6.5, "Pace 9'14\" /km - Thích hợp đi bộ nhanh hoặc mới tập"),
    RUNNER("Zombie Thường (Chạy vừa)", 8.5, "Pace 7'03\" /km - Ép chạy bền duy trì tốc độ"),
    MUTANT("Zombie Đột Biến (Nước rút)", 10.5, "Pace 5'42\" /km - Thử thách bứt tốc cường độ cao")
}

enum class ZombieAlertLevel {
    SAFE,      // > 60m
    WARNING,   // 30m - 60m
    DANGER,    // < 30m
    CAUGHT     // <= 0m
}

data class ZombieChaseState(
    val isEnabled: Boolean = false,
    val difficulty: ZombieDifficulty = ZombieDifficulty.RUNNER,
    val distanceMeters: Double = 80.0, // Distance to zombies
    val alertLevel: ZombieAlertLevel = ZombieAlertLevel.SAFE,
    val caughtCount: Int = 0,
    val escapedSprintsCount: Int = 0
) {
    val distanceFormatted: String
        get() = "${distanceMeters.toInt()}m"
}
