package com.example.m1.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.m1.R
import com.example.m1.data.models.Event
import com.example.m1.ui.viewmodels.MapViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Dialog to display event details
 */
class EventDetailsDialog(
    private val context: Context,
    private val viewModel: MapViewModel,
    private val onDismiss: () -> Unit
) {

    /**
     * Show event details dialog
     * @param event The event to show details for
     */
    fun show(event: Event) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.event_popup, null)
        setupEventDetails(dialogView, event)
        createCommentSection(dialogView, event)

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        // Set dismiss listener
        dialog.setOnDismissListener {
            onDismiss()
        }

        dialog.show()
    }


    private fun setupEventDetails(dialogView: View, event: Event) {
        val eventTitle = dialogView.findViewById<TextView>(R.id.eventTitle)
        val eventWarning = dialogView.findViewById<TextView>(R.id.eventWarning)
        val eventEndDate = dialogView.findViewById<TextView>(R.id.eventEndDate)
        val eventDangerLevel = dialogView.findViewById<TextView>(R.id.eventDangerLevel)
        val eventFooter = dialogView.findViewById<TextView>(R.id.eventFooter)

        eventTitle.text = event.event_name
        eventWarning.text = "Warning: ${event.event_type} detected on ${event.date}"
        eventEndDate.text = "Expected end: ${event.estimated_end_date}"

        val dangerLevel = event.danger_level
        eventDangerLevel.text = "Danger Level: $dangerLevel / 100 based on your proximity."

        when {
            dangerLevel >= 75 -> eventDangerLevel.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.danger_high
                )
            )

            dangerLevel >= 50 -> eventDangerLevel.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.danger_medium
                )
            )

            dangerLevel >= 25 -> eventDangerLevel.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.danger_low
                )
            )

            else -> eventDangerLevel.setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.danger_minimal
                )
            )
        }

        eventFooter.text = "Refer to local authorities for more information."
    }


    private fun createCommentSection(dialogView: View, event: Event) {
        val commentSection = dialogView.findViewById<LinearLayout>(R.id.commentSection)
        val commentInput = dialogView.findViewById<EditText>(R.id.commentInput)
        val addCommentButton = dialogView.findViewById<Button>(R.id.addCommentButton)

        event.comments.forEach { comment ->
            addCommentBubbleToCommentSection(commentSection, comment.user, comment.text)
        }

        addCommentButton.setOnClickListener {
            val newComment = commentInput.text.toString().trim()
            if (newComment.isNotEmpty()) {
                val userName = getSignedInUserName(context)

                CoroutineScope(Dispatchers.Main).launch {
                    val success = viewModel.postComment(event.event_id, newComment, userName)

                    if (success) {
                        addCommentBubbleToCommentSection(commentSection, userName, newComment)
                        commentInput.text.clear()
                        Toast.makeText(context, "Comment added", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to add comment", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "Please enter a comment", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun addCommentBubbleToCommentSection(commentSection: LinearLayout, username: String, comment: String) {
        val bubbleContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 8, 0, 8)
            layoutParams = params
        }

        val usernameTextView = TextView(context).apply {
            text = username
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        bubbleContainer.addView(usernameTextView)

        val commentTextView = TextView(context).apply {
            text = comment

            val bubbleDrawable = if (username == getSignedInUserName(context)) {
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

    private fun getSignedInUserName(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("userName", "Anonymous") ?: "Anonymous"
    }
}