package com.example.m1.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.m1.R
import com.example.m1.data.models.UserMarker
import com.example.m1.ui.viewmodels.MapViewModel
import com.mapbox.geojson.Point

/**
 * Dialog for creating custom markers
 */
class CreateMarkerDialog(
    private val context: Context,
    private val viewModel: MapViewModel,
    private val onMarkerCreated: (UserMarker) -> Unit
) {
    /**
     * Show the create marker dialog for a specific location
     * @param point The map point where the marker should be placed
     */
    fun show(point: Point) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_marker, null)

        // Get references to views
        val tvLocation = dialogView.findViewById<TextView>(R.id.tvLocation)
        val spinnerMarkerType = dialogView.findViewById<Spinner>(R.id.spinnerMarkerType)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
        val btnCreateMarker = dialogView.findViewById<Button>(R.id.btnCreateMarker)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        // Set location text
        val latitude = point.latitude()
        val longitude = point.longitude()
        tvLocation.text = String.format("Location: %.5f, %.5f", latitude, longitude)

        // Create the dialog
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setTitle("Create Marker")
            .setCancelable(true)
            .create()

        // Set up create button click listener
        btnCreateMarker.setOnClickListener {
            handleCreateMarkerClick(spinnerMarkerType, etDescription, latitude, longitude, dialog)
        }

        // Set up cancel button click listener
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun handleCreateMarkerClick(
        spinnerMarkerType: Spinner,
        etDescription: EditText,
        latitude: Double,
        longitude: Double,
        dialog: AlertDialog
    ) {

        val markerType = spinnerMarkerType.selectedItem.toString()
        val description = etDescription.text.toString().trim()

        if (description.isEmpty()) {
            Toast.makeText(context, "Please enter a description", Toast.LENGTH_SHORT).show()
            return // Exit the function early if description is empty
        }

        // Create marker through ViewModel
        val marker = viewModel.addUserMarker(
            type = markerType,
            latitude = latitude,
            longitude = longitude,
            description = description
        )

        // Notify callback
        onMarkerCreated(marker)

        // Show confirmation
        Toast.makeText(context, "Marker created successfully", Toast.LENGTH_SHORT).show()

        // Dismiss dialog
        dialog.dismiss()
    }
}