package com.example.m1.util

import com.example.m1.FavoriteLocationManager
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions

class MarkerUtils {

    fun addSelectedPointMarker(point: Point, annotationManager: PointAnnotationManager?) {
        annotationManager?.deleteAll()
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage("selected_location_icon")
        annotationManager?.create(pointAnnotationOptions)
    }

    fun addFavoriteLocationMarker(point: Point, annotationManager: PointAnnotationManager?) {
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage("favorite_location_icon")
        annotationManager?.create(pointAnnotationOptions)
    }

    fun displayFavoriteLocations(favoriteLocationManager: FavoriteLocationManager, annotationManager: PointAnnotationManager?) {
        val favorites = favoriteLocationManager.getFavoriteLocations()
        annotationManager?.deleteAll()
        for (favorite in favorites) {
            val point = Point.fromLngLat(favorite.longitude, favorite.latitude)
            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage("favorite_location_icon")
            annotationManager?.create(pointAnnotationOptions)
        }
    }
}