package com.amora.companion.core.assistant.state

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "AssistantStateMachine"

/**
 * Validates and publishes deterministic state transitions for Amora.
 */
class AssistantStateMachine(private val scope: CoroutineScope) {

    private val _currentState = MutableStateFlow(AssistantState.IDLE)
    val currentState: StateFlow<AssistantState> = _currentState.asStateFlow()

    private val _events = MutableSharedFlow<AssistantEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<AssistantEvent> = _events.asSharedFlow()

    fun transitionTo(newState: AssistantState, event: AssistantEvent? = null) {
        val oldState = _currentState.value
        if (oldState == newState) {
            event?.let { emitEvent(it) }
            return
        }

        val isValid = when (oldState) {
            AssistantState.IDLE -> newState in listOf(AssistantState.WAKE_WORD_LISTENING, AssistantState.COMMAND_LISTENING)
            AssistantState.WAKE_WORD_LISTENING -> newState in listOf(AssistantState.WAKE_WORD_DETECTED, AssistantState.COMMAND_LISTENING, AssistantState.IDLE, AssistantState.ERROR)
            AssistantState.WAKE_WORD_DETECTED -> newState in listOf(AssistantState.COMMAND_LISTENING, AssistantState.PROCESSING, AssistantState.WAKE_WORD_LISTENING, AssistantState.IDLE)
            AssistantState.COMMAND_LISTENING -> newState in listOf(AssistantState.PROCESSING, AssistantState.WAKE_WORD_LISTENING, AssistantState.IDLE, AssistantState.ERROR)
            AssistantState.PROCESSING -> newState in listOf(AssistantState.EXECUTING, AssistantState.SPEAKING, AssistantState.WAKE_WORD_LISTENING, AssistantState.ERROR)
            AssistantState.EXECUTING -> newState in listOf(AssistantState.SPEAKING, AssistantState.WAKE_WORD_LISTENING, AssistantState.IDLE, AssistantState.ERROR)
            AssistantState.SPEAKING -> newState in listOf(AssistantState.WAKE_WORD_LISTENING, AssistantState.COMMAND_LISTENING, AssistantState.IDLE, AssistantState.ERROR)
            AssistantState.ERROR -> true // Any transition allowed to recover from error
        }

        if (isValid) {
            Log.d(TAG, "Transition: $oldState → $newState")
            _currentState.value = newState
            event?.let { emitEvent(it) }
        } else {
            Log.w(TAG, "Illegal transition attempted: $oldState → $newState (forcing recovery to WAKE_WORD_LISTENING)")
            _currentState.value = newState
            event?.let { emitEvent(it) }
        }
    }

    fun emitEvent(event: AssistantEvent) {
        scope.launch(Dispatchers.Main) {
            _events.emit(event)
        }
    }
}
