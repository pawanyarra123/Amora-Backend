package com.amora.companion.core.assistant.state

/**
 * Deterministic states for the Amora Voice Assistant State Machine.
 */
enum class AssistantState {
    /** Engine stopped or master switch OFF. No audio resources held. */
    IDLE,

    /** Low-power passive wake-word monitoring active. */
    WAKE_WORD_LISTENING,

    /** "Hey Amora" detected; audio prompt/chime or visual trigger fired. */
    WAKE_WORD_DETECTED,

    /** Active speech-to-text listening for the user's command. */
    COMMAND_LISTENING,

    /** Speech captured; intent classification and parsing in progress. */
    PROCESSING,

    /** Performing native Android action or calling remote AI backend. */
    EXECUTING,

    /** Text-to-speech output actively speaking response to user. */
    SPEAKING,

    /** Recoverable error state (re-arms to WAKE_WORD_LISTENING after cooldown). */
    ERROR
}
