package com.example.m1.ui.viewmodels

import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.m1.data.models.Event
import com.example.m1.data.models.UserMarker
import com.example.m1.data.repository.EventRepository
import com.example.m1.util.DangerLevelCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel to handle map-related data and logic
 */
class MapViewModel : ViewModel() {
    private val repository = EventRepository()

    // LiveData for events
    private val _events = MutableLiveData<List<Event>>(emptyList())
    val events: LiveData<List<Event>> = _events

    // LiveData for user-created markers
    private val _userMarkers = MutableLiveData<List<UserMarker>>(emptyList())
    val userMarkers: LiveData<List<UserMarker>> = _userMarkers

    // LiveData for user location
    private val _userLocation = MutableLiveData<Location>()
    val userLocation: LiveData<Location> = _userLocation

    // Job for periodic events fetching
    private var fetchJob: Job? = null

    /**
     * Start periodic fetching of events
     * @param intervalMillis The interval between fetches in milliseconds
     */
    fun startFetchingEvents(intervalMillis: Long = 3600000) { // Default 1 hour
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            while (isActive) {
                fetchEvents()
                delay(intervalMillis)
            }
        }
    }

    /**
     * Fetch events from the repository
     */
    fun fetchEvents() {
        viewModelScope.launch {
            val fetchedEvents = repository.getEvents()
            _events.value = fetchedEvents.take(50) // Limit to 50 events

            // Fetch FIRMS data
            val firmsData = repository.getFIRMSData()
            Log.d("MapViewModel", "FIRMS Data fetched: ${firmsData.size} items")

            // Log the first few items for debugging
            firmsData.take(5).forEach { data ->
                Log.d("MapViewModel", "FIRMS Event: $data")
            }

            // Update danger levels based on user location if available
            _userLocation.value?.let { location ->
                updateEventDangerLevels(location)
            }
        }
    }

    /**
     * Calculate danger levels for events based on user's location
     * @param userLocation The user's current location
     */
    private fun updateEventDangerLevels(userLocation: Location) {
        val currentEvents = _events.value ?: return

        // Use the DangerLevelCalculator to update danger levels
        val updatedEvents = DangerLevelCalculator.updateEventDangerLevels(currentEvents, userLocation)

        // Update the events LiveData with the new values
        _events.value = updatedEvents
    }

    /**
     * Update user location
     * @param location The new user location
     */
    fun updateUserLocation(location: Location) {
        _userLocation.value = location
    }

    /**
     * Add a user marker
     * @param type The type of marker
     * @param latitude The latitude
     * @param longitude The longitude
     * @param description The description
     * @return The created UserMarker
     */
    fun addUserMarker(type: String, latitude: Double, longitude: Double, description: String): UserMarker {
        val marker = UserMarker(
            id = UUID.randomUUID().toString(),
            type = type,
            latitude = latitude,
            longitude = longitude,
            description = description
        )

        val currentMarkers = _userMarkers.value ?: emptyList()
        _userMarkers.value = currentMarkers + marker

        return marker
    }

    /**
     * Post a comment to an event
     * @param eventId The ID of the event
     * @param comment The comment text
     * @param userName The username of the commenter
     * @return True if successful, false otherwise
     */
    suspend fun postComment(eventId: String, comment: String, userName: String): Boolean {
        return repository.postComment(eventId, comment, userName)
    }

    override fun onCleared() {
        super.onCleared()
        fetchJob?.cancel()
    }
}