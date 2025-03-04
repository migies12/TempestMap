package com.example.m1

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
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

        createNotificationChannel()

        remoteMessage.notification?.let {
            // Handle notification payload
            val title = it.title
            val body = it.body

            Log.d("FirebaseMsgService", "title: $title, body: $body")

            // You can show a local notification in your app
            showNotification(title, body)
        }
    }

    private fun showNotification(title: String?, body: String?) {
        // Use the NotificationManager to show a notification in the foreground
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Build and show the notification
        val notification = NotificationCompat.Builder(this, "default")
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.alerts)
            .build()

        notificationManager.notify(0, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "default"
            val channelName = "Default Channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val notificationChannel = NotificationChannel(channelId, channelName, importance)

            Log.d("FirebaseMsgService", "Creating notification channel")

            // Register the channel with the system
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
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


