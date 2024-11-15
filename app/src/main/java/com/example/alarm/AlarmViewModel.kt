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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val alarmsService: AlarmService,
    private val preferences: SharedPreferences
) : ViewModel() {

    private val _alarms = MutableLiveData<List<Alarm>>()
    val alarms: LiveData<List<Alarm>> = _alarms

    private val _initCompleted = MutableLiveData<Boolean>()
    val initCompleted: LiveData<Boolean> get() = _initCompleted

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

    fun updateEnabledAlarm(alarm: Alarm, enabled: Boolean, context: Context, callback: () -> Unit) {
        viewModelScope.launch {
            if (!alarm.enabled) {
                val settings = alarmsService.getSettings()
                MyAlarmManager(context, alarm, settings).startProcess()
            }
            else {
                MyAlarmManager(context, alarm, Settings(0)).endProcess()
            }
            alarmsService.updateEnabled(alarm.id, enabled)
            callback()
        }
    }

    fun addAlarm(alarm: Alarm, context: Context, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = alarmsService.addAlarm(alarm)
            if (result) {
                val settings = alarmsService.getSettings()
                MyAlarmManager(context, alarm, settings).startProcess()
            }
            callback(result)
        }
    }

    fun updateAlarm(alarmNew: Alarm, context: Context, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = alarmsService.updateAlarm(alarmNew)
            if(result) {
                if(alarmNew.enabled) {
                    val settings = alarmsService.getSettings()
                    MyAlarmManager(context, alarmNew, settings).restartProcess()
                }
            }
            callback(result)
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

    fun getPreferencesWallpaperAndInterval(): Pair<String, Int> {
        val wallpaper = preferences.getString(PREF_WALLPAPER, "") ?: ""
        val interval: Int = preferences.getInt(PREF_INTERVAL, 5)
        return Pair(wallpaper, interval)
    }
}