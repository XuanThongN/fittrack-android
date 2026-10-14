package com.fittrack.app.data.model

import org.json.JSONArray

/**
 * Model representing a planned running roadmap / route.
 */
data class Roadmap(
    val id: Long = 0,
    val name: String,
    val distanceMeters: Double,
    val estimatedDurationSeconds: Long,
    val waypoints: List<Pair<Double, Double>>, // User-clicked checkpoints (lat, lng)
    val routeCoordinates: List<Pair<Double, Double>>, // Full snapped street polyline (lat, lng)
    val createdAt: Long = System.currentTimeMillis()
) {
    fun distanceKmFormatted(): String {
        return "%.2f km".format(distanceMeters / 1000.0)
    }

    fun estimatedDurationFormatted(): String {
        val mins = estimatedDurationSeconds / 60
        val secs = estimatedDurationSeconds % 60
        return "%d:%02d".format(mins, secs)
    }

    fun waypointsToJson(): String {
        val array = JSONArray()
        waypoints.forEach { (lat, lng) ->
            val pt = JSONArray()
            pt.put(lat)
            pt.put(lng)
            array.put(pt)
        }
        return array.toString()
    }

    fun routeCoordinatesToJson(): String {
        val array = JSONArray()
        routeCoordinates.forEach { (lat, lng) ->
            val pt = JSONArray()
            pt.put(lat)
            pt.put(lng)
            array.put(pt)
        }
        return array.toString()
    }

    companion object {
        fun parseCoordsJson(jsonStr: String): List<Pair<Double, Double>> {
            if (jsonStr.isBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonStr)
                val list = ArrayList<Pair<Double, Double>>(array.length())
                for (i in 0 until array.length()) {
                    val pt = array.getJSONArray(i)
                    list.add(Pair(pt.getDouble(0), pt.getDouble(1)))
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}

data class PlannedRoute(
    val distanceMeters: Double,
    val estimatedDurationSeconds: Long,
    val routeCoordinates: List<Pair<Double, Double>>
)
