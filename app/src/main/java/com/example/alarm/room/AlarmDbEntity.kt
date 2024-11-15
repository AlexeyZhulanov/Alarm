package com.example.alarm.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.alarm.model.Alarm

@Entity(
    tableName = "alarms",
    indices = [
        Index("time_hours", "time_minutes", unique = true),
    ]
)
data class AlarmDbEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "time_hours") var timeHours: Int,
    @ColumnInfo(name = "time_minutes") var timeMinutes: Int,
    var name: String,
    var enabled: Boolean
    ) {

    fun toAlarm(): Alarm = Alarm(
        id = id,
        timeHours = timeHours,
        timeMinutes = timeMinutes,
        name = name,
        enabled = enabled
    )

    companion object {
        fun fromUserInput(alarm: Alarm): AlarmDbEntity = AlarmDbEntity(
            id = alarm.id,
            timeHours = alarm.timeHours,
            timeMinutes = alarm.timeMinutes,
            name = alarm.name,
            enabled = alarm.enabled
        )
    }
}