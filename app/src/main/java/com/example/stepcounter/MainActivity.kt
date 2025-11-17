package com.example.stepcounter

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
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
        checkBatteryOptimizations()
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

    private fun checkBatteryOptimizations() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager

        if(!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            Log.d("MainActivity", "배터리 최적화 예외가 필요합니다.")

            AlertDialog.Builder(this)
                .setTitle("배터리 최적화 필요")
                .setMessage("만보기가 하루 종일 정확하게 작동하려면, 배터리 사용량 최적화에서 제외 해야합니다.\n\n'예'를 누르면 설정 화면으로 이동합니다.")
                .setPositiveButton("설정으로 이동") { _, _ ->
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                }
                .setNegativeButton("취소", null)
                .show()
        } else {
            Log.d("MainActivity", "배터리 최적화 예외가 이미 설정됨.")
        }
    }
}