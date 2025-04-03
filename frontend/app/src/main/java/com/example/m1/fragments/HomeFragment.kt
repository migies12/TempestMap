package com.example.m1.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.m1.MainActivity
import com.example.m1.R
import com.example.m1.ui.viewmodels.MapViewModel

class HomeFragment : Fragment() {

    private val TAG = "HomeFragment"

    // Quick action components
    private lateinit var actionExploreMap: LinearLayout
    private lateinit var actionSavedLocations: LinearLayout
    private lateinit var actionReportEvent: LinearLayout

    // Other components
    private lateinit var settingsButton: ImageView

    // ViewModel
    private lateinit var viewModel: MapViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        try {
            // Initialize ViewModel
            viewModel = ViewModelProvider(this)[MapViewModel::class.java]

            // Initialize UI components
            initializeViews(view)

            // Set up click listeners
            setupClickListeners()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreateView: ${e.message}", e)
            Toast.makeText(context, "Error initializing home screen", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun initializeViews(view: View) {
        // Quick actions
        actionExploreMap = view.findViewById(R.id.actionExploreMap)
        actionSavedLocations = view.findViewById(R.id.actionSavedLocations)
        actionReportEvent = view.findViewById(R.id.actionReportEvent)

        // Settings button
        settingsButton = view.findViewById(R.id.settingsButton)
    }

    private fun setupClickListeners() {
        // Quick action buttons
        actionExploreMap.setOnClickListener {
            navigateToMapFragment()
        }

        actionSavedLocations.setOnClickListener {
            navigateToFavoritesFragment()
        }

        actionReportEvent.setOnClickListener {
            // Check if user is signed in
            val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val isSignedIn = sharedPreferences.getBoolean("isSignedIn", false)

            if (isSignedIn) {
                navigateToMapFragmentForMarkerPlacement()
            } else {
                showSignInRequiredDialog()
            }
        }

        settingsButton.setOnClickListener {
            navigateToProfileFragment()
        }
    }

    private fun navigateToMapFragment() {
        try {
            // Update bottom navigation selection if possible
            (activity as? MainActivity)?.let { mainActivity ->
                mainActivity.updateBottomNavSelection(R.id.nav_map)
            }

            // Navigate to map fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, MapboxFragment())
                .commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to map: ${e.message}", e)
            Toast.makeText(context, "Error navigating to map", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToAlertsFragment() {
        try {
            // Update bottom navigation selection if possible
            (activity as? MainActivity)?.let { mainActivity ->
                mainActivity.updateBottomNavSelection(R.id.nav_alerts)
            }

            // Navigate to alerts fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, AlertsFragment())
                .commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to alerts: ${e.message}", e)
            Toast.makeText(context, "Error navigating to alerts", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToFavoritesFragment() {
        try {
            // For favorites, we don't update bottom nav since it's not a main tab
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, FavoritesFragment())
                .addToBackStack(null)  // Add to back stack so user can return with back button
                .commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to favorites: ${e.message}", e)
            Toast.makeText(context, "Error navigating to favorites", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToProfileFragment() {
        try {
            // Update bottom navigation selection if possible
            (activity as? MainActivity)?.let { mainActivity ->
                mainActivity.updateBottomNavSelection(R.id.nav_profile)
            }

            // Determine which fragment to show based on sign-in status
            val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val isSignedIn = sharedPreferences.getBoolean("isSignedIn", false)
            val fragment = if (isSignedIn) ProfileFragment() else SignInFragment()

            // Navigate to appropriate fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to profile: ${e.message}", e)
            Toast.makeText(context, "Error navigating to profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToMapFragmentForMarkerPlacement() {
        try {
            // Update bottom navigation selection if possible
            (activity as? MainActivity)?.let { mainActivity ->
                mainActivity.updateBottomNavSelection(R.id.nav_map)
            }

            // Create map fragment (could pass data to indicate marker placement mode)
            val mapFragment = MapboxFragment()

            // Navigate to map fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, mapFragment)
                .commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to map for marker placement: ${e.message}", e)
            Toast.makeText(context, "Error navigating to map", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSignInRequiredDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Sign In Required")
            .setMessage("You need to sign in to report events. Would you like to sign in now?")
            .setPositiveButton("Sign In") { _, _ ->
                navigateToProfileFragment()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Start fetching events
        viewModel.startFetchingEvents()
    }
}