package com.example.alarm

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.di.MainDispatcher
import com.example.alarm.model.Alarm
import com.example.alarm.model.AlarmService
import com.example.alarm.model.AlarmsListener
import com.example.alarm.model.MyAlarmManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val alarmsService: AlarmService,
    private val preferences: SharedPreferences,
    private val myAlarmManager: MyAlarmManager,
    @param:MainDispatcher private val dispatcher: CoroutineDispatcher
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

    fun updateEnabledAlarm(alarm: Alarm, enabled: Boolean, callback: () -> Unit, toastCallback: (String) -> Unit) {
        viewModelScope.launch(dispatcher) {
            if (!alarm.enabled) {
                val settings = alarmsService.getSettings()
                val message = myAlarmManager.startProcess(alarm, settings)
                toastCallback(message)
            }
            else {
                myAlarmManager.endProcess(alarm)
            }
            alarmsService.updateEnabled(alarm.id, enabled)
            callback()
        }
    }

    fun addAlarm(alarm: Alarm, callback: (Long) -> Unit, toastCallback: (String) -> Unit) {
        viewModelScope.launch(dispatcher) {
            val result = alarmsService.addAlarm(alarm)
            if (result != 0L) {
                val settings = alarmsService.getSettings()
                val newAlarm = Alarm(result, alarm.timeHours, alarm.timeMinutes, alarm.name, alarm.enabled)
                val message = myAlarmManager.startProcess(newAlarm, settings)
                toastCallback(message)
            }
            callback(result)
        }
    }

    fun updateAlarm(alarmNew: Alarm, callback: (Boolean) -> Unit) {
        viewModelScope.launch(dispatcher) {
            val result = alarmsService.updateAlarm(alarmNew)
            if(result) {
                if(alarmNew.enabled) {
                    val settings = alarmsService.getSettings()
                    myAlarmManager.restartProcess(alarmNew, settings)
                }
            }
            callback(result)
        }
    }

    fun deleteAlarms(alarmsToDelete: List<Alarm>, context: Context?) {
        viewModelScope.launch(dispatcher) {
            alarmsService.deleteAlarms(alarmsToDelete, context)
        }
    }

    fun getAndNotify() {
        viewModelScope.launch(dispatcher) {
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