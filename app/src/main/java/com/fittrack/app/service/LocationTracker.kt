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

    // GPS route path points (latitude, longitude) for drawing on Map
    private val _pathPoints = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val pathPoints: StateFlow<List<Pair<Double, Double>>> = _pathPoints.asStateFlow()

    private val _currentLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentLocation: StateFlow<Pair<Double, Double>?> = _currentLocation.asStateFlow()

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (!hasPermission() || locationManager == null) return

        _distanceMeters.value = 0.0
        _pathPoints.value = emptyList()
        lastLocation = null
        _isTracking.value = true

        try {
            // Get last known location right away to show on map immediately
            val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val initial = lastGps ?: lastNet
            if (initial != null) {
                _currentLocation.value = Pair(initial.latitude, initial.longitude)
                _pathPoints.value = listOf(Pair(initial.latitude, initial.longitude))
                lastLocation = initial
            }

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1500L, // 1.5 seconds
                    2f,    // 2 meters
                    this
                )
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L,
                    2f,
                    this
                )
            }
        } catch (_: Exception) {}
    }

    fun stopTracking() {
        _isTracking.value = false
        try {
            locationManager?.removeUpdates(this)
        } catch (_: Exception) {}
    }

    fun reset() {
        stopTracking()
        _distanceMeters.value = 0.0
        _pathPoints.value = emptyList()
        _currentLocation.value = null
        lastLocation = null
        _currentSpeedMps.value = 0f
    }

    override fun onLocationChanged(location: Location) {
        if (!_isTracking.value) return
        if (location.hasAccuracy() && location.accuracy > 35f) return

        val point = Pair(location.latitude, location.longitude)
        _currentLocation.value = point

        val prev = lastLocation
        if (prev != null) {
            val delta = prev.distanceTo(location)
            // Filter realistic running movements (between 1.5m and 50m per interval)
            if (delta in 1.5..50.0) {
                _distanceMeters.value += delta
                val currentList = _pathPoints.value.toMutableList()
                currentList.add(point)
                _pathPoints.value = currentList
            }
        } else {
            // First point
            _pathPoints.value = listOf(point)
        }
        lastLocation = location

        if (location.hasSpeed()) {
            _currentSpeedMps.value = location.speed
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
