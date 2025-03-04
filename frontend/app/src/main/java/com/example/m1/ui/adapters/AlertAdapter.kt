package com.example.m1.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.m1.R
import com.example.m1.data.models.Event
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Adapter for displaying weather alerts in a news-feed style
 */
class AlertAdapter(
    private val context: Context,
    private val onAlertClicked: (Event) -> Unit
) : ListAdapter<Event, AlertAdapter.AlertViewHolder>(AlertDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alert, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = getItem(position)
        holder.bind(alert)
    }

    inner class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.alertCardView)
        private val warningIcon: ImageView = itemView.findViewById(R.id.warningIcon)
        private val alertTitle: TextView = itemView.findViewById(R.id.alertTitle)
        private val alertDescription: TextView = itemView.findViewById(R.id.alertDescription)
        private val locationBadge: TextView = itemView.findViewById(R.id.locationBadge)
        private val severityBadge: TextView = itemView.findViewById(R.id.severityBadge)
        private val timeAgo: TextView = itemView.findViewById(R.id.timeAgo)

        fun bind(event: Event) {
            // Set alert title based on event type
            alertTitle.text = getEventTitle(event)

            // Set description
            alertDescription.text = getEventDescription(event)

            // Set location badge
            locationBadge.text = getLocationText(event)

            // Set severity badge
            val dangerLevel = event.danger_level
            severityBadge.text = getSeverityText(dangerLevel)
            severityBadge.setBackgroundResource(getSeverityBackground(dangerLevel))
            severityBadge.setTextColor(getSeverityTextColor(dangerLevel))

            // Set time ago
            timeAgo.text = getTimeAgoString(event.created_time)

            // Set icon based on event type
            warningIcon.setImageResource(getIconForEventType(event.event_type))

            // Set card click listener
            cardView.setOnClickListener {
                onAlertClicked(event)
            }
        }

        private fun getEventTitle(event: Event): String {
            return when (event.event_type) {
                "WF" -> "Wildfire Alert"
                "EQ" -> "Earthquake Warning"
                "TS" -> "Tropical Storm System"
                "HU" -> "Hurricane Warning"
                "FL" -> "Flash Flood Warning"
                "TO" -> "Tornado Warning"
                "BZ" -> "Blizzard Warning"
                "VO" -> "Volcanic Activity"
                "LS" -> "Landslide Warning"
                else -> event.event_name
            }
        }

        private fun getEventDescription(event: Event): String {
            // Use event_name as description, or generate one based on event type
            return when {
                event.event_name.isNotBlank() && event.event_name != event.event_type ->
                    event.event_name
                else -> when (event.event_type) {
                    "WF" -> "Active wildfire detected in this area"
                    "EQ" -> "Seismic activity detected in this region"
                    "TS" -> "Tropical storm system developing"
                    "HU" -> "Hurricane conditions expected"
                    "FL" -> "Heavy rainfall causing flash flooding"
                    "TO" -> "Tornado conditions detected"
                    "BZ" -> "Severe winter weather conditions"
                    "VO" -> "Volcanic activity detected"
                    "LS" -> "Landslide risk in this area"
                    else -> "Extreme weather conditions reported"
                }
            }
        }

        private fun getLocationText(event: Event): String {
            // Use continent or country code for location display
            return when {
                event.country_code.isNotBlank() -> {
                    when (event.country_code) {
                        "US" -> "United States"
                        "CA" -> "Canada"
                        "MX" -> "Mexico"
                        else -> event.country_code
                    }
                }
                event.continent.isNotBlank() -> {
                    when (event.continent) {
                        "NAR" -> "North America"
                        "SAR" -> "South America"
                        "EUR" -> "Europe"
                        "AFR" -> "Africa"
                        "ASA" -> "Asia"
                        "OCN" -> "Oceania"
                        else -> event.continent
                    }
                }
                else -> "Global"
            }
        }

        private fun getSeverityText(dangerLevel: Int): String {
            return when {
                dangerLevel >= 75 -> "High Severity"
                dangerLevel >= 50 -> "Medium Severity"
                dangerLevel >= 25 -> "Low Severity"
                else -> "Minimal Severity"
            }
        }

        private fun getSeverityBackground(dangerLevel: Int): Int {
            return when {
                dangerLevel >= 75 -> R.drawable.bg_high_severity
                dangerLevel >= 50 -> R.drawable.bg_medium_severity
                dangerLevel >= 25 -> R.drawable.bg_low_severity
                else -> R.drawable.bg_minimal_severity
            }
        }

        private fun getSeverityTextColor(dangerLevel: Int): Int {
            return when {
                dangerLevel >= 75 -> ContextCompat.getColor(context, R.color.danger_high)
                dangerLevel >= 50 -> ContextCompat.getColor(context, R.color.danger_medium)
                dangerLevel >= 25 -> ContextCompat.getColor(context, R.color.danger_low)
                else -> ContextCompat.getColor(context, R.color.danger_minimal)
            }
        }

        private fun getTimeAgoString(dateString: String): String {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                val date = dateFormat.parse(dateString) ?: return "Recent"

                val now = Date()
                val diffInMillis = now.time - date.time

                return when {
                    diffInMillis < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                    diffInMillis < TimeUnit.HOURS.toMillis(1) -> {
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
                        "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
                    }
                    diffInMillis < TimeUnit.DAYS.toMillis(1) -> {
                        val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
                        "$hours ${if (hours == 1L) "hour" else "hours"} ago"
                    }
                    diffInMillis < TimeUnit.DAYS.toMillis(7) -> {
                        val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)
                        "$days ${if (days == 1L) "day" else "days"} ago"
                    }
                    else -> {
                        // Format as date for older events
                        SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
                    }
                }
            } catch (e: Exception) {
                return "Recent"
            }
        }

        private fun getIconForEventType(eventType: String): Int {
            return when (eventType) {
                "WF" -> R.drawable.wildfiremarker_icon
                "EQ" -> R.drawable.earthquake_icon
                "FL" -> R.drawable.warning_icon  // Using warning icon as placeholder for flood
                "TS" -> R.drawable.warning_icon  // Using warning icon as placeholder for tropical storm
                "HU" -> R.drawable.warning_icon  // Using warning icon as placeholder for hurricane
                "TO" -> R.drawable.warning_icon  // Using warning icon as placeholder for tornado
                else -> R.drawable.warning_icon
            }
        }
    }

    class AlertDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem.event_id == newItem.event_id
        }

        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem == newItem
        }
    }
}