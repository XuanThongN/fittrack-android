package com.fittrack.app.util

import android.location.Location
import com.fittrack.app.data.model.PlannedRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Engine for pre-run roadmap calculation, road-snapped waypoint routing,
 * distance estimation, and off-track monitoring.
 */
object RoadmapEngine {

    /**
     * Calculates walking roadmap geometry snapping to OpenStreetMap streets
     * across an ordered sequence of 2+ waypoints.
     */
    suspend fun calculateRoute(waypoints: List<Pair<Double, Double>>): PlannedRoute = withContext(Dispatchers.IO) {
        if (waypoints.isEmpty()) {
            return@withContext PlannedRoute(0.0, 0L, emptyList())
        }
        if (waypoints.size == 1) {
            return@withContext PlannedRoute(0.0, 0L, listOf(waypoints[0]))
        }

        try {
            // Build OSRM query: lng1,lat1;lng2,lat2;...
            val coordsParam = waypoints.joinToString(";") { "${it.second},${it.first}" }
            val routeUrl = "https://router.project-osrm.org/route/v1/walking/$coordsParam?overview=full&geometries=geojson"

            val conn = (URL(routeUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "FitTrackApp/1.7 (com.fittrack.app; Android)")
            }

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                val routes = root.optJSONArray("routes")
                if (routes != null && routes.length() > 0) {
                    val r0 = routes.getJSONObject(0)
                    val distMeters = r0.optDouble("distance", 0.0)
                    val durSeconds = r0.optDouble("duration", 0.0).toLong()

                    val geomCoords = r0.getJSONObject("geometry").getJSONArray("coordinates")
                    val fullCoords = ArrayList<Pair<Double, Double>>(geomCoords.length())
                    for (i in 0 until geomCoords.length()) {
                        val pt = geomCoords.getJSONArray(i)
                        fullCoords.add(Pair(pt.getDouble(1), pt.getDouble(0)))
                    }

                    if (fullCoords.isNotEmpty()) {
                        return@withContext PlannedRoute(
                            distanceMeters = distMeters,
                            estimatedDurationSeconds = durSeconds,
                            routeCoordinates = fullCoords
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to straight-line interpolation if offline or OSRM unavailable
        }

        // Direct straight-line segments fallback
        var fallbackDist = 0.0
        val interpolated = mutableListOf<Pair<Double, Double>>()
        for (i in 0 until waypoints.size - 1) {
            val pA = waypoints[i]
            val pB = waypoints[i + 1]
            val segDist = distanceBetween(pA, pB)
            fallbackDist += segDist

            interpolated.add(pA)
            val steps = (segDist / 20.0).toInt().coerceIn(1, 15)
            for (s in 1 until steps) {
                val frac = s.toDouble() / steps
                interpolated.add(
                    Pair(
                        pA.first + (pB.first - pA.first) * frac,
                        pA.second + (pB.second - pA.second) * frac
                    )
                )
            }
        }
        interpolated.add(waypoints.last())

        // Estimate duration based on average jogging speed of 9 km/h (2.5 m/s)
        val estimatedDur = (fallbackDist / 2.5).toLong()
        PlannedRoute(
            distanceMeters = fallbackDist,
            estimatedDurationSeconds = estimatedDur,
            routeCoordinates = interpolated
        )
    }

    /**
     * Calculates minimum perpendicular distance in meters from a point to the roadmap polyline.
     */
    fun distanceToRoute(point: Pair<Double, Double>, route: List<Pair<Double, Double>>): Double {
        if (route.isEmpty()) return 0.0
        if (route.size == 1) return distanceBetween(point, route[0])

        var minDist = Double.MAX_VALUE
        for (i in 0 until route.size - 1) {
            val d = distToSegment(point, route[i], route[i + 1])
            if (d < minDist) {
                minDist = d
            }
        }
        return minDist
    }

    /**
     * Calculates the runner's progress ratio (0.0f to 1.0f) along the roadmap polyline.
     */
    fun calculateProgressRatio(point: Pair<Double, Double>, route: List<Pair<Double, Double>>): Float {
        if (route.size < 2) return 0f
        var bestIndex = 0
        var minDist = Double.MAX_VALUE
        for (i in route.indices) {
            val d = distanceBetween(point, route[i])
            if (d < minDist) {
                minDist = d
                bestIndex = i
            }
        }
        return (bestIndex.toFloat() / (route.size - 1).toFloat()).coerceIn(0f, 1f)
    }

    fun distanceBetween(p1: Pair<Double, Double>, p2: Pair<Double, Double>): Double {
        val results = FloatArray(1)
        Location.distanceBetween(p1.first, p1.second, p2.first, p2.second, results)
        return results[0].toDouble()
    }

    private fun distToSegment(p: Pair<Double, Double>, v: Pair<Double, Double>, w: Pair<Double, Double>): Double {
        val l2 = distSq(v, w)
        if (l2 == 0.0) return distanceBetween(p, v)
        // Project p onto line vw
        val t = (((p.first - v.first) * (w.first - v.first) + (p.second - v.second) * (w.second - v.second)) / l2)
            .coerceIn(0.0, 1.0)
        val proj = Pair(v.first + t * (w.first - v.first), v.second + t * (w.second - v.second))
        return distanceBetween(p, proj)
    }

    private fun distSq(p1: Pair<Double, Double>, p2: Pair<Double, Double>): Double {
        val dLat = p1.first - p2.first
        val dLng = p1.second - p2.second
        return dLat * dLat + dLng * dLng
    }
}
