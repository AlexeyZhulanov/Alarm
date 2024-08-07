package com.example.alarm.model

import javax.inject.Inject

data class Alarm @Inject constructor(
    val id: Long,
    var timeHours: Int = 7,
    var timeMinutes: Int = 0,
    var name: String = "default",
    var enabled: Int = 0
) {
    override fun toString(): String {
        return "id: $id, hours:$timeHours, minutes:$timeMinutes, " +
                "name:$name, enabled:$enabled"
    }
}

