package com.example.m1

import android.Manifest
import android.util.Log
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import kotlinx.coroutines.*



//API CALLS SHOULD BE MOVED TO NEW FILE
//////////////////////////////////////////////////////////////////////////////////////////////////////
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class EventResponse(
    val events: List<Event>
)

data class Event(
    val event_type: String,
    val date: String,
    val estimated_end_date: String,
    val lng: Double,
    val event_id: String,
    val comments: List<String>,
    val lat: Double,
    val country_code: String,
    val created_time: String,
    val source_event_id: String,
    val continent: String,
    val event_name: String
)

interface ApiService {
    @GET("prod/event")
    fun getEvents(): Call<EventResponse>
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

//////////////////////////////////////////////////////////////////////////////////////////////////////





class MapboxActivity : AppCompatActivity(), LocationListener {

    //Define Class Vars
    private lateinit var mapView: MapView
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var eventAnnotationManager: PointAnnotationManager
    private lateinit var locationManager: LocationManager
    private var globalEvents: List<Event> = emptyList()
    private val fetchScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


    companion object {
        private const val DEBUG_TAG = "MapBoxActivity"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1


        // Define the marker icons and sizez
        private const val WILDFIREMARKER_ICON_ID = "wildfiremarker-icon"
        private const val WILDFIREMARKER_ICON_SIZE = 150 // Size of the marker icon in pixels

        private const val HOME_ICON_ID = "home-icon"
        private const val HOME_ICON_SIZE = 150

        private const val EARTHQUAKE_ICON_ID = "earthquake-icon"
        private const val EARTHQUAKE_ICON_SIZE = 100

    }

    /**
     * Called when the activity is first created.
     *
     * Creates the Mapbox and style
     * Verifies the LocationPermissions
     *
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapView = MapView(this)
        setContentView(mapView)

        //Define our class vars
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
        eventAnnotationManager = mapView.annotations.createPointAnnotationManager()


        loadMapStyle()
        checkLocationPermissionAndFetch()

        //Start coroutine to get event data
        //fetchEvents()
        startFetchingEvents()
    }


    /**
     * Helper Function:
     *          Called when activity is created
     *          Loads and creates the Mapbox and its styling
     */
    private fun loadMapStyle() {
        /**
         * Utility function to convert a Drawable(.png OR .xml) to a Bitmap
        **/
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

        /**
         *  Utility function to resize a Bitmap to specified size
          */
         fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
            return Bitmap.createScaledBitmap(bitmap, width, height, false)
        }

        // Load the map style and add the marker icons in Drawables
        mapView.mapboxMap.loadStyle(
            Style.STANDARD
        ) { style ->

            //Loads all the icons into the style
            val wildfireMarkerBitmap = drawableToBitmap(
                ContextCompat.getDrawable(this, R.drawable.wildfiremarker_icon)!!
            )
            val resizedWildfire = resizeBitmap(wildfireMarkerBitmap, WILDFIREMARKER_ICON_SIZE, WILDFIREMARKER_ICON_SIZE)
            style.addImage(WILDFIREMARKER_ICON_ID, resizedWildfire)


            val homeMarkerBitmap = drawableToBitmap(
                ContextCompat.getDrawable(this, R.drawable.home_icon)!!
            )
            val resizedHome = resizeBitmap(homeMarkerBitmap, HOME_ICON_SIZE, HOME_ICON_SIZE)
            style.addImage(HOME_ICON_ID, resizedHome)

            val earthquakeMarkerBitmap = drawableToBitmap(
                ContextCompat.getDrawable(this, R.drawable.earthquake_icon)!!
            )
            val resizedEarthquake = resizeBitmap(earthquakeMarkerBitmap, EARTHQUAKE_ICON_SIZE, EARTHQUAKE_ICON_SIZE)
            style.addImage(EARTHQUAKE_ICON_ID, resizedEarthquake)
        }

    }

    /**
     * Helper Function:
     *      Verifies Location Permissions
     *      Requests the permissions if not granted yet
     *
     * Result: locationManager has active gps location updates
     */
    private fun checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 5000, 10f, this
            )
        }
    }


    /**
     * Called when the location has changed and Does Not Return
     *
     *  Sets camera to UserLocation on every callback
     *
     *
     * **Permissions Must be Enabled**
     **/
    override fun onLocationChanged(location: Location) {
        val userLocation = Point.fromLngLat(location.longitude, location.latitude)

        // Move the camera to the user's location
        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(userLocation)
                .zoom(4.0) // Adjust the zoom level as needed
                .build()
        )



        // Add a marker at the user's location
        addHomeMarker(userLocation)

        // Show a Toast message to confirm the marker was added
        Toast.makeText(this, "Wildfire Marker added at current location", Toast.LENGTH_SHORT).show()
    }

    //TODO: Find a way to make point Annotiation Managers saveable so we can turn them into layers.
    /**
     * Adds a wildfire marker at a given Point
     *
     * @param point Point: Point to add the marker at
     *
     * @precondition Point must have a longitude and latitude
     * @precondition eventAnnotationManager must be initialized
     */
    private fun addWildfireMarker(point: Point) {
        eventAnnotationManager.deleteAll()


        // Create a new point annotation (marker)
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(WILDFIREMARKER_ICON_ID) // Use the marker icon loaded into the style

        // Add the annotation to the map
        eventAnnotationManager.create(pointAnnotationOptions)


    }

    //TODO: Find a way to make point Annotiation Managers saveable so we can turn them into layers.
    /**
     * Adds a home marker at a given Point
     *
     * @param point Point: Point to add the marker at
     *
     * @precondition Point must have a longitude and latitude
     * @precondition eventAnnotationManager must be initialized
     */
    private fun addHomeMarker(point: Point) {


        // Create a new point annotation
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(HOME_ICON_ID)

        // Add the annotation to the map
        pointAnnotationManager.create(pointAnnotationOptions)
    }

    private fun addEarthquakeMarker(point: Point) {


        // Create a new point annotation
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(EARTHQUAKE_ICON_ID)

        // Add the annotation to the map
        eventAnnotationManager.create(pointAnnotationOptions)
    }

    private fun startFetchingEvents() {
        fetchScope.launch {
            while (isActive) {
                fetchEvents()
                delay(3600000) //1 hour delay
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

                        //clear passed events
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
            } else if (event.event_type == "EQ") {
                addEarthquakeMarker(point)
            }
        }


    }




    override fun onDestroy() {
        super.onDestroy()
        fetchScope.cancel() // Cancel coroutine to prevent memory leaks
    }



}