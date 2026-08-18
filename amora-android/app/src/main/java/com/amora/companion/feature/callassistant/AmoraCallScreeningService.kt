package com.amora.companion.feature.callassistant

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.SmsManager
import android.util.Log
import androidx.annotation.RequiresApi
import com.amora.companion.core.data.preferences.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.Q)
@AndroidEntryPoint
class AmoraCallScreeningService : CallScreeningService() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    override fun onScreenCall(callDetails: Call.Details) {
        val handle = callDetails.handle
        val rawNumber = handle?.schemeSpecificPart ?: "Unknown Caller"
        Log.d("AmoraCallScreening", "Incoming call from: $rawNumber")

        CoroutineScope(Dispatchers.IO).launch {
            val isEnabled = preferencesRepository.isCallAssistantEnabled.first()
            val activeMode = preferencesRepository.callAssistantMode.first()

            if (!isEnabled) {
                respondToCall(callDetails, CallResponse.Builder().build())
                return@launch
            }

            Log.d("AmoraCallScreening", "Screening active call under mode: $activeMode")

            // Silence ringer instantly
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            try {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Build mode speech message & SMS text
            val modeSpeech = when (activeMode) {
                "Meeting" -> "The user is currently busy in a meeting. Is this call an emergency or important?"
                "College" -> "The user is currently attending class in college. Is this call an emergency or important?"
                "Sleeping" -> "The user is currently sleeping. Is this call an emergency or important?"
                else -> "The user is currently unavailable. Is this call an emergency or important?"
            }

            // Send Auto SMS to caller
            if (rawNumber.length > 3 && rawNumber != "Unknown Caller") {
                sendSmsResponse(rawNumber, modeSpeech)
            }

            // Log Call to DataStore for Home Dashboard Notification
            val currentLogsStr = preferencesRepository.callScreeningLogsJson.first()
            val jsonArray = if (currentLogsStr.isNotEmpty()) JSONArray(currentLogsStr) else JSONArray()

            val newLog = JSONObject().apply {
                put("id", System.currentTimeMillis().toString())
                put("caller", rawNumber)
                put("timeAgo", "Just now")
                put("mode", activeMode)
                put("isEmergency", false)
                put("reason", modeSpeech)
            }
            jsonArray.put(newLog)
            preferencesRepository.saveCallScreeningLogsJson(jsonArray.toString())

            // Execute Mode Screening Action
            if (activeMode == "College") {
                vibrateDeviceContinuous()
            }

            // Screen call with silenced ringer
            val response = CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSilenceCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()

            respondToCall(callDetails, response)
        }
    }

    private fun sendSmsResponse(number: String, message: String) {
        try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(number, null, message, null, null)
        } catch (e: Exception) {
            Log.e("AmoraCallScreening", "Failed to send SMS: ${e.message}")
        }
    }

    private fun vibrateDeviceContinuous() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                @Suppress("DEPRECATION")
                vibrator.vibrate(2000)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
