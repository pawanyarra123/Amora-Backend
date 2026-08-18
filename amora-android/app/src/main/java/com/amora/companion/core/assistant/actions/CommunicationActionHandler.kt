package com.amora.companion.core.assistant.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.amora.companion.core.assistant.intent.AssistantIntent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunicationActionHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : IActionHandler {

    override fun canHandle(intent: AssistantIntent): Boolean {
        return intent is AssistantIntent.CallContact || intent is AssistantIntent.SendSms
    }

    override suspend fun execute(intent: AssistantIntent): ActionResult {
        return when (intent) {
            is AssistantIntent.CallContact -> {
                try {
                    val dialIntent = if (!intent.phoneNumber.isNullOrBlank()) {
                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${intent.phoneNumber}"))
                    } else {
                        // Open dialer with contact search or standard dialer
                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${intent.contactName}"))
                    }
                    dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(dialIntent)
                    ActionResult(true, "Calling ${intent.contactName}.")
                } catch (e: Exception) {
                    ActionResult(false, "Could not open dialer: ${e.message}")
                }
            }
            is AssistantIntent.SendSms -> {
                try {
                    val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("smsto:${intent.recipient}")
                        putExtra("sms_body", intent.messageBody)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(smsIntent)
                    ActionResult(true, "Opening message composer for ${intent.recipient}.")
                } catch (e: Exception) {
                    ActionResult(false, "Could not open messaging: ${e.message}")
                }
            }
            else -> ActionResult(false, "Unsupported communication intent.")
        }
    }
}
