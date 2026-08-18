package com.amora.companion.core.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.amora.companion.core.data.local.db.dao.AlarmDao
import com.amora.companion.core.data.local.db.dao.AmoraDao
import com.amora.companion.core.data.local.db.entities.*

@Database(
    entities = [
        CallSummaryEntity::class,
        IntruderLogEntity::class,
        UserMemoryEntity::class,
        AutomationRuleEntity::class,
        SkillConfigEntity::class,
        AlarmEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AmoraDatabase : RoomDatabase() {
    abstract fun amoraDao(): AmoraDao
    abstract fun alarmDao(): AlarmDao
}
