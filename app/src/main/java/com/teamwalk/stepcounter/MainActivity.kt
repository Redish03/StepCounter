package com.teamwalk.stepcounter

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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.teamwalk.stepcounter.databinding.ActivityMainBinding
import com.teamwalk.stepcounter.repository.GroupRepository
import com.google.firebase.auth.FirebaseAuth

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

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        stepCounterPrefs =
            getSharedPreferences(StepCounterUtil.PREFERENCE_FILE_NAME, Context.MODE_PRIVATE)

        setClickListeners()
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

    private fun setClickListeners() {
        binding.myTeamButton.setOnClickListener {
            moveToGroupActivity()
        }
        binding.tvPrivacyPolicy.setOnClickListener { showPrivacyPolicyDialog() }
        binding.btnDeleteAccount.setOnClickListener {
            deleteAccount()
        }
    }

    private fun showPrivacyPolicyDialog() {
        val privacyUrl =
            "https://wonderful-report-e58.notion.site/Team-Walk-2b35b07568ed80d08e5dc341be69b019?source=copy_link"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
        startActivity(intent)
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
            && PermissionChecker().isPermissionDenied(
                context = this,
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        ) {
            Log.e("MainActivity", "권한 없음, 서비스 시작 차단")
            return
        }

        StepCounterService.startService(this)
    }

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

    private fun deleteAccount() {
        AlertDialog.Builder(this)
            .setTitle("회원 탈퇴")
            .setMessage("정말로 탈퇴하시겠습니까? 모든 기록이 삭제됩니다.")
            .setPositiveButton("탈퇴") { _, _ ->
                GroupRepository.deleteAccount { result ->
                    handleDeleteResult(result) // 결과를 처리하는 함수로 넘김 (깔끔!)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun handleDeleteResult(result: GroupRepository.DeleteResult) {
        when (result) {
            is GroupRepository.DeleteResult.Success -> {
                Toast.makeText(this, "성공적으로 탈퇴되었습니다.", Toast.LENGTH_SHORT).show()
                moveToLoginScreen()
            }

            is GroupRepository.DeleteResult.RequiresRecentLogin -> {
                showReLoginDialog() // 재로그인 유도 팝업
            }

            is GroupRepository.DeleteResult.Failure -> {
                Toast.makeText(this, "오류 발생: ${result.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showReLoginDialog() {
        AlertDialog.Builder(this)
            .setTitle("재로그인 필요")
            .setMessage("보안을 위해 최근에 로그인한 사용자만 탈퇴할 수 있습니다.\n\n로그아웃 후 다시 로그인하시겠습니까?")
            .setPositiveButton("확인") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                moveToLoginScreen()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun moveToLoginScreen() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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