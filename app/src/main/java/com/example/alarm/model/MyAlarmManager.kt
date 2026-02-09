package com.example.alarm.model

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.alarm.R
import com.example.alarm.SignalActivity
import com.example.alarm.di.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MyAlarmManager @Inject constructor(
    private val context: Context,
    private val alarmManager: AlarmManager,
    private val timeProvider: TimeProvider,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {
    // trigger BroadcastReceiver
    private fun createTriggerIntent(alarm: Alarm, settings: Settings?): PendingIntent {

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.alarm.ALARM_TRIGGERED"
            putExtra("alarmName", alarm.name)
            putExtra("alarmId", alarm.id)
            putExtra("settings", settings)
        }

        return PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // show Activity
    private fun createShowIntent(alarm: Alarm, settings: Settings?): PendingIntent {

        val intent = Intent(context, SignalActivity::class.java).apply {
            putExtra("alarmName", alarm.name)
            putExtra("alarmId", alarm.id)
            putExtra("settings", settings)
        }

        return PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    suspend fun startProcess(alarm: Alarm, settings: Settings? = Settings(0)): String {
        val triggerIntent = createTriggerIntent(alarm, settings)
        val showIntent = createShowIntent(alarm, settings)

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
                AlarmManager.AlarmClockInfo(nextAlarmTime, showIntent),
                triggerIntent
            )
        }

        return message
    }

    suspend fun endProcess(alarm: Alarm) {
        val triggerIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            Intent(context, AlarmReceiver::class.java).apply {
                action = "com.example.alarm.ALARM_TRIGGERED"
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        withContext(defaultDispatcher) {
            alarmManager.cancel(triggerIntent)
        }
    }

    suspend fun restartProcess(alarm: Alarm, settings: Settings? = Settings(0)) {
        endProcess(alarm)
        startProcess(alarm, settings)
    }

    suspend fun repeatProcess(alarm: Alarm, settings: Settings) {
        val triggerIntent = createTriggerIntent(alarm, settings)
        val showIntent = createShowIntent(alarm, settings)

        val nextRepeatTime = timeProvider.getCurrentTimeMillis() + settings.interval * 60000

        withContext(defaultDispatcher) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(nextRepeatTime, showIntent),
                triggerIntent
            )
        }
    }
}