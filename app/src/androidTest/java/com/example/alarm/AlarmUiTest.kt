package com.example.alarm

import android.icu.util.Calendar
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.TimePicker
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AlarmUiTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testAlarmFlow() {
        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Step 1: Handle notifications permission dialog
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                uiDevice.wait(Until.hasObject(By.text("Разрешить")), 5_000)
                uiDevice.findObject(By.text("Разрешить")).click()
            } catch (e: UiObjectNotFoundException) {
                // Permission dialog not shown, continue with the test
            }
        }

        // Step 2: Handle overlay permission dialog
        if (!Settings.canDrawOverlays(InstrumentationRegistry.getInstrumentation().targetContext)) {
            try {
                // Wait for and click the "НАСТРОЙКИ" button
                uiDevice.wait(Until.hasObject(By.text("НАСТРОЙКИ")), 5_000)
                uiDevice.findObject(By.text("НАСТРОЙКИ")).click()

                // Wait for the list of applications and find your app "Будильник"
                uiDevice.wait(Until.hasObject(By.text("Будильник")), 5_000)
                uiDevice.findObject(By.text("Будильник")).click()

                val toggle = uiDevice.findObject(UiSelector().textContains("Поверх других приложений"))
                if (toggle.exists()) {
                    toggle.click()
                }

                // Navigate back to the app
                uiDevice.pressBack()
                uiDevice.pressBack()
            } catch (e: UiObjectNotFoundException) {
                // If the dialog or required options are not found, continue with the test
            }
        }

        // Step 1: Click floatingActionButtonAdd
        onView(withId(R.id.floatingActionButtonAdd)).perform(click())

        // Step 2: Interact with TimePicker
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        // Set time to 1 minute later
        val targetMinute = (currentMinute + 1) % 60
        val targetHour = if (currentMinute == 59) (currentHour + 1) % 24 else currentHour

        onView(withClassName(Matchers.equalTo(TimePicker::class.java.name)))
            .perform(PickerActions.setTime(targetHour, targetMinute))

        // Step 3: Click confirm button
        onView(withId(R.id.confirm_button)).perform(click())

        // Step 4: Close app and turn off screen
        uiDevice.pressHome()
        uiDevice.sleep()

        // Wait for alarm
        Thread.sleep(65_000) // Wait for 1 minute + some buffer

        // Step 5: Wake up the device
        uiDevice.wakeUp()
        uiDevice.swipe(500, 1500, 500, 500, 10) // Unlock if necessary

        // Шаг 6: Проверка видимости slideButton
        //onView(withId(R.id.slideButton)).check(matches(isDisplayed()))

        // Шаг 7: Выполнение свайпа
        //onView(withId(R.id.slideButton)).perform(ViewActions.swipeRight())

    }

    // Ожидание для определенной задержки в миллисекундах
    private fun waitFor(milliSeconds: Long): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isRoot() // Root view не имеет ограничений
            }

            override fun getDescription(): String {
                return "wait for $milliSeconds milliseconds"
            }

            override fun perform(uiController: androidx.test.espresso.UiController?, view: View?) {
                try {
                    Thread.sleep(milliSeconds)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
    }
}