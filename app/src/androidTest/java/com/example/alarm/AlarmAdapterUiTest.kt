package com.example.alarm

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers.endsWith
import org.junit.Rule
import org.junit.Test

class AlarmAdapterUiTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testEnableAlarmAndDelete() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Step 1: Handle notifications permission dialog
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                uiDevice.wait(Until.hasObject(By.text(context.getString(R.string.access))), 5_000)
                uiDevice.findObject(By.text(context.getString(R.string.access))).click()
            } catch (_: UiObjectNotFoundException) {
                // Permission dialog not shown, continue with the test
            }
        }

        // Step 2: Handle overlay permission dialog
        if (!Settings.canDrawOverlays(InstrumentationRegistry.getInstrumentation().targetContext)) {
            try {
                uiDevice.wait(Until.hasObject(By.text(context.getString(R.string.cancel_caps))), 5_000)
                uiDevice.findObject(By.text(context.getString(R.string.cancel_caps))).click()
                } catch (_: UiObjectNotFoundException) {
                // If the dialog or required options are not found, continue with the test
            }
        }

        // Step 3: Enable the second alarm by toggling its switch
        onView(withId(R.id.recyclerview))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    1, // Second item (0-based index)
                    clickChildViewWithId(R.id.switch1)
                )
            )

        // Step 4: Open the popup menu by clicking the toolbar menu (3 dots)
        onView(withClassName(endsWith(context.getString(R.string.overflowmenubutton))))
            .perform(click())

        // Step 5: Click the second item in the popup menu
        onView(withText(R.string.off_all_alarms))
            .perform(click())

        // Step 6: Long click the second item in the RecyclerView
        onView(withId(R.id.recyclerview))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    1,
                    longClick()
                )
            )

        // Step 7: Press the delete FloatingActionButton
        onView(withId(R.id.floatingActionButtonDelete))
            .perform(click())

        // Step 8: Check that the number of items in RecyclerView has decreased by 1
        onView(withId(R.id.recyclerview))
            .check { view, _ ->
                val recyclerView = view as RecyclerView
                val itemCountAfter = recyclerView.adapter?.itemCount ?: 0
                // Assert that the RecyclerView has one less item
                assert(itemCountAfter == 2)
            }
    }

    /**
     * Helper function to click on a child view with a specific ID within a RecyclerView item.
     */
    private fun clickChildViewWithId(id: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): org.hamcrest.Matcher<View> {
                return isAssignableFrom(View::class.java)
            }

            override fun getDescription(): String {
                return "Click on a child view with specified id."
            }

            override fun perform(uiController: UiController, view: View) {
                val childView = view.findViewById<View>(id)
                childView.performClick()
            }
        }
    }
}