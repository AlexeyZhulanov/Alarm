package com.example.alarm.model

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.alarm.R
import com.example.alarm.di.DefaultDispatcher
import com.example.alarm.di.MainDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MyAlarmManager @Inject constructor(
    private val context: Context,
    private val alarmManager: AlarmManager,
    private val timeProvider: TimeProvider,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    private lateinit var alarmIntent: PendingIntent

    private fun createAlarmIntent(isEnd: Boolean, alarm: Alarm, settings: Settings? = Settings(0)): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.alarm.ALARM_TRIGGERED"
            if (!isEnd) {
                putExtra("alarmName", alarm.name)
                putExtra("alarmId", alarm.id)
                putExtra("settings", settings)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    suspend fun startProcess(alarm: Alarm, settings: Settings? = Settings(0), intent: PendingIntent? = null) {
        alarmIntent = intent ?: createAlarmIntent(false, alarm, settings)
        val nextAlarmTime = timeProvider.getNextAlarmTimeMillis(alarm.timeHours, alarm.timeMinutes)
        val timeUntilAlarm = nextAlarmTime - timeProvider.getCurrentTimeMillis()

        val minutesUntilAlarm = (timeUntilAlarm / 60000).toInt()
        val message = when {
            minutesUntilAlarm <= 0 -> context.getString(R.string.alarm_less_min)
            minutesUntilAlarm < 60 -> context.getString(R.string.alarm_without_n) + " $minutesUntilAlarm " + context.getString(R.string.min_point)
            else -> {
                val hours = minutesUntilAlarm / 60
                val minutes = minutesUntilAlarm % 60
                context.getString(R.string.alarm_without_n) + " $hours " + context.getString(R.string.ch) + " $minutes " + context.getString(R.string.min_point)
            }
        }

        withContext(defaultDispatcher) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(nextAlarmTime, alarmIntent),
                alarmIntent
            )
        }

        withContext(mainDispatcher) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun endProcess(alarm: Alarm, settings: Settings? = Settings(0), intent: PendingIntent? = null) {
        alarmIntent = intent ?: createAlarmIntent(true, alarm, settings)
        withContext(defaultDispatcher) {
            alarmManager.cancel(alarmIntent)
        }
    }

    suspend fun restartProcess(alarm: Alarm, settings: Settings? = Settings(0), intent: PendingIntent? = null) {
        endProcess(alarm, settings, intent)
        startProcess(alarm, settings, intent)
    }
    suspend fun repeatProcess(alarm: Alarm, settings: Settings) {
        alarmIntent = createAlarmIntent(false, alarm, settings)
        val nextRepeatTime = timeProvider.getCurrentTimeMillis() + settings.interval * 60000
        withContext(defaultDispatcher) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(nextRepeatTime, alarmIntent),
                alarmIntent
            )
        }
    }
}