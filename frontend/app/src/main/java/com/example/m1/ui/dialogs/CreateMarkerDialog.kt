package com.example.m1.ui.dialogs

import android.content.Context
import android.location.Location
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.m1.R
import com.example.m1.data.models.UserMarker
import com.example.m1.ui.viewmodels.MapViewModel

/**
 * Dialog for creating custom markers
 */
class CreateMarkerDialog(
    private val context: Context,
    private val viewModel: MapViewModel,
    private val onMarkerCreated: (UserMarker) -> Unit
) {
    /**
     * Show the create marker dialog
     * @param lastKnownLocation The user's last known location, used to pre-fill coordinates
     */
    fun show(lastKnownLocation: Location?) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_marker, null)

        // Get references to views
        val etLatitude = dialogView.findViewById<EditText>(R.id.etLatitude)
        val etLongitude = dialogView.findViewById<EditText>(R.id.etLongitude)
        val spinnerMarkerType = dialogView.findViewById<Spinner>(R.id.spinnerMarkerType)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
        val btnCreateMarker = dialogView.findViewById<Button>(R.id.btnCreateMarker)

        // Pre-fill coordinates if location is available
        lastKnownLocation?.let {
            etLatitude.setText(it.latitude.toString())
            etLongitude.setText(it.longitude.toString())
        }

        // Create the dialog
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setTitle("Create Marker")
            .create()

        // Set up create button click listener
        btnCreateMarker.setOnClickListener {
            val latitude = etLatitude.text.toString().toDoubleOrNull()
            val longitude = etLongitude.text.toString().toDoubleOrNull()
            val markerType = spinnerMarkerType.selectedItem.toString()
            val description = etDescription.text.toString()

            if (latitude != null && longitude != null) {
                // Create marker through ViewModel
                val marker = viewModel.addUserMarker(
                    type = markerType,
                    latitude = latitude,
                    longitude = longitude,
                    description = description
                )

                // Notify callback
                onMarkerCreated(marker)

                // Dismiss dialog
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Invalid latitude or longitude", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }
}