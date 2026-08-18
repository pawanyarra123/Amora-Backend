package com.amora.companion.core.assistant.actions

import com.amora.companion.core.assistant.intent.AssistantIntent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Terminal fallback handler: fires only when nothing else in [ActionExecutor]'s
 * handler chain claimed the intent. Replaces the previous AiConversationHandler
 * — this assistant is command-only (no chat backend, no conversational AI),
 * so an unrecognized utterance gets a short spoken nudge back toward a
 * supported command instead of a generated reply.
 */
@Singleton
class UnknownCommandHandler @Inject constructor() : IActionHandler {

    private val sampleCommands = listOf(
        "turn on the flashlight",
        "set an alarm for 7 AM",
        "what's the weather",
        "open YouTube",
        "call mom",
    )

    override fun canHandle(intent: AssistantIntent): Boolean {
        return intent is AssistantIntent.Unknown
    }

    override suspend fun execute(intent: AssistantIntent): ActionResult {
        val example = sampleCommands.random()
        return ActionResult(
            success = false,
            spokenResponse = "Sorry, I didn't catch a command in that. Try something like \"$example.\""
        )
    }
}
