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
import com.google.android.material.snackbar.Snackbar
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
            // Replace with signOut() if bugs
            val signInPrefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            signInPrefs.edit().clear().apply()
            Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, SignInFragment())
                .commit()
        }

        notiButton.setOnClickListener {
            if(sharedPreferences.getString(KEY_FULL_NAME, null) == null) {
                Toast.makeText(context, "Please complete profile creation first.", Toast.LENGTH_SHORT).show()
            }
            else {
                checkLocationPermissions()
            }
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
            // Replace if else with askNotificationsPermission if something breaks.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else {
                Toast.makeText(requireContext(), "Notifications enabled", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Location permissions required for notifications.", Toast.LENGTH_SHORT).show()
            val sharedPrefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .putBoolean("notificationsEnabled", true)
                .apply()
        }
    }

//    private fun askNotificationPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
//        } else {
//            Toast.makeText(requireContext(), "Notifications enabled", Toast.LENGTH_SHORT).show()
//        }
//    }

//    private fun signOut() {
//        val signInPrefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
//        signInPrefs.edit().clear().apply()
//        Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
//        parentFragmentManager.beginTransaction()
//            .replace(R.id.container, SignInFragment())
//            .commit()
//    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val sharedPrefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .putBoolean("notificationsEnabled", true)
                .apply()
            ProfileApiHelper.sendProfileToServer(sharedPreferences, requireContext())
        } else {
            val sharedPrefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .putBoolean("notificationsEnabled", false)
                .apply()
            Toast.makeText(requireContext(), "Please enable notifications for up to date weather info.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "Location permissions granted")
                handleNotificationPermissions()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) ||
                    shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Log.d(TAG, "Request permission rationale true")
                //Replace with showLocationPermissionRationale if bugs
                val alertDialog = AlertDialog.Builder(requireContext())
                    .setTitle("Enable Location Permissions?")
                    .setMessage("Location permissions are necessary for the map feature, and also for notifications. It is highly recommended you turn on location permissions.")
                    .setPositiveButton("Yes") { dialog, _ ->
                        // Request permissions
                        dialog.dismiss()
                        requestLocationPermissions()
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss() // Just close the dialog
                        Toast.makeText(requireContext(), "Location permissions required for notifications.", Toast.LENGTH_SHORT).show()
                    }
                    .create()

                alertDialog.show()
            }

            else -> {
                Log.d(TAG, "Requesting location permissions")
                requestLocationPermissions()
            }
        }
    }

    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            // We only care about fineLocation

            if (fineLocationGranted) {
                handleNotificationPermissions()
                Log.d("Permission", "Location permission granted")
            } else {
                Toast.makeText(context, "Location permissions are required to enable notifications.", Toast.LENGTH_SHORT).show()
                Log.d("Permission", "Location permission denied")
            }
        }

    private fun requestLocationPermissions() {
        requestLocationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

//    private fun showLocationPermissionRationale() {
//        val alertDialog = AlertDialog.Builder(requireContext())
//            .setTitle("Enable Location Permissions?")
//            .setMessage("Location permissions are necessary for the map feature, and also for notifications. It is highly recommended you turn on location permissions.")
//            .setPositiveButton("Yes") { dialog, _ ->
//                // Request permissions
//                dialog.dismiss()
//                requestLocationPermissions()
//            }
//            .setNegativeButton("No") { dialog, _ ->
//                dialog.dismiss() // Just close the dialog
//                Toast.makeText(requireContext(), "Location permissions required for notifications.", Toast.LENGTH_SHORT).show()
//            }
//            .create()
//
//        alertDialog.show()
//    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 0) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                handleNotificationPermissions()
                Log.d("Permission", "Location permission granted")
            } else {
                // Permission denied
                Toast.makeText(requireContext(), "Location permissions required for notifications.", Toast.LENGTH_SHORT).show()
                Log.d("Permission", "Location permission denied")
            }
        }
    }

}




