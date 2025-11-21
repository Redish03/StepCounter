package com.example.stepcounter

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StepCounterUtil {
    const val PREFERENCE_FILE_NAME = "step_counter_prefs"
    const val KEY_CURRENT_STEPS = "current_steps"
    const val ACTION_STEP_UPDATED = "com.example.stepcounter.STEP_UPDATED"
    const val KEY_LAST_UPDATE = "last_saved_date"

    fun getTodayDate(): String {
        val simpleData = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return simpleData.format(Date())
    }
}