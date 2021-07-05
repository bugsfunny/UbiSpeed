package com.goodayedi.ubispeed.viewModel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.IllegalArgumentException

@Suppress("UNCHECKED_CAST")
class ApplicationViewModelFactory(private val application: Application): ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SpeedViewModel::class.java) -> SpeedViewModel(application)
            else -> throw IllegalArgumentException("Unexpected model class $modelClass")
        } as T
    }
}