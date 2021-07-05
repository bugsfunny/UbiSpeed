package com.goodayedi.ubispeed.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.goodayedi.ubispeed.model.SpeedData
import com.goodayedi.ubispeed.utils.convertDegToRad
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import timber.log.Timber
import java.lang.Exception
import kotlin.math.*

sealed class SpeedStatus {
    object Loading: SpeedStatus()
    data class Error(val exception: Exception): SpeedStatus()
    data class Ready(val speed: Double = 0.0): SpeedStatus()
    data class Stopped(
        val averageSpeed: Double? = null
    ): SpeedStatus()
}

class SpeedViewModel(application: Application) : AndroidViewModel(application) {

    private val mApplication = application
    private var firstSubscriber = true

    private var _speedStatus = MutableLiveData<SpeedStatus>()
    val speedStatus: LiveData<SpeedStatus> get() = _speedStatus

    private var speedDataList: MutableList<SpeedData> = mutableListOf()

    private var currentStopTimer: Long = System.currentTimeMillis()

    private val fusedLocationClient = LocationServices
        .getFusedLocationProviderClient(mApplication.applicationContext)

    init {
        _speedStatus.value = SpeedStatus.Loading
        if (firstSubscriber) {
            requestLastLocation()
            createLocationRequest()
            firstSubscriber = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        firstSubscriber = true
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
            for (location in locationResult.locations) {
                Timber.i("location update: $location")

                var speedData = SpeedData(location, System.currentTimeMillis())

                if (speedDataList.size > 1) {
                    speedData = calculateSpeed(speedData, speedDataList.last())
                }

                speedDataList.add(speedData)
                _speedStatus.value = SpeedStatus.Ready(speedData.speed)
            }
        }
    }

    private fun calculateSpeed(last: SpeedData, previous: SpeedData): SpeedData {
        val time = last.elapsedTime - previous.elapsedTime
        if (time <= 0) {
            throw ArithmeticException("zero or negative time not allowed")
        }

        val lastLat = last.location.latitude
        val previousLat = previous.location.latitude
        val lastLng = last.location.longitude
        val previousLng = previous.location.longitude

        if (lastLat == previousLat && lastLng == previousLng) {
            checkUserActivity()
            return last
        } else {
            currentStopTimer = System.currentTimeMillis()
        }

        val dist = calculateDistance(lastLat, previousLat, lastLng, previousLng) // km
        var speed = (dist / time) * 3600000 // FROM km/ms to km/h
        speed = round(speed * 100) / 100

        return SpeedData(last.location, last.elapsedTime, speed)
    }

    /*
    *
    * Haversine formula
    *
    */
    private fun calculateDistance(
        lastLat: Double,
        previousLat: Double,
        lastLng: Double,
        previousLng: Double
    ): Double {
        // R is earth’s radius
        val r = 6371 // km
        val deltaLat = (lastLat - previousLat).convertDegToRad()
        val deltaLng = (lastLng - previousLng).convertDegToRad()
        val a = (sin(deltaLat / 2) * sin(deltaLat / 2)
                + (cos(previousLat.convertDegToRad()) * cos(lastLat.convertDegToRad())
                * sin(deltaLng / 2) * sin(deltaLng / 2)))

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c
    }

    private fun checkUserActivity() {
        if (System.currentTimeMillis() - currentStopTimer >= 30000) {
            var speedAverage = speedDataList.map { it.speed }.average()
            speedAverage = round(speedAverage * 100) / 100
            _speedStatus.value = SpeedStatus.Stopped(speedAverage)
            fusedLocationClient.removeLocationUpdates(locationCallback)
            firstSubscriber = true
        }
    }

    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 10000
        fastestInterval = 5000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    fun createLocationRequest() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(mApplication.applicationContext)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            Timber.i("Location settings: ok")
            requestLocation()
        }
        task.addOnFailureListener {
            Timber.i("Location settings: $it")
            if (it is ResolvableApiException) {
                _speedStatus.value = SpeedStatus.Error(exception = it)
            }
        }
    }

    private fun requestLocation() {
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } catch (e: SecurityException) {
            _speedStatus.value = SpeedStatus.Error(exception = e)
        }
    }

    private fun requestLastLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                val speedData = SpeedData(location, System.currentTimeMillis())
                speedDataList.add(speedData)
            }
            fusedLocationClient.lastLocation.addOnFailureListener { exception ->
                _speedStatus.value = SpeedStatus.Error(exception = exception)
            }
        } catch (e: SecurityException){
            _speedStatus.value = SpeedStatus.Error(exception = e)
        }
    }
}