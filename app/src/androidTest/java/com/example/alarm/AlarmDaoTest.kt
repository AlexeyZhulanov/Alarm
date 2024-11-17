package com.example.alarm

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.alarm.room.AlarmDao
import com.example.alarm.room.AlarmDbEntity
import com.example.alarm.room.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var alarmDao: AlarmDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
            .allowMainThreadQueries() // Разрешить запросы в главном потоке (для тестов)
            .build()

        alarmDao = database.getAlarmDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testInsertAndRetrieveAlarm() = runBlocking {
        val alarm = AlarmDbEntity(0, 8, 30, "Wake Up", true)
        alarmDao.addAlarm(alarm)

        val alarms = alarmDao.getAlarms()
        assertEquals(1, alarms?.size)
        assertEquals(alarm.timeHours, alarms?.first()?.timeHours)
        assertEquals(alarm.timeMinutes, alarms?.first()?.timeMinutes)
    }

    @Test
    fun testUpdateAlarm() = runBlocking {
        val alarm = AlarmDbEntity(0, 8, 30, "Wake Up", true)
        alarmDao.addAlarm(alarm)

        val updatedAlarm = AlarmDbEntity(1, 9, 45, "Meeting", false)
        alarmDao.updateAlarm(updatedAlarm)

        val alarms = alarmDao.getAlarms()
        assertEquals(1, alarms?.size)
        assertEquals("Meeting", alarms?.first()?.name)
        assertEquals(9, alarms?.first()?.timeHours)
    }

    @Test
    fun testDeleteAlarm() = runBlocking {
        val alarm = AlarmDbEntity(50, 8, 30, "Wake Up", true)
        val count = alarmDao.getAlarms()?.size
        alarmDao.addAlarm(alarm)

        alarmDao.deleteAlarm(alarm)
        val alarms = alarmDao.getAlarms()
        assertEquals(count, alarms?.size)
    }

    @Test
    fun testCountAlarmsWithTime() = runBlocking {
        alarmDao.addAlarm(AlarmDbEntity(51, 8, 30, "Wake Up", true))

        val count = alarmDao.countAlarmsWithTime(8, 30)
        assertEquals(1, count)
    }
    @Test
    fun testAddDuplicateAlarms() = runBlocking {
        alarmDao.addAlarm(AlarmDbEntity(51, 8, 30, "Wake Up", true))

        val exception = try {
            alarmDao.addAlarm(AlarmDbEntity(52, 8, 30, "Gym", false))
            null // Если ошибка не произошла
        } catch (e: SQLiteConstraintException) {
            e // Ловим исключение
        }
        assertNotNull(exception)
        assertTrue(exception!!.message!!.contains("UNIQUE constraint failed"))
    }
}
