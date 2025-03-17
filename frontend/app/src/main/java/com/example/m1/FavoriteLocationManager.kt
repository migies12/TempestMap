package com.example.m1

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.m1.FavoriteLocation
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
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
        return try {
            // Get the current list of favorite locations
            val favorites = getFavoriteLocations().toMutableList()

            // Add the new location to the list
            favorites.add(location)

            // Convert the list to JSON
            val json = gson.toJson(favorites)

            // Save the JSON to SharedPreferences
            sharedPreferences.edit()
                .putString(KEY_FAVORITES, json)
                .apply()

            // Return true to indicate success
            true
        } catch (e: JsonSyntaxException) {
            // Handle JSON serialization errors
            Toast.makeText(context, "Failed to save location. Please try again later.", Toast.LENGTH_SHORT).show()
            Log.e("FavoriteLocationManager", "Failed to serialize favorite locations to JSON", e)
            false
        } catch (e: IllegalStateException) {
            // Handle invalid state (e.g., SharedPreferences is not available)
            Toast.makeText(context, "Failed to save location. Please try again later.", Toast.LENGTH_SHORT).show()
            Log.e("FavoriteLocationManager", "Invalid state while saving favorite location", e)
            false
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
        } catch (e: JsonSyntaxException) {
            // Handle JSON parsing errors (e.g., malformed JSON)
            Log.e("FavoriteLocationManager", "Failed to parse favorite locations from JSON", e)
            emptyList()
        } catch (e: JsonParseException) {
            // Handle JSON parsing errors (e.g., invalid JSON structure)
            Log.e("FavoriteLocationManager", "Invalid JSON structure for favorite locations", e)
            emptyList()
        } catch (e: IllegalStateException) {
            // Handle invalid state (e.g., Gson is not properly initialized)
            Log.e("FavoriteLocationManager", "Invalid state while parsing favorite locations", e)
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
        } catch (e: JsonSyntaxException) {
            // Handle JSON serialization errors
            Log.e("FavoriteLocationManager", "Failed to serialize favorite locations to JSON", e)
            return false
        } catch (e: IllegalStateException) {
            // Handle invalid state (e.g., SharedPreferences is not available)
            Log.e("FavoriteLocationManager", "Invalid state while saving favorite locations", e)
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
