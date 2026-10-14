package com.fittrack.app.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationTracker(private val context: Context) : LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private var lastLocation: Location? = null

    private val _distanceMeters = MutableStateFlow(0.0)
    val distanceMeters: StateFlow<Double> = _distanceMeters.asStateFlow()

    private val _currentSpeedMps = MutableStateFlow(0f)
    val currentSpeedMps: StateFlow<Float> = _currentSpeedMps.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    // GPS route path points (latitude, longitude) for drawing on Map
    private val _pathPoints = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val pathPoints: StateFlow<List<Pair<Double, Double>>> = _pathPoints.asStateFlow()

    private val _currentLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentLocation: StateFlow<Pair<Double, Double>?> = _currentLocation.asStateFlow()

    private val _currentBearing = MutableStateFlow(0f)
    val currentBearing: StateFlow<Float> = _currentBearing.asStateFlow()

    init {
        // Query best last known location immediately upon instantiation
        fetchBestLastKnownLocation()
    }

    fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun fetchBestLastKnownLocation(): Pair<Double, Double>? {
        if (!hasPermission() || locationManager == null) return null

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        var bestLoc: Location? = null

        for (provider in providers) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = locationManager.getLastKnownLocation(provider) ?: continue
                    if (bestLoc == null || loc.time > bestLoc.time) {
                        bestLoc = loc
                    }
                }
            } catch (_: Exception) {}
        }

        if (bestLoc != null) {
            val point = Pair(bestLoc.latitude, bestLoc.longitude)
            _currentLocation.value = point
            lastLocation = bestLoc
            return point
        }
        return null
    }

    @SuppressLint("MissingPermission")
    fun startListening() {
        if (!hasPermission() || locationManager == null) return
        if (_isListening.value) return

        fetchBestLastKnownLocation()
        _isListening.value = true

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L, // 1 second
                    1f,    // 1 meter
                    this
                )
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1500L,
                    2f,
                    this
                )
            }
        } catch (_: Exception) {}
    }

    fun stopListening() {
        _isListening.value = false
        if (!_isTracking.value) {
            try {
                locationManager?.removeUpdates(this)
            } catch (_: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (!hasPermission() || locationManager == null) return

        startListening()
        _distanceMeters.value = 0.0
        _isTracking.value = true

        val cur = _currentLocation.value
        if (cur != null) {
            _pathPoints.value = listOf(cur)
        } else {
            _pathPoints.value = emptyList()
        }
        lastLocation = null
    }

    fun stopTracking() {
        _isTracking.value = false
        // Keep listening active so the map continues to show current position
    }

    fun reset() {
        stopTracking()
        _distanceMeters.value = 0.0
        _pathPoints.value = emptyList()
        _currentSpeedMps.value = 0f
        _currentBearing.value = 0f
        lastLocation = null
    }

    fun destroy() {
        _isTracking.value = false
        _isListening.value = false
        try {
            locationManager?.removeUpdates(this)
        } catch (_: Exception) {}
    }

    override fun onLocationChanged(location: Location) {
        val point = Pair(location.latitude, location.longitude)

        // Always update current location pin for map display
        _currentLocation.value = point

        if (location.hasBearing() && location.bearing != 0f) {
            _currentBearing.value = location.bearing
        }

        if (location.hasSpeed()) {
            _currentSpeedMps.value = location.speed
        }

        // If not actively running a workout, don't accumulate distance
        if (!_isTracking.value) return

        // For workout distance tracking, filter out unrealistic jumps or jitter
        if (location.hasAccuracy() && location.accuracy > 45f) return

        val prev = lastLocation
        if (prev != null) {
            val delta = prev.distanceTo(location)
            if (!location.hasBearing()) {
                _currentBearing.value = (prev.bearingTo(location) + 360f) % 360f
            }
            // Filter realistic movement (between 1.0m and 50m per interval)
            if (delta in 1.0..50.0) {
                _distanceMeters.value += delta
                val currentList = _pathPoints.value.toMutableList()
                currentList.add(point)
                _pathPoints.value = currentList
            }
        } else {
            _pathPoints.value = listOf(point)
        }
        lastLocation = location
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
