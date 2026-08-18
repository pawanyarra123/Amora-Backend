package com.amora.companion.core.assistant.intent

/**
 * Replaceable intent understanding engine contract.
 */
interface IIntentEngine {
    suspend fun parseIntent(speechText: String): AssistantIntent
}
