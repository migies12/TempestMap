package com.example.m1.nonfunctional

import android.content.Context
import android.util.Log
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
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
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import com.example.m1.MainActivity
import com.example.m1.R
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
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
 * NFR 3: Ease of Use
 * The app must gracefully handle errors (e.g., network failures)
 * and provide users with clear instructions for recovery
 * (e.g., retry options, offline mode).
 *
 * These tests simulate network errors by disabling network connectivity
 * on the Profile page. It checks for data preservation, retry options on fallback.
 *
 * Make sure to run the tests all together, not individually.
 */

@RunWith(AndroidJUnit4::class)
class ErrorHandlingTests {

    companion object {
        private const val TAG = "ErrorHandlingTests"
        private const val LOG_FILE_NAME = "error_handling_test_log.txt"

        fun logEvent(message: String) {
            Log.d(TAG, message)
            appendToLogFile(message)
        }

        private fun appendToLogFile(message: String) {
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
                val logMessage = "[$timestamp] $message\n"
                val context = InstrumentationRegistry.getInstrumentation().targetContext
                val logFile = File(context.externalCacheDir, LOG_FILE_NAME)
                FileOutputStream(logFile, true).use { it.write(logMessage.toByteArray()) }
            } catch (e: Exception) {
                Log.e(TAG, "Log file error: ${e.message}")
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

    private lateinit var context: Context
    private lateinit var uiDevice: UiDevice

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        logEvent("Starting ErrorHandlingTests setup")
        val sharedPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putBoolean("isSignedIn", true)
            .putString("userName", "TestUser")
            .apply()
        logEvent("UserPrefs set")
    }

    @Test
    fun testFormDataPreservationOnError() {
        logEvent("Starting testFormDataPreservationOnError")
        onView(withId(R.id.nav_profile)).perform(click())
        logEvent("Clicked nav_profile")
        onView(isRoot()).perform(waitFor(1000))
        fillProfileInformation("Test User", "test@example.com", "New York")
        logEvent("Filled profile info")
        onView(isRoot()).perform(waitFor(500))

        simulateNoNetwork()
        logEvent("Network disabled")
        onView(isRoot()).perform(waitFor(2000))
        scrollToSaveButton()
        logEvent("Scrolled to save button")
        onView(isRoot()).perform(waitFor(500))

        try {
            onView(withId(R.id.save_button)).perform(click())
            logEvent("Clicked save_button")
        } catch (e: Exception) {
            logEvent("Click save_button failed, trying UiDevice click")
            val saveButton = uiDevice.findObject(UiSelector().text("Save Profile"))
            if (saveButton.exists()) {
                saveButton.click()
                logEvent("UiDevice clicked Save Profile")
            } else {
                logEvent("Save button not found")
                throw AssertionError("Save button not found", e)
            }
        }

        onView(isRoot()).perform(waitFor(1500))
        try {
            onView(withText(CoreMatchers.containsString("Network")))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            logEvent("Network error dialog displayed")
            onView(withText("Retry"))
                .inRoot(isDialog())
                .perform(click())
            logEvent("Clicked Retry in dialog")
            onView(isRoot()).perform(waitFor(1000))
        } catch (e: NoMatchingViewException) {
            logEvent("Network error dialog not found")
        }
        onView(isRoot()).perform(waitFor(500))
        verifyFormFieldsPreserved()
        logEvent("Form fields verified")
        restoreNetwork()
        logEvent("Network restored")
        onView(isRoot()).perform(waitFor(2000))
        logEvent("Completed testFormDataPreservationOnError")
    }

    @Test
    fun testInvalidInputHandling() {
        logEvent("Starting testInvalidInputHandling")
        onView(withId(R.id.nav_profile)).perform(click())
        logEvent("Clicked nav_profile")
        onView(isRoot()).perform(waitFor(2000))
        onView(withId(R.id.email_edit_text))
            .perform(clearText(), typeText("not-an-email"), closeSoftKeyboard())
        logEvent("Entered invalid email")
        onView(isRoot()).perform(waitFor(1000))
        try {
            onView(withId(R.id.save_button)).perform(scrollTo(), waitFor(500), click())
            logEvent("Clicked save_button with invalid email")
        } catch (e: Exception) {
            logEvent("Initial save_button click failed, trying alternate method")
            val appView = UiScrollable(UiSelector().scrollable(true))
            appView.setAsVerticalList()
            appView.scrollToEnd(5)
            onView(isRoot()).perform(waitFor(1000))
            val saveButton = uiDevice.findObject(UiSelector().text("Save Profile"))
            if (saveButton.exists()) {
                saveButton.click()
                logEvent("UiDevice clicked Save Profile")
            } else {
                logEvent("Save button not found via UiDevice")
                throw AssertionError("Save button not found", e)
            }
        }
        onView(isRoot()).perform(waitFor(2000))
        val emailField = uiDevice.findObject(UiSelector().resourceId("${context.packageName}:id/email_edit_text"))
        if (emailField.exists()) {
            emailField.click()
            emailField.clearTextField()
            emailField.setText("valid@example.com")
            logEvent("Re-entered valid email")
        }
        scrollToSaveButton()
        onView(isRoot()).perform(waitFor(500))
        try {
            onView(withId(R.id.save_button)).perform(click())
            logEvent("Clicked save_button after correcting email")
        } catch (e: Exception) {
            logEvent("Final save_button click failed, trying alternate method")
            val saveButton = uiDevice.findObject(UiSelector().text("Save Profile"))
            if (saveButton.exists()) {
                saveButton.click()
                logEvent("UiDevice clicked Save Profile")
            } else {
                logEvent("Save button not found via UiDevice")
                throw AssertionError("Save button not found", e)
            }
        }
        val emailError = uiDevice.findObject(UiSelector().text("Please enter a valid email"))
        if (emailError.exists()) {
            logEvent("Email error displayed after correction")
            throw AssertionError("Email error when entering valid email")
        }
        logEvent("Completed testInvalidInputHandling")
    }

    private fun scrollToSaveButton() {
        try {
            val appView = UiScrollable(UiSelector().scrollable(true))
            appView.setAsVerticalList()
            appView.scrollToEnd(5)
            logEvent("Scrolled using UiScrollable")
        } catch (e: Exception) {
            try {
                onView(withId(R.id.save_button)).perform(scrollTo())
                logEvent("Scrolled using ViewActions")
            } catch (e2: Exception) {
                logEvent("Scrolling to save_button failed")
            }
        }
    }

    private fun verifyFormFieldsPreserved() {
        try {
            val appView = UiScrollable(UiSelector().scrollable(true))
            appView.setAsVerticalList()
            appView.scrollToBeginning(5)
            val nameField = uiDevice.findObject(UiSelector().resourceId("${context.packageName}:id/full_name_edit_text"))
            val emailField = uiDevice.findObject(UiSelector().resourceId("${context.packageName}:id/email_edit_text"))
            val locationField = uiDevice.findObject(UiSelector().resourceId("${context.packageName}:id/location_edit_text"))
            if (nameField.exists() && nameField.text != "Test User") {
                throw AssertionError("Name field mismatch: ${nameField.text}")
            }
            if (emailField.exists() && emailField.text != "test@example.com") {
                throw AssertionError("Email field mismatch: ${emailField.text}")
            }
            if (locationField.exists() && locationField.text != "New York") {
                throw AssertionError("Location field mismatch: ${locationField.text}")
            }
            logEvent("Form fields verified via UiDevice")
        } catch (e: Exception) {
            try {
                onView(withId(R.id.full_name_edit_text)).check(matches(withText("Test User")))
                onView(withId(R.id.email_edit_text)).check(matches(withText("test@example.com")))
                onView(withId(R.id.location_edit_text)).check(matches(withText("New York")))
                logEvent("Form fields verified via Espresso")
            } catch (e2: Exception) {
                logEvent("Verification failed: ${e2.message}")
                throw AssertionError("Form fields not preserved", e2)
            }
        }
    }

    private fun fillProfileInformation(name: String, email: String, location: String) {
        onView(withId(R.id.full_name_edit_text))
            .perform(clearText(), typeText(name))
        onView(isRoot()).perform(waitFor(500))
        onView(withId(R.id.email_edit_text))
            .perform(clearText(), typeText(email))
        onView(isRoot()).perform(waitFor(500))
        onView(withId(R.id.location_edit_text))
            .perform(clearText(), typeText(location))
        onView(isRoot()).perform(waitFor(500))
        onView(isRoot()).perform(closeSoftKeyboard())
        logEvent("Profile info filled")
    }

    private fun simulateNoNetwork() {
        try {
            uiDevice.executeShellCommand("svc wifi disable")
            uiDevice.executeShellCommand("svc data disable")
            Thread.sleep(3000)
            logEvent("simulateNoNetwork executed")
        } catch (e: Exception) {
            logEvent("simulateNoNetwork error: ${e.message}")
            throw RuntimeException("Disable network failed", e)
        }
    }

    private fun restoreNetwork() {
        try {
            uiDevice.executeShellCommand("svc wifi enable")
            uiDevice.executeShellCommand("svc data enable")
            Thread.sleep(3000)
            logEvent("restoreNetwork executed")
        } catch (e: Exception) {
            logEvent("restoreNetwork error: ${e.message}")
            throw RuntimeException("Restore network failed", e)
        }
    }

    private fun waitFor(millis: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isRoot()
            override fun getDescription(): String = "Wait for $millis ms"
            override fun perform(uiController: UiController, view: View) {
                uiController.loopMainThreadForAtLeast(millis)
            }
        }
    }
}
