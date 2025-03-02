package com.example.m1.data.repository

import android.util.Log
import com.example.m1.data.models.Event
import com.example.m1.data.models.EventResponse
import com.example.m1.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Repository to handle data operations for events and comments
 */
class EventRepository {
    private val apiService = RetrofitClient.apiService

    /**
     * Fetch events from the API
     * @return List of events or empty list if there was an error
     */
    suspend fun getEvents(): List<Event> = withContext(Dispatchers.IO) {
        try {
            suspendCoroutine { continuation ->
                apiService.getEvents().enqueue(object : Callback<EventResponse> {
                    override fun onResponse(call: Call<EventResponse>, response: Response<EventResponse>) {
                        if (response.isSuccessful) {
                            val events = response.body()?.events ?: emptyList()
                            Log.d("EventRepository", "Fetched ${events.size} events")
                            continuation.resume(events)
                        } else {
                            Log.e("EventRepository", "Response not successful: ${response.errorBody()?.string()}")
                            continuation.resume(emptyList())
                        }
                    }

                    override fun onFailure(call: Call<EventResponse>, t: Throwable) {
                        Log.e("EventRepository", "Failed to fetch events: ${t.message}")
                        continuation.resume(emptyList())
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("EventRepository", "Exception fetching events: ${e.message}")
            emptyList()
        }
    }

    /**
     * Post a comment to an event
     * @param eventId The ID of the event
     * @param comment The comment text
     * @param userName The username of the commenter
     * @return True if successful, false otherwise
     */
    suspend fun postComment(eventId: String, comment: String, userName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            suspendCoroutine { continuation ->
                apiService.postComment(eventId, comment, userName).enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            Log.d("EventRepository", "Comment posted successfully")
                            continuation.resume(true)
                        } else {
                            Log.e("EventRepository", "Failed to post comment: ${response.errorBody()?.string()}")
                            continuation.resume(false)
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.e("EventRepository", "Error posting comment: ${t.message}")
                        continuation.resume(false)
                    }
                })
            }
        } catch (e: Exception) {
            Log.e("EventRepository", "Exception posting comment: ${e.message}")
            false
        }
    }
}