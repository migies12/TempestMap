package com.example.m1.util

import android.location.Location
import com.example.m1.data.models.Event
import kotlin.math.max
import kotlin.math.min

/**
 * Utility class for calculating danger levels based on various factors
 */
object DangerLevelCalculator {
    // Maximum distance in meters at which an event is considered dangerous (100km)
    private const val MAX_DANGER_DISTANCE = 100000.0

    // Base danger levels by event type (on a scale of 0-100)
    private val baseDangerLevels = mapOf(
        "WF" to 100,  // Wildfire
        "EQ" to 80,  // Earthquake
        "FL" to 75,  // Flood
        "TS" to 70,  // Tropical storm
        "HU" to 95,  // Hurricane
        "TO" to 90,  // Tornado
        "BZ" to 65,  // Blizzard
        "VO" to 85,  // Volcano
        "LS" to 70   // Landslide
    )

    /**
     * Calculate the danger level for an event based on user location
     *
     * @param event The event
     * @param userLocation The user's current location
     * @return Danger level on a scale of 0-100
     */
    fun calculateDangerLevel(event: Event, userLocation: Location): Int {
        // Create a location object for the event
        val eventLocation = Location("").apply {
            latitude = event.lat
            longitude = event.lng
        }

        // Calculate distance from user to event in meters
        val distanceInMeters = userLocation.distanceTo(eventLocation)

        // Get the base danger level for this event type
        val baseDangerLevel = baseDangerLevels[event.event_type] ?: 50

        // Calculate distance factor (closer = more dangerous)
        // 0 = max distance or more, 1 = right at the event
        val distanceFactor = 1.0 - min(1.0, distanceInMeters / MAX_DANGER_DISTANCE)

        // Apply distance factor to base danger level
        // Events very far away will have their danger reduced to almost 0
        // Events very close will maintain close to their full base danger
        val scaledDangerLevel = (baseDangerLevel * distanceFactor).toInt()

        // Ensure the danger level is in the range 0-100
        return max(0, min(100, scaledDangerLevel))
    }

    /**
     * Update danger levels for a list of events based on user location
     *
     * @param events List of events to update
     * @param userLocation User's current location
     * @return List of events with updated danger levels
     */
    fun updateEventDangerLevels(events: List<Event>, userLocation: Location): List<Event> {
        return events.map { event ->
            val eventDate = event.date ?: "DATE ERROR"
            val eventType = event.event_type ?: "Disaster"
            val dangerLevel = calculateDangerLevel(event, userLocation)
            event.copy(danger_level = dangerLevel, event_type = eventType, date = eventDate)
        }
    }
}