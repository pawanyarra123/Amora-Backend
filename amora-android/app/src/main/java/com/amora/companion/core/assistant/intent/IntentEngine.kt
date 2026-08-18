package com.amora.companion.core.assistant.intent

import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntentEngine @Inject constructor() : IIntentEngine {

    override suspend fun parseIntent(speechText: String): AssistantIntent {
        val trimmed = speechText.trim().lowercase()
        if (trimmed.isBlank()) return AssistantIntent.Unknown("")

        // 1. Session-ending commands
        val endSessionPhrases = listOf(
            "bye", "goodbye", "good bye", "exit", "quit", "stop assistant",
            "stop the assistant", "turn off assistant", "go to sleep",
            "that's all", "thats all", "that will be all", "shut down assistant",
            "see you later", "talk to you later"
        )
        if (trimmed in endSessionPhrases || endSessionPhrases.any { trimmed == it || trimmed.startsWith("$it ") || trimmed.endsWith(" $it") }) {
            return AssistantIntent.EndSession
        }

        // 2. Interruption & Stop commands
        if (trimmed in listOf("stop", "cancel", "shut up", "be quiet", "stop talking", "pause")) {
            return AssistantIntent.StopSpeaking
        }

        // 3. WhatsApp & Messaging Actions (Checked FIRST to avoid OpenApp interference)
        // Pattern 1: "open whatsapp and send (a)? message to <recipient> (that|saying|with) <body>"
        val openWaAndSendMatcher = Pattern.compile(
            "\\b(?:open\\s+whatsapp\\s+(?:and\\s+)?(?:then\\s+)?send\\s+(?:a\\s+)?(?:message\\s+)?to\\s+)(.+?)(?:\\s+(?:that|saying|with|telling)\\s+)(.+)$"
        ).matcher(trimmed)
        if (openWaAndSendMatcher.find()) {
            val recipient = openWaAndSendMatcher.group(1)?.trim() ?: ""
            val body = openWaAndSendMatcher.group(2)?.trim() ?: ""
            if (recipient.isNotBlank() && body.isNotBlank()) {
                return AssistantIntent.SendWhatsAppMessage(recipient = recipient, messageBody = body)
            }
        }

        // Pattern 2: "send (a)? (whatsapp)? message to <recipient> (that|saying|with) <body>"
        val waSendMatcher = Pattern.compile(
            "\\b(?:send\\s+(?:a\\s+)?(?:whatsapp\\s+)?(?:message\\s+)?to\\s+)(.+?)(?:\\s+(?:that|saying|with|telling)\\s+)(.+)$"
        ).matcher(trimmed)
        if (waSendMatcher.find()) {
            val recipient = waSendMatcher.group(1)?.trim() ?: ""
            val body = waSendMatcher.group(2)?.trim() ?: ""
            if (recipient.isNotBlank() && body.isNotBlank()) {
                val isExplicitSms = trimmed.contains("sms") || trimmed.contains("text message")
                return if (isExplicitSms) {
                    AssistantIntent.SendSms(recipient = recipient, messageBody = body)
                } else {
                    AssistantIntent.SendWhatsAppMessage(recipient = recipient, messageBody = body)
                }
            }
        }

        // Pattern 3: "whatsapp <recipient> (that|saying) <body>" or "tell <recipient> (that|saying) <body>"
        val quickWaMatcher = Pattern.compile(
            "\\b(?:whatsapp|tell|message)\\s+([a-zA-Z0-9\\s]+?)(?:\\s+(?:that|saying)\\s+)(.+)$"
        ).matcher(trimmed)
        if (quickWaMatcher.find()) {
            val recipient = quickWaMatcher.group(1)?.trim() ?: ""
            val body = quickWaMatcher.group(2)?.trim() ?: ""
            if (recipient.isNotBlank() && body.isNotBlank()) {
                return AssistantIntent.SendWhatsAppMessage(recipient = recipient, messageBody = body)
            }
        }

        // 4. Phone Calls (Checked BEFORE OpenApp to handle "make a call to mommy", "call to mom", etc.)
        val callMatcher = Pattern.compile(
            "\\b(?:make\\s+(?:a\\s+)?call\\s+to|call\\s+to|call|dial|phone|ring)\\s+([a-zA-Z0-9\\s]+?)(?:\\s+on\\s+phone|\\s+directly)?$"
        ).matcher(trimmed)
        if (callMatcher.find()) {
            val contact = callMatcher.group(1)?.trim() ?: ""
            if (contact.isNotBlank() && contact !in listOf("amora", "assistant", "app", "help", "settings")) {
                return AssistantIntent.CallContact(contactName = contact)
            }
        }

        // 5. Flashlight commands
        if (Pattern.compile("\\b(turn on|enable|open)\\s+(the\\s+)?(torch|flashlight)\\b").matcher(trimmed).find()) {
            return AssistantIntent.SetFlashlight(true)
        }
        if (Pattern.compile("\\b(turn off|disable|close)\\s+(the\\s+)?(torch|flashlight)\\b").matcher(trimmed).find()) {
            return AssistantIntent.SetFlashlight(false)
        }

        // 6. Volume controls
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

        // 7. Wi-Fi & Bluetooth direct toggling
        if (Pattern.compile("\\b(turn on|enable|start)\\s+(the\\s+)?wi-?fi\\b").matcher(trimmed).find()) {
            return AssistantIntent.ToggleWifi(true)
        }
        if (Pattern.compile("\\b(turn off|disable|stop)\\s+(the\\s+)?wi-?fi\\b").matcher(trimmed).find()) {
            return AssistantIntent.ToggleWifi(false)
        }
        if (Pattern.compile("\\b(toggle|switch)\\s+(the\\s+)?wi-?fi\\b").matcher(trimmed).find()) {
            return AssistantIntent.ToggleWifi(null)
        }
        if (trimmed in listOf("wifi settings", "wi-fi settings", "open wifi", "open wi-fi", "open wifi settings", "show wifi settings", "show wifi")) {
            return AssistantIntent.OpenWifiSettings
        }

        if (Pattern.compile("\\b(turn on|enable|start)\\s+(the\\s+)?bluetooth\\b").matcher(trimmed).find()) {
            return AssistantIntent.ToggleBluetooth(true)
        }
        if (Pattern.compile("\\b(turn off|disable|stop)\\s+(the\\s+)?bluetooth\\b").matcher(trimmed).find()) {
            return AssistantIntent.ToggleBluetooth(false)
        }
        if (Pattern.compile("\\b(toggle|switch)\\s+(the\\s+)?bluetooth\\b").matcher(trimmed).find()) {
            return AssistantIntent.ToggleBluetooth(null)
        }
        if (trimmed in listOf("bluetooth settings", "open bluetooth", "open bluetooth settings", "show bluetooth settings", "show bluetooth")) {
            return AssistantIntent.OpenBluetoothSettings
        }

        // 8. Open Apps
        val openAppMatcher = Pattern.compile("\\b(open|launch|start)\\s+(the\\s+)?([a-zA-Z0-9\\s]+?)(?:\\s+app)?$").matcher(trimmed)
        if (openAppMatcher.find()) {
            val rawApp = openAppMatcher.group(3)?.trim()
            if (!rawApp.isNullOrBlank() && rawApp !in listOf("flashlight", "torch", "wifi", "wi-fi", "bluetooth", "alarm", "timer", "camera")) {
                return AssistantIntent.OpenApp(appName = rawApp)
            }
        }

        // 9. Alarms & Timers
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

        // 10. Time & Date
        if (trimmed.contains("what time") || trimmed.contains("current time") || trimmed == "time") {
            return AssistantIntent.GetTime
        }
        if (trimmed.contains("what date") || trimmed.contains("today's date") || trimmed.contains("what is the date")) {
            return AssistantIntent.GetDate
        }

        // 11. Weather
        if (trimmed.contains("weather") || trimmed.contains("temperature") || trimmed.contains("forecast")) {
            val locMatcher = Pattern.compile("\\bweather\\s+in\\s+([a-zA-Z\\s]+)").matcher(trimmed)
            val loc = if (locMatcher.find()) locMatcher.group(1)?.trim() else null
            return AssistantIntent.GetWeather(loc)
        }

        // 12. Fallback SMS Matcher
        val smsMatcher = Pattern.compile("\\b(send\\s+(a\\s+)?(message|sms)\\s+to\\s+)([a-zA-Z0-9\\s]+?)(?:\\s+saying\\s+(.+))?$").matcher(trimmed)
        if (smsMatcher.find()) {
            val recipient = smsMatcher.group(4)?.trim() ?: ""
            val body = smsMatcher.group(5)?.trim() ?: "Hello"
            if (recipient.isNotBlank()) {
                return AssistantIntent.SendSms(recipient = recipient, messageBody = body)
            }
        }

        // 13. Media Controls
        if (trimmed in listOf("play music", "play", "resume music", "resume")) return AssistantIntent.MediaPlay
        if (trimmed in listOf("pause music", "pause song", "stop music")) return AssistantIntent.MediaPause
        if (trimmed in listOf("next song", "next track", "skip song")) return AssistantIntent.MediaNext
        if (trimmed in listOf("previous song", "previous track")) return AssistantIntent.MediaPrevious

        return AssistantIntent.Unknown(rawText = speechText)
    }
}
