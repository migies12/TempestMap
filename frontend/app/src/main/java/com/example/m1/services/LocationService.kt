package com.example.m1.services

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Singleton service to handle location updates across the app
 */
class LocationService private constructor(private val context: Context) : LocationListener {

    private val TAG = "LocationService"

    private val locationManager: LocationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    // LiveData to observe location updates
    private val _currentLocation = MutableLiveData<Location>()
    val currentLocation: LiveData<Location> = _currentLocation

    // Callback for location permission request results
    private var permissionCallback: ((Boolean) -> Unit)? = null

    companion object {
        private const val MIN_TIME_BETWEEN_UPDATES = 5000L // 5 seconds
        private const val MIN_DISTANCE_CHANGE = 10f // 10 meters
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001

        @Volatile
        private var INSTANCE: LocationService? = null

        fun getInstance(context: Context): LocationService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        // Try to get last known location immediately if we have permission
        if (hasLocationPermission()) {
            getLastKnownLocation()?.let {
                _currentLocation.value = it
            }
        }
    }

    /**
     * Check if location permissions are granted
     * @return True if permissions are granted, false otherwise
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request location permissions using the provided activity
     */
    fun requestLocationPermission(activity: androidx.fragment.app.FragmentActivity, callback: (Boolean) -> Unit) {
        permissionCallback = callback
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Handle permission request result
     */
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED

            permissionCallback?.invoke(granted)
            permissionCallback = null

            if (granted) {
                startLocationUpdates()
            }
        }
    }

    /**
     * Start location updates
     */
    fun startLocationUpdates() {
        try {
            // Check if we have permission
            if (!hasLocationPermission()) {
                Log.w(TAG, "Location permission not granted")
                return
            }

            // Check for permission again (to avoid IDE warning)
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            // Request location updates
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_BETWEEN_UPDATES,
                MIN_DISTANCE_CHANGE,
                this
            )

            // Also request updates from network provider as fallback
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                MIN_TIME_BETWEEN_UPDATES,
                MIN_DISTANCE_CHANGE,
                this
            )

            Log.d(TAG, "Location updates started")

            // Try to get last known location immediately
            getLastKnownLocation()?.let {
                _currentLocation.value = it
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting location updates", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location updates", e)
        }
    }

    /**
     * Stop location updates
     */
    fun stopLocationUpdates() {
        try {
            locationManager.removeUpdates(this)
            Log.d(TAG, "Location updates stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location updates", e)
        }
    }

    /**
     * Get last known location if available
     */
    fun getLastKnownLocation(): Location? {
        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return null
            }

            // Try GPS provider first
            var location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            // Fall back to network provider if GPS didn't work
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }

            return location
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last known location", e)
            return null
        }
    }

    /**
     * Save location to shared preferences
     */
    fun saveLocationToPreferences(location: Location) {
        val sharedPreferences = context.getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putFloat("latitude", location.latitude.toFloat())
            .putFloat("longitude", location.longitude.toFloat())
            .apply()

        Log.d(TAG, "Saved location to preferences: ${location.latitude}, ${location.longitude}")
    }

    /**
     * LocationListener implementation
     */
    override fun onLocationChanged(location: Location) {
        Log.d(TAG, "Location changed: ${location.latitude}, ${location.longitude}")
        _currentLocation.value = location
        saveLocationToPreferences(location)
    }
}