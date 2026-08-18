package com.amora.companion.core.assistant.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.amora.companion.core.assistant.intent.AssistantIntent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AppActionHandler"

@Singleton
class AppActionHandler @Inject constructor(
    @ApplicationContext private val context: Context
) : IActionHandler {

    private val commonPackageMap = mapOf(
        "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b", "com.whatsapp.android"),
        "whatsapp business" to listOf("com.whatsapp.w4b", "com.whatsapp"),
        "chrome" to listOf("com.android.chrome", "com.google.android.apps.chrome"),
        "google chrome" to listOf("com.android.chrome"),
        "browser" to listOf("com.android.chrome", "org.mozilla.firefox", "com.microsoft.emmx"),
        "youtube" to listOf("com.google.android.youtube", "com.google.android.youtube.tv"),
        "maps" to listOf("com.google.android.apps.maps"),
        "google maps" to listOf("com.google.android.apps.maps"),
        "settings" to listOf("com.android.settings"),
        "camera" to listOf("com.android.camera", "com.google.android.GoogleCamera", "com.sec.android.app.camera"),
        "gallery" to listOf("com.google.android.apps.photos", "com.sec.android.gallery3d", "com.android.gallery3d"),
        "photos" to listOf("com.google.android.apps.photos", "com.sec.android.gallery3d"),
        "gmail" to listOf("com.google.android.gm"),
        "spotify" to listOf("com.spotify.music", "com.spotify.lite"),
        "instagram" to listOf("com.instagram.android", "com.instagram.lite"),
        "telegram" to listOf("org.telegram.messenger", "org.thunderdog.challegram"),
        "calculator" to listOf("com.google.android.calculator", "com.sec.android.app.popupcalculator"),
        "clock" to listOf("com.google.android.deskclock", "com.sec.android.app.clockpackage"),
        "phone" to listOf("com.google.android.dialer", "com.samsung.android.dialer", "com.android.dialer"),
        "dialer" to listOf("com.google.android.dialer", "com.samsung.android.dialer", "com.android.dialer"),
        "messages" to listOf("com.google.android.apps.messaging", "com.samsung.android.messaging", "com.android.mms"),
        "facebook" to listOf("com.facebook.katana", "com.facebook.lite"),
        "twitter" to listOf("com.twitter.android"),
        "x" to listOf("com.twitter.android")
    )

    override fun canHandle(intent: AssistantIntent): Boolean {
        return intent is AssistantIntent.OpenApp
    }

    override suspend fun execute(intent: AssistantIntent): ActionResult {
        if (intent !is AssistantIntent.OpenApp) return ActionResult(false, "Invalid app intent.")

        var targetApp = intent.appName.lowercase().trim()
        // Clean up common prefixes / noise
        targetApp = targetApp
            .replace(Regex("^(the|my|an|a)\\s+"), "")
            .replace(Regex("\\s+app$"), "")
            .replace("whats app", "whatsapp")
            .replace("what's app", "whatsapp")
            .replace("watsapp", "whatsapp")
            .replace("watsap", "whatsapp")
            .replace("what app", "whatsapp")
            .replace("you tube", "youtube")
            .replace("insta gram", "instagram")
            .replace("face book", "facebook")
            .replace("tele gram", "telegram")
            .trim()

        Log.i(TAG, "Attempting to launch app: raw='${intent.appName}', normalized='$targetApp'")

        // 1. Check known aliases
        val directPackages = commonPackageMap[targetApp]
        if (directPackages != null) {
            for (pkg in directPackages) {
                if (launchPackage(pkg)) {
                    return ActionResult(true, "Opening ${intent.appName.replaceFirstChar { it.uppercase() }}.")
                }
            }
        }

        // 2. Query Launcher Activities via Intent
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val launcherApps = pm.queryIntentActivities(mainIntent, 0)
        for (resolveInfo in launcherApps) {
            val label = resolveInfo.loadLabel(pm).toString().lowercase()
            val pkgName = resolveInfo.activityInfo.packageName.lowercase()
            if (label == targetApp || label.contains(targetApp) || targetApp.contains(label) || pkgName.contains(targetApp)) {
                if (launchPackage(resolveInfo.activityInfo.packageName)) {
                    val appLabel = resolveInfo.loadLabel(pm).toString()
                    return ActionResult(true, "Opening $appLabel.")
                }
            }
        }

        // 3. Fallback: Query all installed applications
        try {
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (pkg in packages) {
                val label = pm.getApplicationLabel(pkg).toString().lowercase()
                if (label.contains(targetApp) || targetApp.contains(label) || pkg.packageName.lowercase().contains(targetApp)) {
                    if (launchPackage(pkg.packageName)) {
                        val appLabel = pm.getApplicationLabel(pkg).toString()
                        return ActionResult(true, "Opening $appLabel.")
                    }
                }
            }
        } catch (_: Exception) {}

        return ActionResult(false, "Could not find an app named ${intent.appName} on your phone.")
    }

    private fun launchPackage(packageName: String): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching package: $packageName", e)
            false
        }
    }
}
