package com.amora.companion.core.assistant.actions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.amora.companion.core.assistant.intent.AssistantIntent
import com.amora.companion.core.system.accessibility.AmoraAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CommunicationActionHandler"

@Singleton
class CommunicationActionHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : IActionHandler {

    override fun canHandle(intent: AssistantIntent): Boolean {
        return intent is AssistantIntent.CallContact ||
                intent is AssistantIntent.SendSms ||
                intent is AssistantIntent.SendWhatsAppMessage
    }

    override suspend fun execute(intent: AssistantIntent): ActionResult {
        return when (intent) {
            is AssistantIntent.CallContact -> {
                try {
                    val phone = intent.phoneNumber ?: findContactPhoneNumber(intent.contactName)
                    val hasCallPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CALL_PHONE
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!phone.isNullOrBlank()) {
                        val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
                        if (hasCallPermission) {
                            // Direct hands-free call placement
                            val directCallIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanPhone")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(directCallIntent)
                            ActionResult(true, "Calling ${intent.contactName} directly.")
                        } else {
                            // Fallback to dialer if CALL_PHONE permission not yet granted
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(dialIntent)
                            ActionResult(true, "Opening phone dialer for ${intent.contactName}.")
                        }
                    } else {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${intent.contactName}")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(dialIntent)
                        ActionResult(true, "Opening dialer for ${intent.contactName}.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error making call", e)
                    ActionResult(false, "Could not place call: ${e.message}")
                }
            }

            is AssistantIntent.SendWhatsAppMessage -> {
                try {
                    val recipientName = intent.recipient
                    val correctedMessage = correctGrammar(intent.messageBody)
                    val foundPhone = findContactPhoneNumber(recipientName)

                    if (!foundPhone.isNullOrBlank()) {
                        val cleanPhone = foundPhone.replace(Regex("[^0-9+]"), "").replace("+", "")
                        val encodedMsg = URLEncoder.encode(correctedMessage, "UTF-8")
                        val whatsappUri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")

                        val waIntent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                            setPackage("com.whatsapp")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }

                        context.startActivity(waIntent)

                        // Trigger Accessibility auto-send if enabled
                        AmoraAccessibilityService.instance?.let { service ->
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                service.clickText("Send") || service.clickText("SEND")
                            }, 1200)
                        }

                        ActionResult(true, "Sending WhatsApp to $recipientName: \"$correctedMessage\"")
                    } else {
                        // Recipient phone not in contacts list — open WhatsApp share intent with grammar-corrected text
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            setPackage("com.whatsapp")
                            putExtra(Intent.EXTRA_TEXT, correctedMessage)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(shareIntent)
                        ActionResult(true, "Opening WhatsApp for $recipientName: \"$correctedMessage\"")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending WhatsApp message", e)
                    ActionResult(false, "Could not open WhatsApp: ${e.message}")
                }
            }

            is AssistantIntent.SendSms -> {
                try {
                    val correctedMessage = correctGrammar(intent.messageBody)
                    val phone = findContactPhoneNumber(intent.recipient) ?: intent.recipient
                    val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("smsto:$phone")
                        putExtra("sms_body", correctedMessage)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(smsIntent)
                    ActionResult(true, "Opening SMS composer for ${intent.recipient}: \"$correctedMessage\"")
                } catch (e: Exception) {
                    ActionResult(false, "Could not open messaging: ${e.message}")
                }
            }

