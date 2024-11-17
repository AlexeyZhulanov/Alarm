package com.example.alarm

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.alarm.model.Settings
import com.example.alarm.room.AppDatabase
import com.example.alarm.room.SettingsDao
import com.example.alarm.room.SettingsDbEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var settingsDao: SettingsDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "test_database.db" // Имя временной базы данных
        )
            .allowMainThreadQueries()
            .createFromAsset("init_db.db") // Загружаем данные из файла
            .build()

        settingsDao = database.getSettingsDao()
    }

    @After
    fun teardown() {
        // Закрываем и удаляем базу данных после тестов
        database.close()
        val dbFile = ApplicationProvider.getApplicationContext<Context>().getDatabasePath("test_database.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }
    }

    @Test
    fun testInsertAndRetrieveSettings() = runBlocking {
        val id = settingsDao.getSettings().id
        val settings = Settings(id, "Melody 7", true, 10, 3)
        settingsDao.updateSettings(SettingsDbEntity.fromUserInput(settings))

        val retrievedSettings = settingsDao.getSettings().toSettings()
        assertEquals(settings.melody, retrievedSettings.melody)
        assertEquals(settings.interval, retrievedSettings.interval)
    }

    @Test
    fun testUpdateSettings() = runBlocking {
        val id = settingsDao.getSettings().id
        val settings = Settings(id, "Default Melody", true, 10, 3)
        settingsDao.updateSettings(SettingsDbEntity.fromUserInput(settings))

        val updatedSettings = Settings(id, "New Melody", false, 15, 5)
        settingsDao.updateSettings(SettingsDbEntity.fromUserInput(updatedSettings))

        val retrievedSettings = settingsDao.getSettings().toSettings()
        assertEquals(updatedSettings.melody, retrievedSettings.melody)
        assertEquals(updatedSettings.interval, retrievedSettings.interval)
    }
}
