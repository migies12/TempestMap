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

    fun getLastKnownLocation(context: Context, locationManager: LocationManager): Location? {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        } else {
            return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }
    }

    /**
     * Start location updates
     * @param minTimeMs Minimum time between updates, in milliseconds
     * @param minDistanceM Minimum distance between updates, in meters
     */
    fun startLocationUpdates(minTimeMs: Long = 5000, minDistanceM: Float = 10f) {
        try {
            // Check if the app has location permissions
            if (!hasLocationPermission()) {
                requestLocationPermission()
                return
            }

            // Double-check permissions before requesting location updates
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Log the permission denial
                Log.w("LocationHandler", "Location permissions not granted. Requesting permissions.")
                requestLocationPermission()
                return
            }

            // Request location updates
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTimeMs,
                minDistanceM,
                locationListener
            )

            Log.d("LocationHandler", "Location updates started successfully.")
        } catch (e: SecurityException) {
            // Handle SecurityException (e.g., permissions revoked at runtime)
            Log.e("LocationHandler", "SecurityException: ${e.message}", e)
            requestLocationPermission()
        } catch (e: IllegalArgumentException) {
            // Handle invalid arguments (e.g., invalid minTimeMs or minDistanceM)
            Log.e("LocationHandler", "IllegalArgumentException: ${e.message}", e)
        } catch (e: IllegalStateException) {
            // Handle illegal state (e.g., location provider not available)
            Log.e("LocationHandler", "IllegalStateException: ${e.message}", e)
        }
    }

    /**
     * Stop location updates
     */
    fun stopLocationUpdates() {
        locationManager.removeUpdates(locationListener)
    }

}