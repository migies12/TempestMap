package com.example.m1.nonfunctional

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.example.m1.MainActivity
import com.example.m1.R
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * ENSURE YOU HAVE ANIMATIONS TURNED OFF! https://developer.android.com/training/testing/espresso/setup#set-up-environment
 */

/*
 * NFR 1: Accessibility Compliance
 * The app must comply with WCAG 2.1 Level AA accessibility standards,
 * ensuring it is usable by individuals with disabilities
 * (e.g., screen reader compatibility, color contrast, keyboard navigation).
 */

@RunWith(AndroidJUnit4::class)
@LargeTest
class AccessibilityTest {
    companion object {
        // Tag for logging
        private const val TAG = "AccessibilityTest"
        private const val LOG_FILE_NAME = "accessibility_test_log.txt"

        // Thresholds for tests
        private const val MIN_CONTRAST_RATIO_NORMAL_TEXT = 4.5
        private const val MIN_CONTRAST_RATIO_LARGE_TEXT = 3.0
        private const val MIN_TOUCH_TARGET_DP = 44 // as per WCAG guidelines

        // Matchers and helper methods

        // matcher for checking touch target size
        fun hasAdequateTouchTargetSize(): Matcher<View> {
            return object : TypeSafeMatcher<View>() {
                override fun matchesSafely(item: View): Boolean {
                    val density = item.resources.displayMetrics.density
                    val minSizePx = MIN_TOUCH_TARGET_DP * density
                    val result = item.width >= minSizePx && item.height >= minSizePx

                    logEvent("Touch target size check for ${getViewName(item)}: " +
                            "width=${item.width}px, height=${item.height}px, " +
                            "min required=${minSizePx}px - ${if(result) "PASS" else "FAIL"}")

                    return result
                }

                override fun describeTo(description: Description?) {
                    description?.appendText("has adequate touch target size (at least ${MIN_TOUCH_TARGET_DP}x${MIN_TOUCH_TARGET_DP}dp)")
                }
            }
        }

        // matcher for checking content description
        fun hasContentDescription(): Matcher<View> {
            return object : TypeSafeMatcher<View>() {
                override fun matchesSafely(item: View): Boolean {
                    val contentDescription = item.contentDescription
                    val result = contentDescription != null && contentDescription.isNotEmpty()

                    logEvent("Content description check for ${getViewName(item)}: " +
                            "${if(contentDescription != null) "\"$contentDescription\"" else "null"} - " +
                            "${if(result) "PASS" else "FAIL"}")

                    return result
                }

                override fun describeTo(description: Description?) {
                    description?.appendText("has a non-empty content description")
                }
            }
        }

        // Helper method to get view identifier for logging
        private fun getViewName(view: View): String {
            val id = view.id
            return if (id != View.NO_ID) {
                try {
                    val resources = view.resources
                    resources.getResourceEntryName(id)
                } catch (e: Exception) {
                    "View(${view.javaClass.simpleName}@${view.id})"
                }
            } else {
                "View(${view.javaClass.simpleName}@${view.hashCode()})"
            }
        }

        // Centralized logging helper
        fun logEvent(message: String) {
            Log.d(TAG, message)
            appendToLogFile(message)
        }

