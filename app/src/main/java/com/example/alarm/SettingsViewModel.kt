package com.example.alarm

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.alarm.model.AlarmService
import com.example.alarm.model.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

const val PREF_THEME = "PREF_THEME"
const val APP_PREFERENCES = "APP_PREFERENCES"
const val PREF_INTERVAL = "PREF_INTERVAL"
const val PREF_WALLPAPER = "PREF_WALLPAPER"
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val alarmsService: AlarmService,
    private val preferences: SharedPreferences
) : ViewModel() {
    private val _wallpaper = MutableLiveData<String>()
    val wallpaper: LiveData<String> = _wallpaper

    fun getPreferencesTheme(): Int {
        return preferences.getInt(PREF_THEME, 0)
    }

    fun editPreferencesWallpaper(wallpaper: String) {
        preferences.edit().putString(PREF_WALLPAPER, wallpaper).apply()
    }

    fun editPreferencesTheme(theme: Int) {
        preferences.edit().putInt(PREF_THEME, theme).apply()
    }

    fun editPreferencesInterval(interval: Int) {
        preferences.edit().putInt(PREF_INTERVAL, interval).apply()
    }

    fun registerPreferences() {
        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun unregisterPreferences() {
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == PREF_WALLPAPER) {
            val tmp = sharedPreferences.getString(PREF_WALLPAPER, "") ?: ""
            _wallpaper.postValue(tmp)
        }
    }

    fun getPreferencesWallpaper(): String {
        val wallpaper = preferences.getString(PREF_WALLPAPER, "")
        return wallpaper ?: ""
    }

    suspend fun getSettings(): Settings = withContext(Dispatchers.IO) {
        return@withContext alarmsService.getSettings()
    }

    suspend fun updateSettings(settings: Settings) = withContext(Dispatchers.IO) {
        alarmsService.updateSettings(settings)
    }
}