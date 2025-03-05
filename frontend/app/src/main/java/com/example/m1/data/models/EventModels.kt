package com.example.m1.data.models

data class EventResponse(
    val events: List<Event>
)

data class CommentResponse(
    val event_id: String,
    val comments: List<Comment>
)

data class UserMarker(
    val id: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val comments: List<Comment> = emptyList()
)

data class Comment(
    val created_at: String,
    val text: String,
    val comment_id: String,
    val user: String
)

data class Event(
    val event_type: String,
    val date: String,
    val estimated_end_date: String,
    val lng: Double,
    val event_id: String,
    val comments: List<Comment>,
    val lat: Double,
    val country_code: String,
    val created_time: String,
    val source_event_id: String,
    val continent: String,
    val event_name: String,
    val danger_level: Int
)

data class FIRMSData(
    val latitude: Double,
    val longitude: Double,
    val bright_ti4: Double,
    val scan: Double,
    val track: Double,
    val acq_date: String,
    val acq_time: String,
    val satellite: String,
    val instrument: String,
    val confidence: String,
    val version: String,
    val bright_ti5: Double,
    val frp: Double,
    val daynight: String
)
