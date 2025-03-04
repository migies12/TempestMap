package com.example.m1.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.m1.R
import com.example.m1.data.models.Event
import com.example.m1.ui.adapters.AlertAdapter
import com.example.m1.ui.viewmodels.MapViewModel
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Fragment to display weather alerts in a news-like format
 */
class AlertsFragment : Fragment() {

    private lateinit var viewModel: MapViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var tabLayout: TabLayout
    private lateinit var emptyStateView: View
    private lateinit var alertAdapter: AlertAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_alerts, container, false)

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[MapViewModel::class.java]

        // Initialize views
        recyclerView = view.findViewById(R.id.alertsRecyclerView)
        tabLayout = view.findViewById(R.id.alertsTabLayout)
        emptyStateView = view.findViewById(R.id.emptyStateView)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        alertAdapter = AlertAdapter(requireContext()) { event ->
            // Navigate to map fragment and show this event
            navigateToEventOnMap(event)
        }
        recyclerView.adapter = alertAdapter

        // Set up TabLayout
        setupTabLayout()

        // Observe events data
        observeEvents()

        // Default to showing all alerts
        filterAlertsByTab(0)

        return view
    }

    private fun setupTabLayout() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                filterAlertsByTab(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                // Not needed
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // Not needed
            }
        })
    }

    private fun observeEvents() {
        viewModel.events.observe(viewLifecycleOwner) { events ->
            if (events.isEmpty()) {
                showEmptyState()
            } else {
                hideEmptyState()
                // Reapply the current filter when we get new data
                val currentTabPosition = tabLayout.selectedTabPosition
                filterAlertsByTab(currentTabPosition)
            }
        }

        // Observe user location changes to update local alerts
        viewModel.userLocation.observe(viewLifecycleOwner) { location ->
            // If the Local tab is selected, refresh it with the new location
            if (tabLayout.selectedTabPosition == 2) {
                filterAlertsByTab(2)
            }
        }
    }

    private fun filterAlertsByTab(tabPosition: Int) {
        val allEvents = viewModel.events.value ?: emptyList()
        val filteredEvents = when (tabPosition) {
            0 -> allEvents // All Alerts
            1 -> allEvents.filter { it.continent != "NAR" } // Global
            2 -> {
                // Local - filter events near the user's location or in their country
                val userLocation = viewModel.userLocation.value
                if (userLocation != null) {
                    allEvents.filter { event ->
                        // Consider events with danger level > 25 as local
                        event.danger_level > 25
                    }
                } else {
                    // If no location, default to North America
                    allEvents.filter { it.continent == "NAR" }
                }
            }
            else -> allEvents
        }

        // Sort events by created time (most recent first) and danger level
        val sortedEvents = filteredEvents.sortedWith(
            compareByDescending<Event> { parseDate(it.created_time) }
                .thenByDescending { it.danger_level }
        )

        alertAdapter.submitList(sortedEvents)

        // Show empty state if no events after filtering
        if (sortedEvents.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
        }
    }

    private fun parseDate(dateString: String): Date {
        return try {
            // Parse the date string
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date() // Return current date if parsing fails
        }
    }

    private fun showEmptyState() {
        emptyStateView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyStateView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    private fun navigateToEventOnMap(event: Event) {
        // Save the selected event to be displayed on the map
//        viewModel.setSelectedEvent(event)

        // Navigate to the MapboxFragment
        val mapboxFragment = MapboxFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.container, mapboxFragment)
            .addToBackStack(null)
            .commit()
    }
}