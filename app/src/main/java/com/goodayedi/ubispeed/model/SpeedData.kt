package com.goodayedi.ubispeed.model

import android.location.Location

data class SpeedData(
    val location: Location,
    val elapsedTime: Long,
    val speed: Double = 0.0
)
