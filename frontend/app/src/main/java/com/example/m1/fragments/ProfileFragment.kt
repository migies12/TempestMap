package com.example.m1.fragments

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.m1.MainActivity
import com.example.m1.MainActivity.Companion
import com.example.m1.R
import com.example.m1.data.remote.ApiService
import com.example.m1.util.NetworkUtils
import com.example.m1.util.LocationHandler
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import java.util.UUID

data class User(
    val user_id: String?,
    val name: String?,
    val email: String?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val regToken: String?,
    val account_type: String?,
    val notifications: Boolean
)

data class UserResponse(
    val user_id: String,
    val name: String,
    val email: String,
    val location: String,
    val regToken: String,
    val account_type: String,
    val notifications: Boolean,
    val created_at: String
)

data class ApiResponse(
    val message: String,
    val user: UserResponse
)


object RetrofitClient {
    private const val BASE_URL = "https://tocuul9kqj.execute-api.us-west-1.amazonaws.com/"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

class ProfileFragment : Fragment() {

    companion object {
         const val TAG = "ProfileFragment"
         const val PREFS_NAME = "UserProfilePrefs"
         const val KEY_FULL_NAME = "fullName"
         const val KEY_EMAIL = "email"
         const val KEY_PHONE = "phone"
         const val KEY_LOCATION = "location"
         const val KEY_SEVERE_WEATHER = "severeWeatherAlerts"
         const val KEY_DAILY_WEATHER = "dailyWeatherUpdates"
         const val KEY_SPECIAL_WEATHER = "specialWeatherEvents"
         const val KEY_USER_ID = "userId" // For API communication
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
    private lateinit var signOutButton: Button
    private lateinit var notiButton: Button

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_profile, container, false)
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        setupUI(rootView)
        loadProfileData()
        return rootView
    }

    private fun setupUI(rootView: View) {
        profilePhoto = rootView.findViewById(R.id.profile_photo)
        fullNameEditText = rootView.findViewById(R.id.full_name_edit_text)
        emailEditText = rootView.findViewById(R.id.email_edit_text)
        phoneEditText = rootView.findViewById(R.id.phone_edit_text)
        locationEditText = rootView.findViewById(R.id.location_edit_text)
        severeWeatherCheck = rootView.findViewById(R.id.severe_weather_checkbox)
        dailyWeatherCheck = rootView.findViewById(R.id.daily_weather_checkbox)
        specialWeatherCheck = rootView.findViewById(R.id.special_weather_checkbox)
        saveButton = rootView.findViewById(R.id.save_button)
        signOutButton = rootView.findViewById(R.id.sign_out_button)
        notiButton = rootView.findViewById(R.id.notification_button)

        profilePhoto.setOnClickListener {
            Toast.makeText(context, "Profile photo upload coming soon", Toast.LENGTH_SHORT).show()
        }

        saveButton.setOnClickListener {
            if (validateAndSaveProfile()) {
                ProfileApiHelper.sendProfileToServer(sharedPreferences, requireContext())
            }
        }

        signOutButton.setOnClickListener {
            signOut()
        }

        notiButton.setOnClickListener {
            handleNotificationPermissions()
        }
    }

    private fun loadProfileData() {
        fullNameEditText.setText(sharedPreferences.getString(KEY_FULL_NAME, ""))
        emailEditText.setText(sharedPreferences.getString(KEY_EMAIL, ""))
        phoneEditText.setText(sharedPreferences.getString(KEY_PHONE, ""))
        locationEditText.setText(sharedPreferences.getString(KEY_LOCATION, ""))
        severeWeatherCheck.isChecked = sharedPreferences.getBoolean(KEY_SEVERE_WEATHER, true)
        dailyWeatherCheck.isChecked = sharedPreferences.getBoolean(KEY_DAILY_WEATHER, false)
        specialWeatherCheck.isChecked = sharedPreferences.getBoolean(KEY_SPECIAL_WEATHER, false)
    }

    private fun validateAndSaveProfile(): Boolean {
        if (!validateInput()) return false

        val fullName = fullNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()
        val location = locationEditText.text.toString().trim()

        sharedPreferences.edit().apply {
            putString(KEY_FULL_NAME, fullName)
            putString(KEY_EMAIL, email)
            putString(KEY_PHONE, phone)
            putString(KEY_LOCATION, location)
            putBoolean(KEY_SEVERE_WEATHER, severeWeatherCheck.isChecked)
            putBoolean(KEY_DAILY_WEATHER, dailyWeatherCheck.isChecked)
            putBoolean(KEY_SPECIAL_WEATHER, specialWeatherCheck.isChecked)
            apply()
        }

        return true
    }

    private fun validateInput(): Boolean {
        var isValid = true

        if (fullNameEditText.text.toString().trim().isEmpty()) {
            fullNameEditText.error = "Name is required"
            isValid = false
        }

        val email = emailEditText.text.toString().trim()
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Please enter a valid email"
            isValid = false
        }

        val phone = phoneEditText.text.toString().trim()
        if (phone.isNotEmpty() && !android.util.Patterns.PHONE.matcher(phone).matches()) {
            phoneEditText.error = "Please enter a valid phone number"
            isValid = false
        }

        if (locationEditText.text.toString().trim().isEmpty()) {
            locationEditText.error = "Location is required for weather alerts"
            isValid = false
        }

        return isValid
    }

    private fun handleNotificationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            askNotificationPermission()
        } else {
            Toast.makeText(requireContext(), "Location permissions required for notifications.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Toast.makeText(requireContext(), "Notifications enabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signOut() {
        val signInPrefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        signInPrefs.edit().clear().apply()
        Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, SignInFragment())
            .commit()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            ProfileApiHelper.sendProfileToServer(sharedPreferences, requireContext())
        } else {
            Toast.makeText(requireContext(), "Please enable notifications for up to date weather info.", Toast.LENGTH_SHORT).show()
        }
    }
}




