package com.example.m1

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.m1.databinding.ActivityMapsBinding

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {

    // Variables required for the Map
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationManager: LocationManager

    companion object {
        private const val TAG = "MapsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "Location permissions not granted");
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10_000, 0f, this)
        Log.d(TAG, "Location permissions granted");
    }

    // Once map is loaded, display additional meta data
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val london = LatLng(51.5072, 0.1276)
        val marker = mMap.addMarker(MarkerOptions().position(london).title("My Favourite City: London"))
        marker?.showInfoWindow()

        // Zoom into the desired location making the favourite city visible
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(london, 8f))
    }

    override fun onLocationChanged(p0: Location) {
        Log.d(TAG, "Location changed: ${p0.longitude}, ${p0.latitude}")
    }
}