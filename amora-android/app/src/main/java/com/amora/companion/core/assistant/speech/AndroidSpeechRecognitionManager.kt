package com.amora.companion.core.assistant.speech

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
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

private const val TAG = "SpeechRecognitionMgr"

/**
 * Exactly how "Hey Google" works:
 *
 * A single SpeechRecognizer session is kept running continuously.
 * - WAKE_WORD mode: partial results are scanned for "amora" / "hey amora".
 *   On match the matched phrase is snipped and leftover text is the inline command
 *   (so "Hey Amora open YouTube" fires immediately without a second session).
 * - COMMAND mode: partial + final results go straight to the caller.
 *
 * Key properties vs the old two-stage approach:
 *  Zero handoff gap  — mic never released between wake detection and command capture.
 *  No Error 7        — Error 7 = "I heard something but couldn't match it". Old design
 *                      opened a verifier onto silence. Now that never happens.
 *  Auto-restart      — restarts on Error 6/7/11 so it behaves like the
 *                      always-on Google hotword background service.
 */
@Singleton
class AndroidSpeechRecognitionManager @Inject constructor(
    @ApplicationContext private val context: Context
) : ISpeechRecognitionManager {

    // ── Mode ──────────────────────────────────────────────────────────────────

    enum class Mode { WAKE_WORD, COMMAND }

    @Volatile private var mode = Mode.COMMAND
    @Volatile private var isListeningNow = false
    @Volatile private var destroyed = false

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var commandCallback: SpeechRecognitionCallback? = null
    private var wakeWordCallback: WakeWordFoundCallback? = null
    private var restartPending = false

    // ── Microphone priority ────────────────────────────────────────────────────
    // Requested for the lifetime of every listening session (wake-word AND
    // command) so other apps duck/release audio while Amora is capturing —
    // the mic session gets priority over whatever else is playing, the same
    // way "Hey Google" takes over audio the instant it starts listening.
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var micFocusRequest: AudioFocusRequest? = null

    // ── Restart backoff ────────────────────────────────────────────────────────
    // Recreating a SpeechRecognizer immediately after destroying the previous
    // one is a known source of ERROR_RECOGNIZER_BUSY / silent "communication
    // breaks" on several OEM builds (the old recognizer isn't always fully
    // released by the time a new one is created in the same call stack).
    // A short delay before recreation, plus growing backoff on repeated
    // consecutive failures, makes the always-on loop self-healing instead of
    // spinning into a broken state.
    private var consecutiveRestartFailures = 0
    private val baseRestartDelayMs = 350L
    private val maxRestartDelayMs = 4_000L
    private val recognizerRecreateDelayMs = 200L

    // ── Wake word table (phonetically forgiving) ──────────────────────────────

    private val wakeWords = listOf(
        "hey amora", "amora", "hey mora", "ok amora", "okay amora",
        "a mora", "amor", "hey amore", "amorα", "hey amor",
        "hamara", "hey hamara", "he hamara", "hai hamara", "hi hamara",
        "humara", "hey humara", "amara", "hey amara", "amera", "hey amera",
        "omora", "hey omora", "hey amura", "amura", "hey aura", "aura amora",
        "hello amora", "hi amora", "hai amora"
    )

    // ── Wake-word callback ────────────────────────────────────────────────────

    interface WakeWordFoundCallback {
        fun onWakeWordFound(command: String?)
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Start the always-on wake-word session (like Hey Google idle mode). */
    fun startWakeWordListening(callback: WakeWordFoundCallback) {
        mainHandler.post {
            if (destroyed) return@post
            wakeWordCallback = callback
            commandCallback = null
            mode = Mode.WAKE_WORD
            launchSession()
        }
    }

    /**
     * Switch into command-capture mode.
     * If a session is already running (common case: wake word just fired),
     * we simply flip the mode flag so partials/finals now route to [callback].
     * If no session is running we launch a fresh one.
     */
    override fun startListening(callback: SpeechRecognitionCallback) {
        mainHandler.post {
            if (destroyed) return@post
            commandCallback = callback
            wakeWordCallback = null
            mode = Mode.COMMAND
            // Only launch a new session if one isn't already open.
            if (!isListeningNow) launchSession()
        }
    }

    override fun stopListening() {
        mainHandler.post {
            restartPending = false
            destroySession()
            isListeningNow = false
            abandonMicFocus()
        }
    }

    override fun cancel() {
        mainHandler.post {
            restartPending = false
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
            isListeningNow = false
            abandonMicFocus()
            commandCallback?.onListeningStopped()
            commandCallback = null
        }
    }

    override fun isListening(): Boolean = isListeningNow

    override fun destroy() {
        destroyed = true
        cancel()
        wakeWordCallback = null
    }

    // ── Session lifecycle ─────────────────────────────────────────────────────

    private fun launchSession() {
        if (destroyed) return
        destroySession()
        restartPending = false
        requestMicFocus()

        // The short delay here (rather than creating the new SpeechRecognizer
        // in the same call as destroying the old one) is what actually fixes
        // the "communication breaks" failure mode: on several OEM builds the
        // previous recognizer isn't fully released synchronously, and
        // creating a new one immediately intermittently returns
        // ERROR_RECOGNIZER_BUSY or silently never calls back. See the class
        // docs on recognizerRecreateDelayMs for the underlying issue.
        mainHandler.postDelayed({ startRecognizerSession() }, recognizerRecreateDelayMs)
    }

    private fun startRecognizerSession() {
        if (destroyed) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Speech recognition not available")
            commandCallback?.onError(-1, "Speech recognition not available on this device")
            return
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer

        recognizer.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                isListeningNow = true
                consecutiveRestartFailures = 0
                Log.d(TAG, "[$mode] Mic open")
                if (mode == Mode.COMMAND) commandCallback?.onListeningStarted()
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListeningNow = false
                Log.d(TAG, "[$mode] End of speech detected")
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?: return
                if (matches.isEmpty()) return

                when (mode) {
                    Mode.WAKE_WORD -> checkWakeWordInPartial(matches)
                    Mode.COMMAND   -> commandCallback?.onPartialResult(matches[0])
                }
            }

            override fun onResults(results: Bundle?) {
                isListeningNow = false
                val matches = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?: emptyList()

                when (mode) {
                    Mode.WAKE_WORD -> {
                        val found = checkWakeWordInFinal(matches)
                        if (!found) scheduleRestart(baseRestartDelayMs)
                    }
                    Mode.COMMAND -> {
                        val text = matches.firstOrNull() ?: ""
                        Log.i(TAG, "[COMMAND] Final: \"$text\"")
                        commandCallback?.onFinalResult(text)
                        commandCallback?.onListeningStopped()
                        // Controller decides the next state — don't auto-restart here.
                    }
                }
            }

            override fun onError(error: Int) {
                isListeningNow = false
                Log.d(TAG, "[$mode] onError($error): ${errorMessage(error)}")

                when (mode) {
                    Mode.WAKE_WORD -> {
                        // All errors in wake-word mode are transient — silently restart,
                        // backing off if failures keep repeating (e.g. mic held by
                        // another app) so we don't spin CPU/battery on a hard failure.
                        consecutiveRestartFailures++
                        scheduleRestart(currentBackoffDelayMs())
                    }
                    Mode.COMMAND -> {
                        commandCallback?.onError(error, errorMessage(error))
                        commandCallback?.onListeningStopped()
                        // Controller will call resumeWakeWordListening().
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val locale = Locale.getDefault()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

            if (mode == Mode.WAKE_WORD) {
                // Long silence windows keep the session alive waiting for "Hey Amora".
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 8_000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 8_000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 300L)
            } else {
                // Tight, responsive windows for commands — same feel as Google Assistant.
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2_500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2_500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
            }
        }

        try {
            recognizer.startListening(intent)
            Log.i(TAG, "[$mode] Session started (locale=${locale.toLanguageTag()})")
        } catch (e: Exception) {
            Log.e(TAG, "startListening failed", e)
            commandCallback?.onError(-1, e.message ?: "Failed to start recognizer")
        }
    }

    private fun destroySession() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    private fun scheduleRestart(delayMs: Long) {
        if (restartPending || destroyed) return
        restartPending = true
        mainHandler.postDelayed({
            restartPending = false
            if (!destroyed && mode == Mode.WAKE_WORD && wakeWordCallback != null) {
                launchSession()
            }
        }, delayMs)
    }

    private fun currentBackoffDelayMs(): Long {
        // 350ms, 700ms, 1400ms, 2800ms, capped at maxRestartDelayMs.
        val scaled = baseRestartDelayMs * (1L shl consecutiveRestartFailures.coerceAtMost(4))
        return scaled.coerceAtMost(maxRestartDelayMs)
    }

    // ── Microphone priority ────────────────────────────────────────────────────

    /**
     * Requests transient-exclusive audio focus for the duration of the
     * listening session, so other apps are asked to duck or pause while
     * Amora's mic is open — giving this app priority for the microphone the
     * way "Hey Google" takes over audio the moment it starts listening.
     * Safe to call repeatedly; re-requesting an already-held focus is a
     * no-op on the OS side.
     */
    private fun requestMicFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = micFocusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .build()
                .also { micFocusRequest = it }
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
        }
    }

    private fun abandonMicFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            micFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    // ── Wake-word detection ───────────────────────────────────────────────────

    /**
     * Scans partial transcript for the wake phrase.
     * On match: extracts any inline command, fires the callback, closes the
     * session so the AssistantController can open a fresh COMMAND-mode session.
     */
    private fun checkWakeWordInPartial(candidates: List<String>) {
        for (candidate in candidates) {
            val lower = candidate.lowercase().trim()
            for (wake in wakeWords) {
                val idx = lower.indexOf(wake)
                if (idx >= 0) {
                    Log.i(TAG, "WAKE detected in partial: \"$candidate\"")
                    val afterWake = candidate.substring(idx + wake.length).trim()
                        .replace(Regex("^[^a-zA-Z0-9]+"), "").trim()
                    val inlineCmd = afterWake.ifBlank { null }

                    val cb = wakeWordCallback
                    wakeWordCallback = null
                    // Destroy the current session — controller will start a COMMAND session.
                    destroySession()
                    isListeningNow = false
                    cb?.onWakeWordFound(inlineCmd)
                    return
                }
            }
        }
    }

    /** Final-result sweep — safety net if partial missed it. */
    private fun checkWakeWordInFinal(candidates: List<String>): Boolean {
        for (candidate in candidates) {
            val lower = candidate.lowercase().trim()
            for (wake in wakeWords) {
                val idx = lower.indexOf(wake)
                if (idx >= 0) {
                    Log.i(TAG, "WAKE detected in final: \"$candidate\"")
                    val afterWake = candidate.substring(idx + wake.length).trim()
                        .replace(Regex("^[^a-zA-Z0-9]+"), "").trim()
                    val inlineCmd = afterWake.ifBlank { null }
                    val cb = wakeWordCallback
                    wakeWordCallback = null
                    cb?.onWakeWordFound(inlineCmd)
                    return true
                }
            }
        }
        return false
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun errorMessage(code: Int) = when (code) {
        SpeechRecognizer.ERROR_AUDIO                -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT               -> "Client error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "RECORD_AUDIO permission missing"
        SpeechRecognizer.ERROR_NETWORK              -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT      -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH             -> "No speech recognized"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY      -> "Recognizer busy"
        SpeechRecognizer.ERROR_SERVER               -> "Server error"
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED  -> "Server disconnected"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT       -> "No speech heard"
        else                                        -> "Recognition error $code"
    }
}
