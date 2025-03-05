package com.example.m1.data.remote

import com.example.m1.data.models.CommentResponse
import com.example.m1.data.models.EventResponse
import com.example.m1.data.models.FIRMSData
import com.example.m1.fragments.ApiResponse
import com.example.m1.fragments.User
import retrofit2.Call
import retrofit2.http.Body
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

    @GET("prod/event/firms")
    fun getFIRMSData(): Call<List<FIRMSData>>

    @GET("prod/comment/{eventId}")
    fun getComments(@Path("eventId") eventId: String): Call<CommentResponse>

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

    @POST("prod/user")
    fun postUser(@Body user: User): Call<ApiResponse>
}