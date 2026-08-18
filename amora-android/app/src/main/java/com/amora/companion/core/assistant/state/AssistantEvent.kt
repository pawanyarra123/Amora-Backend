package com.amora.companion.core.assistant.state

import com.amora.companion.core.assistant.intent.AssistantIntent

/**
 * Real-time events emitted by the Amora Assistant Engine.
 * Shared with Compose UI and Flutter MethodChannel/EventChannel bridge.
 */
sealed class AssistantEvent {
    object Idle : AssistantEvent()
    object WakeWordListeningStarted : AssistantEvent()
    data class WakeWordDetected(val initialCommand: String? = null) : AssistantEvent()
    object CommandListeningStarted : AssistantEvent()
    data class PartialSpeech(val transcript: String) : AssistantEvent()
    data class FinalSpeech(val transcript: String) : AssistantEvent()
    data class IntentClassified(val intent: AssistantIntent) : AssistantEvent()
    data class ActionStarted(val actionDescription: String) : AssistantEvent()
    data class ActionCompleted(val resultMessage: String) : AssistantEvent()
    data class SpeakingStarted(val text: String) : AssistantEvent()
    object SpeakingCompleted : AssistantEvent()
    data class Error(val code: Int, val message: String) : AssistantEvent()
}
