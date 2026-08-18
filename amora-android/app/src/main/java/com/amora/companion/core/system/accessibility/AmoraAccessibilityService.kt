package com.amora.companion.core.system.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AmoraAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AmoraAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i("AmoraAccessibility", "AmoraAccessibilityService connected & ready for app controls.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Active event monitoring
    }

    override fun onInterrupt() {
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    // ── Universal Hands-Free App Control APIs ──────────────────────────────────

    /** Scroll Down (e.g. Next Instagram Reel, Next YouTube Short) */
    fun swipeNextReel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val displayMetrics = resources.displayMetrics
            val middleX = displayMetrics.widthPixels / 2f
            val startY = displayMetrics.heightPixels * 0.8f
            val endY = displayMetrics.heightPixels * 0.2f

            val path = Path().apply {
                moveTo(middleX, startY)
                lineTo(middleX, endY)
            }

            val stroke = GestureDescription.StrokeDescription(path, 0, 300)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, null, null)
        }
    }

    /** Scroll Up (e.g. Previous Reel/Short) */
    fun swipePreviousReel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val displayMetrics = resources.displayMetrics
            val middleX = displayMetrics.widthPixels / 2f
            val startY = displayMetrics.heightPixels * 0.2f
            val endY = displayMetrics.heightPixels * 0.8f

            val path = Path().apply {
                moveTo(middleX, startY)
                lineTo(middleX, endY)
            }

            val stroke = GestureDescription.StrokeDescription(path, 0, 300)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, null, null)
        }
    }

    /** Find node by text and click it (e.g. "Send", "Play", "Pause", "Like") */
    fun clickText(targetText: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(targetText)
        if (nodes != null) {
            for (node in nodes) {
                if (performNodeClick(node)) return true
            }
        }
        return false
    }

    /** Type text into active focused input box and click Send (e.g. WhatsApp, Instagram DM) */
    fun typeAndSend(message: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false

        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, message)
        focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

        // Try clicking Send button
        clickText("Send") || clickText("SEND")
        return true
    }

    private fun performNodeClick(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        return false
    }
}
