package com.amora.companion.core.data.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_summaries")
data class CallSummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callerName: String,
    val phoneNumber: String,
    val reason: String,
    val urgency: String,
    val summaryText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "intruder_logs")
data class IntruderLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val photoPath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val failureReason: String = "Face auth failed"
)

@Entity(tableName = "user_memories")
data class UserMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memoryText: String,
    val category: String = "general",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "automation_rules")
data class AutomationRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleName: String,
    val triggerType: String,
    val actionType: String,
    val isEnabled: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "skill_configs")
data class SkillConfigEntity(
    @PrimaryKey val skillId: String,
    val skillName: String,
    val isEnabled: Boolean = true
)

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "Alarm",
    val isEnabled: Boolean = true,
    /** Unique int used as AlarmManager request code so we can cancel precisely */
    val requestCode: Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
)

