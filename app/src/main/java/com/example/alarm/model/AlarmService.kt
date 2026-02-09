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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


typealias AlarmsListener = (alarms: List<Alarm>) -> Unit
class AlarmService(
    private val alarmDao: AlarmDao,
    private val settingsDao: SettingsDao,
    private val ioDispatcher: CoroutineDispatcher,
    private val myAlarmManager: MyAlarmManager,
    skipInit: Boolean = false
): AlarmRepository {
    private var alarms = mutableListOf<Alarm>()
    private val listeners = mutableSetOf<AlarmsListener>()
    private val uiScope = CoroutineScope(ioDispatcher)
    private var settings = Settings(0)

    private val _initCompleted = MutableLiveData<Boolean>()
    val initCompleted: LiveData<Boolean> get() = _initCompleted

    init {
        if(!skipInit) {
            uiScope.launch {
                alarms = getAlarms()
                _initCompleted.postValue(true)
            }
        }
    }

    override suspend fun getAlarms(): MutableList<Alarm> = withContext(ioDispatcher) {
        alarms.clear()
        val dbAlarms = alarmDao.getAlarms()
        dbAlarms?.forEach {
            alarms.add(it.toAlarm())
        }
        return@withContext alarms
    }

    override suspend fun addAlarm(alarm: Alarm) : Long = withContext(ioDispatcher) {
        val existingAlarmsCount = alarmDao.countAlarmsWithTime(alarm.timeHours, alarm.timeMinutes)
        if(existingAlarmsCount == 0) {
            try {
                alarmDao.addAlarm(AlarmDbEntity.fromUserInput(alarm))
            } catch (e: SQLiteConstraintException) {
                Log.e("AlarmFragment", "Attempt to insert duplicate alarm", e)
            }
            val existingIds = alarms.map { it.id }.toSet()
            alarms = getAlarms()
            val result = alarms.firstOrNull { it.id !in existingIds }?.id ?: 0L
            notifyChanges()
            return@withContext result
        }
        else {
            return@withContext 0L
        }
    }

    override suspend fun updateAlarm(alarm: Alarm) : Boolean = withContext(ioDispatcher) {
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

    override suspend fun updateEnabled(id: Long, enabled: Boolean) = withContext(ioDispatcher) {
        alarmDao.updateEnabled(id, enabled)
        alarms = getAlarms()
    }

    override suspend fun deleteAlarms(list: List<Alarm>, context: Context?) = withContext(ioDispatcher) {
        for(l in list) {
            if(l.enabled) myAlarmManager.endProcess(l)
            alarmDao.deleteAlarm(AlarmDbEntity.fromUserInput(l))
        }
        alarms = getAlarms()
        notifyChanges()
    }

    suspend fun offAlarms() = withContext(ioDispatcher) {
        for(alarm in alarms) {
            if (alarm.enabled) {
                alarmDao.updateEnabled(alarm.id, false)
                myAlarmManager.endProcess(alarm)
            }
        }
        alarms = getAlarms()
        notifyChanges()
    }

    suspend fun getSettings(): Settings = withContext(ioDispatcher) {
        settings = settingsDao.getSettings().toSettings()
        return@withContext settings
    }

    suspend fun updateSettings(settings: Settings) = withContext(ioDispatcher) {
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