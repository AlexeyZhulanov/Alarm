package com.example.alarm

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.model.Alarm
import com.example.alarm.model.AlarmService
import com.example.alarm.model.AlarmsListener
import com.example.alarm.model.MyAlarmManager
import com.example.alarm.model.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

const val APP_PREFERENCES = "APP_PREFERENCES"
const val PREF_INTERVAL = "PREF_INTERVAL"
const val PREF_WALLPAPER = "PREF_WALLPAPER"
const val PREF_THEME = "PREF_THEME"
@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val alarmsService: AlarmService
) : ViewModel() {

    private val _alarms = MutableLiveData<List<Alarm>>()
    val alarms: LiveData<List<Alarm>> = _alarms

    private val _initCompleted = MutableLiveData<Boolean>()
    val initCompleted: LiveData<Boolean> get() = _initCompleted

    private val _wallpaper = MutableLiveData<String>()
    val wallpaper: LiveData<String>  = _wallpaper


    private val alarmsListener: AlarmsListener = {
        _alarms.value = it
    }
    init {
        alarmsService.initCompleted.observeForever { initCompleted ->
            if (initCompleted) {
                _initCompleted.postValue(true)
            }
        }
        alarmsService.addListener(alarmsListener)
    }

    override fun onCleared() {
        super.onCleared()
        alarmsService.removeListener(alarmsListener)
    }

    suspend fun updateEnabledAlarm(alarm: Alarm, enabled: Boolean, context: Context, idx: Int) = withContext(Dispatchers.Main) {
            if (!alarm.enabled) { //turn on
                val s = async(Dispatchers.IO) { alarmsService.getSettings() }
                MyAlarmManager(context, alarm, s.await()).startProcess()
            }
            else {
                MyAlarmManager(context, alarm, Settings(0)).endProcess()
            }
        withContext(Dispatchers.IO) {
            alarmsService.updateEnabled(alarm.id, enabled)
        }
        return@withContext idx
    }

    suspend fun addAlarm(alarm: Alarm, context: Context): Boolean = withContext(Dispatchers.Main) {
            if(withContext(Dispatchers.IO) { alarmsService.addAlarm(alarm)}) {
                val settings = async(Dispatchers.IO) { alarmsService.getSettings() }
                MyAlarmManager(context, alarm, settings.await()).startProcess()
                return@withContext true
            }
            else {
                return@withContext false
            }
    }

    suspend fun updateAlarm(alarmNew: Alarm, context: Context): Boolean = withContext(Dispatchers.Main) {
        if(withContext(Dispatchers.IO) { alarmsService.updateAlarm(alarmNew)}) {
            if (alarmNew.enabled) {
                val settings = async(Dispatchers.IO) { alarmsService.getSettings() }
                MyAlarmManager(context, alarmNew, settings.await()).restartProcess()
            }
            return@withContext true
        }
        else {
            return@withContext false
        }
    }

    fun deleteAlarms(alarmsToDelete: List<Alarm>, context: Context?) {
        viewModelScope.launch {
            alarmsService.deleteAlarms(alarmsToDelete, context)
        }
    }
    fun getAndNotify() {
        viewModelScope.launch {
            alarmsService.getAlarms()
            alarmsService.notifyChanges()
        }
    }
    fun getPreferencesWallpaperAndInterval(context: Context): Pair<String, Int>{
        val preferences: SharedPreferences = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        val wallpaper = preferences.getString(PREF_WALLPAPER, "")
        val interval: Int = preferences.getInt(PREF_INTERVAL, 5)
        return Pair(wallpaper!!, interval)
    }
    fun getPreferencesTheme(context: Context): Int {
        val preferences: SharedPreferences = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        return preferences.getInt(PREF_THEME, 0)
    }

    fun editPreferencesWallpaper(context: Context, wallpaper: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
            val preferences: SharedPreferences =
                context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
            preferences.edit().putString(PREF_WALLPAPER, wallpaper).apply()
        }
        }
    }
    fun editPreferencesInterval(context: Context, interval: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val preferences: SharedPreferences =
                    context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
                preferences.edit().putInt(PREF_INTERVAL, interval).apply()
            }
        }
    }
    fun editPreferencesTheme(context: Context, theme: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val preferences: SharedPreferences =
                    context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
                preferences.edit().putInt(PREF_THEME, theme).apply()
            }
        }
    }

    fun registerPreferences(context: Context) {
        val prefs = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
    fun unregisterPreferences(context: Context) {
        val prefs = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == PREF_WALLPAPER) {
            val tmp = sharedPreferences.getString(PREF_WALLPAPER, "")
            _wallpaper.postValue(tmp!!)
        }
    }

    suspend fun getSettings(): Settings = withContext(Dispatchers.IO) {
        return@withContext alarmsService.getSettings()
    }
    fun updateSettings(settings: Settings) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                alarmsService.updateSettings(settings)
            }
        }
    }
}