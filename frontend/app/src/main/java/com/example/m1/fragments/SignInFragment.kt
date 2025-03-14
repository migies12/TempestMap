package com.example.m1.fragments

import android.content.Context
import android.content.SharedPreferences
import android.credentials.CredentialManager
import android.credentials.GetCredentialException
import android.credentials.GetCredentialRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import com.example.m1.BuildConfig
import com.example.m1.R
import com.example.m1.databinding.FragmentSignInBinding
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * A simple [Fragment] subclass.
 * Use the [SignInFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SignInFragment : Fragment() {

    companion object {
        private const val TAG = "SignInFragment"
    }

    private lateinit var signInButton: Button

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding = FragmentSignInBinding.inflate(inflater, container, false)

        signInButton = binding.signInButton

        signInButton.setOnClickListener {
            signIn()
        }

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun signIn() {
        val credentialManager = androidx.credentials.CredentialManager.create(requireContext())

        val signInWithGoogleOption: GetSignInWithGoogleOption = GetSignInWithGoogleOption
            .Builder(BuildConfig.WEB_CLIENT_ID)
            .setNonce(generateHashedNonce())
            .build()

        val request: androidx.credentials.GetCredentialRequest = androidx.credentials.GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            try {
                Log.d(TAG, "In try")
                val result = credentialManager.getCredential(
                    request = request,
                    context = requireContext(),
                )
                Log.d(TAG, "About to handleSignIn")
                handleSignIn(result)
            } catch (e: GetCredentialCancellationException) {
                // Handle user cancellation
                Log.d(TAG, "User canceled sign-in", e)
                Toast.makeText(context, "Please create a profile to personalize your experience.", Toast.LENGTH_SHORT).show()
            } catch (e: GetCredentialInterruptedException) {
                // Handle interruption (e.g., app backgrounded)
                Log.e(TAG, "Sign-in process was interrupted", e)
                Toast.makeText(context, "Sign-in process was interrupted. Please try again.", Toast.LENGTH_SHORT).show()
            } catch (e: GetCredentialException) {
                // Handle general credential retrieval errors
                Log.e(TAG, "Failed to retrieve credentials", e)
                Toast.makeText(context, "There was an issue verifying your credentials. Please try again later.", Toast.LENGTH_SHORT).show()
            } catch (e: SecurityException) {
                // Handle security-related errors (e.g., missing permissions)
                Log.e(TAG, "Security exception during sign-in", e)
                Toast.makeText(context, "Security error. Please check app permissions.", Toast.LENGTH_SHORT).show()
            } catch (e: IllegalArgumentException) {
                // Handle invalid arguments (e.g., invalid client ID or nonce)
                Log.e(TAG, "Invalid arguments during sign-in", e)
                Toast.makeText(context, "Invalid configuration. Please contact support.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleFailure(e: Exception) {
        Log.e(TAG, "Error getting credential")
        Toast.makeText(context, "Error getting credential", Toast.LENGTH_SHORT).show()
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        // Handle the successfully returned credential.
        val credential = result.credential

        Log.d(TAG, "In HandleSignIn")

        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        // Use the displayName property directly
                        val displayName = googleIdTokenCredential.displayName.toString()

                        // Save both sign-in status and full name to SharedPreferences
                        val sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                        sharedPreferences.edit()
                            .putBoolean("isSignedIn", true)
                            .putString("userName", displayName)
                            .apply()

                        Log.d(TAG, "Received Google ID token. User full name: $displayName")
                        loadProfileFrag()
                    } catch (e: IllegalArgumentException) {
                        // Handle invalid credential data
                        Log.e(TAG, "Invalid Google ID token data", e)
                    } catch (e: IllegalStateException) {
                        // Handle invalid state (e.g., SharedPreferences is not available)
                        Log.e(TAG, "Invalid state while saving user data", e)
                    }
                }
                else {
                    // Catch any unrecognized credential type here.
                    Log.e(TAG, "Unexpected type of credential")
                }
            }

            else -> {
                // Catch any unrecognized credential type here.
                Log.e(TAG, "Unexpected type of credential")
            }
        }
    }

    private fun generateHashedNonce(): String {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") {str, it -> str + "%02x".format(it) }
    }

    private fun loadProfileFrag() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, ProfileFragment())
            .commit()
    }

}


object ProfileApiHelper {
    fun sendProfileToServer(sharedPreferences: SharedPreferences, context: Context) {
        val user = createUserFromPreferences(sharedPreferences, context)
        RetrofitClient.apiService.postUser(user).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "Profile saved successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Account creation failed. Please try again later.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("Retrofit", "Request failed: ${t.message}")
            }
        })
    }

    private fun createUserFromPreferences(sharedPreferences: SharedPreferences, context: Context): User {
        val otherSharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        return User(
            user_id = sharedPreferences.getString(ProfileFragment.KEY_USER_ID, null),
            name = sharedPreferences.getString(ProfileFragment.KEY_FULL_NAME, null),
            location = sharedPreferences.getString(ProfileFragment.KEY_LOCATION, null),
            latitude = sharedPreferences.getFloat("latitude", 0f).toDouble(),
            longitude = sharedPreferences.getFloat("longitude", 0f).toDouble(),
            account_type = "base",
            email = sharedPreferences.getString(ProfileFragment.KEY_EMAIL, null),
            regToken = otherSharedPreferences.getString("registrationToken", null),
            notifications = otherSharedPreferences.getBoolean("notificationsEnabled", false)
        )
    }
}
