package com.example.alarm.model

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.alarm.room.AppDatabase

class AlarmWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val alarmId = inputData.getLong("alarmId", 0L)

        val database = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "database.db")
            .build()
        val alarmServiceBackground = AlarmServiceBackground(database.getAlarmDao())
        alarmServiceBackground.updateEnabled(alarmId, false)

        return Result.success()
    }
}