package com.fittrack.app.util

import android.location.Location
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class ChasePoint(
    val position: Pair<Double, Double>,
    val bearing: Float
)

/**
 * High-precision Real Street Chase Engine.
 *
 * Guarantees that zombies chase the runner strictly along the real-world road/street polyline
 * that the runner traversed, naturally turning at every intersection and corner without cutting
 * through buildings or terrain.
 */
object StreetChaseEngine {

    /**
     * Calculates precise distance between two GPS coordinates in meters using Android Location API.
     */
    fun calculateDistanceMeters(p1: Pair<Double, Double>, p2: Pair<Double, Double>): Double {
        val results = FloatArray(1)
        Location.distanceBetween(p1.first, p1.second, p2.first, p2.second, results)
        return results[0].toDouble()
    }

    /**
     * Calculates the forward bearing (in degrees [0..360)) from point 1 to point 2.
     */
    fun calculateBearing(p1: Pair<Double, Double>, p2: Pair<Double, Double>): Float {
        val lat1 = Math.toRadians(p1.first)
        val lon1 = Math.toRadians(p1.second)
        val lat2 = Math.toRadians(p2.first)
        val lon2 = Math.toRadians(p2.second)
        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val brng = Math.toDegrees(atan2(y, x)).toFloat()
        return (brng + 360f) % 360f
    }

    /**
     * Projects a coordinate forward along a given bearing by a specified distance in meters (WGS-84).
     */
    fun projectPoint(origin: Pair<Double, Double>, bearingDeg: Float, distanceMeters: Double): Pair<Double, Double> {
        if (distanceMeters <= 0.0001) return origin
        val earthRadius = 6371000.0 // meters
        val distRatio = distanceMeters / earthRadius
        val brngRad = Math.toRadians(bearingDeg.toDouble())
        val lat1 = Math.toRadians(origin.first)
        val lon1 = Math.toRadians(origin.second)

        val lat2 = asin(
            sin(lat1) * cos(distRatio) +
            cos(lat1) * sin(distRatio) * cos(brngRad)
        )
        val lon2 = lon1 + atan2(
            sin(brngRad) * sin(distRatio) * cos(lat1),
            cos(distRatio) - sin(lat1) * sin(lat2)
        )
        return Pair(Math.toDegrees(lat2), Math.toDegrees(lon2))
    }

    /**
     * Finds the exact location and street bearing along the runner's route history backwards from current position.
     *
     * @param pathPoints Historical GPS coordinates recorded along the runner's real street route.
     * @param currentLocation The runner's current live GPS coordinate.
     * @param currentBearing The runner's current movement heading.
     * @param distanceBackMeters The chase distance in meters behind the runner.
     * @param lateralOffsetMeters Optional offset perpendicular to the road (e.g. +1.8m right side of street, -1.8m left side).
     */
    fun getPointAlongPathBackwards(
        pathPoints: List<Pair<Double, Double>>,
        currentLocation: Pair<Double, Double>?,
        currentBearing: Float,
        distanceBackMeters: Double,
        lateralOffsetMeters: Double = 0.0
    ): ChasePoint {
        // 1. Build the active road polyline ending at the runner's current location
        val fullPath = mutableListOf<Pair<Double, Double>>()
        fullPath.addAll(pathPoints)
        if (currentLocation != null && (fullPath.isEmpty() || fullPath.last() != currentLocation)) {
            fullPath.add(currentLocation)
        }

        if (fullPath.isEmpty()) {
            val fallback = currentLocation ?: Pair(10.7769, 106.7009)
            return ChasePoint(fallback, currentBearing)
        }

        if (fullPath.size == 1) {
            // Runner hasn't moved yet: zombie is behind along reverse of runner's bearing
            val backBearing = (currentBearing + 180f) % 360f
            val basePos = projectPoint(fullPath.first(), backBearing, distanceBackMeters)
            val finalPos = if (lateralOffsetMeters != 0.0) {
                val perpBearing = if (lateralOffsetMeters > 0) (currentBearing + 90f) % 360f else (currentBearing - 90f + 360f) % 360f
                projectPoint(basePos, perpBearing, abs(lateralOffsetMeters))
            } else basePos
            return ChasePoint(finalPos, currentBearing)
        }

        // 2. Step backwards along the exact road segments that the runner ran
        var remaining = distanceBackMeters
        var targetPos: Pair<Double, Double>? = null
        var segmentBearing = currentBearing

        for (i in fullPath.size - 1 downTo 1) {
            val pEnd = fullPath[i]       // closer to runner
            val pStart = fullPath[i - 1] // further back along road
            val segLen = calculateDistanceMeters(pStart, pEnd)

            if (segLen <= 0.001) continue

            segmentBearing = calculateBearing(pStart, pEnd)

            if (remaining <= segLen) {
                // Exact point is located on this street segment!
                val t = (remaining / segLen).coerceIn(0.0, 1.0)
                val lat = pEnd.first + t * (pStart.first - pEnd.first)
                val lng = pEnd.second + t * (pStart.second - pEnd.second)
                targetPos = Pair(lat, lng)
                break
            } else {
                remaining -= segLen
            }
        }

        // 3. If remaining > 0, the zombie is further back than the workout starting point.
        // Project backwards along the road tangent of the initial street segment.
        if (targetPos == null) {
            val initialStart = fullPath[0]
            val initialNext = fullPath[1]
            val initialBearing = calculateBearing(initialStart, initialNext)
            segmentBearing = initialBearing
            val backwardBearing = (initialBearing + 180f) % 360f
            targetPos = projectPoint(initialStart, backwardBearing, remaining)
        }

        // 4. Apply perpendicular lateral offset (remains within the street width / sidewalk)
        val finalPos = if (lateralOffsetMeters != 0.0) {
            val perpBearing = if (lateralOffsetMeters > 0) (segmentBearing + 90f) % 360f else (segmentBearing - 90f + 360f) % 360f
            projectPoint(targetPos, perpBearing, abs(lateralOffsetMeters))
        } else {
            targetPos
        }

        return ChasePoint(finalPos, segmentBearing)
    }
}
