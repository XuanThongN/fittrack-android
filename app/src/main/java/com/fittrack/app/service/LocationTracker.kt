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
        lastLocation = null
        _isTracking.value = true

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000L, // 2 seconds
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
        lastLocation = null
        _currentSpeedMps.value = 0f
    }

    override fun onLocationChanged(location: Location) {
        if (!_isTracking.value) return
        if (location.hasAccuracy() && location.accuracy > 30f) return

        val prev = lastLocation
        if (prev != null) {
            val delta = prev.distanceTo(location)
            // Filter realistic running speed jumps (under 12 m/s ~ 43 km/h)
            if (delta in 1.0..60.0) {
                _distanceMeters.value += delta
            }
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
