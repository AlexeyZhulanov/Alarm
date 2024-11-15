package com.example.alarm.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Settings(
    val id: Long,
    var melody: String = "default",
    var vibration: Boolean = true,
    var interval: Int = 5,
    var repetitions: Int = 3
    ) : Parcelable