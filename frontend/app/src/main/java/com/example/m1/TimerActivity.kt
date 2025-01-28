package com.example.m1

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class TimerActivity : AppCompatActivity() {

    // Timer Variables
    private lateinit var inputMinutes: EditText
    private lateinit var inputSeconds: EditText
    private lateinit var startTimerButton: Button
    private lateinit var timerTextView: TextView

    // Weather Info Views
    private lateinit var weatherSectionTitle: TextView
    private lateinit var weatherIcon: ImageView
    private lateinit var weatherDescription: TextView
    private lateinit var temperature: TextView
    private lateinit var humidity: TextView
    private lateinit var wind: TextView

    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 0 // in milliseconds

    // Backend API to Display Weather
    private val API_URL = "https://api.weatherstack.com/current?access_key=12dff500bf74f79dca4667d4555cd2dd&query=Vancouver"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        // Initialize Views
        inputMinutes = findViewById(R.id.inputMinutes)
        inputSeconds = findViewById(R.id.inputSeconds)
        startTimerButton = findViewById(R.id.startTimerButton)
        timerTextView = findViewById(R.id.timerTextView)

        weatherSectionTitle = findViewById(R.id.weatherSectionTitle)
        weatherIcon = findViewById(R.id.weatherIcon)
        weatherDescription = findViewById(R.id.weatherDescription)
        temperature = findViewById(R.id.temperature)
        humidity = findViewById(R.id.humidity)
        wind = findViewById(R.id.wind)

        // Button Listener to start the timer when clicked
        startTimerButton.setOnClickListener {
            val minutes = inputMinutes.text.toString().toLongOrNull() ?: 0
            val seconds = inputSeconds.text.toString().toLongOrNull() ?: 0

            // Disable negative values for the time
            if (minutes < 0 || seconds < 0) {
                Toast.makeText(this, "Please enter positive values", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Time can not be 0
            if (minutes == 0L && seconds == 0L) {
                Toast.makeText(this, "Please enter a valid time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            timeLeftInMillis = (minutes * 60 + seconds) * 1000

            // Start the timer
            startTimer()

            // Disable input fields and button to prevent changes during countdown
            inputMinutes.isEnabled = false
            inputSeconds.isEnabled = false
            startTimerButton.isEnabled = false
        }
    }

    private fun startTimer() {
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerText()
            }

            override fun onFinish() {
                timerTextView.text = "Timer Finished!"
                fetchWeatherData()
            }
        }.start()
    }

    private fun updateTimerText() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60

        val timeFormatted = String.format("%02d:%02d", minutes, seconds)
        timerTextView.text = "Time Left: $timeFormatted"
    }

    // Call the Weather API to Retrieve Weather Information
    private fun fetchWeatherData() {

        runOnUiThread {
            weatherSectionTitle.visibility = View.VISIBLE
            weatherIcon.visibility = View.VISIBLE
            weatherDescription.visibility = View.VISIBLE
            temperature.visibility = View.VISIBLE
            humidity.visibility = View.VISIBLE
            wind.visibility = View.VISIBLE
        }

        thread {
            try {
                val url = URL(API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val stream = connection.inputStream
                    val response = stream.bufferedReader().use { it.readText() }
                    parseWeatherData(response)
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to fetch weather data", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Update the weather variables with API data
    private fun parseWeatherData(jsonString: String) {
        try {
            val jsonObject = JSONObject(jsonString)
            val current = jsonObject.getJSONObject("current")
            val description = current.getJSONArray("weather_descriptions").getString(0)
            val temp = current.getInt("temperature")
            val hum = current.getInt("humidity")
            val windSpeed = current.getInt("wind_speed")
            val windDir = current.getString("wind_dir")
            val iconUrl = current.getJSONArray("weather_icons").getString(0)

            runOnUiThread {
                weatherDescription.text = "Weather: $description"
                temperature.text = "Temperature: $temp°C"
                humidity.text = "Humidity: $hum%"
                wind.text = "Wind: $windSpeed km/h $windDir"
                Glide.with(this)
                    .load(iconUrl)
                    .into(weatherIcon)

                // Re-enable the input fields and start button
                inputMinutes.isEnabled = true
                inputSeconds.isEnabled = true
                startTimerButton.isEnabled = true
            }

        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Parsing error", Toast.LENGTH_SHORT).show()
                // **Re-enable the input fields and start button even if parsing fails**
                inputMinutes.isEnabled = true
                inputSeconds.isEnabled = true
                startTimerButton.isEnabled = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
