package com.example.stepcounter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
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
import com.example.stepcounter.databinding.ActivityMainBinding

class StepCounterService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepCountSensor: Sensor? = null
    private var currentSteps = 0
    private lateinit var walkPrefs: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        Log.d("StepCounterService", "서비스 생성")

        walkPrefs = getSharedPreferences(StepCounterUtil.PREFERENCE_FILE_NAME, Context.MODE_PRIVATE)

        currentSteps = walkPrefs.getInt(StepCounterUtil.KEY_CURRENT_STEPS, 0)
        Log.d("StepCounterService", "초기 걸음수 불러옴: $currentSteps")

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        if (stepCountSensor == null) {
            Toast.makeText(this, "만보기 센서가 없습니다", Toast.LENGTH_SHORT)
        } else {
            sensorManager.registerListener(
                this,
                stepCountSensor,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("StepCounterService", "서비스 종료")
        sensorManager.unregisterListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("StepCounterService", "서비스 시작")

        startForegroundService()
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // TODO("Not yet implemented")
    }

    // 포그라운드 서비스 알림 설정
    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0(Oreo) 이상에는 알림 채널 필요
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "만보기",
                NotificationManager.IMPORTANCE_LOW // 중요도를 낮춰 소리랑 진동을 없앰
            )

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // 알림 생성
        val notification: Notification =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("만보기앱")
                .setContentText("$currentSteps 걸음, 만보기가 실행중입니다.")
                .setSmallIcon(R.mipmap.ic_launcher) // TODO: 앱 아이콘으로 변경
                .build()

        // ForeGround 서비스 시작 (ID와 알림 전달)
        startForeground(1, notification)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                if (event.values[0] == 1.0f) {
                    currentSteps++
                    Log.d("StepCounterService", "걸음 수 : $currentSteps")

                    saveStepsToPrefs(currentSteps)
                    sendSetUpdateBroadcast(currentSteps)
                }
            }
        }
    }

    private fun sendSetUpdateBroadcast(steps: Int) {
        val intent = Intent(StepCounterUtil.ACTION_STEP_UPDATED)
        intent.putExtra(StepCounterUtil.KEY_CURRENT_STEPS, steps)
        sendBroadcast(intent)
    }

    private fun saveStepsToPrefs(steps: Int) {
        walkPrefs.edit().putInt(StepCounterUtil.KEY_CURRENT_STEPS, steps).apply()
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "step_counter_channel"
    }
}