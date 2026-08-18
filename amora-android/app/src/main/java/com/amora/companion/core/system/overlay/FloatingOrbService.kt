package com.amora.companion.core.system.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color as AndroidColor
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.amora.companion.core.assistant.controller.AssistantController
import com.amora.companion.core.assistant.state.AssistantState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "FloatingOrbService"

/** Broadcast action sent to MainActivity when the wake word fires. */
const val ACTION_WAKE_WORD_DETECTED = "com.amora.WAKE_WORD_DETECTED"

enum class OrbState(val jsonAsset: String) {
    LISTENING("listening.json"),
    THINKING("thinking.json"),
    SPEAKING("speaking.json")
}

@AndroidEntryPoint
class FloatingOrbService : Service() {

    @Inject
    lateinit var assistantController: AssistantController

    private lateinit var windowManager: WindowManager
    private var overlayView: LottieAnimationView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _orbState = MutableStateFlow(OrbState.LISTENING)
    val orbState: StateFlow<OrbState> = _orbState

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        setupOverlayWindow()
        observeAssistantState()
        assistantController.start()

        mainHandler.postDelayed({
            assistantController.speakAssistantResponse("Hello boss! AMORA companion is active.")
        }, 500L)
    }

    private fun observeAssistantState() {
        serviceScope.launch {
            assistantController.state.collect { state ->
                when (state) {
                    AssistantState.COMMAND_LISTENING, AssistantState.WAKE_WORD_DETECTED -> {
                        setOrbState(OrbState.LISTENING)
                        overlayView?.visibility = View.VISIBLE
                    }
                    AssistantState.PROCESSING, AssistantState.EXECUTING -> {
                        setOrbState(OrbState.THINKING)
                        overlayView?.visibility = View.VISIBLE
                    }
                    AssistantState.SPEAKING -> {
                        setOrbState(OrbState.SPEAKING)
                        overlayView?.visibility = View.VISIBLE
                    }
                    AssistantState.WAKE_WORD_LISTENING, AssistantState.IDLE, AssistantState.ERROR -> {
                        overlayView?.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val restartServiceIntent = Intent(applicationContext, FloatingOrbService::class.java).also {
            it.setPackage(packageName)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartServiceIntent)
        } else {
            startService(restartServiceIntent)
        }
    }

    // ── Notification ─────────────────────────────────────────────────────────

    private fun startForegroundServiceNotification() {
        val channelId = "amora_orb_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AMORA Companion Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("AMORA Companion Active")
            .setContentText("Listening for 'Hey Amora' wake word")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1001, notification)
        }
    }

    // ── Overlay Window ───────────────────────────────────────────────────────

    private fun setupOverlayWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            220, 220,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        overlayView = LottieAnimationView(this).apply {
            setBackgroundColor(AndroidColor.TRANSPARENT)
            setAnimation(OrbState.LISTENING.jsonAsset)
            repeatCount = LottieDrawable.INFINITE
            visibility = View.GONE

            setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            params.x = initialX + (event.rawX - initialTouchX).toInt()
                            params.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager.updateViewLayout(overlayView, params)
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            val diffX = Math.abs(event.rawX - initialTouchX)
                            val diffY = Math.abs(event.rawY - initialTouchY)
                            if (diffX < 12 && diffY < 12) {
                                // Tap to talk directly on floating orb
                                assistantController.triggerManualListening()
                            }
                            return true
                        }
                    }
                    return false
                }
            })
        }

        try {
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding overlay view", e)
        }
    }

    fun setOrbState(state: OrbState) {
        _orbState.value = state
        mainHandler.post {
            try {
                overlayView?.apply {
                    setAnimation(state.jsonAsset)
                    repeatCount = LottieDrawable.INFINITE
                    playAnimation()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        assistantController.stop()
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (_: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
