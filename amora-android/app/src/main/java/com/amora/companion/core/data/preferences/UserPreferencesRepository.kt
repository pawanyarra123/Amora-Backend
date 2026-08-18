package com.amora.companion.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

object AmoraDataStoreHolder {
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "amora_preferences")
}

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = with(AmoraDataStoreHolder) { context.dataStore }

    private object Keys {
        val BACKEND_URL = stringPreferencesKey("backend_url")
        val MASTER_SWITCH_ON = booleanPreferencesKey("master_switch_on")
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val CUSTOM_WALLPAPER_URI = stringPreferencesKey("custom_wallpaper_uri")
        val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
        val WAKE_PHRASE = stringPreferencesKey("wake_phrase")
        val PORCUPINE_API_KEY = stringPreferencesKey("porcupine_api_key")
        val CALL_ASSISTANT_ENABLED = booleanPreferencesKey("call_assistant_enabled")
        val CALL_ASSISTANT_MODE = stringPreferencesKey("call_assistant_mode")
        val PINNED_SHORTCUTS_JSON = stringPreferencesKey("pinned_shortcuts_json")
        val CALL_SCREENING_LOGS_JSON = stringPreferencesKey("call_screening_logs_json")
    }

    val backendUrl: Flow<String> = dataStore.data.map { prefs ->
        sanitizeUrl(prefs[Keys.BACKEND_URL] ?: "http://10.0.2.2:8000")
    }

    val isMasterSwitchOn: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.MASTER_SWITCH_ON] ?: false
    }

    val themePreset: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.THEME_PRESET] ?: "Cyberpunk Neon"
    }

    val customWallpaperUri: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_WALLPAPER_URI] ?: ""
    }

    val selectedLanguage: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.SELECTED_LANGUAGE] ?: "en"
    }

    val wakePhrase: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.WAKE_PHRASE] ?: "Hey Amora"
    }

    val porcupineApiKey: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.PORCUPINE_API_KEY] ?: ""
    }

    val isCallAssistantEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.CALL_ASSISTANT_ENABLED] ?: false
    }

    val callAssistantMode: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.CALL_ASSISTANT_MODE] ?: "Meeting"
    }

    val pinnedShortcutsJson: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.PINNED_SHORTCUTS_JSON] ?: ""
    }

    val callScreeningLogsJson: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.CALL_SCREENING_LOGS_JSON] ?: ""
    }

    suspend fun setBackendUrl(rawUrl: String) {
        val formatted = sanitizeUrl(rawUrl)
        dataStore.edit { prefs -> prefs[Keys.BACKEND_URL] = formatted }
    }

    suspend fun setMasterSwitchOn(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.MASTER_SWITCH_ON] = enabled }
    }

    suspend fun setThemePreset(theme: String) {
        dataStore.edit { prefs -> prefs[Keys.THEME_PRESET] = theme }
    }

    suspend fun setCustomWallpaperUri(uri: String) {
        dataStore.edit { prefs -> prefs[Keys.CUSTOM_WALLPAPER_URI] = uri }
    }

    suspend fun setSelectedLanguage(lang: String) {
        dataStore.edit { prefs -> prefs[Keys.SELECTED_LANGUAGE] = lang }
    }

    suspend fun setPorcupineApiKey(key: String) {
        dataStore.edit { prefs -> prefs[Keys.PORCUPINE_API_KEY] = key }
    }

    suspend fun setCallAssistantEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.CALL_ASSISTANT_ENABLED] = enabled }
    }

    suspend fun setCallAssistantMode(mode: String) {
        dataStore.edit { prefs -> prefs[Keys.CALL_ASSISTANT_MODE] = mode }
    }

    suspend fun savePinnedShortcutsJson(json: String) {
        dataStore.edit { prefs -> prefs[Keys.PINNED_SHORTCUTS_JSON] = json }
    }

    suspend fun saveCallScreeningLogsJson(json: String) {
        dataStore.edit { prefs -> prefs[Keys.CALL_SCREENING_LOGS_JSON] = json }
    }

    suspend fun wipeAllPreferences() {
        dataStore.edit { prefs -> prefs.clear() }
    }

    private fun sanitizeUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "http://10.0.2.2:8000"

        var formatted = trimmed
        if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
            // A local/LAN backend (the common case — your own PC on the same Wi-Fi)
            // almost never has TLS. Defaulting to https:// here silently turned any
            // scheme-less address (e.g. "192.168.1.5:8000") into an https:// request
            // that would fail against a plain HTTP dev server. ngrok/Railway URLs
            // already come with an explicit "https://" from their own dashboards,
            // so they're unaffected by this default.
            formatted = "http://$formatted"
        }
        if (!formatted.endsWith("/")) {
            formatted = "$formatted/"
        }
        return formatted
    }
}
