package com.amora.companion.feature.callassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Telephony
import android.util.Log
import com.amora.companion.core.data.network.AmoraApiService
import com.amora.companion.core.data.network.CallSummarizeRequest
import com.amora.companion.core.data.preferences.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import javax.inject.Inject

private const val TAG = "AmoraSmsReply"

/** Words in the caller's SMS reply that count as "yes, it's important/urgent" —
 *  matches the exact word ("EMERGENCY") the auto-reply SMS asks callers to send,
 *  plus a few common variants for freeform replies. */
private val EMERGENCY_KEYWORDS = listOf(
    "emergency", "urgent", "asap", "important", "help", "911", "hospital", "accident"
)

/**
 * Screening a call today only sends one canned SMS asking "is this an emergency?" —
 * nothing ever reads the caller's reply, so `isEmergency` on the real (non-simulated)
 * path was always false. This receiver closes that loop: it watches for an SMS reply
 * from a number we recently screened, decides if it sounds urgent, and — if so —
 * raises the same alert (max volume + vibrate) the "Sleeping" mode uses for real
 * simulated emergencies, then asks the backend for a short AI summary to store in the log.
 */
@AndroidEntryPoint
class AmoraSmsReplyReceiver : BroadcastReceiver() {

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var apiService: AmoraApiService

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (msg in messages) {
            val sender = msg.originatingAddress ?: continue
            val body = msg.messageBody ?: continue
            handleReply(context.applicationContext, sender, body)
        }
    }

    private fun handleReply(context: Context, sender: String, body: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val logsStr = preferencesRepository.callScreeningLogsJson.first()
                if (logsStr.isEmpty()) return@launch
                val array = JSONArray(logsStr)

                // Find the most recent screened entry for this number that we haven't
                // already flagged, so we don't act on unrelated/duplicate replies.
                var matchIndex = -1
                for (i in array.length() - 1 downTo 0) {
                    val obj = array.getJSONObject(i)
                    val caller = obj.optString("caller", "")
                    if (numbersMatch(caller, sender) && !obj.optBoolean("isEmergency", false)) {
                        matchIndex = i
                        break
                    }
                }
                if (matchIndex == -1) return@launch

                val entry = array.getJSONObject(matchIndex)
                val looksUrgent = EMERGENCY_KEYWORDS.any { body.lowercase().contains(it) }

                if (!looksUrgent) return@launch

                entry.put("isEmergency", true)

                // Ask the backend for a proper 1-line AI summary of the reply; fall back
                // to the raw reply text if the backend is unreachable.
                val reasonText = try {
                    val response = apiService.summarizeCall(
                        CallSummarizeRequest(
                            caller = sender,
                            reason = body,
                            isEmergency = true
                        )
                    )
                    if (response.isSuccessful) {
                        response.body()?.get("summary")?.toString() ?: "Caller replied: \"$body\""
                    } else {
                        "Caller replied: \"$body\""
                    }
                } catch (e: Exception) {
                    "Caller replied: \"$body\""
                }
                entry.put("reason", reasonText)

                array.put(matchIndex, entry)
                preferencesRepository.saveCallScreeningLogsJson(array.toString())

                val mode = entry.optString("mode", "Meeting")
                if (mode == "Sleeping") {
                    raiseAlert(context)
                } else {
                    vibrate(context, 1200)
                }

                Log.i(TAG, "Marked call from $sender as emergency based on SMS reply")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process SMS reply", e)
            }
        }
    }

    /** Loose match since incoming numbers may carry country codes/formatting differences. */
    private fun numbersMatch(a: String, b: String): Boolean {
        val cleanA = a.filter { it.isDigit() }.takeLast(10)
        val cleanB = b.filter { it.isDigit() }.takeLast(10)
        return cleanA.isNotEmpty() && cleanA == cleanB
    }

    private fun raiseAlert(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        vibrate(context, 1500)
    }

    private fun vibrate(context: Context, durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
