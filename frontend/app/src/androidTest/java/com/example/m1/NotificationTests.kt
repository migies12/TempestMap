package com.example.m1

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.Espresso.onView
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
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.example.m1.fragments.ProfileFragment
import org.hamcrest.Matcher
import org.hamcrest.core.AllOf.allOf
import org.hamcrest.core.AnyOf.anyOf
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

//    @get:Rule
//    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
//        android.Manifest.permission.ACCESS_FINE_LOCATION,
//        android.Manifest.permission.ACCESS_COARSE_LOCATION
//    )

    @Test
    fun testSuccessfulNotificationProcess() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext // User signed-in
        val sharedPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putBoolean("isSignedIn", true)
            .commit()

        val sharedPreferences =
            context.getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString("fullName", "testName")
            .commit()

        onView(withId(R.id.nav_profile)).perform(click()) // Navigate to profile page

        scrollToNotiButton()

        onView(withId(R.id.notification_button))
            .check(matches(isDisplayed())) // Verify the button is displayed


        onView(withId(R.id.notification_button)).perform(click())

        Thread.sleep(2000)

//        onView(withText("Allow")).check(matches(isDisplayed()))
//            .perform()
        val allowLocationButton = device.findObject(UiSelector().text("While using the app"))
        if (allowLocationButton.exists()){
            allowLocationButton.click()
        }
        else {
            Log.d("TESTS", "allowLocationButton does not exist")
        }

        Log.d("TESTS", "After thread sleep...")
        val allowNotiButton = device.findObject(UiSelector().text("Allow"))
        if (allowNotiButton.exists()){
            allowNotiButton.click()
        }
        else {
            Log.d("TESTS", "allowNotiButton does not exist")
        }

    }

    @Test
    fun testProfileNotSaved() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext // User signed-in
        val sharedPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putBoolean("isSignedIn", true)
            .commit()

        // Omit userName addition to sharedPreferences, simulate a user that has not completed their profile.
        val nameSharedPrefs = context.getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
        nameSharedPrefs.edit()
            .putString(ProfileFragment.KEY_FULL_NAME, null)
            .commit()

        onView(withId(R.id.nav_profile)).perform(click()) // Navigate to profile page

        scrollToNotiButton()

        onView(withId(R.id.notification_button))
            .check(matches(isDisplayed())) // Verify the button is displayed

        onView(withId(R.id.notification_button)).perform(click())

        onView(withId(R.id.notification_button)) // Verify the button is displayed, which would mean the permission dialog did not appear.
            .check(matches(isDisplayed()))
    }

    class BetterScrollToAction : ViewAction by ScrollToAction() {
        override fun getConstraints(): Matcher<View> {
            return allOf(
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
                isDescendantOfA(
                    anyOf(
                        isAssignableFrom(ScrollView::class.java),
                        isAssignableFrom(HorizontalScrollView::class.java),
                        isAssignableFrom(NestedScrollView::class.java)
                    )
                )
            )
        }
    }

    // convenience method
    fun betterScrollTo(): ViewAction {
        return ViewActions.actionWithAssertions(BetterScrollToAction())
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
}