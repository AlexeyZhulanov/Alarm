package com.example.alarm

import android.content.Context
import androidx.room.Room
import com.example.alarm.model.AlarmService
import com.example.alarm.room.AppDatabase
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AlarmWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val alarmId = inputData.getLong("alarmId", 0L)
        val enabled = inputData.getBoolean("enabled", false)

        val database = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "database.db")
            .build()
        val alarmService = AlarmService(database.getAlarmDao(), database.getSettingsDao())

        alarmService.updateEnabled(alarmId, enabled)

        return Result.success()
    }
}