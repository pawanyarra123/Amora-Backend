package com.amora.companion.core.assistant.intent

/**
 * Strongly typed intents produced by IntentEngine.
 * Eliminates arbitrary command execution and ensures safe validation.
 */
sealed class AssistantIntent {
    data class OpenApp(val appName: String, val packageName: String? = null) : AssistantIntent()
    data class SetFlashlight(val enabled: Boolean) : AssistantIntent()
    data class SetVolume(val percent: Int) : AssistantIntent()
    data class AdjustVolume(val deltaPercent: Int) : AssistantIntent()
    data class ToggleWifi(val enabled: Boolean? = null) : AssistantIntent()
    data class ToggleBluetooth(val enabled: Boolean? = null) : AssistantIntent()
    object OpenWifiSettings : AssistantIntent()
    object OpenBluetoothSettings : AssistantIntent()
    data class SetAlarm(val hour: Int, val minute: Int, val message: String? = null) : AssistantIntent()
    data class SetTimer(val seconds: Int, val message: String? = null) : AssistantIntent()
    data class CallContact(val contactName: String, val phoneNumber: String? = null) : AssistantIntent()
    data class SendSms(val recipient: String, val messageBody: String) : AssistantIntent()
    data class SendWhatsAppMessage(val recipient: String, val messageBody: String) : AssistantIntent()
    object MediaPlay : AssistantIntent()
    object MediaPause : AssistantIntent()
    object MediaNext : AssistantIntent()
    object MediaPrevious : AssistantIntent()
    object GetTime : AssistantIntent()
    object GetDate : AssistantIntent()
    data class GetWeather(val location: String? = null) : AssistantIntent()
    object StopSpeaking : AssistantIntent()
    /** User said "bye" / "exit" / "goodbye" etc. — ends the whole listening session. */
    object EndSession : AssistantIntent()
    data class Unknown(val rawText: String) : AssistantIntent()
}
