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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.stepcounter.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var stepCounterPrefs: SharedPreferences
    private lateinit var stepUpdateReceiver: BroadcastReceiver

    private val batteryOptimizationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.d("MainActivity", "배터리 최적화 설정 화면에서 복귀함")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        stepCounterPrefs =
            getSharedPreferences(StepCounterUtil.PREFERENCE_FILE_NAME, Context.MODE_PRIVATE)

        binding.myTeamButton.setOnClickListener {
            moveToGroupActivity()
        }

        checkBatteryOptimizations()
        startStepCounterService()
        setupStepUpdateReceiver()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            && checkPermissionDenied(Manifest.permission.ACTIVITY_RECOGNITION)
        ) {
            Log.e("MainActivity", "권한 없음, 서비스 시작 차단")
            return
        }

        StepCounterService.startService(this)
    }

    private fun checkPermissionDenied(permission: String) = ContextCompat.checkSelfPermission(
        this,
        permission
    ) == PackageManager.PERMISSION_DENIED

    private fun loadStepsFromPrefs() {
        val currentSteps = stepCounterPrefs.getInt(StepCounterUtil.KEY_CURRENT_STEPS, 0)

        binding.stepCountView.text = currentSteps.toString()
    }

    private fun checkBatteryOptimizations() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager

        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            Log.d("MainActivity", "배터리 최적화 예외가 필요합니다.")

            AlertDialog.Builder(this)
                .setTitle("배터리 최적화 필요")
                .setMessage("만보기가 하루 종일 정확하게 작동하려면, 배터리 사용량 최적화에서 제외 해야합니다.\n\n'예'를 누르면 설정 화면으로 이동합니다.")
                .setPositiveButton("설정으로 이동") { _, _ ->
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                    batteryOptimizationLauncher.launch(intent)
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    private fun moveToGroupActivity() {
        startActivity(Intent(this, GroupActivity::class.java))
    }

    private fun showPermissionGuidanceDialog() {
        AlertDialog.Builder(this)
            .setTitle("권한 필요")
            .setMessage("만보기 기능을 사용하려면 '신체 활동' 및 '알림' 권한이 모두 필요합니다. 설정에서 권한을 허용해주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            .setNegativeButton("취소", null)
            .show()
    }
}