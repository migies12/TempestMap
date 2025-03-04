package com.example.m1

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.credentials.GetCredentialException
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.fragment.app.Fragment
import com.example.m1.fragments.AlertsFragment
import com.example.m1.fragments.HomeFragment
import com.example.m1.fragments.MapboxFragment
import com.example.m1.fragments.ProfileFragment
import com.example.m1.fragments.SignInFragment
import com.google.android.gms.tasks.OnCompleteListener

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val activityScope = CoroutineScope(Dispatchers.Main)

    lateinit var bottomNav : BottomNavigationView

    private  fun loadFragment(fragment: Fragment){
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container,fragment)
        transaction.commit()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
            val sharedPreferences = this.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            sharedPreferences.edit()
                .putBoolean("notificationsEnabled", true)
                .apply()
        } else {
            // TODO: Inform user that that your app will not show notifications.
            Toast.makeText(this, "Please enable notifications for up to date weather info.", Toast.LENGTH_SHORT).show()
            val sharedPreferences = this.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            sharedPreferences.edit()
                .putBoolean("notificationsEnabled", false)
                .apply()
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
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
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Enable Notifications?")
            .setMessage("Notifications allow you to get information on natural disasters quickly." +
                    " It is highly suggested notifications are enabled.")
            .setPositiveButton("Yes") { dialog, _ ->
                // Update UserPrefs sharedPreferences
                val sharedPreferences = this.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                sharedPreferences.edit()
                    .putBoolean("notificationsEnabled", true)
                    .apply()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                val sharedPreferences = this.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                sharedPreferences.edit()
                    .putBoolean("notificationsEnabled", false)
                    .apply()
                dialog.dismiss() // Just close the dialog
            }
            .create()

        alertDialog.show()
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.d(TAG, "Location Permission Granted")
                Toast.makeText(this,
                    "Location permission already granted",
                    Toast.LENGTH_SHORT
                ).show()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                    shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                Log.d(TAG, "Requesting location permission with rationale")
                showLocationPermissionRationale()
            }

            else -> {
                Log.d(TAG, "Requesting Location Permissions")
                requestLocationPermission()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun showLocationPermissionRationale() {
        AlertDialog.Builder(this)
            .setMessage("Location permission is required to show your location")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                requestLocationPermission()
            }.setNegativeButton("Cancel") {dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "PLease grant the location permission", Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun requestLocationPermission() {
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),0
        )
    }

    private fun generateHashedNonce(): String {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") {str, it -> str + "%02x".format(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadFragment(HomeFragment())
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            //val msg = getString(R.string.msg_token_fmt, token)
            Log.d(TAG, "Token: $token")
            Toast.makeText(baseContext, token, Toast.LENGTH_SHORT).show()
        })

        bottomNav = findViewById(R.id.bottomNav) as BottomNavigationView
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_map -> {
                    loadFragment(MapboxFragment())
                    true
                }
                R.id.nav_profile -> {
                    val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                    val isSignedIn = sharedPreferences.getBoolean("isSignedIn", false)

                    val fragment = if (isSignedIn) ProfileFragment() else SignInFragment()
                    Log.d(TAG, "Fragment: " + fragment)
                    loadFragment(fragment)
                    true
                }
                R.id.nav_alerts -> {
                    loadFragment(AlertsFragment())
                    true
                }
                else -> false
            }
        }
    }
}