package com.amora.companion.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amora.companion.core.data.network.AmoraApiService
import com.amora.companion.core.data.preferences.UserPreferencesRepository
import com.amora.companion.core.system.master.MasterSwitchManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val masterSwitchManager: MasterSwitchManager,
    private val apiService: AmoraApiService
) : ViewModel() {

    val isMasterSwitchOn = masterSwitchManager.isMasterSwitchOn
    val themePreset = preferencesRepository.themePreset
    val customWallpaperUri = preferencesRepository.customWallpaperUri

    private val _backendUrl = MutableStateFlow("http://10.0.2.2:8000")
    val backendUrl: StateFlow<String> = _backendUrl

    init {
        viewModelScope.launch {
            preferencesRepository.backendUrl.collect { _backendUrl.value = it }
        }
    }

    fun toggleMasterSwitch(enabled: Boolean) {
        viewModelScope.launch {
            masterSwitchManager.setMasterSwitch(enabled)
        }
    }

    fun setThemePreset(theme: String) {
        viewModelScope.launch {
            preferencesRepository.setThemePreset(theme)
        }
    }

    fun setCustomWallpaperUri(uri: String) {
        viewModelScope.launch {
            preferencesRepository.setCustomWallpaperUri(uri)
        }
    }

    fun updateBackendUrl(url: String) {
        viewModelScope.launch {
            preferencesRepository.setBackendUrl(url)
        }
    }

    /** Actually hits the backend's health endpoint instead of assuming success. */
    suspend fun testBackendConnection(): Pair<Boolean, String> {
        return try {
            val response = apiService.checkHealth()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Pair(true, "Connected — Groq: ${if (body.groq_api_valid) "OK" else "invalid key"}, DB: ${if (body.database_connected) "OK" else "not connected"}")
                } else {
                    Pair(true, "Connected (empty health response)")
                }
            } else {
                Pair(false, "Backend responded with HTTP ${response.code()} — check the URL and that it's running")
            }
        } catch (e: java.net.ConnectException) {
            Pair(false, "Couldn't connect — is the backend running and reachable on your network?")
        } catch (e: java.net.SocketTimeoutException) {
            Pair(false, "Connection timed out — check the address and your Wi-Fi/firewall")
        } catch (e: Exception) {
            Pair(false, "Connection failed: ${e.message ?: e::class.simpleName}")
        }
    }

    fun wipeAllData(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                preferencesRepository.wipeAllPreferences()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                apiService.wipeAllData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onSuccess()
        }
    }
}
