package com.example.alarm

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.alarm.model.*
import com.example.alarm.room.AlarmDao
import com.example.alarm.room.AlarmDbEntity
import com.example.alarm.room.SettingsDao
import com.example.alarm.room.SettingsDbEntity
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmServiceTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var alarmService: AlarmService
    private val mockAlarmDao: AlarmDao = mock()
    private val mockSettingsDao: SettingsDao = mock()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        alarmService = AlarmService(mockAlarmDao, mockSettingsDao, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test getAlarms retrieves alarms`() = runTest {
        val dbAlarms = listOf(
            AlarmDbEntity(1, 8, 30, "Test1", true),
            AlarmDbEntity(2, 9, 30, "Test2", false)
        )
        `when`(mockAlarmDao.getAlarms()).thenReturn(dbAlarms)

        val alarms = alarmService.getAlarms()

        assertEquals(2, alarms.size)
        assertEquals("Test1", alarms[0].name)
    }

    @Test
    fun `test addAlarm returns true for new alarm`() = runTest {
        val alarm = Alarm(60, 7, 15, "New Alarm", false)
        `when`(mockAlarmDao.countAlarmsWithTime(7, 15)).thenReturn(0)

        val result = alarmService.addAlarm(alarm)

        assertEquals(true, result)
        verify(mockAlarmDao).addAlarm(AlarmDbEntity.fromUserInput(alarm))
    }

    @Test
    fun `test addAlarm returns false for duplicate`() = runTest {
        val alarm = Alarm(60, 7, 15, "New Alarm", false)
        `when`(mockAlarmDao.countAlarmsWithTime(7, 15)).thenReturn(1)
        val result = alarmService.addAlarm(alarm)
        assertEquals(false, result)
        verify(mockAlarmDao, never()).addAlarm(AlarmDbEntity.fromUserInput(alarm))
    }

    @Test
    fun `test updateSettings updates database`() = runTest {
        val settings = Settings(0)
        alarmService.updateSettings(settings)

        verify(mockSettingsDao).updateSettings(SettingsDbEntity.fromUserInput(settings))
    }
}
