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
import okio.IOException
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.cancellation.CancellationException

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
                val serverIp = fetchEC2Data("$BACKEND_URL/server_ip")?.getString("ip") ?: "N/A"
                serverIpValue.text = serverIp

                // Fetch Server Local Time
                val serverTime = fetchEC2Data("$BACKEND_URL/local_time")?.getString("time") ?: "N/A"
                serverTimeValue.text = serverTime

                // Fetch Backend User Name
                val backendNameJson = fetchEC2Data("$BACKEND_URL/name")
                val backendFirstName = backendNameJson?.getString("firstName") ?: "N/A"
                val backendLastName = backendNameJson?.getString("lastName") ?: ""
                backendNameValue.text = "$backendFirstName $backendLastName"

                // Fetch Client Public IP
                val clientPublicIp = fetchClientPublicIp() ?: "N/A"
                clientIpValue.text = clientPublicIp

                // Get Client Local Time in GMT
                val clientLocalTime = getClientLocalTime()
                clientTimeValue.text = clientLocalTime

                // Get Logged-In User Name
                val loggedInUserName = getLoggedInUserName()
                loggedInUserValue.text = loggedInUserName

            } catch (e: IOException) {
                // Handle network-related errors (e.g., no internet, server unreachable)
                Log.e(TAG, "Network error fetching dashboard data", e)
                Toast.makeText(this@DashboardActivity, "Network error: Please check your connection", Toast.LENGTH_SHORT).show()

            } catch (e: JSONException) {
                // Handle JSON parsing errors (e.g., malformed JSON response)
                Log.e(TAG, "JSON parsing error fetching dashboard data", e)
                Toast.makeText(this@DashboardActivity, "Data format error: Please try again later", Toast.LENGTH_SHORT).show()

            } catch (e: CancellationException) {
                // Handle coroutine cancellation (e.g., if the scope is cancelled)
                Log.e(TAG, "Coroutine cancelled while fetching dashboard data", e)
                Toast.makeText(this@DashboardActivity, "Operation cancelled", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                // Catch any other unexpected exceptions
                Log.e(TAG, "Unexpected error fetching dashboard data", e)
                Toast.makeText(this@DashboardActivity, "Unexpected error: Please try again later", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun fetchEC2Data(urlString: String): JSONObject? {
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
            } catch (e: MalformedURLException) {
                // Handle invalid URL format
                Log.e(TAG, "Malformed URL: $urlString", e)
                null
            } catch (e: IOException) {
                // Handle network-related errors (e.g., no internet connection, timeouts)
                Log.e(TAG, "Network error while fetching data from $urlString", e)
                null
            } catch (e: JSONException) {
                // Handle JSON parsing errors
                Log.e(TAG, "JSON parsing error while fetching data from $urlString", e)
                null
            } catch (e: SecurityException) {
                // Handle security-related errors (e.g., missing network permissions)
                Log.e(TAG, "Security exception while fetching data from $urlString", e)
                null
            }
        }
    }


    private suspend fun fetchClientPublicIp(): String? {
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
            } catch (e: MalformedURLException) {
                // Handle invalid URL format
                Log.e(TAG, "Malformed URL while fetching public IP", e)
                null
            } catch (e: IOException) {
                // Handle network-related errors (e.g., no internet connection, timeouts)
                Log.e(TAG, "Network error while fetching public IP", e)
                null
            } catch (e: JSONException) {
                // Handle JSON parsing errors
                Log.e(TAG, "JSON parsing error while fetching public IP", e)
                null
            } catch (e: SecurityException) {
                // Handle security-related errors (e.g., missing network permissions)
                Log.e(TAG, "Security exception while fetching public IP", e)
                null
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun getClientLocalTime(): String {
        val now = Calendar.getInstance()
        val sdf = SimpleDateFormat("HH:mm:ss 'GMT'Z", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(now.time)
    }


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
