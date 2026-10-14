package com.fittrack.app.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RunSession(
    val id: Long = 0,
    val startTime: Long = System.currentTimeMillis(),
    val durationSeconds: Long,
    val distanceMeters: Double,
    val caloriesBurned: Double,
    val averagePaceSecondsPerKm: Long,
    val isGpsTracked: Boolean = true,
    val notes: String = ""
) {
    val distanceKm: Double
        get() = distanceMeters / 1000.0

    fun distanceKmFormatted(): String {
        return String.format(Locale.US, "%.2f km", distanceKm)
    }

    fun paceFormatted(): String {
        if (averagePaceSecondsPerKm <= 0 || averagePaceSecondsPerKm > 3600) return "--'--\""
        val minutes = averagePaceSecondsPerKm / 60
        val seconds = averagePaceSecondsPerKm % 60
        return String.format(Locale.US, "%d'%02d\" /km", minutes, seconds)
    }

    fun durationFormatted(): String {
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        val seconds = durationSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun speedKmhFormatted(): String {
        if (durationSeconds <= 0) return "0.0 km/h"
        val hours = durationSeconds / 3600.0
        val speed = distanceKm / hours
        return String.format(Locale.US, "%.1f km/h", speed)
    }

    fun caloriesFormatted(): String {
        return String.format(Locale.US, "%.0f kcal", caloriesBurned)
    }

    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(startTime))
    }
}
