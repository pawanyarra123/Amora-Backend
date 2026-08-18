package com.amora.companion.core.assistant.actions

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.amora.companion.core.assistant.intent.AssistantIntent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmActionHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : IActionHandler {

    override fun canHandle(intent: AssistantIntent): Boolean {
        return intent is AssistantIntent.SetAlarm || intent is AssistantIntent.SetTimer
    }

    override suspend fun execute(intent: AssistantIntent): ActionResult {
        return when (intent) {
            is AssistantIntent.SetAlarm -> {
                try {
                    val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(AlarmClock.EXTRA_HOUR, intent.hour)
                        putExtra(AlarmClock.EXTRA_MINUTES, intent.minute)
                        putExtra(AlarmClock.EXTRA_MESSAGE, intent.message ?: "Amora Alarm")
                        putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(alarmIntent)
                    val minuteStr = if (intent.minute < 10) "0${intent.minute}" else "${intent.minute}"
                    val amPm = if (intent.hour >= 12) "PM" else "AM"
                    val displayHour = if (intent.hour % 12 == 0) 12 else intent.hour % 12
                    ActionResult(true, "Alarm set for $displayHour:$minuteStr $amPm.")
                } catch (e: Exception) {
                    ActionResult(false, "Could not set alarm: ${e.message}")
                }
            }
            is AssistantIntent.SetTimer -> {
                try {
                    val timerIntent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                        putExtra(AlarmClock.EXTRA_LENGTH, intent.seconds)
                        putExtra(AlarmClock.EXTRA_MESSAGE, intent.message ?: "Amora Timer")
                        putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(timerIntent)
                    val mins = intent.seconds / 60
                    val secs = intent.seconds % 60
                    val timeStr = if (mins > 0) "$mins minute${if (mins > 1) "s" else ""}" else "$secs seconds"
                    ActionResult(true, "Timer set for $timeStr.")
                } catch (e: Exception) {
                    ActionResult(false, "Could not set timer: ${e.message}")
                }
            }
            else -> ActionResult(false, "Unsupported alarm action.")
        }
    }
}
