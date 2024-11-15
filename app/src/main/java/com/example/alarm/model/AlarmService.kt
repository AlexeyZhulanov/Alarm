package com.example.alarm.model

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.alarm.room.AlarmDao
import com.example.alarm.room.AlarmDbEntity
import com.example.alarm.room.SettingsDao
import com.example.alarm.room.SettingsDbEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


typealias AlarmsListener = (alarms: List<Alarm>) -> Unit
class AlarmService(
    private val alarmDao: AlarmDao,
    private val settingsDao: SettingsDao
): AlarmRepository {
    private var alarms = mutableListOf<Alarm>()
    private val listeners = mutableSetOf<AlarmsListener>()
    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.IO + job)
    private var settings = Settings(0)

    private val _initCompleted = MutableLiveData<Boolean>()
    val initCompleted: LiveData<Boolean> get() = _initCompleted

    init {
        uiScope.launch {
            alarms = getAlarms()
            _initCompleted.postValue(true)
        }
    }

    override suspend fun getAlarms(): MutableList<Alarm> = withContext(Dispatchers.IO) {
        alarms.clear()
        val dbAlarms = alarmDao.getAlarms()
        dbAlarms?.forEach {
            alarms.add(it.toAlarm())
        }
        return@withContext alarms
    }

    override suspend fun addAlarm(alarm: Alarm) : Boolean = withContext(Dispatchers.IO) {
        val existingAlarmsCount = alarmDao.countAlarmsWithTime(alarm.timeHours, alarm.timeMinutes)
        if(existingAlarmsCount == 0) {
            try {
                alarmDao.addAlarm(AlarmDbEntity.fromUserInput(alarm))
            } catch (e: SQLiteConstraintException) {
                Log.e("AlarmFragment", "Attempt to insert duplicate alarm", e)
            }
            alarms = getAlarms()
            notifyChanges()
            return@withContext true
        }
        else {
            return@withContext false
        }
    }

    override suspend fun updateAlarm(alarm: Alarm) : Boolean = withContext(Dispatchers.IO) {
        val existingAlarmsCount = alarmDao.countAlarmsWithTimeAndName(alarm.timeHours, alarm.timeMinutes, alarm.name)
        if(existingAlarmsCount == 0) {
            try {
                alarmDao.updateAlarm(AlarmDbEntity.fromUserInput(alarm))
            } catch (e: SQLiteConstraintException) {
                Log.e("AlarmFragment", "Attempt to insert duplicate alarm", e)
            }
            alarms = getAlarms()
            notifyChanges()
            return@withContext true
        }
        else {
            return@withContext false
        }
    }

    override suspend fun updateEnabled(id: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        alarmDao.updateEnabled(id, enabled)
        alarms = getAlarms()
    }

    override suspend fun deleteAlarms(list: List<Alarm>, context: Context?) = withContext(Dispatchers.IO) {
        for(l in list) {
            if(l.enabled) MyAlarmManager(context, l, Settings(0)).endProcess()
            alarmDao.deleteAlarm(AlarmDbEntity.fromUserInput(l))
        }
        alarms = getAlarms()
        notifyChanges()
    }

    suspend fun offAlarms(context: Context) = withContext(Dispatchers.IO) {
        for(alarm in alarms) {
            if (alarm.enabled) {
                alarmDao.updateEnabled(alarm.id, false)
                MyAlarmManager(context, alarm, Settings(0)).endProcess()
            }
        }
        alarms = getAlarms()
        notifyChanges()
    }

    suspend fun getSettings(): Settings = withContext(Dispatchers.IO) {
        settings = settingsDao.getSettings().toSettings()
        return@withContext settings
    }

    suspend fun updateSettings(settings: Settings) = withContext(Dispatchers.IO) {
        settingsDao.updateSettings(SettingsDbEntity.fromUserInput(settings))
    }

    fun addListener(listener: AlarmsListener) {
        listeners.add(listener)
        listener.invoke(alarms)
    }
    fun removeListener(listener: AlarmsListener) = listeners.remove(listener)
    suspend fun notifyChanges() = withContext(Dispatchers.Main) {
        listeners.forEach {
            it.invoke(alarms)
        }
    }
}