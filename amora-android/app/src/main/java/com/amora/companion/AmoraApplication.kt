package com.amora.companion

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.amora.companion.core.system.retention.DataRetentionWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class AmoraApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        scheduleDataRetentionWorker()
    }

    private fun scheduleDataRetentionWorker() {
        // Enforces automated data retention TTL (30d intruder photos, 90d call summaries, 7d logs)
        // Runs independently of Master Switch status.
        val retentionRequest = PeriodicWorkRequestBuilder<DataRetentionWorker>(24, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "AmoraDataRetentionWork",
            ExistingPeriodicWorkPolicy.KEEP,
            retentionRequest
        )
    }
}
