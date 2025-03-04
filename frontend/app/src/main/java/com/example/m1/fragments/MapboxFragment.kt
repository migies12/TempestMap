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
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.m1.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions.Companion.mapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationClickListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolygonAnnotationManager
import com.mapbox.maps.toCameraOptions
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.*
import java.util.UUID
import com.example.m1.FavoriteLocation
import com.example.m1.FavoriteLocationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures

data class EventResponse(
    val events: List<Event>
)

data class UserMarker(
    val id: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val comments: List<Comment> = emptyList()
)

data class Comment(
    val created_at: String,
    val text: String,
    val comment_id: String,
    val user: String
)

data class CommentResponse(
    val event_id: String,
    val comments: List<Comment>
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

    @GET("prod/comment/{eventId}")
    fun getComments(@Path("eventId") eventId: String): Call<CommentResponse>
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
    private lateinit var userAnnotationManager: PointAnnotationManager
    private lateinit var locationManager: LocationManager
    private val userMarkers = mutableListOf<UserMarker>()
    private var globalEvents: List<Event> = emptyList()
    private val fetchScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastKnownLocation = Location("")
    private var previousCameraOptions: CameraOptions? = null
    private lateinit var favoriteLocationManager: FavoriteLocationManager
    private var selectedPoint: Point? = null
    private var favoriteMarkerAnnotationManager: PointAnnotationManager? = null
    private var selectedPointAnnotationManager: PointAnnotationManager? = null

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

        private const val SAFEHOUSE_ICON_ID = "safehouse_icon"
        private const val SAFEHOUSE_ICON_SIZE = 75

        private const val WARNING_ICON_ID = "warning_icon"
        private const val WARNING_ICON_SIZE = 75

        private const val RESSOURCES_ICON_ID = "ressources_icon"
        private const val RESSOURCES_ICON_SIZE = 75


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

        // Initialize the FAB
        val fabAddMarker = view.findViewById<FloatingActionButton>(R.id.fabAddMarker)
        fabAddMarker.setOnClickListener {
            // Show options dialog
            showOptionsDialog()
        }

        // Initialize FavoriteLocationManager
        favoriteLocationManager = FavoriteLocationManager(requireContext())

        // Define our class vars
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        eventAnnotationManager = mapView.annotations.createPointAnnotationManager()
        userAnnotationManager = mapView.annotations.createPointAnnotationManager()
        homeAnnotationManager = mapView.annotations.createPointAnnotationManager()
        selectedPointAnnotationManager = mapView.annotations.createPointAnnotationManager()
        favoriteMarkerAnnotationManager = mapView.annotations.createPointAnnotationManager()

        // Set up the click listener for the event annotations
        eventAnnotationManager.addClickListener(
            OnPointAnnotationClickListener { annotation ->
                val event = globalEvents.find { it.lng == annotation.point.longitude() && it.lat == annotation.point.latitude() }
                event?.let {
                    // Store current camera position before zooming in
                    previousCameraOptions = mapView.mapboxMap.cameraState.toCameraOptions()

                    // Animate zoom in to the event location with smooth animation
                    mapView.mapboxMap.flyTo(
                        CameraOptions.Builder()
                            .center(annotation.point)
                            .zoom(12.0) // Adjust zoom level as needed
                            .build(),
                        mapAnimationOptions {
                            duration(2000) // 2 seconds for smooth animation
                        }
                    )

                    showEventDetailsPopup(it)
                }
                true
            }
        )

        // Set up the click listener for the user annotations
        userAnnotationManager.addClickListener(
            OnPointAnnotationClickListener { annotation ->
                val userMarker = userMarkers.find {
                    it.latitude == annotation.point.latitude() && it.longitude == annotation.point.longitude()
                }
                userMarker?.let { showUserMarkerDetailsPopup(it) }
                true
            }
        )

        // Add map click listener for location selection
        mapView.mapboxMap.addOnMapClickListener { point ->
            // Store the selected point
            selectedPoint = point

            // Add a marker for the selected point
            addSelectedPointMarker(point)

            // Show the save to favorites option
            showSaveToFavoritesDialog(point)

            true
        }

        // Check for any passed location from FavoritesFragment
        arguments?.let {
            val latitude = it.getDouble("latitude", 0.0)
            val longitude = it.getDouble("longitude", 0.0)
            val locationName = it.getString("locationName")

            if (latitude != 0.0 && longitude != 0.0) {
                // Navigate to this location
                val point = Point.fromLngLat(longitude, latitude)
                mapView.mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(15.0)
                        .build()
                )

                // Add a marker at this location
                addFavoriteLocationMarker(point)

                // Show a toast with the location name
                locationName?.let { name ->
                    Toast.makeText(context, "Viewing: $name", Toast.LENGTH_SHORT).show()
                }
            }
        }

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
            Style.DARK
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

            val warningMarkerBitmap = drawableToBitmap(
                ContextCompat.getDrawable(requireContext(), R.drawable.warning_icon)!!
            )
            val resizedWarning = resizeBitmap(warningMarkerBitmap, WARNING_ICON_SIZE, WARNING_ICON_SIZE)
            style.addImage(WARNING_ICON_ID, resizedWarning)

            val ressourceMarkerBitmap = drawableToBitmap(
                ContextCompat.getDrawable(requireContext(), R.drawable.ressources_icon)!!
            )
            val resizedRessource = resizeBitmap(ressourceMarkerBitmap, RESSOURCES_ICON_SIZE, RESSOURCES_ICON_SIZE)
            style.addImage(RESSOURCES_ICON_ID, resizedRessource)

            val safehouseMarkerBitmap = drawableToBitmap(
                ContextCompat.getDrawable(requireContext(), R.drawable.safehouse_icon)!!
            )
            val resizedSafehouse = resizeBitmap(safehouseMarkerBitmap, SAFEHOUSE_ICON_SIZE, SAFEHOUSE_ICON_SIZE)
            style.addImage(SAFEHOUSE_ICON_ID, resizedSafehouse)
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

        lastKnownLocation = location

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

    private fun showCreateMarkerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_marker, null)
        val etLatitude = dialogView.findViewById<EditText>(R.id.etLatitude)
        val etLongitude = dialogView.findViewById<EditText>(R.id.etLongitude)
        val spinnerMarkerType = dialogView.findViewById<Spinner>(R.id.spinnerMarkerType)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
        val btnCreateMarker = dialogView.findViewById<Button>(R.id.btnCreateMarker)


        lastKnownLocation?.let {
            etLatitude.setText(it.latitude.toString())
            etLongitude.setText(it.longitude.toString())
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Create Marker")
            .create()

        btnCreateMarker.setOnClickListener {
            val latitude = etLatitude.text.toString().toDoubleOrNull()
            val longitude = etLongitude.text.toString().toDoubleOrNull()
            val markerType = spinnerMarkerType.selectedItem.toString()
            val description = etDescription.text.toString()

            if (latitude != null && longitude != null) {
                val point = Point.fromLngLat(longitude, latitude)
                addUserMarker(point, markerType, description)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Invalid latitude or longitude", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun addUserMarker(point: Point, type: String, description: String) {
        val userMarker = UserMarker(
            id = UUID.randomUUID().toString(), // Generate a unique ID
            type = type,
            latitude = point.latitude(),
            longitude = point.longitude(),
            description = description
        )

        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(getIconForType(type)) // Helper function to get the icon

        userAnnotationManager.create(pointAnnotationOptions)

        // Add the user marker to the list
        userMarkers.add(userMarker)
    }

    private fun getIconForType(type: String): String {
        return when (type) {
            "Warning" -> WARNING_ICON_ID
            "Safehouse" -> SAFEHOUSE_ICON_ID
            "Resource" -> RESSOURCES_ICON_ID
            else -> WARNING_ICON_ID
        }
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

    private fun showUserMarkerDetailsPopup(userMarker: UserMarker) {
        val dialogView = layoutInflater.inflate(R.layout.user_marker_popup, null)
        val userMarkerType = dialogView.findViewById<TextView>(R.id.userMarkerType)
        val userMarkerDescription = dialogView.findViewById<TextView>(R.id.userMarkerDescription)

        // Set user marker details
        userMarkerType.text = "Type: ${userMarker.type}"
        userMarkerDescription.text = "Description: ${userMarker.description}"

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
        userMarker.comments.forEach { comment ->
            addCommentBubble(comment.user, comment.text)
        }

        // OnClick Listener for Comments
        addCommentButton.setOnClickListener {
            val newComment = commentInput.text.toString().trim()
            if (newComment.isNotEmpty()) {

                // Retrieve the signed-in user's name from SharedPreferences
                val userName = getSignedInUserName()

                // Log marker ID, newComment, and username
                Log.d("CommentDebug", "Marker ID: ${userMarker.id}, Comment: $newComment, Username: $userName")

                // Call the POST API with marker ID, comment, and user name
                RetrofitClient.apiService.postComment(userMarker.id, newComment, userName)
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

        // Helper function to add a comment bubble
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

        // Clear any existing comments
        commentSection.removeAllViews()

        // Fetch comments from the server using the new GET endpoint
        RetrofitClient.apiService.getComments(event.event_id).enqueue(object : Callback<CommentResponse> {
            override fun onResponse(call: Call<CommentResponse>, response: Response<CommentResponse>) {
                if (response.isSuccessful) {
                    response.body()?.comments?.forEach { comment ->
                        addCommentBubble(comment.user, comment.text)
                    }
                } else {
                    Log.e("API_ERROR", "Failed to load comments: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<CommentResponse>, t: Throwable) {
                Log.e("API_FAILURE", "Error fetching comments: ${t.message}")
            }
        })

        // OnClick Listener for adding new comments

        addCommentButton.setOnClickListener {
            val userName = getSignedInUserName()
            if (userName == "Anonymous") {
                Toast.makeText(requireContext(), "Please sign in to comment", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newComment = commentInput.text.toString().trim()
            if (newComment.isNotEmpty()) {
                Log.d("CommentDebug", "Event ID: ${event.event_id}, Comment: $newComment, Username: $userName")
                RetrofitClient.apiService.postComment(event.event_id, newComment, userName)
                    .enqueue(object : Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                            if (response.isSuccessful) {
                                // Optionally refresh the comments
                                RetrofitClient.apiService.getComments(event.event_id)
                                    .enqueue(object : Callback<CommentResponse> {
                                        override fun onResponse(call: Call<CommentResponse>, response: Response<CommentResponse>) {
                                            if (response.isSuccessful) {
                                                commentSection.removeAllViews()
                                                response.body()?.comments?.forEach { comment ->
                                                    addCommentBubble(comment.user, comment.text)
                                                }
                                            }
                                        }
                                        override fun onFailure(call: Call<CommentResponse>, t: Throwable) {
                                            Log.e("API_FAILURE", "Error refreshing comments: ${t.message}")
                                        }
                                    })
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

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        // Restore previous camera position when the dialog is dismissed
        dialog.setOnDismissListener {
            previousCameraOptions?.let { prevCamera ->
                mapView.mapboxMap.flyTo(
                    prevCamera,
                    mapAnimationOptions {
                        duration(2000)
                    }
                )
            }
        }

        dialog.show()
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

    private fun showFabMenu() {
        val options = arrayOf("My Favorites", "Add Current Location to Favorites")

        AlertDialog.Builder(requireContext())
            .setTitle("Map Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> navigateToFavoritesFragment()
                    1 -> addCurrentLocationToFavorites()
                }
            }
            .show()
    }

    private fun addCurrentLocationToFavorites() {
        lastKnownLocation?.let { location ->
            val point = Point.fromLngLat(location.longitude, location.latitude)
            showSaveToFavoritesDialog(point)
        } ?: run {
            Toast.makeText(context, "Current location not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSaveToFavoritesDialog(point: Point) {
        // First check if user is logged in
        val sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val isSignedIn = sharedPreferences.getBoolean("isSignedIn", false)

        if (!isSignedIn) {
            // Show sign-in required dialog
            AlertDialog.Builder(requireContext())
                .setTitle("Sign In Required")
                .setMessage("You must be logged in to save locations to favorites.")
                .setPositiveButton("Sign In") { _, _ ->
                    // Navigate to SignInFragment
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.container, SignInFragment())
                        .addToBackStack(null)
                        .commit()
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        // If already a favorite, notify user
        if (favoriteLocationManager.isLocationFavorite(point.latitude(), point.longitude())) {
            Toast.makeText(context, "This location is already in your favorites", Toast.LENGTH_SHORT).show()
            return
        }

        // Create and show the save dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_save_favorite, null)
        val etLocationName = dialogView.findViewById<EditText>(R.id.etLocationName)
        val etLocationDescription = dialogView.findViewById<EditText>(R.id.etLocationDescription)
        val tvLatitude = dialogView.findViewById<TextView>(R.id.tvLatitude)
        val tvLongitude = dialogView.findViewById<TextView>(R.id.tvLongitude)
        val btnSaveLocation = dialogView.findViewById<Button>(R.id.btnSaveLocation)

        // Set coordinate values
        tvLatitude.text = point.latitude().toString()
        tvLongitude.text = point.longitude().toString()

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Save button click listener
        btnSaveLocation.setOnClickListener {
            val locationName = etLocationName.text.toString().trim()
            val description = etLocationDescription.text.toString().trim()

            if (locationName.isEmpty()) {
                etLocationName.error = "Please enter a name for this location"
                return@setOnClickListener
            }

            // Create a new FavoriteLocation object
            val favoriteLocation = FavoriteLocation(
                name = locationName,
                latitude = point.latitude(),
                longitude = point.longitude(),
                description = description
            )

            // Save to preferences
            if (favoriteLocationManager.saveFavoriteLocation(favoriteLocation)) {
                Toast.makeText(context, "Location saved to Favorites", Toast.LENGTH_SHORT).show()

                // Add a marker for this favorite location
                addFavoriteLocationMarker(point)

                dialog.dismiss()
            } else {
                Toast.makeText(context, "Failed to save location. Please try again later.", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun addSelectedPointMarker(point: Point) {
        // Clear any existing selected point markers
        selectedPointAnnotationManager?.let {
            it.deleteAll()
        }

        // Create a point annotation for the selected location
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage("selected_location_icon") // Use an appropriate icon

        selectedPointAnnotationManager?.create(pointAnnotationOptions)
    }

    private fun addFavoriteLocationMarker(point: Point) {
        // Create the annotation manager if not already created
        if (favoriteMarkerAnnotationManager == null) {
            favoriteMarkerAnnotationManager = mapView.annotations.createPointAnnotationManager()
        }

        // Create a point annotation for the favorite location
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage("favorite_location_icon") // Use a star or heart icon

        favoriteMarkerAnnotationManager?.create(pointAnnotationOptions)
    }

    // Add this method to display all favorite locations on the map
    private fun displayFavoriteLocations() {
        val favorites = favoriteLocationManager.getFavoriteLocations()

        // Create the annotation manager if not already created
        if (favoriteMarkerAnnotationManager == null) {
            favoriteMarkerAnnotationManager = mapView.annotations.createPointAnnotationManager()
        }

        // Clear existing favorite markers
        favoriteMarkerAnnotationManager?.deleteAll()

        // Add a marker for each favorite location
        for (favorite in favorites) {
            val point = Point.fromLngLat(favorite.longitude, favorite.latitude)
            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage("favorite_location_icon") // Use a star or heart icon

            favoriteMarkerAnnotationManager?.create(pointAnnotationOptions)
        }
    }

    private fun showOptionsDialog() {
        val options = arrayOf("Create Marker", "Save Current Location", "View Favorites")

        AlertDialog.Builder(requireContext())
            .setTitle("Map Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showCreateMarkerDialog() // Your existing functionality
                    1 -> showSaveCurrentLocationDialog()
                    2 -> navigateToFavoritesFragment()
                }
            }
            .show()
    }

    private fun showSaveCurrentLocationDialog() {
        lastKnownLocation?.let { location ->
            val point = Point.fromLngLat(location.longitude, location.latitude)
            showSaveToFavoritesDialog(point)
        } ?: run {
            Toast.makeText(context, "Current location not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToFavoritesFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, FavoritesFragment())
            .addToBackStack(null)
            .commit()
    }

    // Add this to your onResume method
    override fun onResume() {
        super.onResume()

        // Display favorite locations whenever the fragment resumes
        displayFavoriteLocations()
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