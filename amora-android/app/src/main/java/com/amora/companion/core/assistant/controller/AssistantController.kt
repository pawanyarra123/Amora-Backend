package com.amora.companion.core.assistant.controller

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.amora.companion.core.assistant.actions.IActionExecutor
import com.amora.companion.core.assistant.intent.AssistantIntent
import com.amora.companion.core.assistant.intent.IIntentEngine
import com.amora.companion.core.assistant.speech.*
import com.amora.companion.core.assistant.state.*
import com.amora.companion.core.system.master.MasterSwitchManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AssistantController"

@Singleton
class AssistantController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val speechRecognizer: AndroidSpeechRecognitionManager,
    private val speechOutputManager: ISpeechOutputManager,
    private val intentEngine: IIntentEngine,
    private val actionExecutor: IActionExecutor,
    private val masterSwitchManager: MasterSwitchManager
) {
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val stateMachine = AssistantStateMachine(controllerScope)
    private val mainHandler = Handler(Looper.getMainLooper())

    val state: StateFlow<AssistantState> = stateMachine.currentState
    val events: SharedFlow<AssistantEvent> = stateMachine.events

    private var isStarted = false

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun start() {
        if (isStarted) return
        isStarted = true
        Log.i(TAG, "Starting Amora Assistant Controller")

        speechOutputManager.initialize { ready ->
            if (ready) {
                Log.i(TAG, "TTS ready — arming wake-word session")
            } else {
                Log.w(TAG, "TTS init failed — proceeding anyway")
            }
            resumeWakeWordListening()
        }
    }

    fun stop() {
        isStarted = false
        speechRecognizer.cancel()
        speechOutputManager.stop()
        stateMachine.transitionTo(AssistantState.IDLE, AssistantEvent.Idle)
        Log.i(TAG, "Amora Assistant Controller stopped")
    }

    // ── Wake-word mode ────────────────────────────────────────────────────────

    /**
     * Arms the continuous "Hey Amora" listener — exactly like Google Assistant's
     * always-on background hotword session. The recognizer stays open, scanning
     * partials until it hears the wake phrase, then seamlessly transitions to
     * command capture in the same audio session.
     */
    fun resumeWakeWordListening() {
        if (!isStarted) return
        stateMachine.transitionTo(AssistantState.WAKE_WORD_LISTENING, AssistantEvent.WakeWordListeningStarted)

        speechRecognizer.startWakeWordListening(object : AndroidSpeechRecognitionManager.WakeWordFoundCallback {
            override fun onWakeWordFound(command: String?) {
                Log.i(TAG, "Wake word detected — inline command: $command")
                handleWakeWordDetected(command)
            }
        })
    }

    // ── Manual trigger (tap orb / tap Test Mic button) ────────────────────────

    fun triggerManualListening() {
        if (!isStarted) start()
        speechOutputManager.stop()
        playActivationChime()
        startCommandCapture()
    }

    // ── Wake-word → command transition ────────────────────────────────────────

    private fun handleWakeWordDetected(initialCommand: String?) {
        stateMachine.transitionTo(
            AssistantState.WAKE_WORD_DETECTED,
            AssistantEvent.WakeWordDetected(initialCommand)
        )

        if (!initialCommand.isNullOrBlank()) {
            // Full command spoken in the same utterance as the wake phrase.
            processSpeechTranscript(initialCommand)
        } else {
            playActivationChime()
            startCommandCapture()
        }
    }

    // ── Command capture ───────────────────────────────────────────────────────

    private fun startCommandCapture() {
        stateMachine.transitionTo(
            AssistantState.COMMAND_LISTENING,
            AssistantEvent.CommandListeningStarted
        )

        speechRecognizer.startListening(object : SpeechRecognitionCallback {
            override fun onListeningStarted() {
                Log.d(TAG, "Command mic open")
            }

            override fun onPartialResult(partialTranscript: String) {
                stateMachine.emitEvent(AssistantEvent.PartialSpeech(partialTranscript))
            }

            override fun onFinalResult(finalTranscript: String) {
                stateMachine.emitEvent(AssistantEvent.FinalSpeech(finalTranscript))
                if (finalTranscript.isNotBlank()) {
                    processSpeechTranscript(finalTranscript)
                } else {
                    scheduleReturnToWakeWord(400L)
                }
            }

            override fun onError(errorCode: Int, errorMessage: String) {
                // Silently return to wake-word mode — no error toast shown to user.
                Log.d(TAG, "Command ended ($errorCode) — returning to wake-word")
                scheduleReturnToWakeWord(250L)
            }

            override fun onListeningStopped() {}
        })
    }

    // ── Processing + execution ────────────────────────────────────────────────

    private fun processSpeechTranscript(transcript: String) {
        stateMachine.transitionTo(AssistantState.PROCESSING)

        controllerScope.launch(Dispatchers.IO) {
            try {
                val intent = intentEngine.parseIntent(transcript)
                withContext(Dispatchers.Main) {
                    stateMachine.emitEvent(AssistantEvent.IntentClassified(intent))
                }

                if (intent is AssistantIntent.StopSpeaking) {
                    withContext(Dispatchers.Main) {
                        speechOutputManager.stop()
                        resumeWakeWordListening()
                    }
                    return@launch
                }

                if (intent is AssistantIntent.EndSession) {
                    withContext(Dispatchers.Main) {
                        endSession()
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    stateMachine.transitionTo(
                        AssistantState.EXECUTING,
                        AssistantEvent.ActionStarted("Executing ${intent.javaClass.simpleName}")
                    )
                }

                val result = actionExecutor.executeIntent(intent)

                withContext(Dispatchers.Main) {
                    stateMachine.emitEvent(AssistantEvent.ActionCompleted(result.spokenResponse))
                    speakAssistantResponse(result.spokenResponse)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error executing intent for '$transcript'", e)
                withContext(Dispatchers.Main) {
                    scheduleReturnToWakeWord(800L)
                }
            }
        }
    }

    // ── TTS output ────────────────────────────────────────────────────────────

    fun speakAssistantResponse(text: String) {
        if (text.isBlank()) {
            resumeWakeWordListening()
            return
        }

        stateMachine.transitionTo(AssistantState.SPEAKING, AssistantEvent.SpeakingStarted(text))

        speechOutputManager.speak(text, object : SpeechOutputCallback {
            override fun onSpeakingStarted() {}

            override fun onSpeakingCompleted() {
                stateMachine.emitEvent(AssistantEvent.SpeakingCompleted)
                scheduleReturnToWakeWord(500L)
            }

            override fun onError(errorMessage: String) {
                Log.w(TAG, "TTS error: $errorMessage")
                scheduleReturnToWakeWord(400L)
            }
        })
    }

    fun stopSpeaking() {
        speechOutputManager.stop()
        resumeWakeWordListening()
    }

    // ── Session end (user said "bye" / "exit" / etc.) ─────────────────────────

    private val farewells = listOf(
        "Goodbye!", "See you later!", "Bye for now!", "Talk to you soon!"
    )

    /**
     * Ends the whole wake-word session: speaks a farewell, then turns the
     * assistant off via the same master switch the Settings toggle uses.
     * That's a deliberate choice over a bespoke shutdown path — it stops
     * FloatingOrbService cleanly, persists the off state so the assistant
     * stays off across restarts, and keeps Settings' toggle in sync with
     * what "bye" just did. The user re-enables it from the app UI.
     */
    private fun endSession() {
        val farewell = farewells.random()
        stateMachine.transitionTo(AssistantState.SPEAKING, AssistantEvent.SpeakingStarted(farewell))

        speechOutputManager.speak(farewell, object : SpeechOutputCallback {
            override fun onSpeakingStarted() {}

            override fun onSpeakingCompleted() {
                stateMachine.emitEvent(AssistantEvent.SpeakingCompleted)
                finishSessionShutdown()
            }

            override fun onError(errorMessage: String) {
                finishSessionShutdown()
            }
        })
    }

    private fun finishSessionShutdown() {
        stop()
        controllerScope.launch {
            masterSwitchManager.setMasterSwitch(false)
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private fun scheduleReturnToWakeWord(delayMs: Long) {
        mainHandler.postDelayed({
            if (isStarted) resumeWakeWordListening()
        }, delayMs)
    }

    private fun playActivationChime() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 65)
            toneGen.startTone(ToneGenerator.TONE_PROP_PROMPT, 110)
        } catch (_: Exception) {}
    }

    fun destroy() {
        stop()
        speechOutputManager.shutdown()
        speechRecognizer.destroy()
        controllerScope.cancel()
    }
}
