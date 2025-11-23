package com.teamwalk.stepcounter

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Android 12 이상에서는 BOOT_COMPLETED에서 FGS 시작하지 않음
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d("BootReceiver", "Android 12+에서는 부팅 직후 서비스 자동 시작을 하지 않습니다")
            return
        }

        if (hasPermissions(context)) {
            Log.d("BootReceiver", "권한 확인됨, 서비스 시작")
            StepCounterService.startService(context)
        } else {
            Log.d("BootReceiver", "권한 없음, 서비스 시작 안 함")
        }
    }

    private fun hasPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}