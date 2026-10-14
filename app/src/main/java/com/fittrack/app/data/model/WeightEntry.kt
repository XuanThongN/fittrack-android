package com.fittrack.app.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WeightEntry(
    val id: Long = 0,
    val weightKg: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val bodyFatPercentage: Double? = null
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getFormattedDateTime(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
