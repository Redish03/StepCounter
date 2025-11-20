package com.example.stepcounter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StepCounterService : LifecycleService(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepCountSensor: Sensor? = null
    private lateinit var walkPrefs: SharedPreferences
    private lateinit var notificationManager: NotificationManager

    private var currentSteps = 0
    private var lastSavedSteps = 0

    override fun onCreate() {
        super.onCreate()
        Log.d("StepCounterService", "서비스 생성")

        walkPrefs = getSharedPreferences(StepCounterUtil.PREFERENCE_FILE_NAME, Context.MODE_PRIVATE)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        initCurrentSteps()
        setupSensor()
        launchUpdateLoop()
    }

    private fun initCurrentSteps() {
        currentSteps = walkPrefs.getInt(StepCounterUtil.KEY_CURRENT_STEPS, 0)
        Log.d("StepCounterService", "초기 걸음수 불러옴: $currentSteps")
        lastSavedSteps = currentSteps
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d("StepCounterService", "서비스 시작")

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
                delay(2000)

                if (currentSteps != lastSavedSteps) {
                    Log.d("StepCounterService", "변경 감지: $lastSavedSteps -> $currentSteps")

                    saveStepsToPrefs(currentSteps)

                    launch(Dispatchers.Main) {
                        sendStepUpdateBroadcast(currentSteps)
                    }

                    updateNotification(currentSteps)
                    lastSavedSteps = currentSteps
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

            sensorManager.registerListener(
                this,
                stepCountSensor,
                SensorManager.SENSOR_DELAY_FASTEST
            )
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
        walkPrefs.edit().putInt(StepCounterUtil.KEY_CURRENT_STEPS, steps).commit()
    }

    private fun createNotification(steps: Int): Notification {
        val pendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val contextText = "현재 걸음수 : $steps"

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("만보기앱")
            .setContentText(contextText)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher) // TODO: 앱 아이콘으로 변경
            .setOnlyAlertOnce(true)
            .build()
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