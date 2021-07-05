package com.goodayedi.ubispeed

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.goodayedi.ubispeed.databinding.ActivityMainBinding
import com.goodayedi.ubispeed.viewModel.ApplicationViewModelFactory
import com.goodayedi.ubispeed.viewModel.SpeedStatus
import com.goodayedi.ubispeed.viewModel.SpeedViewModel
import com.google.android.gms.common.api.ResolvableApiException
import timber.log.Timber


private const val REQUEST_LOCATION_PERMISSION_START_UPDATE = 1

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel by viewModels<SpeedViewModel> {
        ApplicationViewModelFactory(application = application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.speedStatus.observe(this){
            handleSpeedData(it!!)
        }
    }

    private fun handleSpeedData(speedStatus: SpeedStatus) {
        when(speedStatus){
            is SpeedStatus.Error -> {
                if (handleLocationException(speedStatus.exception)) return
            }
            is SpeedStatus.Ready -> {
                Timber.i("data from activity: ${speedStatus.speed}")
                binding.loadingProgressBar.hide()
                binding.textviewSpeedLabel.visibility = VISIBLE
                binding.textviewSpeed.text = speedStatus.speed.toString()
            }
            is SpeedStatus.Stopped -> {
                Timber.i("Activity Stopped: ${speedStatus.averageSpeed}")
                val intent = Intent(baseContext, AverageActivity::class.java)
                intent.putExtra("AVERAGE", speedStatus.averageSpeed)
                startActivity(intent)
            }
            else -> {
                binding.textviewSpeedLabel.visibility = GONE
                binding.loadingProgressBar.show()
            }
        }
    }

    private fun handleLocationException(exception: Exception?): Boolean {
        exception ?: return false
        when(exception){
            is SecurityException -> checkLocationPermission(
                REQUEST_LOCATION_PERMISSION_START_UPDATE)
            is ResolvableApiException -> registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                viewModel.createLocationRequest()
            }.launch(IntentSenderRequest.Builder(exception.resolution).build())
        }
        return true
    }


    private fun checkLocationPermission(requestCode: Int): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                requestCode
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            return
        }
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION_START_UPDATE -> viewModel.createLocationRequest()
        }
    }
}