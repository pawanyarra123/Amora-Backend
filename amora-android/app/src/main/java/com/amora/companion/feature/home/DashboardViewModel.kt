package com.amora.companion.feature.home

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amora.companion.core.assistant.bridge.AssistantBridge
import com.amora.companion.core.assistant.state.AssistantEvent
import com.amora.companion.core.assistant.state.AssistantState
import com.amora.companion.core.data.network.AmoraApiService
import com.amora.companion.core.data.network.ChatRequest
import com.amora.companion.core.data.network.NewsArticle
import com.amora.companion.core.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

data class DashboardUiState(
    val greeting: String = "Good Evening",
    val batteryPercent: Int = 85,
    val storageUsageGb: String = "Calculating...",
    val ramUsageGb: String = "Calculating...",
    val backendStatus: String = "Checking...",
    val weather: String = "28°C Clear",
    val newsFeed: List<NewsArticle> = emptyList(),
    val recentActivity: List<String> = listOf("Set DND mode", "Polished message grammar", "Captured intruder attempt"),
    val activeMode: String = "Meeting",
    val masterSwitchOn: Boolean = true,
    val persistentShortcuts: List<AppShortcut> = emptyList(),
    val shortcutsLoaded: Boolean = false,
    val latestScreenedCall: String = "",
    val isTestingVoice: Boolean = false,
    val micRmsLevel: Float = 0f,
    val voiceDiagnosticStatus: String = "Ready to test voice input",
    val lastRecognizedCommand: String = ""
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: AmoraApiService,
    private val preferencesRepository: UserPreferencesRepository,
    private val assistantBridge: AssistantBridge
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        loadRealDeviceMetrics()
        checkBackendHealth()
        fetchWeather()
        fetchNews()
        observePreferences()
        observeAssistantBridge()
    }

    private fun observeAssistantBridge() {
        viewModelScope.launch {
            assistantBridge.state.collect { state ->
                when (state) {
                    AssistantState.COMMAND_LISTENING -> {
                        _uiState.value = _uiState.value.copy(
                            isTestingVoice = true,
                            voiceDiagnosticStatus = "🎙️ Listening... Speak now (say 'turn on flashlight' or 'what's the weather')",
                            lastRecognizedCommand = ""
                        )
                    }
                    AssistantState.PROCESSING, AssistantState.EXECUTING -> {
                        _uiState.value = _uiState.value.copy(
                            isTestingVoice = false,
                            voiceDiagnosticStatus = "⏳ Processing speech..."
                        )
                    }
                    AssistantState.SPEAKING -> {
                        _uiState.value = _uiState.value.copy(
                            isTestingVoice = false,
                            voiceDiagnosticStatus = "🗣️ Amora responding..."
                        )
                    }
                    AssistantState.WAKE_WORD_LISTENING -> {
                        _uiState.value = _uiState.value.copy(
                            isTestingVoice = false,
                            voiceDiagnosticStatus = "🟢 Assistant Active (Say 'Hey Amora' or Tap Test)"
                        )
                    }
                    AssistantState.IDLE -> {
                        _uiState.value = _uiState.value.copy(
                            isTestingVoice = false,
                            voiceDiagnosticStatus = "Ready to test voice input"
                        )
                    }
                    AssistantState.ERROR -> {
                        _uiState.value = _uiState.value.copy(
                            isTestingVoice = false,
                            voiceDiagnosticStatus = "🟢 Ready to test voice input"
                        )
                    }
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            assistantBridge.events.collect { event ->
                when (event) {
                    is AssistantEvent.PartialSpeech -> {
                        _uiState.value = _uiState.value.copy(
                            lastRecognizedCommand = event.transcript
                        )
                    }
                    is AssistantEvent.FinalSpeech -> {
                        _uiState.value = _uiState.value.copy(
                            lastRecognizedCommand = event.transcript,
                            voiceDiagnosticStatus = "✅ Voice Captured: \"${event.transcript}\""
                        )
                    }
                    is AssistantEvent.ActionCompleted -> {
                        _uiState.value = _uiState.value.copy(
                            voiceDiagnosticStatus = "✅ ${event.resultMessage}"
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun startVoiceDiagnosticTest() {
        _uiState.value = _uiState.value.copy(
            isTestingVoice = true,
            micRmsLevel = 0.2f,
            voiceDiagnosticStatus = "🎙️ Listening... Speak now",
            lastRecognizedCommand = ""
        )
        assistantBridge.triggerManualListening()
    }

    fun stopVoiceDiagnosticTest() {
        assistantBridge.stopSpeaking()
        _uiState.value = _uiState.value.copy(
            isTestingVoice = false,
            micRmsLevel = 0f,
            voiceDiagnosticStatus = "Test stopped"
        )
    }

    override fun onCleared() {
        super.onCleared()
    }

    fun loadRealDeviceMetrics() {
        val batteryLevel = getRealBatteryLevel()
        val ramStats = getRealRamUsage()
        val storageStats = getRealStorageUsage()

        _uiState.value = _uiState.value.copy(
            batteryPercent = batteryLevel,
            ramUsageGb = ramStats,
            storageUsageGb = storageStats
        )
    }

    private fun getRealBatteryLevel(): Int {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            85
        }
    }

    private fun getRealRamUsage(): String {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)

            val totalGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
            val availGb = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)
            val usedGb = totalGb - availGb

            String.format("%.1f / %.1f GB", usedGb, totalGb)
        } catch (e: Exception) {
            "4.0 / 8.0 GB"
        }
    }

    private fun getRealStorageUsage(): String {
        return try {
            val path = Environment.getDataDirectory().path
            val stat = StatFs(path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalGb = (totalBlocks * blockSize) / (1024.0 * 1024.0 * 1024.0)
            val freeGb = (availableBlocks * blockSize) / (1024.0 * 1024.0 * 1024.0)
            val usedGb = totalGb - freeGb

            String.format("%.0f / %.0f GB", usedGb, totalGb)
        } catch (e: Exception) {
            "45 / 128 GB"
        }
    }

    fun checkBackendHealth() {
        viewModelScope.launch {
            try {
                val response = apiService.checkHealth()
                if (response.isSuccessful && response.body() != null) {
                    val status = response.body()!!.status
                    _uiState.value = _uiState.value.copy(backendStatus = status)
                } else {
                    _uiState.value = _uiState.value.copy(backendStatus = "Not Connected (HTTP ${response.code()})")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(backendStatus = "Not Connected (${e.javaClass.simpleName})")
            }
        }
    }

    fun fetchWeather() {
        viewModelScope.launch {
            try {
                // Dashboard just wants a quick glanceable summary, not a specific city —
                // matches the backend's own default coordinates (see /v1/weather).
                val response = apiService.getWeather(lat = 13.0827, lon = 80.2707, city = "Chennai")
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = _uiState.value.copy(weather = response.body()!!.display_text)
                }
            } catch (e: Exception) {
                Log.e("AmoraWeather", "Weather fetch exception: ${e.message}")
            }
        }
    }

    fun fetchNews() {
        viewModelScope.launch {
            try {
                val response = apiService.getNews()
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = _uiState.value.copy(newsFeed = response.body()!!.articles)
                }
            } catch (e: Exception) {
                Log.e("AmoraNews", "News fetch exception: ${e.message}")
            }
        }
    }

    fun saveShortcuts(shortcuts: List<AppShortcut>) {
        val listCopy = shortcuts.toList()
        _uiState.value = _uiState.value.copy(persistentShortcuts = listCopy, shortcutsLoaded = true)
        viewModelScope.launch(Dispatchers.IO) {
            val jsonArray = JSONArray()
            for (sc in listCopy) {
                val obj = JSONObject()
                obj.put("name", sc.name)
                obj.put("icon", sc.icon)
                obj.put("urlOrPackage", sc.urlOrPackage)
                jsonArray.put(obj)
            }
            preferencesRepository.savePinnedShortcutsJson(jsonArray.toString())
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.isMasterSwitchOn.collect { isOn ->
                _uiState.value = _uiState.value.copy(masterSwitchOn = isOn)
            }
        }
        viewModelScope.launch {
            preferencesRepository.callAssistantMode.collect { mode ->
                _uiState.value = _uiState.value.copy(activeMode = mode)
            }
        }
        viewModelScope.launch {
            preferencesRepository.pinnedShortcutsJson.collect { jsonStr ->
                if (jsonStr.isEmpty()) {
                    val defaultList = listOf(
                        AppShortcut("Chrome", "🌐", "https://google.com"),
                        AppShortcut("WhatsApp", "💬", "https://web.whatsapp.com"),
                        AppShortcut("YouTube", "▶️", "https://youtube.com"),
                        AppShortcut("ChatGPT", "🤖", "https://chatgpt.com"),
                        AppShortcut("GitHub", "🐙", "https://github.com"),
                        AppShortcut("Wikipedia", "📚", "https://wikipedia.org")
                    )
                    saveShortcuts(defaultList)
                } else {
                    try {
                        val array = JSONArray(jsonStr)
                        val list = mutableListOf<AppShortcut>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            list.add(
                                AppShortcut(
                                    name = obj.getString("name"),
                                    icon = obj.getString("icon"),
                                    urlOrPackage = obj.getString("urlOrPackage")
                                )
                            )
                        }
                        _uiState.value = _uiState.value.copy(persistentShortcuts = list, shortcutsLoaded = true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        viewModelScope.launch {
            preferencesRepository.callScreeningLogsJson.collect { jsonStr ->
                if (jsonStr.isNotEmpty()) {
                    try {
                        val array = JSONArray(jsonStr)
                        if (array.length() > 0) {
                            val lastObj = array.getJSONObject(array.length() - 1)
                            val caller = lastObj.optString("caller", "Unknown")
                            val reason = lastObj.optString("reason", "No reason provided")
                            val isEmerg = lastObj.optBoolean("isEmergency", false)
                            if (!isEmerg) {
                                _uiState.value = _uiState.value.copy(
                                    latestScreenedCall = "📞 Screened Call from $caller: \"$reason\""
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
