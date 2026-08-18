package com.amora.companion.core.system.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class AmoraNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            val packageName = it.packageName
            val title = it.notification.extras.getCharSequence("android.title")?.toString() ?: ""
            val text = it.notification.extras.getCharSequence("android.text")?.toString() ?: ""
            Log.d("AmoraNotif", "Notification posted: [$packageName] $title: $text")
        }
    }
}
