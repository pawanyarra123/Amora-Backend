package com.amora.companion.core.system.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.amora.companion.core.system.accessibility.AmoraAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class PermissionStatus(
    val hasAudioPermission: Boolean,
    val hasCameraPermission: Boolean,
    val hasOverlayPermission: Boolean,
    val hasNotificationListenerPermission: Boolean,
    val hasAccessibilityPermission: Boolean
)

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun checkPermissions(): PermissionStatus {
        val audio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val camera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val overlay = Settings.canDrawOverlays(context)
        val notifListener = isNotificationListenerEnabled()
        val accessibility = isAccessibilityServiceEnabled()

        return PermissionStatus(
            hasAudioPermission = audio,
            hasCameraPermission = camera,
            hasOverlayPermission = overlay,
            hasNotificationListenerPermission = notifListener,
            hasAccessibilityPermission = accessibility
        )
    }

    fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }

    fun openNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(context.packageName)
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        if (AmoraAccessibilityService.instance != null) return true
        val flat = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return flat != null && (flat.contains(context.packageName) || flat.contains("AmoraAccessibilityService"))
    }
}
