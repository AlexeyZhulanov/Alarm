package com.example.alarm

import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.alarm.model.AlarmService
import com.example.alarm.model.MyAlarmManager
import com.example.alarm.model.Settings
import com.example.alarm.room.AlarmDao
import com.example.alarm.room.SettingsDao
import com.example.alarm.room.SettingsDbEntity
import io.mockk.coEvery
import io.mockk.mockk
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
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: SettingsViewModel
    private lateinit var alarmService: AlarmService
    private val mockPreferences: SharedPreferences = mock()
    private val mockAlarmDao: AlarmDao = mockk()
    private val mockSettingsDao: SettingsDao = mockk()
    private val mockEditor: SharedPreferences.Editor = mock()
    private val observer: Observer<String> = mock()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockMyAlarmManager: MyAlarmManager = mockk()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        `when`(mockPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        alarmService = AlarmService(mockAlarmDao, mockSettingsDao, testDispatcher, mockMyAlarmManager, true)
        viewModel = SettingsViewModel(alarmService, mockPreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test editPreferencesWallpaper updates preference`() {
        val wallpaper = "new_wallpaper"
        viewModel.editPreferencesWallpaper(wallpaper)
        verify(mockEditor).putString(PREF_WALLPAPER, wallpaper)
        verify(mockEditor).apply()
    }

    @Test
    fun `test editPreferencesTheme updates preference`() {
        val theme = 1

        `when`(mockPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putInt(eq(PREF_THEME), eq(theme))).thenReturn(mockEditor)
        `when`(mockEditor.apply()).thenAnswer {  } // Мокаем apply, не делая ничего

        viewModel.editPreferencesTheme(theme)
        verify(mockEditor).putInt(PREF_THEME, theme)
        verify(mockEditor).apply()
    }

    @Test
    fun `test getPreferencesWallpaper returns value`() {
        `when`(mockPreferences.getString(PREF_WALLPAPER, "")).thenReturn("wallpaper_value")
        val wallpaper = viewModel.getPreferencesWallpaper()
        assertEquals("wallpaper_value", wallpaper)
    }

    @Test
    fun `test LiveData emits wallpaper updates`() {
        `when`(mockPreferences.getString(PREF_WALLPAPER, "")).thenReturn("updated_wallpaper")

        viewModel.wallpaper.observeForever(observer)
        viewModel.registerPreferences()
        viewModel.editPreferencesWallpaper("updated_wallpaper")

        val preferenceChangeListener = viewModel.preferenceChangeListener
        preferenceChangeListener.onSharedPreferenceChanged(mockPreferences, PREF_WALLPAPER)

        verify(observer).onChanged("updated_wallpaper")
    }

    @Test
    fun `test getSettings returns settings`() = runTest {
        val expectedSettings = Settings(0)
        val mockSettingsDbEntity = SettingsDbEntity.fromUserInput(expectedSettings)
        coEvery { mockSettingsDao.getSettings() } returns mockSettingsDbEntity

        val actualSettings = viewModel.getSettings()

        assertEquals(expectedSettings, actualSettings)
    }
}
