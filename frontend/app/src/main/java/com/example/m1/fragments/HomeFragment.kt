package com.example.m1.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.m1.MainActivity
import com.example.m1.R
import com.example.m1.data.models.Event
import com.example.m1.ui.adapters.AlertsAdapter
import com.example.m1.ui.dialogs.EventBottomSheetDialog
import com.example.m1.ui.viewmodels.MapViewModel
import com.example.m1.util.DangerLevelCalculator
import com.example.m1.util.LocationHandler
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.IOException
import java.util.Locale

class HomeFragment : Fragment(), LocationListener {

    private val TAG = "HomeFragment"

    // UI Components
    private lateinit var tvLocationName: TextView
    private lateinit var tvCoordinates: TextView
    private lateinit var btnViewOnMap: Button
    private lateinit var rvAlerts: RecyclerView
    private lateinit var tvNoAlerts: TextView
    private lateinit var loadingAlerts: ProgressBar
    private lateinit var tvViewAllAlerts: TextView

    // Quick action components
    private lateinit var actionExploreMap: LinearLayout
    private lateinit var actionSavedLocations: LinearLayout
    private lateinit var actionReportEvent: LinearLayout
    private lateinit var actionSafeHouses: LinearLayout
    private lateinit var actionResources: LinearLayout
    private lateinit var actionProfile: LinearLayout

    // Other components
    private lateinit var btnMoreSafetyTips: Button
    private lateinit var btnLearnMore: Button
    private lateinit var settingsButton: ImageView

    // ViewModel and Adapters
    private lateinit var viewModel: MapViewModel
    private lateinit var alertsAdapter: AlertsAdapter

    // Location related
    private lateinit var locationManager: LocationManager
    private lateinit var locationHandler: LocationHandler
    private var currentLocation: Location? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[MapViewModel::class.java]

        // Initialize UI components
        initializeViews(view)

        // Initialize location services
        initializeLocation()

        // Set up RecyclerView
        setupRecyclerView()

        // Set up observers
        setupObservers()

        // Set up click listeners
        setupClickListeners()

