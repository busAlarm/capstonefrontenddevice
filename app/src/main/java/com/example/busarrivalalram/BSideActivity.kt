package com.example.busarrivalalram

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import com.example.busarrivalalram.databinding.ActivityAsideBinding
import com.example.busarrivalalram.databinding.ActivityBsideBinding

class BSideActivity : AppCompatActivity() {
    val binding by lazy { ActivityBsideBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(binding.root)

        // title bar 가리기
        supportActionBar?.hide()

        // full screen
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)


    }
}