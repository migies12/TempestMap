package com.example.m1

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    // Private variables to be used
    companion object {
        private const val TAG = "DashboardActivity"
        private const val BACKEND_URL = "https://zp7t6a07vd.execute-api.us-west-1.amazonaws.com/Development/"
    }

    private val dashboardScope = CoroutineScope(Dispatchers.Main)

    // Variables required to be dispalyed
    private lateinit var serverIpValue: TextView
    private lateinit var serverTimeValue: TextView
    private lateinit var clientIpValue: TextView
    private lateinit var clientTimeValue: TextView
    private lateinit var backendNameValue: TextView
    private lateinit var loggedInUserValue: TextView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize TextViews
        serverIpValue = findViewById(R.id.server_ip_value)
        serverTimeValue = findViewById(R.id.server_time_value)
        clientIpValue = findViewById(R.id.client_ip_value)
        clientTimeValue = findViewById(R.id.client_time_value)
        backendNameValue = findViewById(R.id.backend_name_value)
        loggedInUserValue = findViewById(R.id.logged_in_user_value)

        // Fetch and display data
        fetchDashboardData()
    }

    private fun fetchDashboardData() {
        dashboardScope.launch {
            try {

                // Fetch Server IP
                val serverIp = fetchData("$BACKEND_URL/server_ip")?.getString("ip") ?: "N/A"
                serverIpValue.text = serverIp

                // Fetch Server Local Time
                val serverTime = fetchData("$BACKEND_URL/local_time")?.getString("time") ?: "N/A"
                serverTimeValue.text = serverTime

                // Fetch Backend User Name
                val backendNameJson = fetchData("$BACKEND_URL/name")
                val backendFirstName = backendNameJson?.getString("firstName") ?: "N/A"
                val backendLastName = backendNameJson?.getString("lastName") ?: ""
                backendNameValue.text = "$backendFirstName $backendLastName"

                // Fetch Client Public IP
                val clientPublicIp = fetchPublicIp() ?: "N/A"
                clientIpValue.text = clientPublicIp

                // Get Client Local Time in GMT
                val clientLocalTime = getClientLocalTime()
                clientTimeValue.text = clientLocalTime

                // Get Logged-In User Name
                val loggedInUserName = getLoggedInUserName()
                loggedInUserValue.text = loggedInUserName

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching dashboard data", e)
                Toast.makeText(this@DashboardActivity, "Error fetching data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Fetches JSON data from the Backend Hosted on EC2
     */
    private suspend fun fetchData(urlString: String): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val stream = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = stream.use { it.readText() }
                    JSONObject(response)
                } else {
                    Log.e(TAG, "HTTP error code: $responseCode for URL: $urlString")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while fetching data from $urlString", e)
                null
            }
        }
    }

    /**
     * Fetches the client's public IP address using ipify api
     */
    private suspend fun fetchPublicIp(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.ipify.org?format=json")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val stream = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = stream.use { it.readText() }
                    val json = JSONObject(response)
                    json.getString("ip")
                } else {
                    Log.e(TAG, "Failed to fetch public IP. HTTP code: $responseCode")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while fetching public IP", e)
                null
            }
        }
    }

    /**
     * Retrieves the client's local time in GMT using Calendar
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getClientLocalTime(): String {
        val now = Calendar.getInstance()
        val sdf = SimpleDateFormat("HH:mm:ss 'GMT'Z", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(now.time)
    }

    /**
     * Since the user signs in before navigating to the DashBoard Activity. Retrieves the logged-in user's name
     * from the intent extras in HandleSignIn() in MainAcitivity.kt
     */
    private fun getLoggedInUserName(): String {
        val firstName = intent.getStringExtra("firstName") ?: "N/A"
        val lastName = intent.getStringExtra("lastName") ?: ""
        return "$firstName $lastName"
    }

    override fun onDestroy() {
        super.onDestroy()
        dashboardScope.cancel()
    }
}
