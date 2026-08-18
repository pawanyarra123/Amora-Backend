package com.amora.companion.feature.mic

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MicDiagnostic"

/**
 * Standalone microphone diagnostic that works completely independently
 * of the assistant pipeline. Tells you exactly what the mic hears.
 *
 * Usage: inject and call startTest(callback). That's it.
 * No state machine, no wake words, no assistant, no side effects.
 */
@Singleton
class MicDiagnosticManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    interface MicTestCallback {
        fun onStatus(status: String)        // status text for the UI
        fun onRmsLevel(level: Float)        // 0.0 – 1.0 for waveform animation
        fun onWordRecognized(word: String)  // live partial + final results
        fun onDone(success: Boolean, finalText: String) // test complete
    }

    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var running = false

    fun startTest(callback: MicTestCallback) {
        mainHandler.post {
            if (running) {
                callback.onStatus("⚠️ Already testing — tap Stop first")
                return@post
            }

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                callback.onStatus("❌ Speech recognition not available on this device.\nInstall Google app or check Google Play Services.")
                callback.onDone(false, "")
                return@post
            }

            cleanup()
            running = true
            callback.onStatus("🔵 Initialising microphone...")

            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {

                    override fun onReadyForSpeech(params: Bundle?) {
                        callback.onStatus("🟢 Mic is ready — speak anything now!")
                    }

                    override fun onBeginningOfSpeech() {
                        callback.onStatus("🎙️ Sound detected — keep speaking...")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        callback.onRmsLevel(level)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        callback.onStatus("⏳ Processing your speech...")
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty() && matches[0].isNotBlank()) {
                            callback.onWordRecognized("🗣️ Hearing: \"${matches[0]}\"")
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        running = false
                        val matches = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            Log.i(TAG, "Mic test SUCCESS: \"$text\"")
                            callback.onStatus("✅ Mic working! Heard: \"$text\"")
                            callback.onWordRecognized("✅ \"$text\"")
                            callback.onDone(true, text)
                        } else {
                            callback.onStatus("⚠️ Nothing recognized — try speaking more clearly")
                            callback.onDone(false, "")
                        }
                        cleanup()
                    }

                    override fun onError(error: Int) {
                        running = false
                        val (emoji, msg) = when (error) {
                            SpeechRecognizer.ERROR_AUDIO                -> "🔴" to "Audio hardware error — check mic permission"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "🔴" to "RECORD_AUDIO permission missing — grant it in Settings"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY     -> "🟡" to "Mic busy (another app is using it) — close it and retry"
                            SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
                            SpeechRecognizer.ERROR_NETWORK             -> "🟡" to "Network error — check internet (Google Speech needs it)"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT     -> "🟡" to "Network timeout — slow connection"
                            SpeechRecognizer.ERROR_NO_MATCH            -> "🟡" to "Nothing matched — speak louder / clearer"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT      -> "🟡" to "Silence timeout — mic works! Just say something"
                            SpeechRecognizer.ERROR_CLIENT              -> "🟡" to "Client error — retry"
                            SpeechRecognizer.ERROR_SERVER              -> "🟡" to "Google server error — retry in a moment"
                            else                                       -> "🟡" to "Error code $error"
                        }
                        Log.w(TAG, "Mic test error $error: $msg")
                        callback.onStatus("$emoji $msg")
                        callback.onDone(false, "")
                        cleanup()
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val locale = Locale.getDefault()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                // Generous silence windows — no rush during a manual test
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4_000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4_000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
            }

            try {
                recognizer?.startListening(intent)
                Log.i(TAG, "Mic diagnostic started (locale=${locale.toLanguageTag()})")
            } catch (e: Exception) {
                running = false
                callback.onStatus("❌ Failed to open mic: ${e.message}")
                callback.onDone(false, "")
                cleanup()
            }
        }
    }

    fun stopTest() {
        mainHandler.post {
            running = false
            cleanup()
        }
    }

    private fun cleanup() {
        try {
            recognizer?.stopListening()
            recognizer?.destroy()
        } catch (_: Exception) {}
        recognizer = null
    }
}
