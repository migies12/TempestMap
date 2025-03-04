package com.example.m1.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.m1.R
import com.example.m1.FavoriteLocation
import com.example.m1.FavoriteLocationManager
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FavoritesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoFavorites: TextView
    private lateinit var fabViewOnMap: FloatingActionButton
    private lateinit var favoriteLocationManager: FavoriteLocationManager
    private lateinit var adapter: FavoriteLocationsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_favorites, container, false)

        // Initialize views
        recyclerView = view.findViewById(R.id.rvFavorites)
        tvNoFavorites = view.findViewById(R.id.tvNoFavorites)
        fabViewOnMap = view.findViewById(R.id.fabViewOnMap)

        // Initialize location manager
        favoriteLocationManager = FavoriteLocationManager(requireContext())

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = FavoriteLocationsAdapter(
            favoriteLocationManager.getFavoriteLocations(),
            object : FavoriteLocationClickListener {
                override fun onViewClicked(location: FavoriteLocation) {
                    viewLocationOnMap(location)
                }

                override fun onRemoveClicked(location: FavoriteLocation) {
                    showRemoveConfirmationDialog(location)
                }
            }
        )
        recyclerView.adapter = adapter

        // Update UI based on favorite locations
        updateUI()

        // FAB click listener
        fabViewOnMap.setOnClickListener {
            navigateToMapFragment()
        }

        return view
    }

    private fun updateUI() {
        val favorites = favoriteLocationManager.getFavoriteLocations()
        if (favorites.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvNoFavorites.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvNoFavorites.visibility = View.GONE
            adapter.updateData(favorites)
        }
    }

    private fun viewLocationOnMap(location: FavoriteLocation) {
        // Navigate to MapboxFragment with the selected location
        val bundle = Bundle().apply {
            putDouble("latitude", location.latitude)
            putDouble("longitude", location.longitude)
            putString("locationName", location.name)
        }

        val mapboxFragment = MapboxFragment().apply {
            arguments = bundle
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.container, mapboxFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showRemoveConfirmationDialog(location: FavoriteLocation) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Location")
            .setMessage("Are you sure you want to remove '${location.name}' from your favorites?")
            .setPositiveButton("Remove") { _, _ ->
                if (favoriteLocationManager.removeFavoriteLocation(location.id)) {
                    Toast.makeText(context, "${location.name} removed from favorites", Toast.LENGTH_SHORT).show()
                    updateUI()
                } else {
                    Toast.makeText(context, "Failed to remove location", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToMapFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, MapboxFragment())
            .commit()
    }

    override fun onResume() {
        super.onResume()
        // Update the UI when returning to this fragment
        updateUI()
    }

    // Adapter for the RecyclerView
    inner class FavoriteLocationsAdapter(
        private var locations: List<FavoriteLocation>,
        private val listener: FavoriteLocationClickListener
    ) : RecyclerView.Adapter<FavoriteLocationsAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvLocationName: TextView = itemView.findViewById(R.id.tvLocationName)
            val tvLocationDescription: TextView = itemView.findViewById(R.id.tvLocationDescription)
            val tvCoordinates: TextView = itemView.findViewById(R.id.tvCoordinates)
            val btnViewOnMap: Button = itemView.findViewById(R.id.btnViewOnMap)
            val btnRemove: Button = itemView.findViewById(R.id.btnRemove)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_favorite_location, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val location = locations[position]

            holder.tvLocationName.text = location.name
            holder.tvLocationDescription.text = if (location.description.isNotEmpty())
                location.description else "No description"
            holder.tvCoordinates.text = "Lat: ${location.latitude}, Long: ${location.longitude}"

            holder.btnViewOnMap.setOnClickListener {
                listener.onViewClicked(location)
            }

            holder.btnRemove.setOnClickListener {
                listener.onRemoveClicked(location)
            }
        }

        override fun getItemCount() = locations.size

        fun updateData(newLocations: List<FavoriteLocation>) {
            locations = newLocations
            notifyDataSetChanged()
        }
    }

    // Interface for click events
    interface FavoriteLocationClickListener {
        fun onViewClicked(location: FavoriteLocation)
        fun onRemoveClicked(location: FavoriteLocation)
    }
}