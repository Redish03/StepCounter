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
import android.provider.Settings
import android.util.Log
import android.widget.Toast
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
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_DENIED
            ) {
                permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_DENIED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), 0)
        }
    }


    private fun setupStepUpdateReceiver() {
        stepUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val steps = intent?.getIntExtra(StepCounterUtil.KEY_CURRENT_STEPS, 0)
                binding.stepCountView.text = steps.toString()
                Log.d("MainActivity", "방송 수신: $steps 걸음")
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


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 0) {
            var allGranted = true

            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_DENIED) {
                    allGranted = false
                    break
                }
            }

            if (allGranted) startStepCounterService()
            else {
                Toast.makeText(this, "권한이 거부되어 만보기를 실행할 수 없습니다", Toast.LENGTH_SHORT).show()

                AlertDialog.Builder(this)
                    .setTitle("권한 필요")
                    .setMessage("만보기 기능을 사용하려면 신체 기능 및 알림 권한이 필요합니다. 설정에서 허용해주세요.")
                    .setPositiveButton("설정으로 이동") { _, _ ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }
                    .setNegativeButton("취소", null)
                    .show()
            }
        }
    }
}