package com.example.m1.ui.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.m1.R
import com.example.m1.data.models.Event
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Adapter for displaying alerts/events in a RecyclerView
 */
class AlertsAdapter(
    private val context: Context,
    private var events: List<Event>,
    private val onItemClick: (Event) -> Unit
) : RecyclerView.Adapter<AlertsAdapter.AlertViewHolder>() {

    inner class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eventTypeIndicator: View = itemView.findViewById(R.id.eventTypeIndicator)
        val tvAlertTitle: TextView = itemView.findViewById(R.id.tvAlertTitle)
        val tvAlertDescription: TextView = itemView.findViewById(R.id.tvAlertDescription)
        val tvAlertDangerLevel: TextView = itemView.findViewById(R.id.tvAlertDangerLevel)
        val tvAlertDate: TextView = itemView.findViewById(R.id.tvAlertDate)
        val tvAlertDistance: TextView = itemView.findViewById(R.id.tvAlertDistance)

//        init {
//            itemView.setOnClickListener {
//                val position = adapterPosition
//                if (position != RecyclerView.NO_POSITION) {
//                    onItemClick(events[position])
//                }
//            }
//        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alert, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val event = events[position]

        // Set event title based on type
        when (event.event_type) {
            "WF" -> {
                holder.tvAlertTitle.text = "Wildfire Alert"
                holder.eventTypeIndicator.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.wildfire_color)
                )
            }
            "EQ" -> {
                holder.tvAlertTitle.text = "Earthquake Alert"
                holder.eventTypeIndicator.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.earthquake_color)
                )
            }
            "FL" -> {
                holder.tvAlertTitle.text = "Flood Alert"
                holder.eventTypeIndicator.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.flood_color)
                )
            }
            "TS" -> {
                holder.tvAlertTitle.text = "Tropical Storm Alert"
                holder.eventTypeIndicator.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.storm_color)
                )
            }
            "HU" -> {
                holder.tvAlertTitle.text = "Hurricane Alert"
                holder.eventTypeIndicator.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.hurricane_color)
                )
            }
            else -> {
                holder.tvAlertTitle.text = "${event.event_type} Alert"
                holder.eventTypeIndicator.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.default_event_color)
                )
            }
        }

        // Set description
        val description = event.event_name.ifEmpty {
            when (event.event_type) {
                "WF" -> "Wildfire detected"
                "EQ" -> "Earthquake detected"
                "FL" -> "Flooding detected"
                "TS" -> "Tropical Storm detected"
                "HU" -> "Hurricane detected"
                else -> "${event.event_type} detected"
            }
        }
        holder.tvAlertDescription.text = description

        // Set danger level and color
        val dangerLevel = event.danger_level
        holder.tvAlertDangerLevel.text = "$dangerLevel/100"

        when {
            dangerLevel >= 75 -> holder.tvAlertDangerLevel.setTextColor(
                ContextCompat.getColor(context, R.color.danger_high)
            )
            dangerLevel >= 50 -> holder.tvAlertDangerLevel.setTextColor(
                ContextCompat.getColor(context, R.color.danger_medium)
            )
            dangerLevel >= 25 -> holder.tvAlertDangerLevel.setTextColor(
                ContextCompat.getColor(context, R.color.danger_low)
            )
            else -> holder.tvAlertDangerLevel.setTextColor(
                ContextCompat.getColor(context, R.color.danger_minimal)
            )
        }

        // Format and set date
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            val date = inputFormat.parse(event.date)
            date?.let {
                holder.tvAlertDate.text = outputFormat.format(it)
            } ?: run {
                holder.tvAlertDate.text = event.date
            }
        } catch (e: Exception) {
            holder.tvAlertDate.text = event.date
        }

        // Set estimated distance
        // This would ideally be calculated based on user's location
        // For now, we'll use a placeholder based on danger level
        val estimatedDistance = when {
            dangerLevel >= 75 -> (5..20).random()
            dangerLevel >= 50 -> (20..50).random()
            dangerLevel >= 25 -> (50..100).random()
            else -> (100..300).random()
        }
        holder.tvAlertDistance.text = "~${estimatedDistance}km away"
    }

    override fun getItemCount() = events.size

    /**
     * Update the list of events/alerts
     */
    fun updateEvents(newEvents: List<Event>) {
        this.events = newEvents
        notifyDataSetChanged()
    }
}