package com.example.m1.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.m1.FavoriteLocation
import com.example.m1.FavoriteLocationManager
import com.example.m1.R
import com.example.m1.data.models.Event
import com.example.m1.data.models.UserMarker
import com.example.m1.ui.dialogs.CreateMarkerDialog
import com.example.m1.ui.dialogs.EventBottomSheetDialog
import com.example.m1.ui.dialogs.UserMarkerBottomSheetDialog
import com.example.m1.ui.map.MarkerManager
import com.example.m1.ui.viewmodels.MapViewModel
import com.example.m1.util.LocationHandler
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.toCameraOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MapboxFragment : Fragment(), LocationListener {

    // ViewModel
    private lateinit var viewModel: MapViewModel

    // UI Components
    private lateinit var mapView: MapView
    private lateinit var fabAddMarker: FloatingActionButton

    // Mapbox components
    private lateinit var mapboxMap: MapboxMap
    private lateinit var homeAnnotationManager: PointAnnotationManager
    private lateinit var eventAnnotationManager: PointAnnotationManager
    private lateinit var userAnnotationManager: PointAnnotationManager
    private lateinit var locationManager: LocationManager

    // Helper classes
    private lateinit var locationHandler: LocationHandler
    private lateinit var markerManager: MarkerManager
    private lateinit var favoriteLocationManager: FavoriteLocationManager

    // State
    private var previousCameraOptions: CameraOptions? = null
    private var markerPlacementMode = false
    private var selectedPoint: Point? = null
    private var favoriteMarkerAnnotationManager: PointAnnotationManager? = null
    private var selectedPointAnnotationManager: PointAnnotationManager? = null
    private var lastKnownLocation: Location? = null
    private val fetchScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "MapboxFragment"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mapbox, container, false)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[MapViewModel::class.java]

        // Initialize UI components
        mapView = view.findViewById(R.id.mapView)
        fabAddMarker = view.findViewById(R.id.fabAddMarker)

        // Get the MapboxMap instance
        mapboxMap = mapView.mapboxMap

        // Initialize helper classes
        locationHandler = LocationHandler(requireContext(), requireActivity(), this)
        markerManager = MarkerManager(requireContext())
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        favoriteLocationManager = FavoriteLocationManager(requireContext())

        // Set initial camera position
        mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(-95.7129, 37.0902)) // Center on North America
                .zoom(2.0)
                .build()
        )

        // Initialize annotation managers
        eventAnnotationManager = mapView.annotations.createPointAnnotationManager()
        userAnnotationManager = mapView.annotations.createPointAnnotationManager()
        homeAnnotationManager = mapView.annotations.createPointAnnotationManager()
        selectedPointAnnotationManager = mapView.annotations.createPointAnnotationManager()
        favoriteMarkerAnnotationManager = mapView.annotations.createPointAnnotationManager()

        // Setup FAB click listener
        fabAddMarker.setOnClickListener {
            showOptionsDialog()
        }

        // Setup other event listeners
        setupClickListeners()

        // Setup observers
        setupObservers()

        // Load map style
        loadMapStyle()

        // Start location updates
        startLocationUpdates()

        // Start fetching events
        viewModel.startFetchingEvents()

        val sharedPreferences = requireContext().getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
        val currLocation = getLastKnownLocation()
        if (currLocation != null) {
            Log.d(TAG, "Adding Location, ${currLocation.latitude}, ${currLocation.longitude}")
            sharedPreferences.edit()
                .putFloat("latitude", currLocation.latitude.toFloat())
                .putFloat("longitude", currLocation.longitude.toFloat())
                .apply()
        }

        // Check for any passed location from FavoritesFragment
        arguments?.let {
            val latitude = it.getDouble("latitude", 0.0)
            val longitude = it.getDouble("longitude", 0.0)
            val locationName = it.getString("locationName")

            if (latitude != 0.0 && longitude != 0.0) {
                // Navigate to this location
                val point = Point.fromLngLat(longitude, latitude)
                mapView.mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(15.0)
                        .build()
                )

                // Add a marker at this location
                addFavoriteLocationMarker(point)

                // Show a toast with the location name
                locationName?.let { name ->
                    Toast.makeText(context, "Viewing: $name", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return view
    }

    private fun getLastKnownLocation(): Location? {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.d(TAG, "Don't have required location permissions")
            return null
        } else {
            return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }
    }

    private fun setupClickListeners() {
        // Map click listener
        mapboxMap.addOnMapClickListener { point ->
            if (markerPlacementMode) {
                // Show marker creation dialog at this point
                showCreateMarkerDialog(point)
                // Exit marker placement mode
                toggleMarkerPlacementMode()
                return@addOnMapClickListener true
            } else {
                // Store the selected point
                selectedPoint = point

                // Add a marker for the selected point
                addSelectedPointMarker(point)

                // Show the save to favorites option
                showSaveToFavoritesDialog(point)

                return@addOnMapClickListener true
            }
        }

        // Event marker click listener
        eventAnnotationManager.addClickListener(
            OnPointAnnotationClickListener { annotation ->
                // If in marker placement mode, ignore marker clicks
                if (markerPlacementMode) return@OnPointAnnotationClickListener true

                // Find the event by coordinates
                val event = viewModel.events.value?.find {
                    it.lng == annotation.point.longitude() && it.lat == annotation.point.latitude()
                }

                event?.let {
                    // Reload comments for the event by fetching the latest comments
                    viewModel.fetchComments(it.event_id)

                    // Store current camera position
                    previousCameraOptions = mapboxMap.cameraState.toCameraOptions()

                    // Zoom to event
                    mapboxMap.flyTo(
                        CameraOptions.Builder()
                            .center(annotation.point)
                            .zoom(12.0)
                            .build(),
                        MapAnimationOptions.mapAnimationOptions {
                            duration(2000)
                        }
                    )

                    // Show event details (which should observe the updated comments LiveData)
                    showEventDetailsDialog(it)
                }
                true
            }
        )

        // User marker click listener
        userAnnotationManager.addClickListener(
            OnPointAnnotationClickListener { annotation ->
                // If in marker placement mode, ignore marker clicks
                if (markerPlacementMode) return@OnPointAnnotationClickListener true

                // Find the user marker by coordinates
                val userMarker = viewModel.userMarkers.value?.find {
                    it.longitude == annotation.point.longitude() &&
                            it.latitude == annotation.point.latitude()
                }

                userMarker?.let {
                    showUserMarkerDetailsDialog(it)
                }
                true
            }
        )
    }

    private fun toggleMarkerPlacementMode() {
        markerPlacementMode = !markerPlacementMode

        if (markerPlacementMode) {
            // Visual indicator that we're in placement mode
            fabAddMarker.setImageResource(R.drawable.ic_close)
            Toast.makeText(context, "Tap on the map to place a marker", Toast.LENGTH_SHORT).show()
        } else {
            // Reset to normal
            fabAddMarker.setImageResource(R.drawable.ic_add)
        }
    }

    private fun setupObservers() {
        // Observe events
        viewModel.events.observe(viewLifecycleOwner) { events ->
            // Update event markers
            markerManager.addEventMarkers(events, eventAnnotationManager)
        }

        // Observe user markers
        viewModel.userMarkers.observe(viewLifecycleOwner) { markers ->
            // Update user markers
            markerManager.addUserMarkers(markers, userAnnotationManager)
        }

        // Observe user location
        viewModel.userLocation.observe(viewLifecycleOwner) { location ->
            // Update home marker
            location?.let {
                val point = Point.fromLngLat(it.longitude, it.latitude)
                markerManager.addHomeMarker(point, homeAnnotationManager)
            }
        }
    }

    private fun loadMapStyle() {
        mapboxMap.loadStyle(Style.DARK) { style ->
            // Load marker icons
            markerManager.loadMarkerIcons(style)
        }
    }

    private fun startLocationUpdates() {
        locationHandler.startLocationUpdates()
    }

    private fun showCreateMarkerDialog(point: Point) {
        CreateMarkerDialog(requireContext(), viewModel) { marker ->
            // Marker created callback - no extra action needed as ViewModel handles it
            Toast.makeText(context, "Marker created successfully", Toast.LENGTH_SHORT).show()
        }.show(point)
    }

    private fun showEventDetailsDialog(event: Event) {
        EventBottomSheetDialog(requireContext(), viewModel) {
            // On dismiss - restore camera position
            previousCameraOptions?.let { prevCamera ->
                mapboxMap.flyTo(
                    prevCamera,
                    MapAnimationOptions.mapAnimationOptions {
                        duration(2000)
                    }
                )
            }
        }.show(event)
    }

    private fun showUserMarkerDetailsDialog(userMarker: UserMarker) {
        // Store current camera position before showing dialog
        previousCameraOptions = mapboxMap.cameraState.toCameraOptions()

        // Zoom to the marker location
        val markerPoint = Point.fromLngLat(userMarker.longitude, userMarker.latitude)
        mapboxMap.flyTo(
            CameraOptions.Builder()
                .center(markerPoint)
                .zoom(14.0) // Slightly higher zoom for markers since they're more precise
                .build(),
            MapAnimationOptions.mapAnimationOptions {
                duration(1500)
            }
        )

        // Show the bottom sheet dialog
        UserMarkerBottomSheetDialog(requireContext(), viewModel) {
            // On dismiss - restore camera position
            previousCameraOptions?.let { prevCamera ->
                mapboxMap.flyTo(
                    prevCamera,
                    MapAnimationOptions.mapAnimationOptions {
                        duration(1500)
                    }
                )
            }
        }.show(userMarker)
    }

    // LocationListener implementation
    override fun onLocationChanged(location: Location) {
        // Store last known location
        lastKnownLocation = location

        // Update ViewModel with new location
        viewModel.updateUserLocation(location)

        // Recalculate danger levels based on new location
        viewModel.fetchEvents()

        // Move camera to user's location if this is the first location update
        if (previousCameraOptions == null) {
            val userLocation = Point.fromLngLat(location.longitude, location.latitude)
            mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(userLocation)
                    .zoom(3.0)
                    .build()
            )
        }

        Log.d(TAG, "Location changed: $location")

        val sharedPreferences = requireContext().getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putFloat("latitude", location.latitude.toFloat())
            .putFloat("longitude", location.longitude.toFloat())
            .apply()
    }

    private fun showOptionsDialog() {
        val options = arrayOf("Create Marker", "Save Current Location", "View Favorites")

        AlertDialog.Builder(requireContext())
            .setTitle("Map Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> toggleMarkerPlacementMode() // Enable marker placement mode
                    1 -> showSaveCurrentLocationDialog()
                    2 -> navigateToFavoritesFragment()
                }
            }
            .show()
    }

    private fun showSaveCurrentLocationDialog() {
        lastKnownLocation?.let { location ->
            val point = Point.fromLngLat(location.longitude, location.latitude)
            showSaveToFavoritesDialog(point)
        } ?: run {
            Toast.makeText(context, "Current location not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSaveToFavoritesDialog(point: Point) {
        // First check if user is logged in
        val sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val isSignedIn = sharedPreferences.getBoolean("isSignedIn", false)

        if (!isSignedIn) {
            // Show sign-in required dialog
            AlertDialog.Builder(requireContext())
                .setTitle("Sign In Required")
                .setMessage("You must be logged in to save locations to favorites.")
                .setPositiveButton("Sign In") { _, _ ->
                    // Navigate to SignInFragment
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.container, SignInFragment())
                        .addToBackStack(null)
                        .commit()
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        // If already a favorite, notify user
        if (favoriteLocationManager.isLocationFavorite(point.latitude(), point.longitude())) {
            Toast.makeText(context, "This location is already in your favorites", Toast.LENGTH_SHORT).show()
            return
        }

        // Create and show the save dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_save_favorite, null)
        val etLocationName = dialogView.findViewById<EditText>(R.id.etLocationName)
        val etLocationDescription = dialogView.findViewById<EditText>(R.id.etLocationDescription)
        val tvLatitude = dialogView.findViewById<TextView>(R.id.tvLatitude)
        val tvLongitude = dialogView.findViewById<TextView>(R.id.tvLongitude)
        val btnSaveLocation = dialogView.findViewById<Button>(R.id.btnSaveLocation)

        // Set coordinate values
        tvLatitude.text = point.latitude().toString()
        tvLongitude.text = point.longitude().toString()

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Save button click listener
        btnSaveLocation.setOnClickListener {
            val locationName = etLocationName.text.toString().trim()
            val description = etLocationDescription.text.toString().trim()

            if (locationName.isEmpty()) {
                etLocationName.error = "Please enter a name for this location"
                return@setOnClickListener
            }

            // Create a new FavoriteLocation object
            val favoriteLocation = FavoriteLocation(
                name = locationName,
                latitude = point.latitude(),
                longitude = point.longitude(),
                description = description
            )

            // Save to preferences
            if (favoriteLocationManager.saveFavoriteLocation(favoriteLocation)) {
                Toast.makeText(context, "Location saved to Favorites", Toast.LENGTH_SHORT).show()

                // Add a marker for this favorite location
                addFavoriteLocationMarker(point)

                dialog.dismiss()
            } else {
                Toast.makeText(context, "Failed to save location. Please try again later.", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun addSelectedPointMarker(point: Point) {
        // Clear any existing selected point markers
        selectedPointAnnotationManager?.deleteAll()

        // Create a point annotation for the selected location
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage("selected_location_icon") // Use an appropriate icon

        selectedPointAnnotationManager?.create(pointAnnotationOptions)
    }

    private fun addFavoriteLocationMarker(point: Point) {
        // Create a point annotation for the favorite location
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage("favorite_location_icon") // Use a star or heart icon

        favoriteMarkerAnnotationManager?.create(pointAnnotationOptions)
    }

    // Add this method to display all favorite locations on the map
    private fun displayFavoriteLocations() {
        val favorites = favoriteLocationManager.getFavoriteLocations()

        // Clear existing favorite markers
        favoriteMarkerAnnotationManager?.deleteAll()

        // Add a marker for each favorite location
        for (favorite in favorites) {
            val point = Point.fromLngLat(favorite.longitude, favorite.latitude)
            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage("favorite_location_icon") // Use a star or heart icon

            favoriteMarkerAnnotationManager?.create(pointAnnotationOptions)
        }
    }

    private fun navigateToFavoritesFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, FavoritesFragment())
            .addToBackStack(null)
            .commit()
    }

    // Helper to get the signed-in user's name from SharedPreferences
    private fun getSignedInUserName(): String {
        val sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("userName", "Anonymous") ?: "Anonymous"
    }

    // Add this to your onResume method
    override fun onResume() {
        super.onResume()

        // Display favorite locations whenever the fragment resumes
        displayFavoriteLocations()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationHandler.stopLocationUpdates()
        fetchScope.cancel() // Cancel coroutine to prevent memory leaks
    }

    // Lifecycle methods
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}