package com.example.m1.data.repository

import android.util.Log
import com.example.m1.data.models.Comment
import com.example.m1.data.models.CommentResponse
import com.example.m1.data.models.Event
import com.example.m1.data.models.EventResponse
import com.example.m1.data.models.FIRMSData
import com.example.m1.data.remote.RetrofitClient
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.IOException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import kotlin.coroutines.cancellation.CancellationException
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
        } catch (e: IOException) {
            // Handle network-related errors (e.g., no internet, server unreachable)
            Log.e("EventRepository", "Network error fetching events: ${e.message}")
            emptyList()
        } catch (e: HttpException) {
            // Handle HTTP errors (e.g., 404, 500)
            Log.e("EventRepository", "HTTP error fetching events: ${e.message}")
            emptyList()
        } catch (e: CancellationException) {
            // Handle coroutine cancellation
            Log.e("EventRepository", "Coroutine cancelled while fetching events: ${e.message}")
            emptyList()
        }
    }

    suspend fun getFIRMSData(): List<FIRMSData> = withContext(Dispatchers.IO) {
        try {
            suspendCoroutine { continuation ->
                apiService.getFIRMSData().enqueue(object : Callback<List<FIRMSData>> {
                    override fun onResponse(call: Call<List<FIRMSData>>, response: Response<List<FIRMSData>>) {
                        try {
                            if (response.isSuccessful) {
                                val firmsData = response.body() ?: emptyList()
                                Log.d("EventRepository", "Fetched FIRMS Data: ${firmsData.size} FIRMS data points")
                                continuation.resume(firmsData)
                            } else {
                                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                                Log.e("EventRepository", "Response not successful: $errorBody")
                                continuation.resume(emptyList())
                            }
                        } catch (e: CancellationException) {
                            // Handle coroutine cancellation
                            Log.e("EventRepository", "Coroutine was cancelled while fetching FIRMS data", e)
                        } catch (e: IOException) {
                            // Handle network-related errors (e.g., no internet connection)
                            Log.e("EventRepository", "Network error while fetching FIRMS data: ${e.message}", e)
                        } catch (e: JsonSyntaxException) {
                            // Handle JSON parsing errors
                            Log.e("EventRepository", "JSON parsing error while fetching FIRMS data: ${e.message}", e)
                        }
                    }

                    override fun onFailure(call: Call<List<FIRMSData>>, t: Throwable) {
                        Log.e("EventRepository", "Failed to fetch FIRMS data: ${t.message}", t)
                        continuation.resume(emptyList())
                    }
                })
            }
        } catch (e: CancellationException) {
            // Handle coroutine cancellation
            Log.e("EventRepository", "Coroutine was cancelled while fetching FIRMS data", e)
            emptyList()
        } catch (e: IOException) {
            // Handle network-related errors (e.g., no internet connection)
            Log.e("EventRepository", "Network error while fetching FIRMS data: ${e.message}", e)
            emptyList()
        } catch (e: JsonSyntaxException) {
            // Handle JSON parsing errors
            Log.e("EventRepository", "JSON parsing error while fetching FIRMS data: ${e.message}", e)
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
        }catch (e: CancellationException) {
            // Handle coroutine cancellation
            Log.e("EventRepository", "Coroutine was cancelled while when posting comment", e)
            false
        } catch (e: IOException) {
            // Handle network-related errors (e.g., no internet connection)
            Log.e("EventRepository", "Network error while fetching when posting comment: ${e.message}", e)
            false
        } catch (e: JsonSyntaxException) {
            // Handle JSON parsing errors
            Log.e("EventRepository", "JSON parsing error while fetching when posting comment: ${e.message}", e)
            false
        }
    }

    /**
     * Fetch comments for an event from the API
     * @param eventId The ID of the event
     * @return List of comments or empty list if there was an error
     */
    suspend fun getComments(eventId: String): List<Comment> = withContext(Dispatchers.IO) {
        try {
            suspendCoroutine { continuation ->
                apiService.getComments(eventId).enqueue(object : Callback<CommentResponse> {
                    override fun onResponse(call: Call<CommentResponse>, response: Response<CommentResponse>) {
                        if (response.isSuccessful) {
                            val comments = response.body()?.comments ?: emptyList()
                            Log.d("EventRepository", "Fetched ${comments.size} comments")
                            continuation.resume(comments)
                        } else {
                            Log.e("EventRepository", "Response not successful: ${response.errorBody()?.string()}")
                            continuation.resume(emptyList())
                        }
                    }
                    override fun onFailure(call: Call<CommentResponse>, t: Throwable) {
                        Log.e("EventRepository", "Failed to fetch comments: ${t.message}")
                        continuation.resume(emptyList())
                    }
                })
            }
        } catch (e: CancellationException) {
            // Handle coroutine cancellation
            Log.e("EventRepository", "Coroutine was cancelled while fetching comments", e)
            emptyList()
        } catch (e: IOException) {
            // Handle network-related errors (e.g., no internet connection)
            Log.e("EventRepository", "Network error while fetching comments: ${e.message}", e)
            emptyList()
        } catch (e: JsonSyntaxException) {
            // Handle JSON parsing errors
            Log.e("EventRepository", "JSON parsing error while fetching comments: ${e.message}", e)
            emptyList()
        }
    }
}