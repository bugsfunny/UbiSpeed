package com.goodayedi.ubispeed

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.goodayedi.ubispeed.databinding.ActivityAverageBinding

class AverageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAverageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAverageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val speed = intent.getDoubleExtra("AVERAGE", 0.0)
        binding.textviewAverage.text = speed.toString()

    }
}