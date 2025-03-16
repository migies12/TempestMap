package com.example.m1

import android.content.Context
import android.os.IBinder
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.TYPE_TOAST
import androidx.test.core.app.ActivityScenario.ActivityAction
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Root
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/*
 * ENSURE YOU HAVE ANIMATIONS TURNED OFF! https://developer.android.com/training/testing/espresso/setup#set-up-environment
 */
@RunWith(AndroidJUnit4::class)
class CustomMarkerTests {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @Test
    fun testSuccessfulMarker() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext // User signed-in
        val sharedPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putBoolean("isSignedIn", true)
            .commit()

        onView(withId(R.id.nav_map)).perform(click())

        onView(withId(R.id.fabAddMarker)).perform(click())

        onView(withText("Map Options")).check(matches(isDisplayed()))

        onView(withText("Create Marker")).perform(click())

        onView(isRoot()).perform(object : ViewAction {
            override fun getConstraints() = isDisplayed()

            override fun getDescription() = "Click at specific position"

            override fun perform(uiController: UiController, view: View) {
                val screenPos = IntArray(2)
                view.getLocationOnScreen(screenPos)
                val x = screenPos[0] + 250 // Adjust X position
                val y = screenPos[1] + 100// Adjust Y position
                uiController.injectMotionEventSequence(
                    listOf(
                        MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x.toFloat(), y.toFloat(), 0),
                        MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, x.toFloat(), y.toFloat(), 0))
                )
            }
        })

        onView(withText("Add a Custom Marker"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))

        onView(withId(R.id.etDescription))
            .inRoot(isDialog())
            .perform(click(), replaceText("This is a test"), closeSoftKeyboard())

        onView(isRoot()).perform(object : ViewAction {
            override fun getConstraints() = isDisplayed()

            override fun getDescription() = "Click at specific position"

            override fun perform(uiController: UiController, view: View) {
                val screenPos = IntArray(2)
                view.getLocationOnScreen(screenPos)
                val x = screenPos[0] + 250 // Adjust X position
                val y = screenPos[1] + 100// Adjust Y position
                uiController.injectMotionEventSequence(
                    listOf(
                        MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x.toFloat(), y.toFloat(), 0),
                        MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, x.toFloat(), y.toFloat(), 0))
                )
            }
        })

        onView(withText("This is a test")).check(matches(isDisplayed()))
    }

    @Test
    fun userNotSignedIn() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext // User signed-in
        val sharedPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putBoolean("isSignedIn", false)
            .commit()

        onView(withId(R.id.nav_map)).perform(click())

        onView(withId(R.id.fabAddMarker)).perform(click())

        onView(withId(R.id.signInButton)).check(matches(isDisplayed())) // Verify we are redirected to sign-in page
    }

}

/*
class ToastMatcher : TypeSafeMatcher<Root?>() {
    override fun describeTo(description: Description?) {
        description?.appendText("is toast")
    }

    override fun matchesSafely(item: Root?): Boolean {
        val type: Int? = item?.windowLayoutParams?.get()?.type
        if (type == WindowManager.LayoutParams.TYPE_TOAST) {
            val windowToken: IBinder = item.getDecorView().getWindowToken()
            val appToken: IBinder = item.getDecorView().getApplicationWindowToken()
            if (windowToken === appToken) {
                return true
            }
        }
        return false
    }

}
*/