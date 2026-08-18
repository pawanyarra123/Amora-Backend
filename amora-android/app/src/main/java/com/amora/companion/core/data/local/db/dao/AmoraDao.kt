package com.amora.companion.core.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.amora.companion.core.data.local.db.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AmoraDao {

    // Call Summaries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallSummary(summary: CallSummaryEntity)

    @Query("SELECT * FROM call_summaries ORDER BY timestamp DESC")
    fun getAllCallSummaries(): Flow<List<CallSummaryEntity>>

    @Query("DELETE FROM call_summaries WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteCallSummariesOlderThan(cutoffTimestamp: Long): Int

    // Intruder Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntruderLog(log: IntruderLogEntity)

    @Query("SELECT * FROM intruder_logs ORDER BY timestamp DESC")
    fun getAllIntruderLogs(): Flow<List<IntruderLogEntity>>

    @Query("SELECT * FROM intruder_logs WHERE timestamp < :cutoffTimestamp")
    suspend fun getIntruderLogsOlderThan(cutoffTimestamp: Long): List<IntruderLogEntity>

    @Query("DELETE FROM intruder_logs WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteIntruderLogsOlderThan(cutoffTimestamp: Long): Int

    // User Memories
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserMemory(memory: UserMemoryEntity)

    @Query("SELECT * FROM user_memories ORDER BY timestamp DESC")
    fun getAllUserMemories(): Flow<List<UserMemoryEntity>>

    @Query("DELETE FROM user_memories")
    suspend fun deleteAllUserMemories()

    // Automation Rules
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomationRule(rule: AutomationRuleEntity)

    @Query("SELECT * FROM automation_rules ORDER BY timestamp DESC")
    fun getAllAutomationRules(): Flow<List<AutomationRuleEntity>>

    // Skill Configs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkillConfig(config: SkillConfigEntity)

    @Query("SELECT * FROM skill_configs")
    fun getAllSkillConfigs(): Flow<List<SkillConfigEntity>>

    // Master Wipe
    @Query("DELETE FROM call_summaries")
    suspend fun deleteAllCallSummaries()

    @Query("DELETE FROM intruder_logs")
    suspend fun deleteAllIntruderLogs()
}