        // Write logs to a file for better analysis
        private fun appendToLogFile(message: String) {
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
                val logMessage = "[$timestamp] $message\n"

                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val logFile = File(context.externalCacheDir, LOG_FILE_NAME)

                FileOutputStream(logFile, true).use { it.write(logMessage.toByteArray()) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write to log file: ${e.message}")
            }
        }
    }

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @Before
    fun setUp() {
        logEvent("================================")
        logEvent("Starting Accessibility Test")
        logEvent("================================")

        // Enable accessibility checks
        AccessibilityChecks.enable()
        logEvent("Accessibility checks enabled")
    }

    @Test
    fun testContentDescriptions() {
        logEvent("Starting content descriptions test")
        ActivityScenario.launch(MainActivity::class.java)

        // Test navigation items for content descriptions
        val navItems = listOf(
            R.id.nav_home to "Home Navigation",
            R.id.nav_map to "Map Navigation",
            R.id.nav_profile to "Profile Navigation"
        )

        for ((id, description) in navItems) {
            logEvent("Checking content description for $description (ID: $id)")
            onView(withId(id)).check(matches(hasContentDescription()))
        }

        logEvent("Content descriptions test completed")
    }

    @Test
    fun testTextContrast() {
        logEvent("Starting text contrast test")
        ActivityScenario.launch(MainActivity::class.java)
        onView(isRoot()).perform(CheckTextContrastAction())
        logEvent("Text contrast test completed")
    }

    @Test
    fun testTouchTargetSize() {
        logEvent("Starting touch target size test")
        ActivityScenario.launch(MainActivity::class.java)

        logEvent("Navigating to map screen")
        onView(withId(R.id.nav_map)).perform(click())

        logEvent("Checking touch target size for add marker button")
        onView(withId(R.id.fabAddMarker)).check(matches(hasAdequateTouchTargetSize()))

        logEvent("Touch target size test completed")
    }

    private class CheckTextContrastAction : ViewAction {
        override fun getConstraints(): Matcher<View> = isRoot()

        override fun getDescription(): String = "Checking text contrast ratio for all TextViews"

        override fun perform(uiController: UiController, view: View) {
            logEvent("Scanning view hierarchy for text contrast issues")
            val failedViews = mutableListOf<String>()
            val passedViews = mutableListOf<String>()

            checkTextContrastRecursively(view, failedViews, passedViews)

            logEvent("Text contrast check completed. Passing views: ${passedViews.size}, Failing views: ${failedViews.size}")

            if (failedViews.isNotEmpty()) {
                logEvent("FAILED text contrast checks:")
                failedViews.forEach { logEvent("  - $it") }
            }

            // Assert that all views pass
            assertTrue("${failedViews.size} text views have inadequate contrast ratio", failedViews.isEmpty())
        }

        private fun checkTextContrastRecursively(view: View, failedViews: MutableList<String>, passedViews: MutableList<String>) {
            if (view is TextView) {
                val textColor = view.currentTextColor
                val backgroundColor = getBackgroundColor(view)
                val contrastRatio = calculateContrastRatio(textColor, backgroundColor)
                val viewName = getViewName(view)
                val textSize = view.textSize / view.resources.displayMetrics.density // convert to dp

                // WCAG 2.1 AA requires a contrast ratio of at least 4.5:1 for normal text
                // and 3:1 for large text (18sp or 14pt and bold).
                val isBold = view.typeface?.isBold ?: false
                val isLargeText = textSize >= 18 || (textSize >= 14 && isBold)
                val requiredRatio = if (isLargeText) MIN_CONTRAST_RATIO_LARGE_TEXT else MIN_CONTRAST_RATIO_NORMAL_TEXT
                val passes = contrastRatio >= requiredRatio

                // Log the result
                val textColorHex = String.format("#%08X", textColor)
                val bgColorHex = String.format("#%08X", backgroundColor)

                val logMessage = "TextView contrast check: $viewName - text color: $textColorHex, " +
                        "bg color: $bgColorHex, size: ${textSize}dp${if(isBold) " (bold)" else ""}, " +
                        "ratio: ${"%.2f".format(contrastRatio)}, required: $requiredRatio - ${if(passes) "PASS" else "FAIL"}"

                logEvent(logMessage)

                if (passes) {
                    passedViews.add(logMessage)
                } else {
                    failedViews.add(logMessage)
                }
            }

            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    checkTextContrastRecursively(view.getChildAt(i), failedViews, passedViews)
                }
            }
        }

        // helper method to get the background color
        private fun getBackgroundColor(view: View): Int {
            val bg = view.background
            var currentView = view

            // If this view doesn't have a background, traverse up the parent hierarchy
            if (bg == null || bg !is ColorDrawable) {
                var parentView: View? = view.parent as? View
                while (parentView != null) {
                    val parentBg = parentView.background
                    if (parentBg is ColorDrawable) {
                        return parentBg.color
                    }
                    parentView = parentView.parent as? View
                }
                // If no parent has a color, assume white background
                return Color.WHITE
            }

            return (bg as ColorDrawable).color
        }

        // calculates contrast ratio per WCAG formula
        private fun calculateContrastRatio(foreground: Int, background: Int): Double {
            val luminance1 = calculateRelativeLuminance(foreground)
            val luminance2 = calculateRelativeLuminance(background)
            val lighter = maxOf(luminance1, luminance2)
            val darker = minOf(luminance1, luminance2)
            return (lighter + 0.05) / (darker + 0.05)
        }

        // calculates relative luminance per WCAG formula
        private fun calculateRelativeLuminance(color: Int): Double {
            val red = (color shr 16) and 0xff
            val green = (color shr 8) and 0xff
            val blue = color and 0xff

            var r = red / 255.0
            var g = green / 255.0
            var b = blue / 255.0

            r = if (r <= 0.03928) r / 12.92 else Math.pow((r + 0.055) / 1.055, 2.4)
            g = if (g <= 0.03928) g / 12.92 else Math.pow((g + 0.055) / 1.055, 2.4)
            b = if (b <= 0.03928) b / 12.92 else Math.pow((b + 0.055) / 1.055, 2.4)

            return 0.2126 * r + 0.7152 * g + 0.0722 * b
        }
    }
}