            else -> ActionResult(false, "Unsupported communication intent.")
        }
    }

    /**
     * Automated smart grammar and voice transcription correction engine.
     * Capitalizes sentences, expands contractions, corrects spoken phrasing,
     * and ensures proper punctuation before sending.
     */
    private fun correctGrammar(rawText: String): String {
        var text = rawText.trim()
        if (text.isBlank()) return text

        // Replace common speech-to-text phrasing errors
        val phraseReplacements = mapOf(
            "(?i)\\bcoming to home\\b" to "coming home",
            "(?i)\\breach to home\\b" to "reach home",
            "(?i)\\bwent to home\\b" to "went home",
            "(?i)\\bcall to you\\b" to "call you",
            "(?i)\\btalk with you\\b" to "talk to you",
            "(?i)\\bi am\\b" to "I am",
            "(?i)\\bi will\\b" to "I will",
            "(?i)\\bim\\b" to "I'm",
            "(?i)\\bi'm\\b" to "I'm",
            "(?i)\\bive\\b" to "I've",
            "(?i)\\bi've\\b" to "I've",
            "(?i)\\bid\\b" to "I'd",
            "(?i)\\bi'd\\b" to "I'd",
            "(?i)\\bill\\b" to "I'll",
            "(?i)\\bi'll\\b" to "I'll",
            "(?i)\\bdont\\b" to "don't",
            "(?i)\\bcant\\b" to "can't",
            "(?i)\\bwont\\b" to "won't",
            "(?i)\\bdidnt\\b" to "didn't",
            "(?i)\\bisnt\\b" to "isn't",
            "(?i)\\barent\\b" to "aren't",
            "(?i)\\bhavent\\b" to "haven't",
            "(?i)\\bhasnt\\b" to "hasn't",
            "(?i)\\bwhats\\b" to "what's",
            "(?i)\\bthats\\b" to "that's",
            "(?i)\\btheres\\b" to "there's",
            "(?i)\\blets\\b" to "let's",
            "(?i)\\bpls\\b" to "please",
            "(?i)\\bplz\\b" to "please",
            "(?i)\\bu\\b" to "you",
            "(?i)\\bur\\b" to "your",
            "(?i)\\br\\b" to "are"
        )

        for ((regex, replacement) in phraseReplacements) {
            text = text.replace(Regex(regex), replacement)
        }

        // Capitalize standalone "i"
        text = text.replace(Regex("\\bi\\b"), "I")

        val matcher = java.util.regex.Pattern.compile("([.!?]\\s*)").matcher(text)
        var segmentStart = 0

        val segments = mutableListOf<String>()
        while (matcher.find()) {
            val segment = text.substring(segmentStart, matcher.start())
            val delimiter = matcher.group(1)
            segments.add(capitalizeSentence(segment) + delimiter)
            segmentStart = matcher.end()
        }
        if (segmentStart < text.length) {
            segments.add(capitalizeSentence(text.substring(segmentStart)))
        }

        var result = if (segments.isNotEmpty()) segments.joinToString("") else capitalizeSentence(text)

        // Ensure closing punctuation if it looks like a complete sentence
        if (!result.endsWith(".") && !result.endsWith("!") && !result.endsWith("?")) {
            result += "."
        }

        return result
    }

    private fun capitalizeSentence(str: String): String {
        val trimmed = str.trim()
        if (trimmed.isEmpty()) return str
        val capitalized = trimmed.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val leadingSpaces = str.takeWhile { it.isWhitespace() }
        val trailingSpaces = str.takeLastWhile { it.isWhitespace() }
        return leadingSpaces + capitalized + trailingSpaces
    }

    /**
     * Contact query supporting phonetic aliases for family & relations.
     */
    private fun findContactPhoneNumber(contactName: String): String? {
        val cleanName = contactName.trim().lowercase()
        if (cleanName.matches(Regex("^[0-9+]+$"))) return cleanName

        // Expand family relationship aliases
        val searchNames = mutableListOf(cleanName)
        when (cleanName) {
            "mommy", "mummy", "mom", "mother", "amma", "mum" -> {
                searchNames.addAll(listOf("mommy", "mummy", "mom", "mother", "amma", "mum", "ma"))
            }
            "daddy", "dad", "father", "appa", "pop" -> {
                searchNames.addAll(listOf("daddy", "dad", "father", "appa", "pop", "baba"))
            }
            "bro", "brother" -> {
                searchNames.addAll(listOf("brother", "bro", "bhai"))
            }
            "sis", "sister" -> {
                searchNames.addAll(listOf("sister", "sis", "didi"))
            }
        }

        for (name in searchNames.distinct()) {
            var cursor: Cursor? = null
            try {
                val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
                val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
                val selectionArgs = arrayOf("%$name%")

                cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numberIndex != -1) {
                        val number = cursor.getString(numberIndex)
                        if (!number.isNullOrBlank()) {
                            return number
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to query contacts for $name", e)
            } finally {
                cursor?.close()
            }
        }
        return null
    }
}
