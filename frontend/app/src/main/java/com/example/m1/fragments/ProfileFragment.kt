package com.example.m1.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.m1.R
import java.util.UUID

class ProfileFragment : Fragment() {

    companion object {
        private const val TAG = "ProfileFragment"
        private const val PREFS_NAME = "UserProfilePrefs"
        private const val KEY_FULL_NAME = "fullName"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHONE = "phone"
        private const val KEY_LOCATION = "location"
        private const val KEY_SEVERE_WEATHER = "severeWeatherAlerts"
        private const val KEY_DAILY_WEATHER = "dailyWeatherUpdates"
        private const val KEY_SPECIAL_WEATHER = "specialWeatherEvents"
        private const val KEY_USER_ID = "userId" // For API communication
    }

    private lateinit var profilePhoto: ImageView
    private lateinit var fullNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var locationEditText: EditText
    private lateinit var severeWeatherCheck: CheckBox
    private lateinit var dailyWeatherCheck: CheckBox
    private lateinit var specialWeatherCheck: CheckBox
    private lateinit var saveButton: Button

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Initialize views
        initializeViews(rootView)

        // Load existing profile data
        loadProfileData()

        // Set click listeners
        setupClickListeners()

        return rootView
    }

    private fun initializeViews(rootView: View) {
        profilePhoto = rootView.findViewById(R.id.profile_photo)
        fullNameEditText = rootView.findViewById(R.id.full_name_edit_text)
        emailEditText = rootView.findViewById(R.id.email_edit_text)
        phoneEditText = rootView.findViewById(R.id.phone_edit_text)
        locationEditText = rootView.findViewById(R.id.location_edit_text)

        severeWeatherCheck = rootView.findViewById(R.id.severe_weather_checkbox)
        dailyWeatherCheck = rootView.findViewById(R.id.daily_weather_checkbox)
        specialWeatherCheck = rootView.findViewById(R.id.special_weather_checkbox)

        saveButton = rootView.findViewById(R.id.save_button)
    }

    private fun setupClickListeners() {
        // Profile photo click listener
        profilePhoto.setOnClickListener {
            // In a real app, you would launch an image picker here
            Toast.makeText(context, "Profile photo upload coming soon", Toast.LENGTH_SHORT).show()
        }

        // Save button click listener
        saveButton.setOnClickListener {
            if (validateInput()) {
                saveProfileData()
                // In a real app, you would also update the server
                // updateProfileOnServer()
                Toast.makeText(context, "Profile saved successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateInput(): Boolean {
        var isValid = true

        // Basic validation for full name
        if (fullNameEditText.text.toString().trim().isEmpty()) {
            fullNameEditText.error = "Name is required"
            isValid = false
        }

        // Email validation
        val email = emailEditText.text.toString().trim()
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Please enter a valid email"
            isValid = false
        }

        // Phone validation (optional)
        val phone = phoneEditText.text.toString().trim()
        if (phone.isNotEmpty() && !android.util.Patterns.PHONE.matcher(phone).matches()) {
            phoneEditText.error = "Please enter a valid phone number"
            isValid = false
        }

        // Location validation
        if (locationEditText.text.toString().trim().isEmpty()) {
            locationEditText.error = "Location is required for weather alerts"
            isValid = false
        }

        return isValid
    }

    private fun loadProfileData() {
        // Load user data from SharedPreferences
        fullNameEditText.setText(sharedPreferences.getString(KEY_FULL_NAME, ""))
        emailEditText.setText(sharedPreferences.getString(KEY_EMAIL, ""))
        phoneEditText.setText(sharedPreferences.getString(KEY_PHONE, ""))
        locationEditText.setText(sharedPreferences.getString(KEY_LOCATION, ""))

        // Load notification preferences
        severeWeatherCheck.isChecked = sharedPreferences.getBoolean(KEY_SEVERE_WEATHER, true)
        dailyWeatherCheck.isChecked = sharedPreferences.getBoolean(KEY_DAILY_WEATHER, false)
        specialWeatherCheck.isChecked = sharedPreferences.getBoolean(KEY_SPECIAL_WEATHER, false)
    }

    private fun saveProfileData() {
        // Get values from UI
        val fullName = fullNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()
        val location = locationEditText.text.toString().trim()

        val severeWeatherEnabled = severeWeatherCheck.isChecked
        val dailyWeatherEnabled = dailyWeatherCheck.isChecked
        val specialWeatherEnabled = specialWeatherCheck.isChecked

        // Get or create user ID
        var userId = sharedPreferences.getString(KEY_USER_ID, null)
        if (userId == null) {
            userId = UUID.randomUUID().toString()
        }

        // Save to SharedPreferences
        sharedPreferences.edit().apply {
            putString(KEY_FULL_NAME, fullName)
            putString(KEY_EMAIL, email)
            putString(KEY_PHONE, phone)
            putString(KEY_LOCATION, location)
            putBoolean(KEY_SEVERE_WEATHER, severeWeatherEnabled)
            putBoolean(KEY_DAILY_WEATHER, dailyWeatherEnabled)
            putBoolean(KEY_SPECIAL_WEATHER, specialWeatherEnabled)
            putString(KEY_USER_ID, userId)
            apply()
        }

        Log.d(TAG, "Profile saved: $fullName, $email, $location")
    }

    /**
     * In a production app, this would communicate with your backend
     */
    private fun updateProfileOnServer() {
        // Example implementation:
        // val retrofit = RetrofitClient.getInstance()
        // val apiService = retrofit.create(ApiService::class.java)

        // Create profile data object
        // val profileData = UserProfile(
        //     userId = sharedPreferences.getString(KEY_USER_ID, ""),
        //     name = fullNameEditText.text.toString(),
        //     email = emailEditText.text.toString(),
        //     ...
        // )

        // Launch coroutine to make API call
        // lifecycleScope.launch {
        //     try {
        //         val response = apiService.updateProfile(profileData)
        //         if (response.isSuccessful) {
        //             Toast.makeText(context, "Profile updated on server", Toast.LENGTH_SHORT).show()
        //         } else {
        //             Toast.makeText(context, "Server error: ${response.message()}", Toast.LENGTH_LONG).show()
        //         }
        //     } catch (e: Exception) {
        //         Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
        //     }
        // }
    }
}