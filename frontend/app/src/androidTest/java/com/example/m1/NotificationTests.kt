package com.example.m1

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.test.InstrumentationRegistry.getTargetContext
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ScrollToAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.example.m1.fragments.ProfileFragment
import junit.framework.TestCase.assertTrue
import org.hamcrest.Matcher
import org.hamcrest.core.AllOf.allOf
import org.hamcrest.core.AnyOf.anyOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/*
 * ENSURE YOU HAVE ANIMATIONS TURNED OFF! https://developer.android.com/training/testing/espresso/setup#set-up-environment
 */
@RunWith(AndroidJUnit4::class)
class NotificationTests {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var device: UiDevice

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
fun fullTest() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    setupSharedPreferences(context, true, null) // User signed-in, unfinished profile

    // ======================================
    // User with unfinished profile
    // ======================================
    testUnfinishedProfile(context)

    // ======================================
    // User that has completed their profile, but rejects location permissions
    // ======================================
    setupSharedPreferences(context, true, "testName") // Complete profile
    testRejectLocationPermissions(context)

    // ======================================
    // User that has completed their profile, but rejects notification permissions
    // ======================================
    testRejectNotificationPermissions(context)

    // ======================================
    // User that has completed their profile, has location permissions accepted, and accepts notification permission
    // ======================================
    testAcceptAllPermissions(context)
}

// ======================================
// Private Subfunctions
// ======================================

private fun setupSharedPreferences(context: Context, isSignedIn: Boolean, fullName: String?) {
    val sharedPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean("isSignedIn", isSignedIn).commit()

    val nameSharedPrefs = context.getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
    nameSharedPrefs.edit().putString(ProfileFragment.KEY_FULL_NAME, fullName).commit()
}

private fun testUnfinishedProfile(context: Context) {
    onView(withId(R.id.nav_profile)).perform(click()) // Navigate to profile page
    scrollToNotiButton()

    onView(withId(R.id.notification_button))
        .check(matches(isDisplayed())) // Verify the button is displayed
    onView(withId(R.id.notification_button)).perform(click())

    Thread.sleep(1000)
    onView(withText("Please complete profile creation first"))
        .check(matches(isCompletelyDisplayed()))

    Thread.sleep(1000)
    assertNoDialogsPresent()
    assertPermissionsDenied(context)
}

private fun testRejectLocationPermissions(context: Context) {
    onView(withId(R.id.notification_button))
        .check(matches(isDisplayed())) // Verify the button is displayed
    onView(withId(R.id.notification_button)).perform(click())

    Thread.sleep(2000)
    handleRationaleDialog()

    val denyLocationButton = device.findObject(UiSelector().textContains("Don")) // Reject location
    if (denyLocationButton.exists()) {
        denyLocationButton.click()
    } else {
        throw AssertionError("The 'Don't Allow' location button was not found.")
    }

    Thread.sleep(1000)
    onView(withText("Location services are required for notifications"))
        .check(matches(isCompletelyDisplayed()))

    Thread.sleep(1000)
    assertNoNotificationPrompt()
    assertPermissionsDenied(context)
}

private fun testRejectNotificationPermissions(context: Context) {
    onView(withId(R.id.notification_button))
        .check(matches(isDisplayed())) // Verify the button is displayed
    onView(withId(R.id.notification_button)).perform(click())

    Thread.sleep(2000)
    handleRationaleDialog()

    val allowLocationButton = device.findObject(UiSelector().textContains("While")) // Accept location
    if (allowLocationButton.exists()) {
        allowLocationButton.click()
    } else {
        throw AssertionError("The location permission dialog was not found")
    }

    Thread.sleep(2000)
    val denyNotiButton = device.findObject(UiSelector().textContains("Don"))
    if (denyNotiButton.exists()) {
        denyNotiButton.click()
    } else {
        throw AssertionError("Notification dialog not found")
    }

    Thread.sleep(1000)
    onView(withText("Please enable notifications for up to date weather info"))
        .check(matches(isCompletelyDisplayed()))

    assertTrue("Location permissions should be granted", checkLocationPermissions(true))
    assertTrue("Notification permissions should be denied", checkNotificationPermissions(false))
}

private fun testAcceptAllPermissions(context: Context) {
    onView(withId(R.id.notification_button))
        .check(matches(isDisplayed())) // Verify the button is displayed
    onView(withId(R.id.notification_button)).perform(click())

    Thread.sleep(2000)
    handleRationaleDialog()

    val allowNotiButton = device.findObject(UiSelector().text("Allow"))
    if (allowNotiButton.exists()) {
        allowNotiButton.click()
    } else {
        throw AssertionError("The 'Allow' notification button was not found.")
    }

    onView(withText("Notifications enabled"))
        .check(matches(isCompletelyDisplayed()))

    assertTrue("Location permissions should be granted", checkLocationPermissions(true))
    assertTrue("Notification permissions should be granted", checkNotificationPermissions(true))
}

private fun handleRationaleDialog() {
    val shouldShowButton = device.findObject(UiSelector().textContains("Yes")) // Accept rationale dialog if present
    if (shouldShowButton.exists()) {
        shouldShowButton.click()
        Thread.sleep(2000)
    }
}

private fun assertNoDialogsPresent() {
    val shouldShowButton = device.findObject(UiSelector().textContains("Yes"))
    if (shouldShowButton.exists()) {
        throw AssertionError("No dialog button should be present.")
    }

    val allowLocationButton = device.findObject(UiSelector().text("While using the app"))
    if (allowLocationButton.exists()) {
        throw AssertionError("No dialog button should be present.")
    }
}

private fun assertNoNotificationPrompt() {
    val allowNotiButton = device.findObject(UiSelector().text("Allow"))
    if (allowNotiButton.exists()) {
        throw AssertionError("Notification prompt showing when location permissions rejected")
    } else {
        onView(withId(R.id.notification_button)).check(matches(isDisplayed()))
    }
}

private fun assertPermissionsDenied(context: Context) {
    assertTrue("Location permissions should be denied", checkLocationPermissions(false))
    assertTrue("Notification permissions should be denied", checkNotificationPermissions(false))
}


    private fun scrollToNotiButton() {
        try {
            val appView = UiScrollable(UiSelector().scrollable(true))
            appView.setAsVerticalList()
            appView.scrollToEnd(5)
        } catch (e: Exception) {
            try {
                onView(withId(R.id.notification_button)).perform(ViewActions.scrollTo())
            } catch (e2: Exception) {
                // Continue if both scroll attempts fail
            }
        }
    }

    private fun checkNotificationPermissions(notification: Boolean) : Boolean {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        val notificationPermissionStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        )

        val NSP = sharedPreferences.getBoolean("notificationsEnabled", false)

        Log.d("TESTS", "$NSP, $notification, $notificationPermissionStatus")

        return if (notification) {
            notificationPermissionStatus == PackageManager.PERMISSION_GRANTED && NSP == notification
        } else {
            notificationPermissionStatus != PackageManager.PERMISSION_GRANTED && NSP == notification
        }

    }

    private fun checkLocationPermissions(location: Boolean) : Boolean {
        val context = InstrumentationRegistry.getInstrumentation().targetContext // User signed-in

        val locationPermissionStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        return if (location) {
            locationPermissionStatus == PackageManager.PERMISSION_GRANTED
        } else {
            locationPermissionStatus != PackageManager.PERMISSION_GRANTED
        }
    }

}