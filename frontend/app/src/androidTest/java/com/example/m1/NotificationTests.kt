package com.example.m1

import android.content.Context
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.mapbox.maps.extension.style.expressions.dsl.generated.match
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
    fun testSuccessfulNotificationProcess() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext // User signed-in
        val sharedPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putBoolean("isSignedIn", true)
            .commit()

        onView(withId(R.id.nav_profile)).perform(click()) // Navigate to profile page

        onView(withId(R.id.notification_button))
            .perform(scrollTo()) // Perform scroll action
            .check(matches(isDisplayed()))

        onView(withId(R.id.notification_button))
            .check(matches(isDisplayed()))  // Check visibility

        onView(withId(R.id.notification_button)).perform(click())

        val allowFineLocationButton = device.findObject(UiSelector().text("Allow"))
       // val denyButton = device.findObject(UiSelector().text("Deny"))
        allowFineLocationButton.click()

        val allowNotiButton = device.findObject(UiSelector().text("Allow"))
        allowNotiButton.click()
    }
}