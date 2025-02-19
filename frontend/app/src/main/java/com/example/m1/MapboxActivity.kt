package com.example.m1

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

class MapboxActivity : AppCompatActivity(), LocationListener {

    //Define Class Vars
    private lateinit var mapView: MapView
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var locationManager: LocationManager

    companion object {
        private const val DEBUG_TAG = "MapBoxActivity"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val WILDFIREMARKER_ICON_ID = "wildfiremarker-icon" // Unique ID for the marker icon
        private const val WILDFIREMARKER_ICON_SIZE = 200 // Size of the marker icon in pixels
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

        loadMapStyle()
        checkLocationPermissionAndFetch()
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
            val markerBitmap = drawableToBitmap(
                ContextCompat.getDrawable(this, R.drawable.wildfiremarker_icon)!!
            )
            val resizedWildfire = resizeBitmap(markerBitmap, WILDFIREMARKER_ICON_SIZE, WILDFIREMARKER_ICON_SIZE)
            style.addImage(WILDFIREMARKER_ICON_ID, resizedWildfire)
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
                .zoom(2.0) // Adjust the zoom level as needed
                .build()
        )

        // Add a marker at the user's location
        addWildfireMarker(userLocation)
    }

    //TODO: Find a way to make point Annotiation Managers saveable so we can turn them into layers.
    /**
     * Adds a wildfire marker at a given Point
     *
     * @param point Point: Point to add the marker at
     *
     * @precondition Point must have a longitude and latitude
     * @precondition pointAnnotationManager must be initialized
     */
    private fun addWildfireMarker(point: Point) {
        // Delete all existing annotations (optional)
        pointAnnotationManager.deleteAll()

        // Create a new point annotation (marker)
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(WILDFIREMARKER_ICON_ID) // Use the marker icon loaded into the style

        // Add the annotation to the map
        pointAnnotationManager.create(pointAnnotationOptions)

        // Show a Toast message to confirm the marker was added
        Toast.makeText(this, "Marker added at current location", Toast.LENGTH_SHORT).show()
    }


}