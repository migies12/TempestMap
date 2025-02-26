package com.example.m1.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.m1.R

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_profile, container, false)

        // Grab references to views
        val profilePhoto = rootView.findViewById<ImageView>(R.id.profile_photo)
        val fullNameEditText = rootView.findViewById<EditText>(R.id.full_name_edit_text)
        val emailEditText = rootView.findViewById<EditText>(R.id.email_edit_text)
        val phoneEditText = rootView.findViewById<EditText>(R.id.phone_edit_text)
        val locationEditText = rootView.findViewById<EditText>(R.id.location_edit_text)

        val severeWeatherCheck = rootView.findViewById<CheckBox>(R.id.severe_weather_checkbox)
        val dailyWeatherCheck = rootView.findViewById<CheckBox>(R.id.daily_weather_checkbox)
        val specialWeatherCheck = rootView.findViewById<CheckBox>(R.id.special_weather_checkbox)

        val saveButton = rootView.findViewById<Button>(R.id.save_button)

        // Example usage: On Save click
        saveButton.setOnClickListener {
            val fullName = fullNameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()
            val location = locationEditText.text.toString().trim()

            val severeWeatherEnabled = severeWeatherCheck.isChecked
            val dailyWeatherEnabled = dailyWeatherCheck.isChecked
            val specialWeatherEnabled = specialWeatherCheck.isChecked

            // You can now store these values in SharedPreferences, Room, or send to a server
            // e.g. MyPrefs.saveProfileData(...)
        }

        // If you want to load existing data, do so here and set them on the views:
        // fullNameEditText.setText(...)
        // severeWeatherCheck.isChecked = ...

        return rootView
    }
}
