package com.amora.companion.feature.mic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Completely standalone mic test screen.
 *
 * Launch it from adb or any button in the app to verify that the device
 * microphone + Google speech recognition work correctly — completely independent
 * of the assistant pipeline.
 *
 * Launch via:
 *   adb shell am start -n com.amora.companion/.feature.mic.MicTestActivity
 *
 * Or add a button in any screen:
 *   startActivity(Intent(context, MicTestActivity::class.java))
 */
@AndroidEntryPoint
class MicTestActivity : ComponentActivity() {

    @Inject
    lateinit var micDiagnostic: MicDiagnosticManager

    private lateinit var statusText: TextView
    private lateinit var partialText: TextView
    private lateinit var logView: TextView
    private lateinit var rmsBar: ProgressBar
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startMicTest()
        } else {
            appendLog("❌ RECORD_AUDIO permission DENIED\nGo to Settings → Apps → AMORA → Permissions → Microphone → Allow")
            statusText.text = "❌ Permission denied"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Build layout programmatically (no XML needed) ─────────────────────
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
            setBackgroundColor(0xFF1A1A2E.toInt())
        }

        val title = TextView(this).apply {
            text = "🎙️ AMORA Mic Diagnostic"
            textSize = 22f
            setTextColor(0xFF00E5FF.toInt())
            setPadding(0, 0, 0, 24)
        }

        statusText = TextView(this).apply {
            text = "Tap START to test the microphone"
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 0, 0, 8)
        }

        rmsBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            setPadding(0, 0, 0, 16)
        }

        partialText = TextView(this).apply {
            text = ""
            textSize = 16f
            setTextColor(0xFF4CAF50.toInt())
            setPadding(0, 0, 0, 16)
        }

        startButton = Button(this).apply {
            text = "▶  START MIC TEST"
            setTextColor(0xFF000000.toInt())
            setBackgroundColor(0xFF00E5FF.toInt())
            setOnClickListener { onStartClicked() }
        }

        stopButton = Button(this).apply {
            text = "⏹  STOP"
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFFE53935.toInt())
            isEnabled = false
            setOnClickListener { onStopClicked() }
        }

        logView = TextView(this).apply {
            text = "=== Log ===\n"
            textSize = 12f
            setTextColor(0xFFBBBBBB.toInt())
            setPadding(0, 16, 0, 0)
        }

        val scrollView = ScrollView(this)
        scrollView.addView(logView)

        root.addView(title)
        root.addView(statusText)
        root.addView(rmsBar)
        root.addView(partialText)
        root.addView(startButton)
        root.addView(stopButton)
        root.addView(scrollView)

        setContentView(root)
    }

    private fun onStartClicked() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            appendLog("⚠️ Requesting RECORD_AUDIO permission...")
            requestPermission.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        startMicTest()
    }

    private fun startMicTest() {
        startButton.isEnabled = false
        stopButton.isEnabled = true
        partialText.text = ""
        appendLog("--- Test started ---")

        micDiagnostic.startTest(object : MicDiagnosticManager.MicTestCallback {
            override fun onStatus(status: String) {
                runOnUiThread {
                    statusText.text = status
                    appendLog(status)
                }
            }

            override fun onRmsLevel(level: Float) {
                runOnUiThread {
                    rmsBar.progress = (level * 100).toInt()
                }
            }

            override fun onWordRecognized(word: String) {
                runOnUiThread {
                    partialText.text = word
                }
            }

            override fun onDone(success: Boolean, finalText: String) {
                runOnUiThread {
                    rmsBar.progress = 0
                    startButton.isEnabled = true
                    stopButton.isEnabled = false
                    if (success) {
                        appendLog("✅ SUCCESS: mic captured \"$finalText\"")
                    } else {
                        appendLog("⚠️ Done (see status above)")
                    }
                }
            }
        })
    }

    private fun onStopClicked() {
        micDiagnostic.stopTest()
        rmsBar.progress = 0
        startButton.isEnabled = true
        stopButton.isEnabled = false
        statusText.text = "Stopped"
        appendLog("--- Test stopped by user ---")
    }

    private fun appendLog(msg: String) {
        logView.text = "${logView.text}$msg\n"
    }

    override fun onDestroy() {
        super.onDestroy()
        micDiagnostic.stopTest()
    }
}
