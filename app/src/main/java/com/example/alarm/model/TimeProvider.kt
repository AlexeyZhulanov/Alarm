package com.example.alarm.model

/**
 * TimeProvider - замена Calendar в MyAlarmManager, чтобы тесты могли выполниться
 */
interface TimeProvider {
    fun getCurrentTimeMillis(): Long
    fun getNextAlarmTimeMillis(hours: Int, minutes: Int): Long
}