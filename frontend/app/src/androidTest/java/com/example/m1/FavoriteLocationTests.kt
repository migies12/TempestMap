package com.example.m1

import android.content.Context
import android.location.Location
import android.os.IBinder
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.Root
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.gson.JsonSyntaxException
import junit.framework.TestCase.assertFalse
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/*
 * ENSURE YOU HAVE ANIMATIONS TURNED OFF! https://developer.android.com/training/testing/espresso/setup#set-up-environment
 */
@RunWith(AndroidJUnit4::class)
class FavoriteLocationTests {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    @Test
    fun successfulFavoriteAdd() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext // User signed-in
        val sharedPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putBoolean("isSignedIn", true)
            .commit()

        onView(withId(R.id.nav_map)).perform(click())

        Thread.sleep(5000) // Let map load

        onView(isRoot()).perform(object : ViewAction { // Click somewhere
            override fun getConstraints() = isDisplayed()

            override fun getDescription() = "Click at specific position"

            override fun perform(uiController: UiController, view: View) {
                val screenPos = IntArray(2)
                view.getLocationOnScreen(screenPos)
                val x = screenPos[0] + 250 // Adjust X position
                val y = screenPos[1] + 100// Adjust Y position
                uiController.injectMotionEventSequence(
                    listOf(
                        MotionEvent.obtain(
                            0,
                            0,
                            MotionEvent.ACTION_DOWN,
                            x.toFloat(),
                            y.toFloat(),
                            0
                        ),
                        MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, x.toFloat(), y.toFloat(), 0)
                    )
                )
            }
        })

        onView(withText("Save Location to Favorites"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))

        onView(withId(R.id.etLocationName))
            .inRoot(isDialog())
            .perform(click(), replaceText("Test Favorite Location"), closeSoftKeyboard())

        onView(withText("Save to Favorites")).perform(click())

        Thread.sleep(1000)

        onView(withText("Location saved to Favorites"))
            .check(matches(isCompletelyDisplayed())) // ensure prompt appears

        onView(withId(R.id.fabAddMarker)).perform(click())

        onView(withText("Map Options")).check(matches(isDisplayed()))

        onView(withText("View Favorites")).perform(click())

        onView(allOf(withId(R.id.tvLocationName), withText("Test Favorite Location")))
            .check(matches(isDisplayed()))
    }

    @Test
    fun notSignedInFavoriteAdd() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext // User signed-in
        val sharedPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putBoolean("isSignedIn", false)
            .commit()

        onView(withId(R.id.nav_map)).perform(click())

        Thread.sleep(5000)

        onView(isRoot()).perform(object : ViewAction { // Click somewhere
            override fun getConstraints() = isDisplayed()

            override fun getDescription() = "Click at specific position"

            override fun perform(uiController: UiController, view: View) {
                val screenPos = IntArray(2)
                view.getLocationOnScreen(screenPos)
                val x = screenPos[0] + 250 // Adjust X position
                val y = screenPos[1] + 100// Adjust Y position
                uiController.injectMotionEventSequence(
                    listOf(
                        MotionEvent.obtain(
                            0,
                            0,
                            MotionEvent.ACTION_DOWN,
                            x.toFloat(),
                            y.toFloat(),
                            0
                        ),
                        MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, x.toFloat(), y.toFloat(), 0)
                    )
                )
            }
        })

        Thread.sleep(1000) // Necessary delay for trans

        onView(withText("Sign In Required")).check(matches(isDisplayed())) // Verify dialog

        onView(withText("YES")).perform(click())

        onView(withId(R.id.signInButton)).check(matches(isDisplayed())) // Verify we are redirected to sign-in page
    }

}
