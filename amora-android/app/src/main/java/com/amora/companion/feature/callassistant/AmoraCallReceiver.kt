package com.amora.companion.feature.callassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import com.amora.companion.core.data.preferences.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class AmoraCallReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        @Suppress("DEPRECATION")
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: "Unknown Caller"

        Log.d("AmoraCallReceiver", "Phone State Changed: $state, Incoming: $incomingNumber")

        if (state == TelephonyManager.EXTRA_STATE_RINGING) {
            CoroutineScope(Dispatchers.IO).launch {
                val isEnabled = preferencesRepository.isCallAssistantEnabled.first()
                if (!isEnabled) return@launch

                val activeMode = preferencesRepository.callAssistantMode.first()
                Log.d("AmoraCallReceiver", "Call Screening Triggered! Active Mode: $activeMode")

                // 1. Silence / Mute Ringer automatically based on active mode
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                try {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 2. Prepare Mode-Specific SMS Response & Screening Log
                val (smsMessage, isEmergency) = when (activeMode) {
                    "Meeting" -> "Hi, I am currently in a meeting. AMORA AI Companion has screened your call. If this is an emergency, reply EMERGENCY." to false
                    "College" -> "Hi, I am currently attending class in college. AMORA AI Companion has screened your call. If this is urgent, reply EMERGENCY." to false
                    "Sleeping" -> "Hi, I am currently sleeping. AMORA AI Companion has muted this call. If this is an urgent emergency, reply EMERGENCY." to false
                    else -> "Hi, I am currently unavailable. AMORA AI Companion screened your call." to false
                }

                // 3. Send SMS Auto-Reply if incoming number is available
                if (incomingNumber.length > 3 && incomingNumber != "Unknown Caller") {
                    sendAutoSms(incomingNumber, smsMessage)
                }

                // 4. Log Screened Call to DataStore
                val currentLogsStr = preferencesRepository.callScreeningLogsJson.first()
                val jsonArray = if (currentLogsStr.isNotEmpty()) JSONArray(currentLogsStr) else JSONArray()

                val newLog = JSONObject().apply {
                    put("id", System.currentTimeMillis().toString())
                    put("caller", incomingNumber)
                    put("timeAgo", "Just now")
                    put("mode", activeMode)
                    put("isEmergency", isEmergency)
                    put("reason", smsMessage)
                }
                jsonArray.put(newLog)
                preferencesRepository.saveCallScreeningLogsJson(jsonArray.toString())

                // 5. Mode Specific Action
                if (activeMode == "Sleeping") {
                    // Mute for normal calls, boost volume if caller retried or flagged emergency
                } else if (activeMode == "College") {
                    vibratePhone(context)
                }
            }
        }
    }

    private fun sendAutoSms(phoneNumber: String, message: String) {
        try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d("AmoraCallReceiver", "Auto SMS sent successfully to $phoneNumber")
        } catch (e: Exception) {
            Log.e("AmoraCallReceiver", "Failed to send auto SMS: ${e.message}")
        }
    }

    private fun vibratePhone(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                @Suppress("DEPRECATION")
                vibrator.vibrate(1000)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
