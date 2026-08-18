package com.amora.companion.feature.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Fired by AlarmManager when an alarm time is reached.
 * Starts [AlarmService] which plays the ringtone and listens for voice dismissal.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_LABEL = "alarm_label"
        const val EXTRA_REQUEST_CODE = "request_code"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("AlarmReceiver", "Alarm fired — starting AlarmService")

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(EXTRA_ALARM_ID, intent.getLongExtra(EXTRA_ALARM_ID, -1L))
            putExtra(EXTRA_ALARM_LABEL, intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Alarm")
            putExtra(EXTRA_REQUEST_CODE, intent.getIntExtra(EXTRA_REQUEST_CODE, 0))
        }

        context.startForegroundService(serviceIntent)
    }
}
