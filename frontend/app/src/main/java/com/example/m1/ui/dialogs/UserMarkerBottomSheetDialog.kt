package com.example.m1.ui.dialogs

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.m1.R
import com.example.m1.data.models.UserMarker
import com.example.m1.ui.viewmodels.MapViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Bottom sheet dialog to display user marker details
 */
class UserMarkerBottomSheetDialog(
    private val context: Context,
    private val viewModel: MapViewModel,
    private val onDismiss: () -> Unit
) {
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    /**
     * Show user marker details in a bottom sheet
     * @param userMarker The user marker to show details for
     */
    fun show(userMarker: UserMarker) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.event_bottom_sheet_dialog, null)

        // Set up content
        setupMarkerDetails(dialogView, userMarker)
        setupCommentSection(dialogView, userMarker)

        // Show detailed section immediately for expanded view
        val detailSection = dialogView.findViewById<View>(R.id.detailSection)
        detailSection.visibility = View.VISIBLE

        bottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetDialogTheme)
        bottomSheetDialog.setContentView(dialogView)

        // Remove the dark scrim behind the bottom sheet
        bottomSheetDialog.window?.setDimAmount(0f)

        // Get the behavior
        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet!!)

        // Set initial state to expanded (showing all details)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetBehavior.peekHeight = context.resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek_height)

        // Setup drag handle to indicate the sheet can be expanded
        val dragHandle = dialogView.findViewById<View>(R.id.dragHandle)
        dragHandle.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        // Set dismiss listener
        bottomSheetDialog.setOnDismissListener {
            onDismiss()
        }

        bottomSheetDialog.show()
    }


    private fun setupMarkerDetails(dialogView: View, userMarker: UserMarker) {
        val eventTitle = dialogView.findViewById<TextView>(R.id.eventTitle)
        val eventWarning = dialogView.findViewById<TextView>(R.id.eventWarning)
        val eventEndDate = dialogView.findViewById<TextView>(R.id.eventEndDate)
        val eventDangerLevel = dialogView.findViewById<TextView>(R.id.eventDangerLevel)
        val eventFooter = dialogView.findViewById<TextView>(R.id.eventFooter)
        val eventTypeIndicator = dialogView.findViewById<View>(R.id.eventTypeIndicator)

        // Set marker details
        eventTitle.text = userMarker.type
        eventWarning.text = userMarker.description

        // Hide fields that aren't applicable to user markers
        eventEndDate.visibility = View.GONE
        eventDangerLevel.visibility = View.GONE

        // Set marker-specific footer
        eventFooter.text = "Custom marker created by a user"

        // Set marker type indicator color based on marker type
        when (userMarker.type) {
            "Safehouse" -> eventTypeIndicator.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.safehouse_color)
            )
            "Resource" -> eventTypeIndicator.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.resource_color)
            )
            "Warning" -> eventTypeIndicator.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.warning_color)
            )
            else -> eventTypeIndicator.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.default_marker_color)
            )
        }
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