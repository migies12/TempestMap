package com.example.m1.util

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.m1.FavoriteLocation
import com.example.m1.FavoriteLocationManager
import com.example.m1.R
import com.mapbox.geojson.Point

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

        // Set the click listener for the save button
        btnSaveLocation.setOnClickListener {
            handleSaveLocationClick(
                context,
                point,
                etLocationName,
                etLocationDescription,
                favoriteLocationManager,
                dialog
            )
        }

        // Show the dialog
        dialog.show()
    }

    private fun handleSaveLocationClick(
        context: Context,
        point: Point,
        etLocationName: EditText,
        etLocationDescription: EditText,
        favoriteLocationManager: FavoriteLocationManager,
        dialog: AlertDialog
    ) {
        val locationName = etLocationName.text.toString().trim()
        val description = etLocationDescription.text.toString().trim()

        // Validate the location name
        if (locationName.isEmpty()) {
            etLocationName.error = "Please enter a name for this location"
            return
        }

        // Create a new FavoriteLocation object
        val favoriteLocation = FavoriteLocation(
            name = locationName,
            latitude = point.latitude(),
            longitude = point.longitude(),
            description = description
        )

        // Save the location and handle the result
        if (favoriteLocationManager.saveFavoriteLocation(favoriteLocation)) {
            Toast.makeText(context, "Location saved to Favorites", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        } else {
            Toast.makeText(context, "Failed to save location. Please try again later.", Toast.LENGTH_SHORT).show()
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
}