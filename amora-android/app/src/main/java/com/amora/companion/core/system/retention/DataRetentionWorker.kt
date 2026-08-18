package com.amora.companion.core.system.retention

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.amora.companion.core.data.local.db.AmoraDatabase
import java.io.File

class DataRetentionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val photoCutoff = now - (30L * 24 * 3600 * 1000)
        val summaryCutoff = now - (90L * 24 * 3600 * 1000)

        try {
            val db = Room.databaseBuilder(
                applicationContext,
                AmoraDatabase::class.java,
                "amora_db"
            ).fallbackToDestructiveMigration().build()

            val amoraDao = db.amoraDao()

            // Delete expired intruder photo files
            val expiredLogs = amoraDao.getIntruderLogsOlderThan(photoCutoff)
            for (log in expiredLogs) {
                val file = File(log.photoPath)
                if (file.exists()) {
                    file.delete()
                }
            }
            amoraDao.deleteIntruderLogsOlderThan(photoCutoff)

            // Delete expired call summaries
            amoraDao.deleteCallSummariesOlderThan(summaryCutoff)

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}
