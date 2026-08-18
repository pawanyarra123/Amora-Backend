package com.amora.companion.core.assistant.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.amora.companion.core.assistant.intent.AssistantIntent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppActionHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : IActionHandler {

    private val commonPackageMap = mapOf(
        "chrome" to "com.android.chrome",
        "google chrome" to "com.android.chrome",
        "browser" to "com.android.chrome",
        "youtube" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "settings" to "com.android.settings",
        "camera" to "com.android.camera",
        "gallery" to "com.google.android.apps.photos",
        "photos" to "com.google.android.apps.photos",
        "gmail" to "com.google.android.gm",
        "spotify" to "com.spotify.music",
        "instagram" to "com.instagram.android",
        "telegram" to "org.telegram.messenger",
        "calculator" to "com.google.android.calculator",
        "clock" to "com.google.android.deskclock"
    )

    override fun canHandle(intent: AssistantIntent): Boolean {
        return intent is AssistantIntent.OpenApp
    }

    override suspend fun execute(intent: AssistantIntent): ActionResult {
        if (intent !is AssistantIntent.OpenApp) return ActionResult(false, "Invalid app intent.")

        val targetApp = intent.appName.lowercase().trim()

        // 1. Check known aliases
        val directPackage = commonPackageMap[targetApp]
        if (directPackage != null && launchPackage(directPackage)) {
            return ActionResult(true, "Opening ${intent.appName}.")
        }

        // 2. Query Installed Applications via PackageManager
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (pkg in packages) {
            val label = pm.getApplicationLabel(pkg).toString().lowercase()
            if (label.contains(targetApp) || targetApp.contains(label)) {
                if (launchPackage(pkg.packageName)) {
                    val appLabel = pm.getApplicationLabel(pkg).toString()
                    return ActionResult(true, "Opening $appLabel.")
                }
            }
        }

        return ActionResult(false, "Could not find an app named ${intent.appName} on your phone.")
    }

    private fun launchPackage(packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
