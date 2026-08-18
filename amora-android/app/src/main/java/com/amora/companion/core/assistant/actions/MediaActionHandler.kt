package com.amora.companion.core.assistant.actions

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import com.amora.companion.core.assistant.intent.AssistantIntent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaActionHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : IActionHandler {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    override fun canHandle(intent: AssistantIntent): Boolean {
        return intent is AssistantIntent.MediaPlay ||
                intent is AssistantIntent.MediaPause ||
                intent is AssistantIntent.MediaNext ||
                intent is AssistantIntent.MediaPrevious
    }

    override suspend fun execute(intent: AssistantIntent): ActionResult {
        val keyCode = when (intent) {
            is AssistantIntent.MediaPlay -> KeyEvent.KEYCODE_MEDIA_PLAY
            is AssistantIntent.MediaPause -> KeyEvent.KEYCODE_MEDIA_PAUSE
            is AssistantIntent.MediaNext -> KeyEvent.KEYCODE_MEDIA_NEXT
            is AssistantIntent.MediaPrevious -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> return ActionResult(false, "Unsupported media intent.")
        }

        return try {
            val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            audioManager.dispatchMediaKeyEvent(eventDown)
            audioManager.dispatchMediaKeyEvent(eventUp)
            val desc = when (intent) {
                is AssistantIntent.MediaPlay -> "Resuming media playback."
                is AssistantIntent.MediaPause -> "Pausing media."
                is AssistantIntent.MediaNext -> "Playing next track."
                is AssistantIntent.MediaPrevious -> "Playing previous track."
                else -> "Media command executed."
            }
            ActionResult(true, desc)
        } catch (e: Exception) {
            ActionResult(false, "Media control failed: ${e.message}")
        }
    }
}
