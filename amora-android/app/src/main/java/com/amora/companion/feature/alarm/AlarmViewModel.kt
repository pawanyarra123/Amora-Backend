package com.amora.companion.feature.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amora.companion.core.data.local.db.dao.AlarmDao
import com.amora.companion.core.data.local.db.entities.AlarmEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmDao: AlarmDao
) : ViewModel() {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val alarms: StateFlow<List<AlarmEntity>> = alarmDao.getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addAlarm(hour: Int, minute: Int, label: String = "Alarm") {
        viewModelScope.launch {
            val requestCode = ((System.currentTimeMillis() / 1000) % Int.MAX_VALUE).toInt()
            val alarm = AlarmEntity(hour = hour, minute = minute, label = label, requestCode = requestCode)
            val id = alarmDao.insertAlarm(alarm)
            scheduleAlarm(alarm.copy(id = id))
            Log.i("AlarmVM", "Alarm set for $hour:$minute ($label)")
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            cancelAlarm(alarm)
            alarmDao.deleteAlarm(alarm)
        }
    }

    fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = enabled)
            alarmDao.updateAlarm(updated)
            if (enabled) scheduleAlarm(updated) else cancelAlarm(updated)
        }
    }

    private fun scheduleAlarm(alarm: AlarmEntity) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If the time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_ALARM_LABEL, alarm.label)
            putExtra(AlarmReceiver.EXTRA_REQUEST_CODE, alarm.requestCode)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            alarm.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Fallback to inexact if permission not granted
            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pending)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pending)
        }
    }

    private fun cancelAlarm(alarm: AlarmEntity) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            alarm.requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pending?.let { alarmManager.cancel(it) }
    }
}
