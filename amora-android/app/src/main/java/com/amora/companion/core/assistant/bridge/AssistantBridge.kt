package com.amora.companion.core.assistant.bridge

import com.amora.companion.core.assistant.controller.AssistantController
import com.amora.companion.core.assistant.state.AssistantEvent
import com.amora.companion.core.assistant.state.AssistantState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Clean bridge between Kotlin Native Assistant Controller and UI (Compose or Flutter MethodChannel/EventChannel).
 */
@Singleton
class AssistantBridge @Inject constructor(
    private val controller: AssistantController
) {
    val state: StateFlow<AssistantState> = controller.state
    val events: SharedFlow<AssistantEvent> = controller.events

    fun start() = controller.start()
    fun stop() = controller.stop()
    fun triggerManualListening() = controller.triggerManualListening()
    fun speak(text: String) = controller.speakAssistantResponse(text)
    fun stopSpeaking() = controller.stopSpeaking()
}
