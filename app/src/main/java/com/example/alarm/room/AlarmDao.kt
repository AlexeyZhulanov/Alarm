package com.example.alarm.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY time_hours ASC, time_minutes ASC")
    suspend fun getAlarms(): List<AlarmDbEntity>?

    @Query("SELECT COUNT(*) FROM alarms WHERE time_hours = :hours AND time_minutes = :minutes")
    suspend fun countAlarmsWithTime(hours: Int, minutes: Int): Int

    @Query("SELECT COUNT(*) FROM alarms WHERE time_hours = :hours AND time_minutes = :minutes AND name = :name")
    suspend fun countAlarmsWithTimeAndName(hours: Int, minutes: Int, name: String): Int

    @Insert
    suspend fun addAlarm(alarmDbEntity: AlarmDbEntity)

    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    suspend fun updateEnabled(id: Long, enabled: Boolean)

    @Update
    suspend fun updateAlarm(alarmDbEntity: AlarmDbEntity)

    @Delete(entity = AlarmDbEntity::class)
    suspend fun deleteAlarm(alarmDbEntity: AlarmDbEntity)
}