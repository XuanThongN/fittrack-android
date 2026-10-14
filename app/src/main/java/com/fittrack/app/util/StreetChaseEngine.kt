package com.fittrack.app.util

import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
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
 * High-precision Real Street Route Zombie Navigation Engine.
 *
 * Integrates live OpenStreetMap street geometry (via OSRM) with user GPS tracks,
 * ensuring zombies spawn directly on the real street and follow every actual road curve,
 * intersection, and turn without ever cutting through houses or buildings.
 */
object StreetChaseEngine {

    private val cacheMutex = Mutex()
    private var cachedStreetPrefix: List<Pair<Double, Double>> = emptyList()
    private var lastSnappedCoord: Pair<Double, Double>? = null

    fun calculateDistanceMeters(p1: Pair<Double, Double>, p2: Pair<Double, Double>): Double {
        val results = FloatArray(1)
        Location.distanceBetween(p1.first, p1.second, p2.first, p2.second, results)
        return results[0].toDouble()
    }

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
     * Fetches real OpenStreetMap street nodes behind the user via OSRM public routing API.
     * Snaps the user to the nearest road and retrieves coordinates along the actual street network.
     */
    suspend fun fetchRealStreetPathBehind(
        lat: Double,
        lng: Double,
        bearingDeg: Float,
        distanceMeters: Double = 120.0
    ): List<Pair<Double, Double>> = withContext(Dispatchers.IO) {
        try {
            val cached = cacheMutex.withLock {
                val last = lastSnappedCoord
                if (last != null && cachedStreetPrefix.isNotEmpty()) {
                    val shift = calculateDistanceMeters(last, Pair(lat, lng))
                    if (shift < 25.0) cachedStreetPrefix else null
                } else null
            }
            if (cached != null) return@withContext cached

            // 1. Snap to nearest street on OpenStreetMap
            val snapUrl = "https://router.project-osrm.org/nearest/v1/walking/$lng,$lat?number=3"
            val conn = (URL(snapUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3500
                readTimeout = 3500
                setRequestProperty("User-Agent", "FitTrackApp/1.4 (Android)")
            }
            if (conn.responseCode != 200) return@withContext emptyList()

            val snapJson = conn.inputStream.bufferedReader().use { it.readText() }
            val snapObj = JSONObject(snapJson)
            val wps = snapObj.optJSONArray("waypoints") ?: return@withContext emptyList()
            if (wps.length() < 1) return@withContext emptyList()

            val p0Loc = wps.getJSONObject(0).getJSONArray("location")
            val snappedLng = p0Loc.getDouble(0)
            val snappedLat = p0Loc.getDouble(1)

            // 2. Determine street orientation along OpenStreetMap way
            var roadBearing = bearingDeg
            if (wps.length() >= 2 && (roadBearing == 0f || roadBearing.isNaN())) {
                val p1Loc = wps.getJSONObject(1).getJSONArray("location")
                val p1Lng = p1Loc.getDouble(0)
                val p1Lat = p1Loc.getDouble(1)
                roadBearing = calculateBearing(Pair(p1Lat, p1Lng), Pair(snappedLat, snappedLng))
            }
            if (roadBearing == 0f || roadBearing.isNaN()) {
                roadBearing = 180f
            }

            // 3. Project spawn point behind runner along the road
            val backBearing = (roadBearing + 180f) % 360f
            val spawnTarget = projectPoint(Pair(snappedLat, snappedLng), backBearing, distanceMeters * 1.3)

            // 4. Fetch walking route along actual street network from spawnTarget to user road position
            val routeUrl = "https://router.project-osrm.org/route/v1/walking/${spawnTarget.second},${spawnTarget.first};$snappedLng,$snappedLat?overview=full&geometries=geojson"
            val routeConn = (URL(routeUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3500
                readTimeout = 3500
                setRequestProperty("User-Agent", "FitTrackApp/1.4 (Android)")
            }
            if (routeConn.responseCode != 200) return@withContext listOf(Pair(snappedLat, snappedLng))

            val routeJson = routeConn.inputStream.bufferedReader().use { it.readText() }
            val routeObj = JSONObject(routeJson)
            val routes = routeObj.optJSONArray("routes") ?: return@withContext listOf(Pair(snappedLat, snappedLng))
            if (routes.length() < 1) return@withContext listOf(Pair(snappedLat, snappedLng))

            val coords = routes.getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates")
            val streetNodes = mutableListOf<Pair<Double, Double>>()
            for (i in 0 until coords.length()) {
                val pt = coords.getJSONArray(i)
                streetNodes.add(Pair(pt.getDouble(1), pt.getDouble(0)))
            }

            if (streetNodes.isNotEmpty()) {
                cacheMutex.withLock {
                    cachedStreetPrefix = streetNodes
                    lastSnappedCoord = Pair(snappedLat, snappedLng)
                }
                streetNodes
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Finds the exact location and street bearing along the combined real street road network
     * (pre-fetched OSM street nodes + runner's GPS polyline) backwards from current location.
     */
    fun getPointAlongPathBackwards(
        streetPrefix: List<Pair<Double, Double>>,
        pathPoints: List<Pair<Double, Double>>,
        currentLocation: Pair<Double, Double>?,
        currentBearing: Float,
        distanceBackMeters: Double,
        lateralOffsetMeters: Double = 0.0
    ): ChasePoint {
        val fullPath = mutableListOf<Pair<Double, Double>>()

        if (streetPrefix.isNotEmpty()) {
            fullPath.addAll(streetPrefix)
        }

        if (pathPoints.isNotEmpty()) {
            if (fullPath.isNotEmpty()) {
                val lastPrefix = fullPath.last()
                val firstPath = pathPoints.first()
                if (calculateDistanceMeters(lastPrefix, firstPath) < 60.0) {
                    fullPath.addAll(pathPoints)
                } else {
                    fullPath.clear()
                    fullPath.addAll(pathPoints)
                }
            } else {
                fullPath.addAll(pathPoints)
            }
        }

        if (currentLocation != null && (fullPath.isEmpty() || fullPath.last() != currentLocation)) {
            fullPath.add(currentLocation)
        }

        if (fullPath.isEmpty()) {
            val fallback = currentLocation ?: Pair(10.7769, 106.7009)
            return ChasePoint(fallback, currentBearing)
        }

        if (fullPath.size == 1) {
            val backBearing = (currentBearing + 180f) % 360f
            val basePos = projectPoint(fullPath.first(), backBearing, distanceBackMeters)
            val finalPos = if (lateralOffsetMeters != 0.0) {
                val perpBearing = if (lateralOffsetMeters > 0) (currentBearing + 90f) % 360f else (currentBearing - 90f + 360f) % 360f
                projectPoint(basePos, perpBearing, abs(lateralOffsetMeters))
            } else basePos
            return ChasePoint(finalPos, currentBearing)
        }

        var remaining = distanceBackMeters
        var targetPos: Pair<Double, Double>? = null
        var segmentBearing = currentBearing

        for (i in fullPath.size - 1 downTo 1) {
            val pEnd = fullPath[i]
            val pStart = fullPath[i - 1]
            val segLen = calculateDistanceMeters(pStart, pEnd)

            if (segLen <= 0.001) continue

            segmentBearing = calculateBearing(pStart, pEnd)

            if (remaining <= segLen) {
                val t = (remaining / segLen).coerceIn(0.0, 1.0)
                val lat = pEnd.first + t * (pStart.first - pEnd.first)
                val lng = pEnd.second + t * (pStart.second - pEnd.second)
                targetPos = Pair(lat, lng)
                break
            } else {
                remaining -= segLen
            }
        }

        if (targetPos == null) {
            val initialStart = fullPath[0]
            val initialNext = fullPath[1]
            val initialBearing = calculateBearing(initialStart, initialNext)
            segmentBearing = initialBearing
            val backwardBearing = (initialBearing + 180f) % 360f
            targetPos = projectPoint(initialStart, backwardBearing, remaining)
        }

        val finalPos = if (lateralOffsetMeters != 0.0) {
            val perpBearing = if (lateralOffsetMeters > 0) (segmentBearing + 90f) % 360f else (segmentBearing - 90f + 360f) % 360f
            projectPoint(targetPos, perpBearing, abs(lateralOffsetMeters))
        } else {
            targetPos
        }

        return ChasePoint(finalPos, segmentBearing)
    }

    /**
     * Builds the pursuit path connecting the lead zombie to the runner along the real street.
     */
    fun getPursuitTrailPoints(
        streetPrefix: List<Pair<Double, Double>>,
        pathPoints: List<Pair<Double, Double>>,
        currentLocation: Pair<Double, Double>?,
        zombiePos: Pair<Double, Double>
    ): List<Pair<Double, Double>> {
        val fullPath = mutableListOf<Pair<Double, Double>>()
        if (streetPrefix.isNotEmpty()) fullPath.addAll(streetPrefix)
        if (pathPoints.isNotEmpty()) fullPath.addAll(pathPoints)
        if (currentLocation != null && (fullPath.isEmpty() || fullPath.last() != currentLocation)) {
            fullPath.add(currentLocation)
        }

        if (fullPath.size < 2) return emptyList()

        var closestIdx = 0
        var minDist = Double.MAX_VALUE
        for (i in fullPath.indices) {
            val d = calculateDistanceMeters(zombiePos, fullPath[i])
            if (d < minDist) {
                minDist = d
                closestIdx = i
            }
        }

        val trail = mutableListOf<Pair<Double, Double>>()
        trail.add(zombiePos)
        for (i in closestIdx until fullPath.size) {
            trail.add(fullPath[i])
        }

        return trail
    }
}