        return view
    }

    private fun initializeViews(view: View) {
        // Location section
        tvLocationName = view.findViewById(R.id.tvLocationName)
        tvCoordinates = view.findViewById(R.id.tvCoordinates)
        btnViewOnMap = view.findViewById(R.id.btnViewOnMap)

        // Alerts section
        rvAlerts = view.findViewById(R.id.rvAlerts)
        tvNoAlerts = view.findViewById(R.id.tvNoAlerts)
        loadingAlerts = view.findViewById(R.id.loadingAlerts)
        tvViewAllAlerts = view.findViewById(R.id.tvViewAllAlerts)

        // Quick actions
        actionExploreMap = view.findViewById(R.id.actionExploreMap)
        actionSavedLocations = view.findViewById(R.id.actionSavedLocations)
        actionReportEvent = view.findViewById(R.id.actionReportEvent)
        actionSafeHouses = view.findViewById(R.id.actionSafeHouses)
        actionResources = view.findViewById(R.id.actionResources)
        actionProfile = view.findViewById(R.id.actionProfile)

        // Other buttons
        btnMoreSafetyTips = view.findViewById(R.id.btnMoreSafetyTips)
        btnLearnMore = view.findViewById(R.id.btnLearnMore)
        settingsButton = view.findViewById(R.id.settingsButton)
    }

    private fun setupRecyclerView() {
        alertsAdapter = AlertsAdapter(requireContext(), emptyList()) { event ->
            // Handle event click - show bottom sheet dialog
            showEventDetails(event)
        }

        rvAlerts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = alertsAdapter
        }
    }

    private fun setupObservers() {
        // Observe events data from ViewModel
        viewModel.events.observe(viewLifecycleOwner) { events ->
            // Process events and update UI
            handleEvents(events)
        }

        // Observe user location
        viewModel.userLocation.observe(viewLifecycleOwner) { location ->
            location?.let {
                updateLocationDisplay(it)

                // Update current location reference
                currentLocation = it

                // Refresh events with new location for proper danger calculation
                viewModel.fetchEvents()
            }
        }
    }

    private fun setupClickListeners() {
        btnViewOnMap.setOnClickListener {
            navigateToMapFragment()
        }

        tvViewAllAlerts.setOnClickListener {
            navigateToAlertsFragment()
        }

        // Quick action buttons
        actionExploreMap.setOnClickListener {
            navigateToMapFragment()
        }

        actionSavedLocations.setOnClickListener {
            navigateToFavoritesFragment()
        }

        actionReportEvent.setOnClickListener {
            // Check if user is signed in
            val sharedPreferences =
                requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val isSignedIn = sharedPreferences.getBoolean("isSignedIn", false)

            if (isSignedIn) {
                navigateToMapFragmentForMarkerPlacement()
            } else {
                showSignInRequiredDialog()
            }
        }

        actionSafeHouses.setOnClickListener {
            Toast.makeText(context, "Safe Houses feature coming soon", Toast.LENGTH_SHORT).show()
        }

        actionResources.setOnClickListener {
            Toast.makeText(context, "Resources feature coming soon", Toast.LENGTH_SHORT).show()
        }

        actionProfile.setOnClickListener {
            navigateToProfileFragment()
        }

        // Other buttons
        btnMoreSafetyTips.setOnClickListener {
            Toast.makeText(context, "Safety tips feature coming soon", Toast.LENGTH_SHORT).show()
        }

        btnLearnMore.setOnClickListener {
            Toast.makeText(context, "About Tempest feature coming soon", Toast.LENGTH_SHORT).show()
        }

        settingsButton.setOnClickListener {
            navigateToProfileFragment()
        }
    }

    private fun initializeLocation() {
        locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationHandler = LocationHandler(requireContext(), requireActivity(), this)

        // Check if we have location permission
        if (locationHandler.hasLocationPermission()) {
            // Start location updates
            locationHandler.startLocationUpdates()

            // Try to get last known location
            val lastLocation =
                locationHandler.getLastKnownLocation(requireContext(), locationManager)
            lastLocation?.let {
                updateLocationDisplay(it)
                currentLocation = it
                viewModel.updateUserLocation(it)
            }
        } else {
            locationHandler.requestLocationPermission()
        }
    }

    private fun updateLocationDisplay(location: Location) {
        // Update coordinates display
        tvCoordinates.text =
            "Lat: ${location.latitude.format(5)}, Lng: ${location.longitude.format(5)}"

        // Try to get address from coordinates
        getAddressFromLocation(location)
    }

    private fun getAddressFromLocation(location: Location) {
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Use the new API for Android 13+
                geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                ) { addresses ->
                    if (addresses.isNotEmpty()) {
                        displayAddress(addresses[0])
                    }
                }
            } else {
                // Use the deprecated API for older Android versions
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                )
                if (addresses != null && addresses.isNotEmpty()) {
                    displayAddress(addresses[0])
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error getting address: ${e.message}")
            tvLocationName.text = "Unknown Location"
        }
    }

    private fun displayAddress(address: Address) {
        val locality = address.locality ?: ""
        val subAdminArea = address.subAdminArea ?: ""
        val adminArea = address.adminArea ?: ""

        val locationName = when {
            locality.isNotEmpty() -> locality
            subAdminArea.isNotEmpty() -> subAdminArea
            adminArea.isNotEmpty() -> adminArea
            else -> "Unknown Location"
        }

        activity?.runOnUiThread {
            tvLocationName.text = locationName
        }
    }

    private fun handleEvents(events: List<Event>) {
        // Filter and sort events by danger level (if user location is available)
        val processedEvents = currentLocation?.let { location ->
            // Create location object from current location
            val userLocation = Location("").apply {
                latitude = location.latitude
                longitude = location.longitude
            }

            // Update danger levels based on user's location
            val eventsWithDanger =
                DangerLevelCalculator.updateEventDangerLevels(events, userLocation)

            // Sort by danger level (highest first)
            eventsWithDanger.sortedByDescending { it.danger_level }
        } ?: events

        // Update UI based on events
        if (processedEvents.isEmpty()) {
            rvAlerts.visibility = View.GONE
            tvNoAlerts.visibility = View.VISIBLE
            loadingAlerts.visibility = View.GONE
        } else {
            rvAlerts.visibility = View.VISIBLE
            tvNoAlerts.visibility = View.GONE
            loadingAlerts.visibility = View.GONE

            // Take top 3 events for the home screen
            val topEvents = processedEvents.take(3)
            alertsAdapter.updateEvents(topEvents)
        }
    }

    private fun showEventDetails(event: Event) {
        // Use the existing EventBottomSheetDialog to show event details
        val dialog = EventBottomSheetDialog(requireContext(), viewModel) {
            // On dismiss callback
        }
        dialog.show(event)
    }

    private fun navigateToMapFragment() {
        // Update the bottom navigation view to show the Map tab as selected
        (activity as? MainActivity)?.updateBottomNavSelection(R.id.nav_map)

        parentFragmentManager.beginTransaction()
            .replace(R.id.container, MapboxFragment())
            .commit()
    }

    private fun navigateToAlertsFragment() {
        // Update the bottom navigation view to show the Alerts tab as selected
        (activity as? MainActivity)?.updateBottomNavSelection(R.id.nav_alerts)

        parentFragmentManager.beginTransaction()
            .replace(R.id.container, AlertsFragment())
            .commit()
    }

    private fun navigateToFavoritesFragment() {
        // For Favorites we don't update bottom nav since it's not a main tab
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, FavoritesFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToProfileFragment() {
        // Update the bottom navigation view to show the Profile tab as selected
        (activity as? MainActivity)?.updateBottomNavSelection(R.id.nav_profile)

        val sharedPreferences =
            requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val isSignedIn = sharedPreferences.getBoolean("isSignedIn", false)

        val fragment = if (isSignedIn) ProfileFragment() else SignInFragment()

        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    private fun navigateToMapFragmentForMarkerPlacement() {
        // Update the bottom navigation view to show the Map tab as selected
        (activity as? MainActivity)?.updateBottomNavSelection(R.id.nav_map)

        val mapFragment = MapboxFragment()
        // We could potentially pass a bundle to indicate marker placement mode

        parentFragmentManager.beginTransaction()
            .replace(R.id.container, mapFragment)
            .commit()
    }

    private fun showSignInRequiredDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Sign In Required")
            .setMessage("You need to sign in to report events. Would you like to sign in now?")
            .setPositiveButton("Sign In") { _, _ ->
                navigateToProfileFragment()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Extension function to format double to specified decimal places
    private fun Double.format(decimals: Int): String {
        return "%.${decimals}f".format(this)
    }

    override fun onLocationChanged(location: Location) {
        // Update location in ViewModel
        viewModel.updateUserLocation(location)

        Log.d(TAG, "Location changed: ${location.latitude}, ${location.longitude}")
    }

    override fun onResume() {
        super.onResume()
        // Start fetching events
        viewModel.startFetchingEvents()

        // Resume location updates
        if (locationHandler.hasLocationPermission()) {
            locationHandler.startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        // Stop location updates to save battery
        locationHandler.stopLocationUpdates()
    }
}