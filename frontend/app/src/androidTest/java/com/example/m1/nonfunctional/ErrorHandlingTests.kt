package com.example.m1.nonfunctional

import android.content.Context
import android.os.Build
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.typeText
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
import org.hamcrest.TypeSafeMatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.util.Log
import java.io.IOException

/*
 * ENSURE YOU HAVE ANIMATIONS TURNED OFF! https://developer.android.com/training/testing/espresso/setup#set-up-environment
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
        context = InstrumentationRegistry.getInstrumentation().targetContext
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        val sharedPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putBoolean("isSignedIn", true)
            .putString("userName", "TestUser")
            .apply()
    }

    @Test
    fun testFormDataPreservationOnError() {
        onView(withId(R.id.nav_profile)).perform(click())
        onView(isRoot()).perform(waitFor(1000))
        fillProfileInformation("Test User", "test@example.com", "New York")
        onView(isRoot()).perform(waitFor(500))

        simulateNoNetwork()
        onView(isRoot()).perform(waitFor(2000))

        scrollToSaveButton()
        onView(isRoot()).perform(waitFor(500))

        try {
            onView(withId(R.id.save_button)).perform(click())
        } catch (e: Exception) {
            val saveButton = uiDevice.findObject(UiSelector().text("Save Profile"))
            if (saveButton.exists()) {
                saveButton.click()
            } else {
                throw AssertionError("Could not find or click Save button", e)
            }
        }

        onView(isRoot()).perform(waitFor(1500))

        try {
            onView(withText(CoreMatchers.containsString("Network")))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))

            onView(withText("Retry"))
                .inRoot(isDialog())
                .perform(click())
            onView(isRoot()).perform(waitFor(1000))
        } catch (e: NoMatchingViewException) {
            // If dialog not found, continue
        }

        onView(isRoot()).perform(waitFor(500))
        verifyFormFieldsPreserved()
        restoreNetwork()
        onView(isRoot()).perform(waitFor(2000))
    }

    private fun scrollToSaveButton() {
        try {
            val appView = UiScrollable(UiSelector().scrollable(true))
            appView.setAsVerticalList()
            appView.scrollToEnd(5)
        } catch (e: Exception) {
            try {
                onView(withId(R.id.save_button)).perform(ViewActions.scrollTo())
            } catch (e2: Exception) {
                // Continue if both scroll attempts fail
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
                throw AssertionError("Name field does not contain expected text. Found: ${nameField.text}")
            }

            if (emailField.exists() && emailField.text != "test@example.com") {
                throw AssertionError("Email field does not contain expected text. Found: ${emailField.text}")
            }

            if (locationField.exists() && locationField.text != "New York") {
                throw AssertionError("Location field does not contain expected text. Found: ${locationField.text}")
            }
        } catch (e: Exception) {
            try {
                onView(withId(R.id.full_name_edit_text)).check(matches(withText("Test User")))
                onView(withId(R.id.email_edit_text)).check(matches(withText("test@example.com")))
                onView(withId(R.id.location_edit_text)).check(matches(withText("New York")))
            } catch (e2: Exception) {
                throw AssertionError("Could not verify form fields are preserved", e2)
            }
        }
    }

    @Test
    fun testInvalidInputHandling() {
        onView(withId(R.id.nav_profile)).perform(click())
        onView(isRoot()).perform(waitFor(2000))

        onView(withId(R.id.email_edit_text))
            .perform(clearText())
            .perform(typeText("not-an-email"))
            .perform(closeSoftKeyboard())

        onView(isRoot()).perform(waitFor(1000))

        try {
            onView(withId(R.id.save_button)).perform(ViewActions.scrollTo(), waitFor(500), click())
        } catch (e: Exception) {
            val appView = UiScrollable(UiSelector().scrollable(true))
            appView.setAsVerticalList()
            appView.scrollToEnd(5)
            onView(isRoot()).perform(waitFor(1000))

            val saveButton = uiDevice.findObject(UiSelector().text("Save Profile"))
            if (saveButton.exists()) {
                saveButton.click()
            } else {
                throw AssertionError("Could not find Save button")
            }
        }

        onView(isRoot()).perform(waitFor(2000))

        val emailField = uiDevice.findObject(UiSelector().resourceId("${context.packageName}:id/email_edit_text"))
        if (emailField.exists()) {
            emailField.click()
            emailField.clearTextField()
            emailField.setText("valid@example.com")
        }

        scrollToSaveButton()
        onView(isRoot()).perform(waitFor(500))

        try {
            onView(withId(R.id.save_button)).perform(click())
        } catch (e: Exception) {
            val saveButton = uiDevice.findObject(UiSelector().text("Save Profile"))
            if (saveButton.exists()) {
                saveButton.click()
            } else {
                throw AssertionError("Could not find or click Save button", e)
            }
        }

        val emailError = uiDevice.findObject(UiSelector().text("Please enter a valid email"))
        if(emailError.exists()) {
            throw AssertionError("Email error when entering valid email")
        }
    }

    private fun fillProfileInformation(name: String, email: String, location: String) {
        onView(withId(R.id.full_name_edit_text))
            .perform(clearText())
            .perform(typeText(name))
        onView(isRoot()).perform(waitFor(500))

        onView(withId(R.id.email_edit_text))
            .perform(clearText())
            .perform(typeText(email))
        onView(isRoot()).perform(waitFor(500))

        onView(withId(R.id.location_edit_text))
            .perform(clearText())
            .perform(typeText(location))
        onView(isRoot()).perform(waitFor(500))

        onView(isRoot()).perform(closeSoftKeyboard())
    }

    private fun simulateNoNetwork() {
        try {
            uiDevice.executeShellCommand("svc wifi disable")
            uiDevice.executeShellCommand("svc data disable")
            Thread.sleep(3000)
        } catch (e: IOException) {
            // Handle IO-related exceptions (e.g., issues with executing shell commands)
            Log.e("ErrorHandlingTests", "IO error while disabling network: ${e.message}", e)
            throw IOException("Failed to disable network connectivity due to IO error", e)
        } catch (e: SecurityException) {
            // Handle cases where the app doesn't have permission to execute shell commands
            Log.e("ErrorHandlingTests", "Security exception while disabling network: ${e.message}", e)
            throw SecurityException("Failed to disable network connectivity due to security restrictions", e)
        } 
    }

    private fun restoreNetwork() {
        try {
            uiDevice.executeShellCommand("svc wifi enable")
            uiDevice.executeShellCommand("svc data enable")
            Thread.sleep(3000)
        } catch (e: IOException) {
            // Handle IO-related exceptions (e.g., issues with executing shell commands)
            Log.e("ErrorHandlingTests", "IO error while disabling network: ${e.message}", e)
            throw IOException("Failed to disable network connectivity due to IO error", e)
        } catch (e: SecurityException) {
            // Handle cases where the app doesn't have permission to execute shell commands
            Log.e("ErrorHandlingTests", "Security exception while disabling network: ${e.message}", e)
            throw SecurityException("Failed to disable network connectivity due to security restrictions", e)
        } 
    }

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
}