package com.example.m1

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.m1.MainActivity.Companion
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

//TODO: implement if necessary, needed right now to suppress errors in AndroidManifest
class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "Firebase"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FirebaseMsgService", "From: ${remoteMessage.from}")

        remoteMessage.notification?.let {
            Log.d("FirebaseMsgService", "Message Notification Body: ${it.body}")
        }

        // Handle notification logic here (e.g., show ]a notification)
    }

    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        // TODO: sendRegistrationToServer(token)
        val sharedPreferences = this.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString("registrationToken", token)
            .apply()
    }

}


