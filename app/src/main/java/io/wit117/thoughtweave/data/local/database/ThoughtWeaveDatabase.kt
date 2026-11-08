package io.wit117.thoughtweave.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.wit117.thoughtweave.data.local.dao.MemoDao
import io.wit117.thoughtweave.data.local.dao.ScheduleDao
import io.wit117.thoughtweave.data.local.entity.MemoEntity
import io.wit117.thoughtweave.data.local.entity.MemoSynthesisSourceEntity
import io.wit117.thoughtweave.data.local.entity.ScheduleEntity

@Database(
    entities = [MemoEntity::class, MemoSynthesisSourceEntity::class, ScheduleEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ThoughtWeaveDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
    abstract fun scheduleDao(): ScheduleDao
}
