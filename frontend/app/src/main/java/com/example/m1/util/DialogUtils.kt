package com.example.m1.util

import android.app.Activity
import android.content.Context
import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.m1.FavoriteLocation
import com.example.m1.FavoriteLocationManager
import com.example.m1.R
import com.example.m1.fragments.ProfileFragment
import com.example.m1.fragments.SignInFragment
import com.google.android.material.snackbar.Snackbar
import com.mapbox.geojson.Point

data class SaveLocationDialogState(
    val context: Context,
    val point: Point,
    val favoriteLocationManager: FavoriteLocationManager,
    val dialog: AlertDialog,
    val etLocationName: EditText,
    val etLocationDescription: EditText
)

class DialogUtils {

    fun showSaveToFavoritesDialog(
        context: Context,
        point: Point,
        favoriteLocationManager: FavoriteLocationManager
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_save_favorite, null)
        val etLocationName = dialogView.findViewById<EditText>(R.id.etLocationName)
        val etLocationDescription = dialogView.findViewById<EditText>(R.id.etLocationDescription)
        val tvLatitude = dialogView.findViewById<TextView>(R.id.tvLatitude)
        val tvLongitude = dialogView.findViewById<TextView>(R.id.tvLongitude)
        val btnSaveLocation = dialogView.findViewById<Button>(R.id.btnSaveLocation)

        // Set coordinate values
        tvLatitude.text = point.latitude().toString()
        tvLongitude.text = point.longitude().toString()

        // Create the dialog
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        // Create the dialog state
        val dialogState = SaveLocationDialogState(
            context = context,
            point = point,
            favoriteLocationManager = favoriteLocationManager,
            dialog = dialog,
            etLocationName = etLocationName,
            etLocationDescription = etLocationDescription,
        )

        // Set the click listener for the save button
        btnSaveLocation.setOnClickListener {
            handleSaveLocationClick(dialogState)
        }

        // Show the dialog
        dialog.show()
    }

    private fun handleSaveLocationClick(dialogState: SaveLocationDialogState) {
        val locationName = dialogState.etLocationName.text.toString().trim()
        val description = dialogState.etLocationDescription.text.toString().trim()

        // Validate the location name
        if (locationName.isEmpty()) {
            dialogState.etLocationName.error = "Please enter a name for this location"
            return
        }

        // Create a new FavoriteLocation object
        val favoriteLocation = FavoriteLocation(
            name = locationName,
            latitude = dialogState.point.latitude(),
            longitude = dialogState.point.longitude(),
            description = description
        )

        // Save the location and handle the result
        if (dialogState.favoriteLocationManager.saveFavoriteLocation(favoriteLocation)) {
            if (dialogState.context is Activity) {
                Snackbar.make(dialogState.context.findViewById(android.R.id.content), "Location saved to Favorites", Snackbar.LENGTH_SHORT).show()
            }
            dialogState.dialog.dismiss()
        } else {
            if (dialogState.context is Activity) {
                Snackbar.make(dialogState.context.findViewById(android.R.id.content), "Failed to save location. Please try again later.", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    fun showSignInRequiredDialog(context: Context, onSignIn: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Sign In Required")
            .setMessage("You must be logged in to save locations to favorites.")
            .setPositiveButton("Sign In") { _, _ -> onSignIn() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showOptionsDialog(
        context: Context,
        favoriteLocationManager: FavoriteLocationManager,
        toggleMarkerPlacementMode: () -> Unit,
        navigateToFavoritesFragment: () -> Unit,
        lastKnownLocation: Location?
    ) {

        val options = arrayOf("Create Marker", "Save Current Location", "View Favorites")

        AlertDialog.Builder(context)
            .setTitle("Map Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> toggleMarkerPlacementMode() // Enable marker placement mode
                    1 -> showSaveCurrentLocationDialog(context, favoriteLocationManager, lastKnownLocation)
                    2 -> navigateToFavoritesFragment()
                }
            }
            .show()
    }

    private fun showSaveCurrentLocationDialog(
        context: Context,
        favoriteLocationManager: FavoriteLocationManager,
        lastKnownLocation: Location?
    ) {
        lastKnownLocation?.let { location ->
            val point = Point.fromLngLat(location.longitude, location.latitude)
            showSaveToFavoritesDialog(context, point, favoriteLocationManager)
        } ?: run {
            Toast.makeText(context, "Current location not available", Toast.LENGTH_SHORT).show()
        }
    }


}