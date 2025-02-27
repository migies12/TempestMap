package com.example.m1.fragments

import android.content.Context
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
            } catch (e: Exception) {
                when (e) {
                    is GetCredentialCancellationException -> {
                        Log.d(TAG, "User canceled sign-in")
                        Toast.makeText(context, "Please sign-in", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        handleFailure(e)
                    }
                }
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
                    } catch (e: Exception) {
                        Log.e(TAG, "Received an invalid google id token response", e)
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