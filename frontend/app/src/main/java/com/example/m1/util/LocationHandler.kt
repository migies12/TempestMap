package com.example.m1.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import android.util.Log

/**
 * Handler for location-related functionality
 */
class LocationHandler(
    private val context: Context,
    private val activity: FragmentActivity,
    private val locationListener: LocationListener
) {

    private val locationManager: LocationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
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
     * Request location permissions
     */
    fun requestLocationPermission() {
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
     * Start location updates
     * @param minTimeMs Minimum time between updates, in milliseconds
     * @param minDistanceM Minimum distance between updates, in meters
     */
    fun startLocationUpdates(minTimeMs: Long = 5000, minDistanceM: Float = 10f) {
        if (hasLocationPermission()) {
            try {
                // Use checkSelfPermission again right before the call to be absolutely safe
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        minTimeMs,
                        minDistanceM,
                        locationListener
                    )
                }
            } catch (e: SecurityException) {
                // Log the exception
                Log.e("LocationHandler", "SecurityException: ${e.message}")
                // Request permissions again
                requestLocationPermission()
            }
        } else {
            requestLocationPermission()
        }
    }

    /**
     * Stop location updates
     */
    fun stopLocationUpdates() {
        locationManager.removeUpdates(locationListener)
    }

    /**
     * Get the last known location
     * @return The last known location, or null if not available
     */
    fun getLastKnownLocation(): Location? {
        if (hasLocationPermission()) {
            try {
                // Try GPS provider first with explicit permission check
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
                        return it
                    }
                }

                // Try network provider with explicit permission check
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    return locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                }
            } catch (e: SecurityException) {
                Log.e("LocationHandler", "SecurityException: ${e.message}")
            } catch (e: Exception) {
                Log.e("LocationHandler", "Error getting location: ${e.message}")
            }
        }
        return null
    }
}