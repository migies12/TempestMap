package com.example.m1.ui.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.location.Location
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.m1.R
import com.example.m1.data.models.Event
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/**
 * Adapter for displaying alerts/events in a RecyclerView
 */
class AlertsAdapter(
    private val context: Context,
    private var events: List<Event>,
    private val onItemClick: (Event) -> Unit
) : RecyclerView.Adapter<AlertsAdapter.AlertViewHolder>() {

    // User location for distance calculation
    private var userLocation: Location? = null

    // Cache for calculated distances - using event_id as key
    private val distanceCache = ConcurrentHashMap<String, String>()

    // Flag to track if we need to recalculate all distances
    private var locationChanged = false

    inner class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eventTypeIndicator: View = itemView.findViewById(R.id.eventTypeIndicator)
        val tvAlertTitle: TextView = itemView.findViewById(R.id.tvAlertTitle)
        val tvAlertDescription: TextView = itemView.findViewById(R.id.tvAlertDescription)
        val tvAlertDangerLevel: TextView = itemView.findViewById(R.id.tvAlertDangerLevel)
        val tvAlertDate: TextView = itemView.findViewById(R.id.tvAlertDate)
        val tvAlertDistance: TextView = itemView.findViewById(R.id.tvAlertDistance)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(events[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alert, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val event = events[position]

        // Set event title based on type
        when (event.event_type) {
            "WF" -> {
                holder.tvAlertTitle.text = "Wildfire Alert"
                holder.eventTypeIndicator.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.wildfire_color)
                )
            }
            "EQ" -> {
                holder.tvAlertTitle.text = "Earthquake Alert"
                holder.eventTypeIndicator.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.earthquake_color)
                )
            }
            "FL" -> {
                holder.tvAlertTitle.text = "Flood Alert"
                holder.eventTypeIndicator.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.flood_color)
                )
            }
            "TS" -> {
                holder.tvAlertTitle.text = "Tropical Storm Alert"
                holder.eventTypeIndicator.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.storm_color)
                )
            }
            "HU" -> {
                holder.tvAlertTitle.text = "Hurricane Alert"
                holder.eventTypeIndicator.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.hurricane_color)
                )
            }
            else -> {
                holder.tvAlertTitle.text = "${event.event_type} Alert"
                holder.eventTypeIndicator.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.default_event_color)
                )
            }
        }

        // Set description
        val description = event.event_name.ifEmpty {
            when (event.event_type) {
                "WF" -> "Wildfire detected"
                "EQ" -> "Earthquake detected"
                "FL" -> "Flooding detected"
                "TS" -> "Tropical Storm detected"
                "HU" -> "Hurricane detected"
                else -> "${event.event_type} detected"
            }
        }
        holder.tvAlertDescription.text = description

        // Set danger level and color
        val dangerLevel = event.danger_level
        holder.tvAlertDangerLevel.text = "$dangerLevel/100"

        when {
            dangerLevel >= 75 -> holder.tvAlertDangerLevel.setTextColor(
                ContextCompat.getColor(context, R.color.danger_high)
            )
            dangerLevel >= 50 -> holder.tvAlertDangerLevel.setTextColor(
                ContextCompat.getColor(context, R.color.danger_medium)
            )
            dangerLevel >= 25 -> holder.tvAlertDangerLevel.setTextColor(
                ContextCompat.getColor(context, R.color.danger_low)
            )
            else -> holder.tvAlertDangerLevel.setTextColor(
                ContextCompat.getColor(context, R.color.danger_minimal)
            )
        }

        // Format and set date
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            val date = inputFormat.parse(event.date)
            date?.let {
                holder.tvAlertDate.text = outputFormat.format(it)
            } ?: run {
                holder.tvAlertDate.text = event.date
            }
        } catch (e: Exception) {
            holder.tvAlertDate.text = event.date
        }

        // Set distance text - use cached value if available
        getDistanceText(event).let { distanceText ->
            holder.tvAlertDistance.text = distanceText
        }
    }

    private fun getDistanceText(event: Event): String {
        // Check if we need to refresh the cache
        if (locationChanged) {
            // Location changed, so clear the cache for recalculation
            distanceCache.clear()
            locationChanged = false
        }

        // Get the cached distance or calculate a new one
        return distanceCache.getOrPut(event.event_id) {
            calculateDistanceText(event)
        }
    }

    private fun calculateDistanceText(event: Event): String {
        return userLocation?.let { location ->
            // Create location for the event
            val eventLocation = Location("").apply {
                latitude = event.lat
                longitude = event.lng
            }

            // Calculate distance in meters
            val distanceInMeters = location.distanceTo(eventLocation)

            // Convert to appropriate unit and round
            when {
                distanceInMeters < 1000 -> {
                    // Less than 1km, show in meters
                    val distanceRounded = distanceInMeters.roundToInt()
                    "~${distanceRounded}m away"
                }
                distanceInMeters < 10000 -> {
                    // Less than 10km, show with 1 decimal place
                    val distanceKm = (distanceInMeters / 100).roundToInt() / 10.0
                    "~${distanceKm}km away"
                }
                else -> {
                    // More than 10km, round to nearest km
                    val distanceKm = (distanceInMeters / 1000).roundToInt()
                    "~${distanceKm}km away"
                }
            }
        } ?: "Distance unknown"
    }

    override fun getItemCount() = events.size

    /**
     * Update the list of events/alerts
     */
    fun updateEvents(newEvents: List<Event>) {
        // Check if we have new events that need distance calculation
        val newEventIds = newEvents.map { it.event_id }.toSet()
        val oldEventIds = events.map { it.event_id }.toSet()

        // If we have new events, we might need to calculate new distances
        // But we'll still keep the cached distances for existing events
        if (newEventIds != oldEventIds) {
            // Clear cache for events that are no longer in the list
            distanceCache.keys
                .filter { it !in newEventIds }
                .forEach { distanceCache.remove(it) }
        }

        this.events = newEvents
        notifyDataSetChanged()
    }

    /**
     * Update the user's location for distance calculations
     */
    fun updateUserLocation(location: Location) {
        // Check if location has significantly changed (more than 100m)
        val significantChange = userLocation?.let {
            it.distanceTo(location) > 100
        } ?: true

        if (significantChange) {
            userLocation = location
            // Mark that we need to recalculate distances
            locationChanged = true
            notifyDataSetChanged()
        }
    }
}