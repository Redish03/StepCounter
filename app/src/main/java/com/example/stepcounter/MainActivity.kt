package com.example.stepcounter

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.stepcounter.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var stepCounterPrefs: SharedPreferences
    private lateinit var stepUpdateReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        stepCounterPrefs =
            getSharedPreferences(StepCounterUtil.PREFERENCE_FILE_NAME, Context.MODE_PRIVATE)

        checkActivityPermission()
        setupStepUpdateReceiver()
        startStepCounterService()
    }

    override fun onResume() {
        super.onResume()
        loadStepsFromPrefs()

        val filter = IntentFilter(StepCounterUtil.ACTION_STEP_UPDATED)
        ContextCompat.registerReceiver(
            this,
            stepUpdateReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(stepUpdateReceiver)
    }

    fun checkActivityPermission() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (checkPermissionDenied(Manifest.permission.ACTIVITY_RECOGNITION)
            ) {
                permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkPermissionDenied(Manifest.permission.POST_NOTIFICATIONS)
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), 0)
        }
    }

    private fun checkPermissionDenied(permission: String) = ContextCompat.checkSelfPermission(
        this,
        permission
    ) == PackageManager.PERMISSION_DENIED


    private fun setupStepUpdateReceiver() {
        stepUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == StepCounterUtil.ACTION_STEP_UPDATED) {
                    val steps = intent.getIntExtra(StepCounterUtil.KEY_CURRENT_STEPS, 0)
                    Log.d("MainActivity", "방송 수신 성공: $steps 걸음")
                    binding.stepCountView.text = steps.toString()
                } else {
                    Log.w("MainActivity", "방송은 수신했으나 Action이 다름: ${intent?.action}")
                }
            }
        }
    }

    private fun startStepCounterService() {
        val serviceIntent = Intent(this, StepCounterService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun loadStepsFromPrefs() {
        val currentSteps = stepCounterPrefs.getInt(StepCounterUtil.KEY_CURRENT_STEPS, 0)

        binding.stepCountView.text = currentSteps.toString()
    }
}