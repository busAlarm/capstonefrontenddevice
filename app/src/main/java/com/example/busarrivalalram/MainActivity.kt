package com.example.busarrivalalram

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatDelegate
import com.example.busarrivalalram.databinding.ActivityMainBinding
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(binding.root)

        // 바로가기
        binding.goToASideBtn.setOnClickListener {
            Log.d("btnEventA", "pushed")
            val intent = Intent(this, ASideActivity::class.java)
            startActivity(intent)
            Log.d("btnEventA", "startActivity")
        }

        binding.goToBSideBtn.setOnClickListener {
            Log.d("btnEventB", "pushed")
            val intent = Intent(this, BSideActivity::class.java)
            startActivity(intent)
            Log.d("btnEventB", "startActivity")
        }

        binding.exitAppBtn.setOnClickListener {
            finish()
        }
    }
}