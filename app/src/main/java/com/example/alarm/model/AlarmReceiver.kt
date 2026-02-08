package com.example.alarm.model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
            putExtras(intent)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}