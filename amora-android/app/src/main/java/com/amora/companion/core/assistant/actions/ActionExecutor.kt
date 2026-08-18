package com.amora.companion.core.assistant.actions

import com.amora.companion.core.assistant.intent.AssistantIntent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActionExecutor @Inject constructor(
    private val deviceActionHandler: DeviceActionHandler,
    private val appActionHandler: AppActionHandler,
    private val alarmActionHandler: AlarmActionHandler,
    private val communicationActionHandler: CommunicationActionHandler,
    private val mediaActionHandler: MediaActionHandler,
    private val weatherActionHandler: WeatherActionHandler,
    private val unknownCommandHandler: UnknownCommandHandler
) : IActionExecutor {

    private val handlers: List<IActionHandler> = listOf(
        deviceActionHandler,
        appActionHandler,
        alarmActionHandler,
        communicationActionHandler,
        mediaActionHandler,
        weatherActionHandler,
        unknownCommandHandler
    )

    override suspend fun executeIntent(intent: AssistantIntent): ActionResult {
        // StopSpeaking and EndSession are handled by AssistantController directly
        // (they change the listening session's lifecycle, not just produce a
        // spoken reply), so ActionExecutor treats them as no-ops if it ever
        // sees one — defensive only, the controller shouldn't route them here.
        if (intent is AssistantIntent.StopSpeaking || intent is AssistantIntent.EndSession) {
            return ActionResult(true, "")
        }

        for (handler in handlers) {
            if (handler.canHandle(intent)) {
                return handler.execute(intent)
            }
        }

        // unknownCommandHandler.canHandle() covers Unknown, so this is only
        // reached for an intent type nothing was wired up for — keep it safe.
        return ActionResult(false, "I'm not able to do that yet.")
    }
}
