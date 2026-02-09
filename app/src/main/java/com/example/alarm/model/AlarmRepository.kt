package com.example.alarm.model

import android.content.Context

interface AlarmRepository {
    suspend fun getAlarms(): MutableList<Alarm>

    suspend fun addAlarm(alarm: Alarm) : Long

    suspend fun updateAlarm(alarm: Alarm) : Boolean

    suspend fun updateEnabled(id: Long, enabled: Boolean)

    suspend fun deleteAlarms(list: List<Alarm>, context: Context?)
}