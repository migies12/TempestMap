package com.example.m1.fragments

import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.m1.R
import com.example.m1.data.models.Event
import com.example.m1.data.models.UserMarker
import com.example.m1.ui.dialogs.CreateMarkerDialog
import com.example.m1.ui.dialogs.EventDetailsDialog
import com.example.m1.ui.dialogs.UserMarkerDetailsDialog
import com.example.m1.ui.map.MarkerManager
import com.example.m1.ui.viewmodels.MapViewModel
import com.example.m1.util.LocationHandler
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.toCameraOptions

class MapboxFragment : Fragment(), LocationListener {

    // ViewModel
    private lateinit var viewModel: MapViewModel

    // UI Components
    private lateinit var mapView: MapView
    private lateinit var fabAddMarker: FloatingActionButton

    // Mapbox components
    private lateinit var homeAnnotationManager: PointAnnotationManager
    private lateinit var eventAnnotationManager: PointAnnotationManager
    private lateinit var userAnnotationManager: PointAnnotationManager

    // Helper classes
    private lateinit var locationHandler: LocationHandler
    private lateinit var markerManager: MarkerManager

    // State
    private var previousCameraOptions: CameraOptions? = null

    companion object {
        private const val TAG = "MapboxFragment"
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

        // Initialize helper classes
        locationHandler = LocationHandler(requireContext(), requireActivity(), this)
        markerManager = MarkerManager(requireContext())

        // Set initial camera position
        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(-95.7129, 37.0902)) // Center on North America
                .zoom(2.0)
                .build()
        )

        // Initialize annotation managers
        eventAnnotationManager = mapView.annotations.createPointAnnotationManager()
        userAnnotationManager = mapView.annotations.createPointAnnotationManager()
        homeAnnotationManager = mapView.annotations.createPointAnnotationManager()

        // Setup click listeners
        setupClickListeners()

        // Setup observers
        setupObservers()

        // Load map style
        loadMapStyle()

        // Start location updates
        startLocationUpdates()

        // Start fetching events
        viewModel.startFetchingEvents()

        return view
    }

    private fun setupClickListeners() {
        // FAB click listener
        fabAddMarker.setOnClickListener {
            showCreateMarkerDialog()
        }

        // Event marker click listener
        eventAnnotationManager.addClickListener(
            OnPointAnnotationClickListener { annotation ->
                // Find the event by coordinates
                val event = viewModel.events.value?.find {
                    it.lng == annotation.point.longitude() && it.lat == annotation.point.latitude()
                }

                event?.let {
                    // Store current camera position
                    previousCameraOptions = mapView.mapboxMap.cameraState.toCameraOptions()

                    // Zoom to event
                    mapView.mapboxMap.flyTo(
                        CameraOptions.Builder()
                            .center(annotation.point)
                            .zoom(12.0)
                            .build(),
                        MapAnimationOptions.mapAnimationOptions {
                            duration(2000)
                        }
                    )

                    // Show event details
                    showEventDetailsDialog(it)
                }
                true
            }
        )

        // User marker click listener
        userAnnotationManager.addClickListener(
            OnPointAnnotationClickListener { annotation ->
                // Find the user marker by coordinates
                val userMarker = viewModel.userMarkers.value?.find {
                    it.latitude == annotation.point.latitude() &&
                            it.longitude == annotation.point.longitude()
                }

                userMarker?.let {
                    showUserMarkerDetailsDialog(it)
                }
                true
            }
        )
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
        mapView.mapboxMap.loadStyle(Style.DARK) { style ->
            // Load marker icons
            markerManager.loadMarkerIcons(style)
        }
    }

    private fun startLocationUpdates() {
        locationHandler.startLocationUpdates()
    }

    private fun showCreateMarkerDialog() {
        val lastLocation = viewModel.userLocation.value

        CreateMarkerDialog(requireContext(), viewModel) { marker ->
            // Marker created callback - no extra action needed as ViewModel handles it
        }.show(lastLocation)
    }

    private fun showEventDetailsDialog(event: Event) {
        EventDetailsDialog(requireContext(), viewModel) {
            // On dismiss - restore camera position
            previousCameraOptions?.let { prevCamera ->
                mapView.mapboxMap.flyTo(
                    prevCamera,
                    MapAnimationOptions.mapAnimationOptions {
                        duration(2000)
                    }
                )
            }
        }.show(event)
    }

    private fun showUserMarkerDetailsDialog(userMarker: UserMarker) {
        UserMarkerDetailsDialog(requireContext(), viewModel).show(userMarker)
    }

    // LocationListener implementation
    override fun onLocationChanged(location: Location) {
        // Update ViewModel with new location
        viewModel.updateUserLocation(location)

        // Recalculate danger levels based on new location
        viewModel.fetchEvents()

        // Move camera to user's location if this is the first location update
        if (previousCameraOptions == null) {
            val userLocation = Point.fromLngLat(location.longitude, location.latitude)
            mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(userLocation)
                    .zoom(3.0)
                    .build()
            )
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        locationHandler.stopLocationUpdates()
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