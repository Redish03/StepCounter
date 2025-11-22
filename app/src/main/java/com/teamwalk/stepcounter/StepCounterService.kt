package com.teamwalk.stepcounter

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.teamwalk.stepcounter.repository.GroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StepCounterService : LifecycleService(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepCountSensor: Sensor? = null
    private lateinit var walkPrefs: SharedPreferences
    private lateinit var notificationManager: NotificationManager

    private var currentSteps = 0
    private var lastSavedSteps = 0
    private var lastUploadedSteps = 0
    private var lastUploadTime = 0L // 마지막 업로드 시간

    override fun onCreate() {
        super.onCreate()
        Log.d("StepCounterService", "서비스 생성")

        walkPrefs = getSharedPreferences(StepCounterUtil.PREFERENCE_FILE_NAME, Context.MODE_PRIVATE)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        checkDateAndReset()

        setupSensor()
        launchUpdateLoop()
    }

    private fun checkDateAndReset() {
        val lastDate = walkPrefs.getString(StepCounterUtil.KEY_LAST_UPDATE, "") ?: ""
        val todayDate = StepCounterUtil.getTodayDate()

        if (lastDate != todayDate) {
            // 날짜가 다르면(하루가 지났으면) 리셋!
            Log.d("StepCounterService", "날짜 변경 감지! ($lastDate -> $todayDate). 걸음수 0으로 리셋")
            currentSteps = 0
            lastSavedSteps = 0

            // 리셋된 정보 저장 (날짜 갱신)
            saveStepsToPrefs(0)
        } else {
            // 날짜가 같으면 기존 걸음수 불러오기
            currentSteps = walkPrefs.getInt(StepCounterUtil.KEY_CURRENT_STEPS, 0)
            lastSavedSteps = currentSteps
            Log.d("StepCounterService", "오늘 걸음수 유지: $currentSteps")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d("StepCounterService", "서비스 시작")

        if (!hasPermission()) {
            Log.e("StepCounterService", "권한이 없어 서비스를 시작할 수 없습니다")
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundServiceNotification()
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                if (event.values[0] == 1.0f) {
                    currentSteps++
                }
            }
        }
    }

    private fun launchUpdateLoop() {
        lifecycleScope.launch(Dispatchers.Default) {
            while (true) {
                val lastDate = walkPrefs.getString(StepCounterUtil.KEY_LAST_UPDATE, "") ?: ""
                val todayDate = StepCounterUtil.getTodayDate()

                if (lastDate != todayDate) {
                    Log.d("StepCounterService", "자정이 지나 리셋 실행")
                    currentSteps = 0
                    lastSavedSteps - 1 // 강제로 저장 로직이 돌게 하기 위해서 설정
                }

                if (currentSteps != lastSavedSteps) {
                    Log.d("StepCounterService", "변경 감지: $lastSavedSteps -> $currentSteps")
                    saveStepsToPrefs(currentSteps)
                    launch(Dispatchers.Main) {
                        sendStepUpdateBroadcast(currentSteps)
                    }

                    updateNotification(currentSteps)
                    lastSavedSteps = currentSteps

                    if(shouldUpdateToServer(currentSteps)) {
                        Log.d("StepCounterService", "서버에 업로드 실행")
                        GroupRepository.updateMySteps(currentSteps)

                        lastUploadedSteps = currentSteps
                        lastUploadTime = System.currentTimeMillis()
                    }
                }
            }
        }
    }

    private fun sendStepUpdateBroadcast(steps: Int) {
        val intent = Intent(StepCounterUtil.ACTION_STEP_UPDATED)
        intent.putExtra(StepCounterUtil.KEY_CURRENT_STEPS, steps)
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }

    private fun setupSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        if (stepCountSensor == null) {
            Toast.makeText(this, "만보기 센서가 없습니다", Toast.LENGTH_SHORT)
        } else {
            try {
                sensorManager.registerListener(
                    this,
                    stepCountSensor,
                    SensorManager.SENSOR_DELAY_FASTEST
                )
            } catch (e: SecurityException) {
                Log.e("StepCounterService", "권한이 없어 센서를 등록할 수 없습니다: ${e.message}")
                Toast.makeText(this, "권한이 필요합니다", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("StepCounterService", "센서 등록 중 오류 발생: ${e.message}")
            }
        }
    }

    // 포그라운드 서비스 알림 설정
    private fun startForegroundServiceNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0(Oreo) 이상에는 알림 채널 필요
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "만보기",
                NotificationManager.IMPORTANCE_LOW // 중요도를 낮춰 소리랑 진동을 없앰
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 알림 생성 -> 초기에는 현재 걸음수로 초기화
        val notification = createNotification(currentSteps)
        // ForeGround 서비스 시작 (ID와 알림 전달)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun saveStepsToPrefs(steps: Int) {
        val todayDate = StepCounterUtil.getTodayDate()
        walkPrefs.edit()
            .putInt(StepCounterUtil.KEY_CURRENT_STEPS, steps)
            .putString(StepCounterUtil.KEY_LAST_UPDATE, todayDate)
            .commit()
    }

    private fun createNotification(steps: Int): Notification {
        val pendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val contextText = "현재 걸음수 : $steps"

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Team Walk")
            .setContentText(contextText)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun shouldUpdateToServer(current: Int): Boolean {
        val stepDiff = current - lastUploadedSteps
        val timeDiff = System.currentTimeMillis() - lastUploadTime

        // 조건 1: 마지막 업로드보다 50보 차이날 때
        if (stepDiff >= 50) return true

        // 조건 2: 마지막 업로드보다 5분(300,000ms) 이상 지났는데, 걸음 수 변화가 조금이라도 있을 때
        if (timeDiff >= 5 * 60 * 1000 && stepDiff > 0) return true

        return false
    }

    private fun updateNotification(steps: Int) {
        val notification = createNotification(steps)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(p0: Intent): IBinder? {
        super.onBind(p0)
        return null
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // TODO("Not yet implemented")
    }

    private fun resetSteps() {
        currentSteps = 0
        lastSavedSteps = 0

        saveStepsToPrefs(currentSteps)
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "step_counter_channel"

        fun startService(context: Context) {
            val intent = Intent(context, StepCounterService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, StepCounterService::class.java)
            context.stopService(intent)
        }
    }
}