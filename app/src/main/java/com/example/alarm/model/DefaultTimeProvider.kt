package com.example.alarm.model

import android.icu.util.Calendar

class DefaultTimeProvider : TimeProvider {
    override fun getCurrentTimeMillis(): Long {
        return System.currentTimeMillis()
    }

    override fun getNextAlarmTimeMillis(hours: Int, minutes: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hours)
            set(Calendar.MINUTE, minutes)
            set(Calendar.SECOND, 0)
        }
        return if (now.timeInMillis > target.timeInMillis) {
            target.timeInMillis + 86400000 // Next day
        } else {
            target.timeInMillis
        }
    }
}
