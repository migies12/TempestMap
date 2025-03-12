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
import com.example.m1.util.MarkerUtils
import com.example.m1.util.DialogUtils
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
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
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

    private val markerUtils = MarkerUtils()
    private val dialogUtils = DialogUtils()

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

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[MapViewModel::class.java]

        initGlobalsAndListeners(inflater, container)


        val sharedPreferences =
            requireContext().getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
        val currLocation = locationHandler.getLastKnownLocation(requireContext(), locationManager)
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
                markerUtils.addFavoriteLocationMarker(point, favoriteMarkerAnnotationManager)

                // Show a toast with the location name
                locationName?.let { name ->
                    Toast.makeText(context, "Viewing: $name", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return view
    }

    private fun initGlobalsAndListeners(inflater: LayoutInflater, container: ViewGroup?){
        val view = inflater.inflate(R.layout.fragment_mapbox, container, false)

        // Initialize UI components
        mapView = view.findViewById(R.id.mapView)
        fabAddMarker = view.findViewById(R.id.fabAddMarker)

        // Get the MapboxMap instance
        mapboxMap = mapView.mapboxMap

        // Initialize helper classes
        locationHandler = LocationHandler(requireContext(), requireActivity(), this)
        markerManager = MarkerManager(requireContext())
        locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
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
        mapboxMap.loadStyle(Style.DARK) { style ->
            // Load marker icons
            markerManager.loadMarkerIcons(style)
        }

        // Start location updates
        locationHandler.startLocationUpdates()

        // Start fetching events
        viewModel.startFetchingEvents()

    }


    private fun setupClickListeners() {
        // Map click listener
        mapboxMap.addOnMapClickListener { point ->
            handleMapClick(point)
            true // Return true to indicate the click event is consumed
        }

        // Event marker click listener
        eventAnnotationManager.addClickListener(
            OnPointAnnotationClickListener { annotation ->
                handleEventMarkerClick(annotation)
                true // Return true to indicate the click event is consumed
            }
        )

        // User marker click listener
        userAnnotationManager.addClickListener(
            OnPointAnnotationClickListener { annotation ->
                handleUserMarkerClick(annotation)
                true // Return true to indicate the click event is consumed
            }
        )
    }

    private fun handleMapClick(point: Point) {
        if (markerPlacementMode) {
            // Show marker creation dialog at this point
            CreateMarkerDialog(requireContext(), viewModel) {
                // Marker created callback - no extra action needed as ViewModel handles it
                Toast.makeText(context, "Marker created successfully", Toast.LENGTH_SHORT).show()
            }.show(point)
            // Exit marker placement mode
            toggleMarkerPlacementMode()
        } else {
            // Store the selected point
            selectedPoint = point

            // Add a marker for the selected point
            markerUtils.addSelectedPointMarker(point, selectedPointAnnotationManager)

            // Show the save to favorites option
            dialogUtils.showSaveToFavoritesDialog(requireContext(), point, favoriteLocationManager)
        }
    }

    private fun handleEventMarkerClick(annotation: PointAnnotation) {
        // If in marker placement mode, ignore marker clicks
        if (markerPlacementMode) return

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
    }

    private fun handleUserMarkerClick(annotation: PointAnnotation) {
        // If in marker placement mode, ignore marker clicks
        if (markerPlacementMode) return

        // Find the user marker by coordinates
        val userMarker = viewModel.userMarkers.value?.find {
            it.longitude == annotation.point.longitude() &&
                    it.latitude == annotation.point.latitude()
        }

        userMarker?.let {
            showUserMarkerDetailsDialog(it)
        }
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


        //don't update map if the fragment is not attached. This will crash system, requires context
        if (!isAdded || isDetached) {
            return
        }
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

        val sharedPreferences =
            requireContext().getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
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
            dialogUtils.showSaveToFavoritesDialog(requireContext(), point, favoriteLocationManager)
        } ?: run {
            Toast.makeText(context, "Current location not available", Toast.LENGTH_SHORT).show()
        }
    }


    private fun navigateToFavoritesFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, FavoritesFragment())
            .addToBackStack(null)
            .commit()
    }

}