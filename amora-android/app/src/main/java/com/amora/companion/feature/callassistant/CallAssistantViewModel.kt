package com.amora.companion.feature.callassistant

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amora.companion.core.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

data class CallScreeningLog(
    val id: String,
    val callerName: String,
    val timeAgo: String,
    val mode: String,
    val isEmergency: Boolean,
    val reason: String
)

@HiltViewModel
class CallAssistantViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val isEnabled: Flow<Boolean> = preferencesRepository.isCallAssistantEnabled
    val activeMode: Flow<String> = preferencesRepository.callAssistantMode

    private val _logs = MutableStateFlow<List<CallScreeningLog>>(emptyList())
    val logs: StateFlow<List<CallScreeningLog>> = _logs

    private val _emergencyAlert = MutableStateFlow<CallScreeningLog?>(null)
    val emergencyAlert: StateFlow<CallScreeningLog?> = _emergencyAlert

    init {
        observeLogs()
    }

    fun toggleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setCallAssistantEnabled(enabled)
        }
    }

    fun selectMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.setCallAssistantMode(mode)
        }
    }

    fun simulateCall(caller: String, mode: String, isEmergency: Boolean, reason: String) {
        viewModelScope.launch {
            val logItem = CallScreeningLog(
                id = System.currentTimeMillis().toString(),
                callerName = caller,
                timeAgo = "Just now",
                mode = mode,
                isEmergency = isEmergency,
                reason = reason
            )

            // Append to DataStore log
            val currentList = _logs.value.toMutableList()
            currentList.add(0, logItem)

            val jsonArray = JSONArray()
            for (item in currentList) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("caller", item.callerName)
                obj.put("timeAgo", item.timeAgo)
                obj.put("mode", item.mode)
                obj.put("isEmergency", item.isEmergency)
                obj.put("reason", item.reason)
                jsonArray.put(obj)
            }
            preferencesRepository.saveCallScreeningLogsJson(jsonArray.toString())

            // Emergency Handling Logic
            if (isEmergency) {
                _emergencyAlert.value = logItem

                if (mode == "Sleeping") {
                    // Max out volume for emergency
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
                }

                // Vibrate device for emergency
                vibrateDevice()
            }
        }
    }

    fun dismissEmergencyAlert() {
        _emergencyAlert.value = null
    }

    private fun vibrateDevice() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(1500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                @Suppress("DEPRECATION")
                vibrator.vibrate(1500)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun observeLogs() {
        viewModelScope.launch {
            preferencesRepository.callScreeningLogsJson.map { jsonStr ->
                if (jsonStr.isEmpty()) {
                    listOf(
                        CallScreeningLog("1", "Mom", "10m ago", "Sleeping", true, "Emergency: Need car keys"),
                        CallScreeningLog("2", "Alex", "1h ago", "Meeting", false, "Wanted to ask about project presentation")
                    )
                } else {
                    try {
                        val array = JSONArray(jsonStr)
                        val list = mutableListOf<CallScreeningLog>()
                        for (i in (array.length() - 1) downTo 0) {
                            val obj = array.getJSONObject(i)
                            list.add(
                                CallScreeningLog(
                                    id = obj.optString("id", i.toString()),
                                    callerName = obj.optString("caller", "Unknown"),
                                    timeAgo = obj.optString("timeAgo", "Recently"),
                                    mode = obj.optString("mode", "Meeting"),
                                    isEmergency = obj.optBoolean("isEmergency", false),
                                    reason = obj.optString("reason", "No reason provided")
                                )
                            )
                        }
                        list
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }.collect { list ->
                _logs.value = list
            }
        }
    }
}
