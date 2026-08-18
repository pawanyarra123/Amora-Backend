package com.amora.companion.feature.alarm

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat

private const val TAG = "AlarmService"
const val ACTION_DISMISS_ALARM = "com.amora.DISMISS_ALARM"

/**
 * Foreground service that:
 * 1. Plays the system alarm ringtone in a loop
 * 2. Vibrates the phone
 * 3. Continuously listens for "amora turn off alarm" / "stop alarm" etc.
 * 4. Only stops when the voice command is detected (or [ACTION_DISMISS_ALARM] broadcast)
 */
class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var vibrator: Vibrator? = null
    private var stopRequested = false

    private val dismissPhrases = listOf(
        "amora turn off alarm",
        "amora stop alarm",
        "amora stop",
        "stop alarm",
        "turn off alarm",
        "dismiss alarm",
        "alarm off",
        "stop",
        "off"
    )

    override fun onCreate() {
        super.onCreate()
        acquireScreenLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISMISS_ALARM) {
            stopAlarm()
            return START_NOT_STICKY
        }

        val label = intent?.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: "Alarm"
        startForegroundNotification(label)
        startRingtone()
        startVibration()
        startVoiceListening()

        return START_STICKY
    }

    // ── Lock Screen ───────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun acquireScreenLock() {
        // Screen wake is handled via FULL_SCREEN_INTENT on the notification
    }

    // ── Notification ─────────────────────────────────────────────────────────

    private fun startForegroundNotification(label: String) {
        val channelId = "amora_alarm_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AMORA Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { setBypassDnd(true) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("⏰ $label")
            .setContentText("Say \"Amora turn off alarm\" to dismiss")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(null, true)
            .setOngoing(true)
            .build()

        startForeground(2001, notification)
    }

    // ── Ringtone ─────────────────────────────────────────────────────────────

    private fun startRingtone() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(this@AlarmService, alarmUri)
                isLooping = true
                prepare()
                start()
            }
            Log.i(TAG, "Ringtone started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ringtone", e)
        }
    }

    // ── Vibration ─────────────────────────────────────────────────────────────

    private fun startVibration() {
        val pattern = longArrayOf(0, 500, 300, 500)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    // ── Voice Listening ───────────────────────────────────────────────────────

    private fun startVoiceListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.w(TAG, "SpeechRecognizer not available — alarm must be dismissed manually")
            return
        }
        recognitionCycle()
    }

    private fun recognitionCycle() {
        if (stopRequested) return

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: android.os.Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onEvent(eventType: Int, params: android.os.Bundle?) {}

                    override fun onPartialResults(partialResults: android.os.Bundle?) {
                        val matches = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?: return
                        if (checkDismissPhrase(matches)) stopAlarm()
                    }

                    override fun onResults(results: android.os.Bundle?) {
                        val matches = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?: emptyList<String>()
                        if (!checkDismissPhrase(matches) && !stopRequested) {
                            recognitionCycle()
                        }
                    }

                    override fun onError(error: Int) {
                        if (!stopRequested) {
                            android.os.Handler(android.os.Looper.getMainLooper())
                                .postDelayed({ recognitionCycle() }, 600)
                        }
                    }
                })
            }

            val listenIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            }
            speechRecognizer?.startListening(listenIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error in recognition cycle", e)
        }
    }

    private fun checkDismissPhrase(candidates: List<String>): Boolean {
        for (candidate in candidates) {
            val lower = candidate.lowercase().trim()
            for (phrase in dismissPhrases) {
                if (lower.contains(phrase)) {
                    Log.i(TAG, "Dismiss phrase detected: '$candidate'")
                    return true
                }
            }
        }
        return false
    }

    // ── Stop ──────────────────────────────────────────────────────────────────

    private fun stopAlarm() {
        if (stopRequested) return
        stopRequested = true
        Log.i(TAG, "Stopping alarm")

        try { speechRecognizer?.stopListening() } catch (_: Exception) {}
        try { speechRecognizer?.cancel() } catch (_: Exception) {}
        try { speechRecognizer?.destroy() } catch (_: Exception) {}

        try { mediaPlayer?.stop() } catch (_: Exception) {}
        try { mediaPlayer?.release() } catch (_: Exception) {}

        try { vibrator?.cancel() } catch (_: Exception) {}

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
