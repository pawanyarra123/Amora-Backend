package com.amora.companion.core.assistant.actions

import com.amora.companion.core.assistant.intent.AssistantIntent
import com.amora.companion.core.data.network.AmoraApiService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles "what's the weather" style voice commands by calling the app's own
 * weather endpoint directly — the same one the Dashboard card uses. This
 * replaced routing GetWeather through the AI chat backend: it's a device
 * command with a factual answer, not something that needs a conversational
 * model in the loop.
 *
 * Named-location queries ("weather in Paris") aren't geocoded yet, so those
 * get an honest "can't do that yet" response rather than silently answering
 * for the wrong city.
 */
@Singleton
class WeatherActionHandler @Inject constructor(
    private val apiService: AmoraApiService
) : IActionHandler {

    // Matches the default coordinates the Dashboard card uses when no
    // location is available — keeps voice and dashboard weather consistent.
    private val defaultLat = 13.0827
    private val defaultLon = 80.2707
    private val defaultCity = "Chennai"

    override fun canHandle(intent: AssistantIntent): Boolean {
        return intent is AssistantIntent.GetWeather
    }

    override suspend fun execute(intent: AssistantIntent): ActionResult {
        if (intent !is AssistantIntent.GetWeather) return ActionResult(false, "Unsupported weather request.")

        if (!intent.location.isNullOrBlank()) {
            return ActionResult(
                success = false,
                spokenResponse = "I can only check the weather for your current area right now, not ${intent.location} yet."
            )
        }

        return try {
            val response = apiService.getWeather(lat = defaultLat, lon = defaultLon, city = defaultCity)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                ActionResult(true, body.display_text)
            } else {
                ActionResult(false, "I couldn't reach the weather service right now.")
            }
        } catch (e: Exception) {
            ActionResult(false, "I couldn't reach the weather service right now.", error = e.message)
        }
    }
}
