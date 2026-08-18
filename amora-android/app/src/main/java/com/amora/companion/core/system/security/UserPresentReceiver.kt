package com.amora.companion.core.system.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class UserPresentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_USER_PRESENT == intent.action || Intent.ACTION_BOOT_COMPLETED == intent.action) {
            Log.d("AmoraSecurity", "System unlock event detected via ACTION_USER_PRESENT")
            
            // Best-effort launch of FaceAuthActivity immediately after system unlock
            val lockIntent = Intent(context, FaceAuthActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            try {
                context.startActivity(lockIntent)
            } catch (e: Exception) {
                Log.e("AmoraSecurity", "Could not start FaceAuthActivity: ${e.message}")
            }
        }
    }
}
