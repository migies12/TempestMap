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
import com.example.m1.data.models.Comment
import com.example.m1.data.models.Event
import com.example.m1.ui.viewmodels.MapViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Bottom sheet dialog to display event details while keeping the map visible
 */
class EventBottomSheetDialog(
    private val context: Context,
    private val viewModel: MapViewModel,
    private val onDismiss: () -> Unit
) {
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>


    private fun updateCommentSection(dialogView: View, comments: List<Comment>) {
        val commentSection = dialogView.findViewById<LinearLayout>(R.id.commentSection)
        // Clear the existing views before updating.
        commentSection.removeAllViews()
        comments.forEach { comment ->
            addCommentBubble(commentSection, comment.user, comment.text)
        }
    }

    fun show(event: Event) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.event_bottom_sheet_dialog, null)

        // Set up the event details and initial comment section.
        setupEventDetails(dialogView, event)
        setupCommentSection(dialogView, event)

        // Observe LiveData for updated comments.
        // Here we cast context to LifecycleOwner; ensure your activity implements LifecycleOwner.
        (context as? androidx.lifecycle.LifecycleOwner)?.let { lifecycleOwner ->
            viewModel.comments.observe(lifecycleOwner) { comments ->
                updateCommentSection(dialogView, comments)
            }
        }

        // Continue with dialog setup
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


    private fun setupEventDetails(dialogView: View, event: Event) {
        val eventTitle = dialogView.findViewById<TextView>(R.id.eventTitle)
        val eventWarning = dialogView.findViewById<TextView>(R.id.eventWarning)
        val eventEndDate = dialogView.findViewById<TextView>(R.id.eventEndDate)
        val eventDangerLevel = dialogView.findViewById<TextView>(R.id.eventDangerLevel)
        val eventFooter = dialogView.findViewById<TextView>(R.id.eventFooter)
        val eventTypeIndicator = dialogView.findViewById<View>(R.id.eventTypeIndicator)

        // Set event details
        eventTitle.text = event.event_name
        eventWarning.text = "Warning: ${event.event_type} detected on ${formatDate(event.date)}"
        eventEndDate.text = "Expected end: ${formatDate(event.estimated_end_date)}"

        // Set danger level with appropriate color indicator
        val dangerLevel = event.danger_level
        eventDangerLevel.text = "Danger Level: $dangerLevel / 100 based on your proximity."

        // Set event type indicator color based on event type
        when (event.event_type) {
            "WF" -> eventTypeIndicator.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.wildfire_color)
            )
            "EQ" -> eventTypeIndicator.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.earthquake_color)
            )
            else -> eventTypeIndicator.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.default_event_color)
            )
        }

        // Set text color based on danger level
        when {
            dangerLevel >= 75 -> eventDangerLevel.setTextColor(
                ContextCompat.getColor(context, R.color.danger_high)
            )
            dangerLevel >= 50 -> eventDangerLevel.setTextColor(
                ContextCompat.getColor(context, R.color.danger_medium)
            )
            dangerLevel >= 25 -> eventDangerLevel.setTextColor(
                ContextCompat.getColor(context, R.color.danger_low)
            )
            else -> eventDangerLevel.setTextColor(
                ContextCompat.getColor(context, R.color.danger_minimal)
            )
        }

        eventFooter.text = "Refer to local authorities for more information."
    }

    private fun setupCommentSection(dialogView: View, event: Event) {
        val commentSection = dialogView.findViewById<LinearLayout>(R.id.commentSection)
        val commentInput = dialogView.findViewById<EditText>(R.id.commentInput)
        val addCommentButton = dialogView.findViewById<Button>(R.id.addCommentButton)

        // Populate existing comments
        event.comments.forEach { comment ->
            addCommentBubble(commentSection, comment.user, comment.text)
        }

        // Set up add comment button
        addCommentButton.setOnClickListener {
            handleAddComment(commentSection, commentInput, event) // Pass event here
        }
    }

    private fun handleAddComment(
        commentSection: LinearLayout,
        commentInput: EditText,
        event: Event // Add event as a parameter
    ) {
        // Check if user is signed in before allowing a comment
        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val isSignedIn = sharedPreferences.getBoolean("isSignedIn", false)

        if (!isSignedIn) {
            Toast.makeText(context, "Please sign in to comment", Toast.LENGTH_SHORT).show()
            return // Early return if the user is not signed in
        }

        val newComment = commentInput.text.toString().trim()
        if (newComment.isEmpty()) {
            Toast.makeText(context, "Please enter a comment", Toast.LENGTH_SHORT).show()
            return // Early return if the comment is empty
        }

        // Get user name from SharedPreferences
        val userName = getSignedInUserName(context)

        // Post comment using a coroutine
        CoroutineScope(Dispatchers.Main).launch {
            val success = viewModel.postComment(event.event_id, newComment, userName) // Use event here
            if (success) {
                // Add the new comment bubble to the comment section
                addCommentBubble(commentSection, userName, newComment)
                commentInput.text.clear()
                Toast.makeText(context, "Comment added", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to add comment", Toast.LENGTH_SHORT).show()
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

    private fun formatDate(dateString: String): String {
        return dateString.replace("T", " ").replace(".000Z", "")
    }


    private fun getSignedInUserName(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("userName", "Anonymous") ?: "Anonymous"
    }
}