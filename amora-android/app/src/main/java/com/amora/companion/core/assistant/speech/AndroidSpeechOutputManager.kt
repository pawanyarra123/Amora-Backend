package com.amora.companion.core.assistant.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SpeechOutputManager"

@Singleton
class AndroidSpeechOutputManager @Inject constructor(
    @ApplicationContext private val context: Context
) : ISpeechOutputManager, TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var isSpeakingNow = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var onReadyCallback: ((Boolean) -> Unit)? = null
    private var currentCallback: SpeechOutputCallback? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null

    override fun initialize(onReady: (Boolean) -> Unit) {
        onReadyCallback = onReady
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeakingNow = true
                    requestDuckFocus()
                    mainHandler.post { currentCallback?.onSpeakingStarted() }
                }

                override fun onDone(utteranceId: String?) {
                    isSpeakingNow = false
                    abandonDuckFocus()
                    mainHandler.post {
                        val cb = currentCallback
                        currentCallback = null
                        cb?.onSpeakingCompleted()
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    isSpeakingNow = false
                    abandonDuckFocus()
                    mainHandler.post {
                        val cb = currentCallback
                        currentCallback = null
                        cb?.onError("TTS playback error")
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    isSpeakingNow = false
                    abandonDuckFocus()
                    mainHandler.post {
                        val cb = currentCallback
                        currentCallback = null
                        cb?.onError("TTS error code: $errorCode")
                    }
                }
            })
            isReady = true
            onReadyCallback?.invoke(true)
        } else {
            isReady = false
            Log.e(TAG, "Failed to initialize TextToSpeech (status=$status)")
            onReadyCallback?.invoke(false)
        }
    }

    override fun speak(text: String, callback: SpeechOutputCallback?) {
        if (!isReady || text.isBlank()) {
            callback?.onSpeakingCompleted()
            return
        }

        stop() // Halt any ongoing utterance immediately
        currentCallback = callback

        val safetyTimeoutMs = (text.length * 80L + 3_000L).coerceAtMost(20_000L)
        mainHandler.postDelayed({
            if (isSpeakingNow && currentCallback != null) {
                Log.w(TAG, "TTS timeout safety triggered after ${safetyTimeoutMs}ms")
                stop()
            }
        }, safetyTimeoutMs)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "amora_tts_stream")
        } else {
            @Suppress("DEPRECATION")
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    override fun stop() {
        if (isSpeakingNow) {
            try {
                tts?.stop()
            } catch (_: Exception) {}
            isSpeakingNow = false
            abandonDuckFocus()
            val cb = currentCallback
            currentCallback = null
            cb?.onSpeakingCompleted()
        }
    }

    override fun isSpeaking(): Boolean = isSpeakingNow

    private fun requestDuckFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (focusRequest == null) {
                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
            }
            focusRequest?.let { audioManager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }
    }

    private fun abandonDuckFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    override fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
