package com.example.m1.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.m1.R
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.*



data class EventResponse(
    val events: List<Event>
)

data class Comment(
    val created_at: String,
    val text: String,
    val comment_id: String,
    val user: String
)

data class Event(
    val event_type: String,
    val date: String,
    val estimated_end_date: String,
    val lng: Double,
    val event_id: String,
    val comments: List<Comment>, // Updated from List<String> to List<Comment>
    val lat: Double,
    val country_code: String,
    val created_time: String,
    val source_event_id: String,
    val continent: String,
    val event_name: String,
    val danger_level: Int
)

interface ApiService {
    @GET("prod/event")
    fun getEvents(): Call<EventResponse>

    @POST("prod/comment/{eventId}")
    fun postComment(
        @Path("eventId") eventId: String,
        @Query("comment") comment: String,
        @Query("user") user: String
    ): Call<Void>
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

class MapboxFragment : Fragment(), LocationListener {

    private lateinit var mapView: MapView
    private lateinit var homeAnnotationManager: PointAnnotationManager
    private lateinit var eventAnnotationManager: PointAnnotationManager
    private lateinit var locationManager: LocationManager
    private var globalEvents: List<Event> = emptyList()
    private val fetchScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val DEBUG_TAG = "MapBoxFragment"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

        // Define the marker icons and sizes
        private const val WILDFIREMARKER_ICON_ID = "wildfiremarker-icon"
        private const val WILDFIREMARKER_ICON_SIZE = 75 // Size of the marker icon in pixels

        private const val HOME_ICON_ID = "home-icon"
        private const val HOME_ICON_SIZE = 150

        private const val EARTHQUAKE_ICON_ID = "earthquake-icon"
        private const val EARTHQUAKE_ICON_SIZE = 75
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mapbox, container, false)

