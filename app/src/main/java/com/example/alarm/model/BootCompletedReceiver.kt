package com.example.alarm.model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.example.alarm.room.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database =
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "database.db"
                    ).build()

                val alarmDao = database.getAlarmDao()
                val enabledAlarms = alarmDao.getEnabledAlarms()

                val settingsDao = database.getSettingsDao()
                val settingsEntity = settingsDao.getSettings()

                val alarmManager =
                    context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

                val timeProvider = DefaultTimeProvider()

                val myAlarmManager = MyAlarmManager(
                    context.applicationContext,
                    alarmManager,
                    timeProvider,
                    Dispatchers.Default
                )

                enabledAlarms?.forEach { alarmEntity ->
                    myAlarmManager.startProcess(alarmEntity.toAlarm(), settingsEntity.toSettings())
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}