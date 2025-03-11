package com.example.m1.ui.viewmodels

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.m1.data.models.Comment
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

    // LiveData for comments associated with an event
    private val _comments = MutableLiveData<List<Comment>>(emptyList())
    val comments: LiveData<List<Comment>> = _comments

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

            // Sanitize the fetched events
            val sanitizedEvents = fetchedEvents.map { event ->
                Event(
                    event_type = event.event_type ?: "unknown-type",
                    date = event.date ?: "unknown-date",
                    estimated_end_date = event.estimated_end_date ?: "unknown-end-date",
                    lng = event.lng ?: 0.0,
                    event_id = event.event_id ?: "unknown-id",
                    comments = event.comments ?: emptyList(),
                    lat = event.lat ?: 0.0,
                    country_code = event.country_code ?: "unknown-country",
                    created_time = event.created_time ?: "unknown-created-time",
                    source_event_id = event.source_event_id ?: "unknown-source-id",
                    continent = event.continent ?: "unknown-continent",
                    event_name = event.event_name ?: "unknown-name",
                    danger_level = event.danger_level ?: 0
                )
            }

            // Limit to 50 events and update LiveData
            _events.value = sanitizedEvents.take(50)

            // Fetch FIRMS data (logging/debug omitted)
            val firmsData = repository.getFIRMSData()
            // Optionally log or process FIRMS data here

            // Update danger levels based on user location if available
            _userLocation.value?.let { location ->
                updateEventDangerLevels(location)
            }
        }
    }


    private fun updateEventDangerLevels(userLocation: Location) {
        val currentEvents = _events.value ?: return
        val updatedEvents = DangerLevelCalculator.updateEventDangerLevels(currentEvents, userLocation)
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
     * Fetch comments for a specific event from the repository
     * @param eventId The ID of the event
     */
    fun fetchComments(eventId: String) {
        // Clear current comments to force an update
        _comments.value = emptyList()
        viewModelScope.launch {
            val fetchedComments = repository.getComments(eventId)
            _comments.value = fetchedComments
        }
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
