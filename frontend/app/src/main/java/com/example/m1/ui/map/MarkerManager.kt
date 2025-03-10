package com.example.m1.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.example.m1.R
import com.example.m1.data.models.Event
import com.example.m1.data.models.UserMarker
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions

/**
 * Manager class to handle map markers
 */
class MarkerManager(private val context: Context) {

    // Icon IDs and sizes
    companion object {
        private const val WILDFIRE_ICON_ID = "wildfiremarker-icon"
        private const val WILDFIRE_ICON_SIZE = 75

        private const val HOME_ICON_ID = "home-icon"
        private const val HOME_ICON_SIZE = 150

        private const val EARTHQUAKE_ICON_ID = "earthquake-icon"
        private const val EARTHQUAKE_ICON_SIZE = 75

        private const val SAFEHOUSE_ICON_ID = "safehouse_icon"
        private const val SAFEHOUSE_ICON_SIZE = 75

        private const val WARNING_ICON_ID = "warning_icon"
        private const val WARNING_ICON_SIZE = 75

        private const val RESOURCES_ICON_ID = "ressources_icon"
        private const val RESOURCES_ICON_SIZE = 75
    }

    /**
     * Load marker icons into the map style
     * @param style The Mapbox style to load icons into
     */
    fun loadMarkerIcons(style: Style) {
        // Add wildfire icon
        addIconToStyle(
            style,
            WILDFIRE_ICON_ID,
            R.drawable.wildfiremarker_icon,
            WILDFIRE_ICON_SIZE
        )

        // Add home icon
        addIconToStyle(
            style,
            HOME_ICON_ID,
            R.drawable.home_icon,
            HOME_ICON_SIZE
        )

        // Add earthquake icon
        addIconToStyle(
            style,
            EARTHQUAKE_ICON_ID,
            R.drawable.earthquake_icon,
            EARTHQUAKE_ICON_SIZE
        )

        // Add safehouse icon
        addIconToStyle(
            style,
            SAFEHOUSE_ICON_ID,
            R.drawable.safehouse_icon,
            SAFEHOUSE_ICON_SIZE
        )

        // Add warning icon
        addIconToStyle(
            style,
            WARNING_ICON_ID,
            R.drawable.warning_icon,
            WARNING_ICON_SIZE
        )

        // Add resources icon
        addIconToStyle(
            style,
            RESOURCES_ICON_ID,
            R.drawable.ressources_icon,
            RESOURCES_ICON_SIZE
        )
    }

    /**
     * Add event markers to the map
     * @param events The list of events to add markers for
     * @param annotationManager The annotation manager to add markers to
     */
    fun addEventMarkers(events: List<Event>, annotationManager: PointAnnotationManager) {
        // Clear existing markers
        annotationManager.deleteAll()

        // Add markers for each event
        for (event in events) {
            val point = Point.fromLngLat(event.lng, event.lat)
            when (event.event_type) {
                "WF" -> addWildfireMarker(point, annotationManager)
                "EQ" -> addEarthquakeMarker(point, annotationManager)
                // Add other event types here
            }
        }
    }

    /**
     * Add user markers to the map
     * @param userMarkers The list of user markers to add
     * @param annotationManager The annotation manager to add markers to
     */
    fun addUserMarkers(userMarkers: List<UserMarker>, annotationManager: PointAnnotationManager) {
        // Clear existing markers
        annotationManager.deleteAll()

        // Add markers for each user marker
        for (marker in userMarkers) {
            val point = Point.fromLngLat(marker.longitude, marker.latitude)
            val iconId = getIconForType(marker.type)

            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(iconId)

            annotationManager.create(pointAnnotationOptions)
        }
    }

    /**
     * Add home marker to the map
     * @param point The point to add the marker at
     * @param annotationManager The annotation manager to add the marker to
     */
    fun addHomeMarker(point: Point, annotationManager: PointAnnotationManager) {
        // Clear existing markers
        annotationManager.deleteAll()

        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(HOME_ICON_ID)

        annotationManager.create(pointAnnotationOptions)
    }

    /**
     * Add wildfire marker to the map
     * @param point The point to add the marker at
     * @param annotationManager The annotation manager to add the marker to
     */
    private fun addWildfireMarker(point: Point, annotationManager: PointAnnotationManager) {
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(WILDFIRE_ICON_ID)

        annotationManager.create(pointAnnotationOptions)
    }

    /**
     * Add earthquake marker to the map
     * @param point The point to add the marker at
     * @param annotationManager The annotation manager to add the marker to
     */
    private fun addEarthquakeMarker(point: Point, annotationManager: PointAnnotationManager) {
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(EARTHQUAKE_ICON_ID)

        annotationManager.create(pointAnnotationOptions)
    }

    /**
     * Get icon ID for marker type
     * @param type The marker type
     * @return The icon ID
     */
    private fun getIconForType(type: String): String {
        return when (type) {
            "Warning" -> WARNING_ICON_ID
            "Safehouse" -> SAFEHOUSE_ICON_ID
            "Resource" -> RESOURCES_ICON_ID
            else -> WARNING_ICON_ID
        }
    }

    /**
     * Add icon to the map style
     * @param style The Mapbox style to add the icon to
     * @param iconId The ID of the icon
     * @param drawableId The drawable resource ID
     * @param size The size of the icon
     */
    private fun addIconToStyle(style: Style, iconId: String, drawableId: Int, size: Int) {
        val drawable = ContextCompat.getDrawable(context, drawableId)
            ?: return

        val bitmap = drawableToBitmap(drawable)
        val resizedBitmap = resizeBitmap(bitmap, size, size)

        style.addImage(iconId, resizedBitmap)
    }

    /**
     * Convert drawable to bitmap
     * @param drawable The drawable to convert
     * @return The bitmap
     */
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
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
     * Resize bitmap
     * @param bitmap The bitmap to resize
     * @param width The new width
     * @param height The new height
     * @return The resized bitmap
     */
    private fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, false)
    }
}