package com.example.m1

import android.Manifest
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

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
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

    private fun updateWelcomeMessage(name: String) {
        val welcomeTextView = findViewById<TextView>(R.id.welcome_text_view)

        if (name.isEmpty()) {
            welcomeTextView.text = "Hello, CPEN321!"
        } else {
            welcomeTextView.text = "Hello, $name!"
        }
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

    private fun handleSignIn(result: GetCredentialResponse) {
        // Handle the successfully returned credential.
        val credential = result.credential

        when (credential) {

            // GoogleIdToken credential
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        // Use googleIdTokenCredential and extract the ID to validate and
                        // authenticate on your server.
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)
                        Log.d(TAG, "Received Google ID Token: ${googleIdTokenCredential.idToken.take(10)}")

                        val displayName = googleIdTokenCredential.displayName.toString()
                        updateWelcomeMessage(displayName)

                        // Assuming displayName is in "First Last" format
                        val nameParts = displayName.split(" ")
                        val firstName = if (nameParts.isNotEmpty()) nameParts[0] else ""
                        val lastName = if (nameParts.size > 1) nameParts[1] else ""

                        // Navigate to DashboardActivity storing the intent of the first and last name
                        val intent = Intent(this, DashboardActivity::class.java).apply {
                            putExtra("firstName", firstName)
                            putExtra("lastName", lastName)
                        }
                        startActivity(intent)

                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Received an invalid google id token response", e)
                    }
                } else {
                    // Catch any unrecognized custom credential type here.
                    Log.e(TAG, "Unexpected type of credential")
                }
            }

            else -> {
                // Catch any unrecognized credential type here.
                Log.e(TAG, "Unexpected type of credential")
            }
        }
    }

    private fun handleFailure(e: GetCredentialException) {
        Log.e(TAG, "Error getting credential", e)
        Toast.makeText(this, "Error getting credential", Toast.LENGTH_SHORT).show()
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

        // Button #1: Google Maps
        findViewById<Button>(R.id.google_maps_activity).setOnClickListener() {

            // Location Permission + Grant
            Log.d(TAG, "Location permission button clicked")
            Toast.makeText(this, "Location permission button clicked", Toast.LENGTH_SHORT).show()

            val shouldShowFindRationals =
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
            val shouldSHowCoarseRationale =
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)

            val finePermissionGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val coarsePermissionGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            Log.d (
                TAG,
                "onClick: shouldShowRequestPermissionRationel for FINE Location: $shouldShowFindRationals"
            )
            Log.d (
                TAG,
                "onClick: shouldShowRequestPermissionRationel for COARSE Location: $shouldSHowCoarseRationale"
            )
            Log.d (
                TAG,
                "onClick: Permission STATUS for LOCATIONS: $finePermissionGranted, COARSE Location: $coarsePermissionGranted"
            )
            checkLocationPermission()

            // Map Intent
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }

        // Button #2: Device Info
        findViewById<Button>(R.id.open_composed_button).setOnClickListener {

            // Location Permission + Grant
            Log.d(TAG, "Location permission button clicked")
            Toast.makeText(this, "Location permission button clicked", Toast.LENGTH_SHORT).show()

            val shouldShowFindRationals =
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
            val shouldSHowCoarseRationale =
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)

            val finePermissionGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val coarsePermissionGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            Log.d (
                TAG,
                "onClick: shouldShowRequestPermissionRationel for FINE Location: $shouldShowFindRationals"
            )
            Log.d (
                TAG,
                "onClick: shouldShowRequestPermissionRationel for COARSE Location: $shouldSHowCoarseRationale"
            )
            Log.d (
                TAG,
                "onClick: Permission STATUS for LOCATIONS: $finePermissionGranted, COARSE Location: $coarsePermissionGranted"
            )
            checkLocationPermission()

            // Navigate to ComposedBasedActivity
            val intent = Intent(this, ComposeBasedActivity::class.java)
            startActivity(intent)
        }

        // Button #3: Server Sign In Info
        findViewById<Button>(R.id.sign_in).setOnClickListener() {

            // Location Permission + Grant
            Log.d(TAG, "Location permission button clicked")
            Toast.makeText(this, "Location permission button clicked", Toast.LENGTH_SHORT).show()

            val shouldShowFindRationals =
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
            val shouldSHowCoarseRationale =
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)

            val finePermissionGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val coarsePermissionGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            Log.d (
                TAG,
                "onClick: shouldShowRequestPermissionRationel for FINE Location: $shouldShowFindRationals"
            )
            Log.d (
                TAG,
                "onClick: shouldShowRequestPermissionRationel for COARSE Location: $shouldSHowCoarseRationale"
            )
            Log.d (
                TAG,
                "onClick: Permission STATUS for LOCATIONS: $finePermissionGranted, COARSE Location: $coarsePermissionGranted"
            )
            checkLocationPermission()

            Log.d(TAG, "Sign in clicked")
            Log.d(TAG, "Web Client Id: ${BuildConfig.WEB_CLIENT_ID}")

            val credentialManager = CredentialManager.create(this)
            val signInWithGoogleOption: GetSignInWithGoogleOption = GetSignInWithGoogleOption
                .Builder(BuildConfig.WEB_CLIENT_ID)
                .setNonce(generateHashedNonce())
                .build()

            val request: GetCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            activityScope.launch {
                try {
                    val result = credentialManager.getCredential(
                        request = request,
                        context = this@MainActivity,
                    )
                    handleSignIn(result)
                } catch (e: GetCredentialException) {
                    handleFailure(e)
                }
            }
        }

        // Button #4: Timer
        findViewById<Button>(R.id.timer).setOnClickListener() {
            val intent = Intent(this, TimerActivity::class.java)
            startActivity(intent)
        }

        // Location Permission
        findViewById<Button>(R.id.location_permission_button).setOnClickListener {
            Log.d(TAG, "Location permission button clicked")
            Toast.makeText(this, "Location permission button clicked", Toast.LENGTH_SHORT).show()

            val shouldShowFindRationals =
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
            val shouldSHowCoarseRationale =
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)

            val finePermissionGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val coarsePermissionGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            Log.d (
                TAG,
                "onClick: shouldShowRequestPermissionRationel for FINE Location: $shouldShowFindRationals"
            )
            Log.d (
                TAG,
                "onClick: shouldShowRequestPermissionRationel for COARSE Location: $shouldSHowCoarseRationale"
            )
            Log.d (
                TAG,
                "onClick: Permission STATUS for LOCATIONS: $finePermissionGranted, COARSE Location: $coarsePermissionGranted"
            )
            checkLocationPermission()
        }

        // Sign Out
        findViewById<Button>(R.id.sign_out).setOnClickListener() {
            Log.d(TAG, "Sign out clicked")

            val credentialManager = CredentialManager.create(this)
            activityScope.launch {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    Toast.makeText(
                        this@MainActivity,
                        "Logged Out Successfully",
                        Toast.LENGTH_SHORT).show()
                    updateWelcomeMessage("")
                } catch (e: Exception) {
                    Log.d(TAG, "Error clearing credential state", e)
                    Toast.makeText(
                        this@MainActivity,
                        "Error clearing credential state",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

