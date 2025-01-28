package com.example.m1

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume

class ComposeBasedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocationInfoScreen()
        }
    }
}

@Composable
fun LocationInfoScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Variables to be displayed
    var cityName by remember { mutableStateOf("Fetching city name...") }
    var manufacturer by remember { mutableStateOf(Build.MANUFACTURER) }
    var model by remember { mutableStateOf(Build.MODEL) }
    var locationPermissionGranted by remember { mutableStateOf(false) }
    var locationServicesEnabled by remember { mutableStateOf(true) }

    // Activity Result Launcher for requesting permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            locationPermissionGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (locationPermissionGranted) {
                coroutineScope.launch {
                    val city = getCityName(context)
                    cityName = city ?: "Unable to fetch city name"
                }
            } else {
                cityName = "Location permission denied"
            }
        }
    )

    // Check if location services are enabled and request permissions
    LaunchedEffect(Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationServicesEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!locationServicesEnabled) {
            Log.e("LocationInfo", "Location services are disabled.")
            cityName = "Location services disabled"
            // Optionally, prompt the user to enable location services
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Display of the phone information
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "City: $cityName")
        Text(text = "Manufacturer: $manufacturer")
        Text(text = "Model: $model")
    }
}


suspend fun getCityName(context: Context): String? {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    return try {
        Log.d("getCityName", "Requesting current location...")
        val location = suspendCancellableCoroutine<android.location.Location?> { cont ->
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)

                    Log.d("getCityName", "Location obtained: ${locationResult.lastLocation}")
                    cont.resume(locationResult.lastLocation)
                    fusedLocationClient.removeLocationUpdates(this)
                }

                override fun onLocationAvailability(availability: LocationAvailability) {
                    super.onLocationAvailability(availability)
                    Log.d("getCityName", "Location availability: ${availability.isLocationAvailable}")
                }
            }

            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 1000
                fastestInterval = 500
                numUpdates = 1
            }

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("getCityName", "Location permissions are not granted.")
                cont.resume(null)
                return@suspendCancellableCoroutine
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )

            cont.invokeOnCancellation {
                Log.d("getCityName", "Coroutine cancelled, removing location updates.")
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        }

        if (location != null) {
            Log.d("getCityName", "Location received: $location")
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                Log.d("getCityName", "Address obtained: $address")

                // Attempt to retrieve city name using multiple fields
                val city = address.locality
                    ?: address.subAdminArea
                    ?: address.adminArea
                    ?: address.featureName
                    ?: "Unknown Location"

                Log.d("getCityName", "Determined city name: $city")
                city
            } else {
                Log.e("getCityName", "No addresses found for the location.")
                "Unknown City"
            }
        } else {
            Log.e("getCityName", "Location is null.")
            "Location not available"
        }
    } catch (e: Exception) {
        Log.e("LocationInfo", "Error fetching location", e)
        null
    }
}

