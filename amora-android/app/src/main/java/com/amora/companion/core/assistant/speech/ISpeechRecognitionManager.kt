package com.amora.companion.core.assistant.speech

interface SpeechRecognitionCallback {
    fun onListeningStarted()
    fun onPartialResult(partialTranscript: String)
    fun onFinalResult(finalTranscript: String)
    fun onError(errorCode: Int, errorMessage: String)
    fun onListeningStopped()
}

/**
 * Replaceable speech recognition manager contract.
 */
interface ISpeechRecognitionManager {
    fun startListening(callback: SpeechRecognitionCallback)
    fun stopListening()
    fun cancel()
    fun isListening(): Boolean
    fun destroy()
}
