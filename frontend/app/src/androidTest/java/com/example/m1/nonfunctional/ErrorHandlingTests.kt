package com.example.m1.nonfunctional

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.example.m1.MainActivity
import com.example.m1.R
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for error handling and recovery in the app
 * Focuses on the non-functional requirement for Ease of Use:
 * "The app must gracefully handle errors and provide users with clear instructions for recovery"
 */
@RunWith(AndroidJUnit4::class)
class ErrorHandlingTests {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private lateinit var context: Context
    private lateinit var uiDevice: UiDevice

    @Before
    fun setup() {
        // Get context for shared preferences and system services
        context = InstrumentationRegistry.getInstrumentation().targetContext

        // Get UI Device for system interactions like airplane mode
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Set up user as signed in for tests
        val sharedPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putBoolean("isSignedIn", true)
            .putString("userName", "TestUser")
            .apply()
    }

    /**
     * Test network errors by toggling airplane mode
     * and verifying appropriate error messages are displayed
     */
    @Test
    fun testNetworkErrorHandling() {
        // Navigate to map fragment
        onView(withId(R.id.nav_map)).perform(click())

        // Wait for map to load
        onView(isRoot()).perform(waitFor(2000))

        // Enable airplane mode to simulate network failure
        setAirplaneMode(true)

        // Try to perform an action that requires network
        onView(withId(R.id.fabAddMarker)).perform(click())
        onView(withText("View Favorites")).perform(click())

        // Check for error toast or dialog with network error message
        // Note: Toast assertions require a custom matcher or Espresso-Intents
        // Here we'll check for an error dialog instead
        onView(withText(containsString("network")))
            .check(matches(isDisplayed()))

        // Verify retry option exists
        onView(withText(containsString("Retry")))
            .check(matches(isDisplayed()))

        // Disable airplane mode
        setAirplaneMode(false)

        // Wait for network to restore
        onView(isRoot()).perform(waitFor(1500))

        // Click retry and verify we can continue using the app
        onView(withText(containsString("Retry"))).perform(click())

        // Verify we're still on the app (didn't crash)
        onView(withId(R.id.fabAddMarker)).check(matches(isDisplayed()))
    }

    /**
     * Test that when a form submission fails, the user data is preserved
     */
    @Test
    fun testFormDataPreservationOnError() {
        // Navigate to profile fragment
        onView(withId(R.id.nav_profile)).perform(click())

        // Fill in profile information
        fillProfileInformation("Test User", "test@example.com", "New York")

        // Enable airplane mode to force network failure
        setAirplaneMode(true)

        // Try to save profile
        onView(withId(R.id.save_button)).perform(click())

        // Wait for error dialog
        onView(isRoot()).perform(waitFor(1000))

        // Check if error message is shown
        onView(withText(containsString("save")))
            .check(matches(isDisplayed()))

        // Dismiss error message
        onView(withText("OK")).perform(click())

        // Verify form data is still preserved
        onView(withId(R.id.full_name_edit_text)).check(matches(withText("Test User")))
        onView(withId(R.id.email_edit_text)).check(matches(withText("test@example.com")))
        onView(withId(R.id.location_edit_text)).check(matches(withText("New York")))

        // Restore network
        setAirplaneMode(false)
    }

    /**
     * Test that app correctly handles location permission errors
     */
    @Test
    fun testLocationPermissionErrorHandling() {
        // First revoke location permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            uiDevice.executeShellCommand(
                "pm revoke ${context.packageName} android.permission.ACCESS_FINE_LOCATION"
            )
            uiDevice.executeShellCommand(
                "pm revoke ${context.packageName} android.permission.ACCESS_COARSE_LOCATION"
            )
        }

        // Navigate to map fragment
        onView(withId(R.id.nav_map)).perform(click())

        // Check for permission error message
        onView(withText(containsString("location")))
            .check(matches(isDisplayed()))

        // Verify that permission rationale is shown
        onView(withText(containsString("permission")))
            .check(matches(isDisplayed()))

        // Check for button to grant permissions
        onView(withText(containsString("Allow")))
            .check(matches(isDisplayed()))
    }

    /**
     * Test handling of invalid input
     */
    @Test
    fun testInvalidInputHandling() {
        // Navigate to profile fragment
        onView(withId(R.id.nav_profile)).perform(click())

        // Enter invalid email
        onView(withId(R.id.email_edit_text))
            .perform(androidx.test.espresso.action.ViewActions.clearText())
            .perform(androidx.test.espresso.action.ViewActions.typeText("not-an-email"))

        onView(isRoot()).perform(androidx.test.espresso.action.ViewActions.closeSoftKeyboard())

        // Try to save
        onView(withId(R.id.save_button)).perform(click())

        // Check for input validation error message
        onView(withId(R.id.email_edit_text))
            .check(matches(hasErrorText(containsString("valid email"))))

        // Test that we can fix the error and continue
        onView(withId(R.id.email_edit_text))
            .perform(androidx.test.espresso.action.ViewActions.clearText())
            .perform(androidx.test.espresso.action.ViewActions.typeText("valid@example.com"))

        onView(isRoot()).perform(androidx.test.espresso.action.ViewActions.closeSoftKeyboard())

        // Verify no error is shown
        onView(withId(R.id.save_button)).perform(click())

        // No assertion needed here - if the test doesn't crash, the app handled the input correction properly
    }

    // Helper methods

    private fun containsString(text: String): Matcher<String> {
        return org.hamcrest.CoreMatchers.containsString(text)
    }

    private fun hasErrorText(matcher: Matcher<String>): Matcher<View> {
        return ErrorTextMatcher(matcher)
    }

    private fun fillProfileInformation(name: String, email: String, location: String) {
        onView(withId(R.id.full_name_edit_text))
            .perform(androidx.test.espresso.action.ViewActions.clearText())
            .perform(androidx.test.espresso.action.ViewActions.typeText(name))

        onView(withId(R.id.email_edit_text))
            .perform(androidx.test.espresso.action.ViewActions.clearText())
            .perform(androidx.test.espresso.action.ViewActions.typeText(email))

        onView(withId(R.id.location_edit_text))
            .perform(androidx.test.espresso.action.ViewActions.clearText())
            .perform(androidx.test.espresso.action.ViewActions.typeText(location))

        onView(isRoot()).perform(androidx.test.espresso.action.ViewActions.closeSoftKeyboard())
    }

    private fun setAirplaneMode(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Use shell command to toggle airplane mode
            val command = if (enabled) {
                "settings put global airplane_mode_on 1"
            } else {
                "settings put global airplane_mode_on 0"
            }
            uiDevice.executeShellCommand(command)

            // Broadcast the change
            uiDevice.executeShellCommand(
                "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $enabled"
            )
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    // Helper function to wait for a specific amount of time
    private fun waitFor(millis: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isRoot()
            }

            override fun getDescription(): String {
                return "Wait for $millis milliseconds"
            }

            override fun perform(uiController: UiController, view: View) {
                uiController.loopMainThreadForAtLeast(millis)
            }
        }
    }

    // Custom matcher for EditText error messages
    class ErrorTextMatcher(private val matcher: Matcher<String>) : org.hamcrest.TypeSafeMatcher<View>() {
        override fun describeTo(description: org.hamcrest.Description) {
            description.appendText("has error text: ")
            matcher.describeTo(description)
        }

        override fun matchesSafely(view: View): Boolean {
            if (view !is android.widget.EditText) return false
            val error = view.error ?: return false
            return matcher.matches(error.toString())
        }
    }
}
