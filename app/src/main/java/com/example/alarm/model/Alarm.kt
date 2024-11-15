package com.example.alarm.model

data class Alarm(
    val id: Long,
    var timeHours: Int = 7,
    var timeMinutes: Int = 0,
    var name: String = "default",
    var enabled: Boolean = false
) {
    override fun toString(): String {
        return "id: $id, hours:$timeHours, minutes:$timeMinutes, " +
                "name:$name, enabled:$enabled"
    }
}

