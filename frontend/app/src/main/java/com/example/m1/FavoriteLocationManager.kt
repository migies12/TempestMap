package com.example.m1

import android.content.Context
import android.content.SharedPreferences
import com.example.m1.FavoriteLocation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Manager class to handle saving, retrieving, and removing favorite locations
 */
class FavoriteLocationManager(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "FavoriteLocationsPrefs"
        private const val KEY_FAVORITES = "favorite_locations"
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    /**
     * Save a new favorite location
     * @return true if saved successfully, false otherwise
     */
    fun saveFavoriteLocation(location: FavoriteLocation): Boolean {
        try {
            val favorites = getFavoriteLocations().toMutableList()
            favorites.add(location)

            val json = gson.toJson(favorites)
            sharedPreferences.edit().putString(KEY_FAVORITES, json).apply()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Get all saved favorite locations
     */
    fun getFavoriteLocations(): List<FavoriteLocation> {
        val json = sharedPreferences.getString(KEY_FAVORITES, null) ?: return emptyList()

        val type = object : TypeToken<List<FavoriteLocation>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Remove a favorite location by ID
     * @return true if removed successfully, false otherwise
     */
    fun removeFavoriteLocation(locationId: String): Boolean {
        try {
            val favorites = getFavoriteLocations().toMutableList()
            val updated = favorites.filterNot { it.id == locationId }

            if (updated.size == favorites.size) {
                return false  // Nothing was removed
            }

            val json = gson.toJson(updated)
            sharedPreferences.edit().putString(KEY_FAVORITES, json).apply()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Check if a location is already saved as a favorite
     */
    fun isLocationFavorite(latitude: Double, longitude: Double): Boolean {
        return getFavoriteLocations().any {
            Math.abs(it.latitude - latitude) < 0.0001 &&
                    Math.abs(it.longitude - longitude) < 0.0001
        }
    }
}
