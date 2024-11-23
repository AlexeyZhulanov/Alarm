package com.example.alarm.model

import android.util.Log
import com.example.alarm.room.AlarmDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AlarmServiceBackground(private val alarmDao: AlarmDao) {
    suspend fun updateEnabled(id: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        alarmDao.updateEnabled(id, enabled)
    }
}