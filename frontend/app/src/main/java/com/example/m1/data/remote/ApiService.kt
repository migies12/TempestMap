package com.example.m1.data.remote

import com.example.m1.data.models.EventResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Interface for the Tempest API endpoints
 */
interface ApiService {
    /**
     * Get all disaster events
     */
    @GET("prod/event")
    fun getEvents(): Call<EventResponse>

    /**
     * Post a comment to an event
     * @param eventId The ID of the event
     * @param comment The comment text
     * @param user The username of the commenter
     */
    @POST("prod/comment/{eventId}")
    fun postComment(
        @Path("eventId") eventId: String,
        @Query("comment") comment: String,
        @Query("user") user: String
    ): Call<Void>
}