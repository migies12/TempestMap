package com.example.m1.fragments

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
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
import com.example.m1.R
import com.example.m1.data.remote.ApiService
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
    val name: String?,
    val email: String?,
    val location: String?,
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
interface ApiService {
    @POST("prod/user")
    fun postUser(@Body user:User): Call<ApiResponse>

    /* @PUT("prod/user/{userID}")  Add if using put
    fun putUser(
        @Path("userID") userID: String,
        @Query("comment") comment: String,
        @Query("user") user: String
    ): Call<Void> */
}

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
    private lateinit var signOutButton: Button  // <-- New sign out button
    private lateinit var notiButton: Button

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sharedPreferencesTesting = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferencesTesting.getString("registrationToken", "No token")
        Log.d(TAG, "Token: $token")

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
        signOutButton = rootView.findViewById(R.id.sign_out_button) // Initialize sign out button
        notiButton = rootView.findViewById(R.id.notification_button) // New noti button
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

        // Sign out button click listener
        signOutButton.setOnClickListener {
            signOut()
        }

        notiButton.setOnClickListener {
            val sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val name = sharedPreferences.getString(KEY_FULL_NAME, null)
            if (name != null){
                askNotificationPermission()
            }
            else {
                Toast.makeText(requireContext(), "Please save profile information before enabling notifications.", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
            val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            sharedPreferences.edit()
                .putBoolean("notificationsEnabled", true)
                .apply()
        } else {
            // TODO: Inform user that that your app will not show notifications.
            Toast.makeText(requireContext(), "Please enable notifications for up to date weather info.", Toast.LENGTH_SHORT).show()
            val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            sharedPreferences.edit()
                .putBoolean("notificationsEnabled", false)
                .apply()
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                sharedPreferences.edit()
                    .putBoolean("notificationsEnabled", true)
                    .apply()
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
                showNotificationPermissionDialog()
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun showNotificationPermissionDialog() {
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Enable Notifications?")
            .setMessage("Notifications allow you to get information on natural disasters quickly." +
                    " It is highly suggested notifications are enabled.")
            .setPositiveButton("Yes") { dialog, _ ->
                // Update UserPrefs sharedPreferences
                val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                sharedPreferences.edit()
                    .putBoolean("notificationsEnabled", true)
                    .apply()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                sharedPreferences.edit()
                    .putBoolean("notificationsEnabled", false)
                    .apply()
                dialog.dismiss() // Just close the dialog
            }
            .create()

        alertDialog.show()
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
        /* if (userId == null) {
            userId = UUID.randomUUID().toString()
        } */

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

        sendProfileToServer()

        Log.d(TAG, "Profile saved: $fullName, $email, $location")
    }

    private fun signOut() {
        // Clear sign-in credentials stored in "UserPrefs"
        val signInPrefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        signInPrefs.edit().clear().apply()
        Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
        // Navigate back to the SignInFragment
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, SignInFragment())
            .commit()
    }

    private fun sendProfileToServer() {
        val sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val otherSharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val username = sharedPreferences.getString(KEY_FULL_NAME, null)
        val user_location = sharedPreferences.getString(KEY_LOCATION, null)
        val user_account_type = "base"
        val user_email = sharedPreferences.getString(KEY_EMAIL, null)
        val user_regToken = otherSharedPreferences.getString("registrationToken", null)
        val user_notifications = otherSharedPreferences.getBoolean("notificationsEnabled", false)

        val user = User(
            name = username,
            location = user_location,
            account_type = user_account_type,
            email = user_email,
            regToken = user_regToken,
            notifications = user_notifications
        )

        RetrofitClient.apiService.postUser(user).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    val userResponse = response.body()
                    Log.d("Retrofit", "User created: ${userResponse?.user?.name}")
                    if (userResponse != null) {
                        sharedPreferences.edit()
                            .putString(KEY_USER_ID, userResponse.user.user_id)
                            .apply()
                    }

                } else {
                    Log.e("Retrofit", "Error: ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("Retrofit", "Request failed: ${t.message}")
            }
        })

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