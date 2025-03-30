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
import com.example.m1.R
import com.example.m1.data.models.UserMarker
import com.example.m1.ui.viewmodels.MapViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Dialog to display user marker details
 */
class UserMarkerDetailsDialog(
    private val context: Context,
    private val viewModel: MapViewModel
) {

    /**
     * Show user marker details dialog
     * @param userMarker The user marker to show details for
     */
    fun show(userMarker: UserMarker) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.user_marker_popup, null)

        // Setup marker details
        setupMarkerDetails(dialogView, userMarker)

        // Setup comment section
        setupCommentSection(dialogView, userMarker)

        // Create and show dialog
        AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    private fun setupMarkerDetails(dialogView: View, userMarker: UserMarker) {
        val userMarkerType = dialogView.findViewById<TextView>(R.id.userMarkerType)
        val userMarkerDescription = dialogView.findViewById<TextView>(R.id.userMarkerDescription)

        // Set marker details
        userMarkerType.text = "Type: ${userMarker.type}"
        userMarkerDescription.text = "Description: ${userMarker.description}"
    }


    private fun setupCommentSection(dialogView: View, userMarker: UserMarker) {
        val commentSection = dialogView.findViewById<LinearLayout>(R.id.commentSection)
        val commentInput = dialogView.findViewById<EditText>(R.id.commentInput)
        val addCommentButton = dialogView.findViewById<Button>(R.id.addCommentButton)

        // Populate existing comments
        userMarker.comments.forEach { comment ->
            addCommentBubble(commentSection, comment.user, comment.text)
        }

        // Set up add comment button
        addCommentButton.setOnClickListener {
            val newComment = commentInput.text.toString().trim()
            if (newComment.isNotEmpty()) {
                // Get user name from shared preferences
                val userName = getSignedInUserName(context)

                // Post comment
                CoroutineScope(Dispatchers.Main).launch {
                    val id = userMarker.id ?: ""
                    val success = viewModel.postComment(id, newComment, userName)

                    if (success) {
                        // Add comment bubble
                        addCommentBubble(commentSection, userName, newComment)
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


    private fun addCommentBubble(commentSection: LinearLayout, username: String, comment: String) {
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

            // Set the background depending on the user
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