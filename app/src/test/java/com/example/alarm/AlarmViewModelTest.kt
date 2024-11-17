package com.example.alarm

import android.content.SharedPreferences
import android.icu.util.Calendar
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.alarm.model.Alarm
import com.example.alarm.model.AlarmService
import com.example.alarm.model.AlarmsListener
import com.example.alarm.model.MyAlarmManager
import com.example.alarm.model.Settings
import com.example.alarm.room.AlarmDao
import com.example.alarm.room.AlarmDbEntity
import com.example.alarm.room.SettingsDao
import com.example.alarm.room.SettingsDbEntity
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var alarmViewModel: AlarmViewModel
    private lateinit var alarmService: AlarmService
    private val mockAlarmDao: AlarmDao = mockk()
    private val mockSettingsDao: SettingsDao = mockk()
    private val mockPreferences: SharedPreferences = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        alarmService = AlarmService(mockAlarmDao, mockSettingsDao, testDispatcher, skipInit = true, skipManager = true)
        alarmViewModel = AlarmViewModel(alarmService, mockPreferences, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `addAlarm should start process and return true`() = runTest {
        // Arrange
        val alarm = Alarm(50, 10, 0, "Test Alarm", true)
        coEvery { mockAlarmDao.countAlarmsWithTime(alarm.timeHours, alarm.timeMinutes) } returns 0
        coEvery { mockAlarmDao.addAlarm(any()) } just Runs
        coEvery { mockAlarmDao.getAlarms() } returns mutableListOf()

        val mockSettings = mockk<Settings>()
        coEvery { mockSettingsDao.getSettings().toSettings() } returns mockSettings

        var callbackResult: Boolean = false

        // Act
        alarmViewModel.addAlarm(alarm, mock(), true) { result ->
            callbackResult = result
        }

        // Assert
        assertTrue(callbackResult)
        coVerify { mockAlarmDao.countAlarmsWithTime(alarm.timeHours, alarm.timeMinutes) } // Проверка, что метод countAlarmsWithTime был вызван
        coVerify { mockAlarmDao.addAlarm(any()) } // Проверка, что добавление будильника произошло
    }

    @Test
    fun `updateEnabledAlarm should enable alarm and start process`() = runTest {
        // Arrange
        val alarm = Alarm(1, 10, 0, "Test Alarm", false)
        coEvery { mockAlarmDao.countAlarmsWithTime(alarm.timeHours, alarm.timeMinutes) } returns 0
        coEvery { mockAlarmDao.addAlarm(any()) } just Runs
        coEvery { mockAlarmDao.updateEnabled(alarm.id, true) } just Runs
        coEvery { mockAlarmDao.getAlarms() } returns mutableListOf()

        val mockSettings = mockk<Settings>()
        coEvery { mockSettingsDao.getSettings().toSettings() } returns mockSettings

        mockAlarmDao.addAlarm(AlarmDbEntity.fromUserInput(alarm))

        // Act
        alarmViewModel.updateEnabledAlarm(alarm, true, mock(), true) {}

        // Assert
        coVerify { mockAlarmDao.updateEnabled(alarm.id, true) }
    }

    @Test
    fun `getPreferencesWallpaperAndInterval should return correct values`() {
        // Arrange
        `when`(mockPreferences.getString(PREF_WALLPAPER, "")).thenReturn("wallpaper_1")
        `when`(mockPreferences.getInt(PREF_INTERVAL, 5)).thenReturn(10)

        // Act
        val result = alarmViewModel.getPreferencesWallpaperAndInterval()

        // Assert
        assertEquals(Pair("wallpaper_1", 10), result)
        verify(mockPreferences).getString(PREF_WALLPAPER, "")
        verify(mockPreferences).getInt(PREF_INTERVAL, 5)
    }

    @Test
    fun `deleteAlarms should call alarmService deleteAlarms`() = runTest {
        // Arrange
        val alarm1 = Alarm(1, 10, 0, "Test Alarm", false)
        val alarm2 = Alarm(2, 11, 30, "Test Alarm2", false)
        val alarmsToDelete = listOf(alarm1, alarm2)
        coEvery { mockAlarmDao.countAlarmsWithTime(alarm1.timeHours, alarm1.timeMinutes) } returns 0
        coEvery { mockAlarmDao.countAlarmsWithTime(alarm2.timeHours, alarm2.timeMinutes) } returns 0
        coEvery { mockAlarmDao.addAlarm(any()) } just Runs
        coEvery { mockAlarmDao.deleteAlarm(any()) } just Runs
        coEvery { alarmService.deleteAlarms(alarmsToDelete, mock()) } just Runs
        coEvery { mockAlarmDao.getAlarms() } returns mutableListOf()

        val mockSettings = mockk<Settings>()
        coEvery { mockSettingsDao.getSettings().toSettings() } returns mockSettings

        mockAlarmDao.addAlarm(AlarmDbEntity.fromUserInput(alarm1))
        mockAlarmDao.addAlarm(AlarmDbEntity.fromUserInput(alarm2))
        // Act
        alarmViewModel.deleteAlarms(alarmsToDelete, mock())

        // Assert
        coVerify { mockAlarmDao.deleteAlarm(AlarmDbEntity.fromUserInput(alarm1)) }
        coVerify { mockAlarmDao.deleteAlarm(AlarmDbEntity.fromUserInput(alarm2)) }
    }

    @Test
    fun `updateAlarm should restart process if enabled and return true`() = runTest {
        // Arrange
        val alarm = Alarm(10, 10, 0, "Test Alarm", false)
        val alarmNew = Alarm(10, 15, 30, "Wake up", false)
        coEvery { mockAlarmDao.countAlarmsWithTimeAndName(alarm.timeHours, alarm.timeMinutes, alarm.name) } returns 0
        coEvery { mockAlarmDao.countAlarmsWithTimeAndName(alarmNew.timeHours, alarmNew.timeMinutes, alarmNew.name) } returns 0
        coEvery { mockAlarmDao.addAlarm(any()) } just Runs
        coEvery { mockAlarmDao.updateAlarm(AlarmDbEntity.fromUserInput(alarmNew)) } just Runs
        coEvery { mockAlarmDao.getAlarms() } returns mutableListOf()

        val mockSettings = mockk<Settings>()
        coEvery { mockSettingsDao.getSettings().toSettings() } returns mockSettings

        mockAlarmDao.addAlarm(AlarmDbEntity.fromUserInput(alarm))

        var callbackResult: Boolean = false

        // Act
        alarmViewModel.updateAlarm(alarmNew, mock(), true) { result ->
            callbackResult = result
        }

        // Assert
        assertTrue(callbackResult)
        coVerify { mockAlarmDao.updateAlarm(AlarmDbEntity.fromUserInput(alarmNew)) }
    }

    @Test
    fun `getAndNotify should call getAlarms and notifyChanges`() = runTest {
        // Arrange
        val mockAlarms = mutableListOf<Alarm>()
        val mockListener: AlarmsListener = mockk(relaxed = true)
        alarmService.addListener(mockListener)
        coEvery { alarmService.getAlarms() } returns mockAlarms
        coEvery { alarmService.notifyChanges() } just Runs
        coEvery { mockAlarmDao.getAlarms() } returns mutableListOf()

        // Act
        alarmViewModel.getAndNotify()

        // Assert
        coVerify { mockAlarmDao.getAlarms() }
        coVerify { mockListener.invoke(mockAlarms) }
    }
}
