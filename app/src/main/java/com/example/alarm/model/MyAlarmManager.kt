package com.example.alarm.model

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.icu.util.ULocale
import android.widget.Toast
import com.example.alarm.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class MyAlarmManager(
    private val context: Context?,
    val alarm: Alarm,
    val settings: Settings
) {
    private lateinit var alarmManager: AlarmManager
    private lateinit var alarmIntent: PendingIntent
    private val calendar = Calendar.getInstance()
    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Default + job)

    private fun initialFunc(isEnd: Boolean) {
        alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmIntent = Intent(context, AlarmReceiver::class.java).let { intent ->
            if(!isEnd) {
                intent.putExtra("alarmName", alarm.name)
                intent.putExtra("alarmId", alarm.id)
                intent.putExtra("settings", settings)
            }
            intent.action = "com.example.alarm.ALARM_TRIGGERED"
            PendingIntent.getBroadcast(
                context,
                alarm.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    suspend fun startProcess() {
        initialFunc(false)
        val part = uiScope.async {
            calendar.set(Calendar.HOUR_OF_DAY, alarm.timeHours)
            calendar.set(Calendar.MINUTE, alarm.timeMinutes)
            calendar.set(Calendar.SECOND, 0)
            val calendar2 = Calendar.getInstance(ULocale.ROOT)
            val longTime: Long = if (calendar2.timeInMillis > calendar.timeInMillis) {
                calendar.timeInMillis + 86400000
            } else calendar.timeInMillis

            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(longTime, alarmIntent),
                alarmIntent
            )

            val minutes: Int = if (calendar2.timeInMillis > calendar.timeInMillis) {
                ((longTime - calendar2.timeInMillis) / 60000).toInt()
            } else ((calendar.timeInMillis - calendar2.timeInMillis) / 60000).toInt()
            var str = ""
            when (minutes) {
                0 -> str += context?.getString(R.string.alarm_less_min)
                in 1..59 -> str += context?.getString(R.string.alarm_without_n) +  " $minutes " +
                        context?.getString(R.string.min_point)
                else -> {
                    val hours = minutes / 60
                    str += context?.getString(R.string.alarm_without_n) + " $hours " +
                            context?.getString(R.string.ch) + " ${minutes % 60} " +
                            context?.getString(R.string.min_point)
                }
            }
            return@async str
        }
        val res = part.await()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, res, Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun endProcess() = withContext(Dispatchers.Default) {
        initialFunc(true)
        alarmManager.cancel(alarmIntent)
    }

    suspend fun restartProcess() = withContext(Dispatchers.Main) {
        endProcess()
        startProcess()
    }
    suspend fun repeatProcess() = withContext(Dispatchers.Default) {
        initialFunc(false)
        val calendar = Calendar.getInstance(ULocale.ROOT)
        val time = calendar.timeInMillis + settings.interval.toLong()*60000
        alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(time, alarmIntent), alarmIntent)
    }
}