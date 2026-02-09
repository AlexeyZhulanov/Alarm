package com.example.alarm

import android.content.Context
import android.icu.util.Calendar
import android.os.Build
import android.provider.Settings
import android.widget.TimePicker
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.Until
import org.hamcrest.Matchers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmUiTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testAlarmFlow() {
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
        // Step 3: Click floatingActionButtonAdd
        onView(withId(R.id.floatingActionButtonAdd)).perform(click())

        // Step 4: Interact with TimePicker
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        // Set time to 1 minute later
        val targetMinute = (currentMinute + 1) % 60
        val targetHour = if (currentMinute == 59) (currentHour + 1) % 24 else currentHour

        onView(withClassName(Matchers.equalTo(TimePicker::class.java.name)))
            .perform(PickerActions.setTime(targetHour, targetMinute))

        // Step 5: Click confirm button
        onView(withId(R.id.confirm_button)).perform(click())

        // Step 6: Close app and turn off screen
        uiDevice.pressHome()
        uiDevice.sleep()

        // Step 7: Waiting screen ON
        var screenOn = false
        for (i in 1..60) {
            if (uiDevice.isScreenOn) {
                screenOn = true
                Thread.sleep(5000) // для наглядности
                break
            }
            Thread.sleep(1000)
        }

        assertTrue(context.getString(R.string.error_screen_on), screenOn)

        // Step 8: Check and swipe slideButton
        onView(withId(R.id.slideButton)).check(matches(isDisplayed()))
        onView(withId(R.id.slideButton)).perform(ViewActions.swipeRight())
    }
}