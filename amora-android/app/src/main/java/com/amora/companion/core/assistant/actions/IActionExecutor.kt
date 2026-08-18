package com.amora.companion.core.assistant.actions

import com.amora.companion.core.assistant.intent.AssistantIntent

data class ActionResult(
    val success: Boolean,
    val spokenResponse: String,
    val error: String? = null
)

interface IActionHandler {
    fun canHandle(intent: AssistantIntent): Boolean
    suspend fun execute(intent: AssistantIntent): ActionResult
}

interface IActionExecutor {
    suspend fun executeIntent(intent: AssistantIntent): ActionResult
}
