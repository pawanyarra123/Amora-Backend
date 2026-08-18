package com.amora.companion.core.assistant.speech

interface SpeechOutputCallback {
    fun onSpeakingStarted()
    fun onSpeakingCompleted()
    fun onError(errorMessage: String)
}

/**
 * Replaceable Text-to-Speech output manager contract.
 */
interface ISpeechOutputManager {
    fun initialize(onReady: (Boolean) -> Unit)
    fun speak(text: String, callback: SpeechOutputCallback? = null)
    fun stop()
    fun isSpeaking(): Boolean
    fun shutdown()
}
