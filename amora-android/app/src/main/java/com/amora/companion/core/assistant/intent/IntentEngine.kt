package com.amora.companion.core.assistant.intent

import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntentEngine @Inject constructor() : IIntentEngine {

    override suspend fun parseIntent(speechText: String): AssistantIntent {
        val trimmed = speechText.trim().lowercase()
        if (trimmed.isBlank()) return AssistantIntent.Unknown("")

        // 1. Session-ending commands — checked first since "stop" alone means
        // "stop talking" but "stop the assistant" / "bye" / "exit" ends the
        // whole wake-word loop, not just the current utterance.
        val endSessionPhrases = listOf(
            "bye", "goodbye", "good bye", "exit", "quit", "stop assistant",
            "stop the assistant", "turn off assistant", "go to sleep",
            "that's all", "thats all", "that will be all", "shut down assistant",
            "see you later", "talk to you later"
        )
        if (trimmed in endSessionPhrases || endSessionPhrases.any { trimmed == it || trimmed.startsWith("$it ") || trimmed.endsWith(" $it") }) {
            return AssistantIntent.EndSession
        }

        // 3. Interruption & Stop commands
        if (trimmed in listOf("stop", "cancel", "shut up", "be quiet", "stop talking", "pause")) {
            return AssistantIntent.StopSpeaking
        }

        // 3. Flashlight commands
        if (Pattern.compile("\\b(turn on|enable|open)\\s+(the\\s+)?(torch|flashlight)\\b").matcher(trimmed).find()) {
            return AssistantIntent.SetFlashlight(true)
        }
        if (Pattern.compile("\\b(turn off|disable|close)\\s+(the\\s+)?(torch|flashlight)\\b").matcher(trimmed).find()) {
            return AssistantIntent.SetFlashlight(false)
        }

        // 4. Volume controls
        val volSetMatcher = Pattern.compile("\\bset\\s+volume\\s+(to\\s+)?(\\d+)\\s*%?\\b").matcher(trimmed)
        if (volSetMatcher.find()) {
            val pct = volSetMatcher.group(2)?.toIntOrNull() ?: 50
            return AssistantIntent.SetVolume(pct.coerceIn(0, 100))
        }
        if (trimmed.contains("volume up") || trimmed.contains("increase volume") || trimmed.contains("turn up volume")) {
            return AssistantIntent.AdjustVolume(15)
        }
        if (trimmed.contains("volume down") || trimmed.contains("decrease volume") || trimmed.contains("turn down volume") || trimmed.contains("lower volume")) {
            return AssistantIntent.AdjustVolume(-15)
        }

        // 5. Wi-Fi & Bluetooth settings
        if (Pattern.compile("\\b(turn on|turn off|open|toggle)\\s+(the\\s+)?wi-?fi\\b").matcher(trimmed).find()) {
            return AssistantIntent.OpenWifiSettings
        }
        if (Pattern.compile("\\b(turn on|turn off|open|toggle)\\s+(the\\s+)?bluetooth\\b").matcher(trimmed).find()) {
            return AssistantIntent.OpenBluetoothSettings
        }

        // 6. Open Apps
        val openAppMatcher = Pattern.compile("\\b(open|launch|start)\\s+(the\\s+)?([a-zA-Z0-9\\s]+?)(?:\\s+app)?$").matcher(trimmed)
        if (openAppMatcher.find()) {
            val rawApp = openAppMatcher.group(3)?.trim()
            if (!rawApp.isNullOrBlank() && rawApp !in listOf("flashlight", "torch", "wifi", "wi-fi", "bluetooth", "alarm", "timer", "camera")) {
                return AssistantIntent.OpenApp(appName = rawApp)
            }
        }

        // 7. Alarms & Timers
        val alarmMatcher = Pattern.compile("\\b(set\\s+(an?\\s+)?alarm\\s+(for|at)\\s+)(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b").matcher(trimmed)
        if (alarmMatcher.find()) {
            var hour = alarmMatcher.group(4)?.toIntOrNull() ?: 7
            val min = alarmMatcher.group(5)?.toIntOrNull() ?: 0
            val amPm = alarmMatcher.group(6)
            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0
            return AssistantIntent.SetAlarm(hour = hour, minute = min)
        }

        val timerMatcher = Pattern.compile("\\b(set\\s+(a\\s+)?timer\\s+(for\\s+)?(\\d+)\\s*(minutes?|seconds?|mins?|secs?))\\b").matcher(trimmed)
        if (timerMatcher.find()) {
            val amount = timerMatcher.group(4)?.toIntOrNull() ?: 1
            val unit = timerMatcher.group(5) ?: "minutes"
            val totalSeconds = if (unit.startsWith("sec")) amount else amount * 60
            return AssistantIntent.SetTimer(seconds = totalSeconds)
        }

        // 8. Time & Date
        if (trimmed.contains("what time") || trimmed.contains("current time") || trimmed == "time") {
            return AssistantIntent.GetTime
        }
        if (trimmed.contains("what date") || trimmed.contains("today's date") || trimmed.contains("what is the date")) {
            return AssistantIntent.GetDate
        }

        // 9. Weather
        if (trimmed.contains("weather") || trimmed.contains("temperature") || trimmed.contains("forecast")) {
            val locMatcher = Pattern.compile("\\bweather\\s+in\\s+([a-zA-Z\\s]+)").matcher(trimmed)
            val loc = if (locMatcher.find()) locMatcher.group(1)?.trim() else null
            return AssistantIntent.GetWeather(loc)
        }

        // 10. Phone Calls
        val callMatcher = Pattern.compile("\\b(call|dial)\\s+([a-zA-Z0-9\\s]+)").matcher(trimmed)
        if (callMatcher.find()) {
            val contact = callMatcher.group(2)?.trim() ?: ""
            if (contact.isNotBlank()) {
                return AssistantIntent.CallContact(contactName = contact)
            }
        }

        // 11. SMS / Messages
        val smsMatcher = Pattern.compile("\\b(send\\s+(a\\s+)?(message|sms)\\s+to\\s+)([a-zA-Z0-9\\s]+?)(?:\\s+saying\\s+(.+))?$").matcher(trimmed)
        if (smsMatcher.find()) {
            val recipient = smsMatcher.group(4)?.trim() ?: ""
            val body = smsMatcher.group(5)?.trim() ?: "Hello"
            if (recipient.isNotBlank()) {
                return AssistantIntent.SendSms(recipient = recipient, messageBody = body)
            }
        }

        // 12. Media Controls
        if (trimmed in listOf("play music", "play", "resume music", "resume")) return AssistantIntent.MediaPlay
        if (trimmed in listOf("pause music", "pause song", "stop music")) return AssistantIntent.MediaPause
        if (trimmed in listOf("next song", "next track", "skip song")) return AssistantIntent.MediaNext
        if (trimmed in listOf("previous song", "previous track")) return AssistantIntent.MediaPrevious

        // 13. No pattern matched — handled by UnknownCommandHandler with a
        // spoken "sorry, I didn't catch that" response. No AI chat fallback:
        // this assistant only executes recognized device commands.
        return AssistantIntent.Unknown(rawText = speechText)
    }
}