        // Initialize the MapView
        mapView = view.findViewById(R.id.mapView)

        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(-95.7129, 37.0902)) // Center on North America
                .zoom(2.0) // Adjust the zoom level as needed
                .build()
        )

        // Define our class vars
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        eventAnnotationManager = mapView.annotations.createPointAnnotationManager()
        homeAnnotationManager = mapView.annotations.createPointAnnotationManager()

        // Set up the click listener for the event annotations
        eventAnnotationManager.addClickListener(
            OnPointAnnotationClickListener { annotation ->
                val event = globalEvents.find { it.lng == annotation.point.longitude() && it.lat == annotation.point.latitude() }
                event?.let { showEventDetailsPopup(it) }
                true
            }
        )

        loadMapStyle()
        checkLocationPermissionAndFetch()

        // Start coroutine to get event data
        startFetchingEvents()

        return view
    }

    private fun loadMapStyle() {
        fun drawableToBitmap(drawable: Drawable): Bitmap {
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }

        fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
            return Bitmap.createScaledBitmap(bitmap, width, height, false)
        }

        // Load the map style and add the marker icons in Drawables
        mapView.mapboxMap.loadStyle(
            Style.SATELLITE_STREETS
        ) { style ->
            // Loads all the icons into the style
            val wildfireMarkerBitmap = drawableToBitmap(
                ContextCompat.getDrawable(requireContext(), R.drawable.wildfiremarker_icon)!!
            )
            val resizedWildfire = resizeBitmap(wildfireMarkerBitmap, WILDFIREMARKER_ICON_SIZE, WILDFIREMARKER_ICON_SIZE)
            style.addImage(WILDFIREMARKER_ICON_ID, resizedWildfire)

            val homeMarkerBitmap = drawableToBitmap(
                ContextCompat.getDrawable(requireContext(), R.drawable.home_icon)!!
            )
            val resizedHome = resizeBitmap(homeMarkerBitmap, HOME_ICON_SIZE, HOME_ICON_SIZE)
            style.addImage(HOME_ICON_ID, resizedHome)

            val earthquakeMarkerBitmap = drawableToBitmap(
                ContextCompat.getDrawable(requireContext(), R.drawable.earthquake_icon)!!
            )
            val resizedEarthquake = resizeBitmap(earthquakeMarkerBitmap, EARTHQUAKE_ICON_SIZE, EARTHQUAKE_ICON_SIZE)
            style.addImage(EARTHQUAKE_ICON_ID, resizedEarthquake)
        }
    }

    private fun checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 5000, 10f, this
            )
        }
    }

    override fun onLocationChanged(location: Location) {
        val userLocation = Point.fromLngLat(location.longitude, location.latitude)

        // Move the camera to the user's location
        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(userLocation)
                .zoom(3.0) // Adjust the zoom level as needed
                .build()
        )

        // Add a marker at the user's location
        addHomeMarker(userLocation)
    }

    private fun addWildfireMarker(point: Point) {
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(WILDFIREMARKER_ICON_ID)
        eventAnnotationManager.create(pointAnnotationOptions)
    }

    private fun addHomeMarker(point: Point) {
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(HOME_ICON_ID)
        homeAnnotationManager.create(pointAnnotationOptions)
    }

    private fun addEarthquakeMarker(point: Point) {
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(EARTHQUAKE_ICON_ID)
        eventAnnotationManager.create(pointAnnotationOptions)
    }

    private fun showEventDetailsPopup(event: Event) {
        val dialogView = layoutInflater.inflate(R.layout.event_popup, null)
        val eventTitle = dialogView.findViewById<TextView>(R.id.eventTitle)
        val eventWarning = dialogView.findViewById<TextView>(R.id.eventWarning)
        val eventEndDate = dialogView.findViewById<TextView>(R.id.eventEndDate)
        val eventDangerLevel = dialogView.findViewById<TextView>(R.id.eventDangerLevel)
        val eventFooter = dialogView.findViewById<TextView>(R.id.eventFooter)

        // Set event details
        eventTitle.text = event.event_name
        eventWarning.text = "Warning: ${event.event_type} detected on ${event.date}"
        eventEndDate.text = "Expected end: ${event.estimated_end_date}"
        eventDangerLevel.text = "Danger Level: ${event.danger_level} / 100 based on your proximity."
        eventFooter.text = "Refer to local authorities for more information."

        // Comment UI elements
        val commentSection = dialogView.findViewById<LinearLayout>(R.id.commentSection)
        val commentInput = dialogView.findViewById<EditText>(R.id.commentInput)
        val addCommentButton = dialogView.findViewById<Button>(R.id.addCommentButton)

        // Comment Bubble - Helper Function
        fun addCommentBubble(username: String, comment: String) {
            val bubbleContainer = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(8, 8, 8, 8)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 8, 0, 8)
                layoutParams = params
            }

            val usernameTextView = TextView(requireContext()).apply {
                text = username
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            bubbleContainer.addView(usernameTextView)

            val commentTextView = TextView(requireContext()).apply {
                text = comment

                // Set the background depending on the user
                val bubbleDrawable = if (username == getSignedInUserName()) {
                    R.drawable.comment_bubble_background_light_blue
                } else {
                    R.drawable.comment_bubble_background
                }
                setBackgroundResource(bubbleDrawable)
                setPadding(16, 8, 16, 8)
            }
            bubbleContainer.addView(commentTextView)

            commentSection.addView(bubbleContainer)
        }

        // Populate existing comments
        event.comments.forEach { comment ->
            addCommentBubble(comment.user, comment.text)
        }

        // OnClick Listener for Comments
        addCommentButton.setOnClickListener {
            val newComment = commentInput.text.toString().trim()
            if (newComment.isNotEmpty()) {

                // Retrieve the signed-in user's name from SharedPreferences
                val userName = getSignedInUserName()

                // Log event_id, newComment, and username
                Log.d("CommentDebug", "Event ID: ${event.event_id}, Comment: $newComment, Username: $userName")

                // Call the POST API with event.event_id, comment, and user name
                RetrofitClient.apiService.postComment(event.event_id, newComment, userName)
                    .enqueue(object : Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                            if (response.isSuccessful) {

                                // Add the comment bubble only if the POST is successful
                                addCommentBubble(userName, newComment)
                                commentInput.text.clear()
                                Toast.makeText(requireContext(), "Comment added", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Failed to add comment", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<Void>, t: Throwable) {
                            Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            } else {
                Toast.makeText(requireContext(), "Please enter a comment", Toast.LENGTH_SHORT).show()
            }
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Helper to get the signed-in user's name from SharedPreferences
    private fun getSignedInUserName(): String {
        val sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("userName", "Anonymous") ?: "Anonymous"
    }


    private fun startFetchingEvents() {
        fetchScope.launch {
            while (isActive) {
                fetchEvents()
                delay(3600000) // 1 hour delay
            }
        }
    }

    private fun fetchEvents() {
        RetrofitClient.apiService.getEvents().enqueue(object : Callback<EventResponse> {
            override fun onResponse(call: Call<EventResponse>, response: Response<EventResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        globalEvents = it.events.take(50)
                        Log.d("API_SUCCESS", "Fetched ${globalEvents.size} events")
                        Log.d("API_SUCCESS", "First event: ${globalEvents[0]}")

                        // Clear passed events
                        eventAnnotationManager.deleteAll()
                        populateEventsOnMap()
                    }
                } else {
                    Log.e("API_ERROR", "Response not successful: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<EventResponse>, t: Throwable) {
                Log.e("API_FAILURE", "Failed to fetch events: ${t.message}")
            }
        })
    }

    private fun populateEventsOnMap() {
        for (event in globalEvents) {
            val point = Point.fromLngLat(event.lng, event.lat)
            Log.d("API_POPULATE", "Populating event: ${event.event_name}")
            if (event.event_type == "WF") {
                addWildfireMarker(point)
            }
            if (event.event_type == "EQ") {
                addEarthquakeMarker(point)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fetchScope.cancel() // Cancel coroutine to prevent memory leaks
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}