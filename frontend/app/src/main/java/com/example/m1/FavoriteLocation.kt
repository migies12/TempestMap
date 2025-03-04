package com.example.m1

import java.util.UUID

/**
 * Data class to represent a favorite location saved by the user
 */
data class FavoriteLocation(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
