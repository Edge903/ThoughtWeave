package io.wit117.thoughtweave.data.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.wit117.thoughtweave.data.local.dao.MemoDao
import io.wit117.thoughtweave.data.local.dao.ScheduleDao
import io.wit117.thoughtweave.data.local.database.ThoughtWeaveDatabase
import io.wit117.thoughtweave.data.repository.MemoRepositoryImpl
import io.wit117.thoughtweave.data.repository.ScheduleRepositoryImpl
import io.wit117.thoughtweave.domain.repository.MemoRepository
import io.wit117.thoughtweave.domain.repository.ScheduleRepository
import java.time.Clock
import java.time.ZoneId
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindMemoRepository(impl: MemoRepositoryImpl): MemoRepository

    @Binds
    abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): ThoughtWeaveDatabase =
            Room.databaseBuilder(context, ThoughtWeaveDatabase::class.java, "thoughtweave.db")
                .build()

        @Provides
        fun provideMemoDao(database: ThoughtWeaveDatabase): MemoDao = database.memoDao()

        @Provides
        fun provideScheduleDao(database: ThoughtWeaveDatabase): ScheduleDao = database.scheduleDao()

        @Provides
        fun provideClock(): Clock = Clock.systemUTC()

        /** Not a singleton: the device time zone can change while the app is running. */
        @Provides
        fun provideZoneId(): ZoneId = ZoneId.systemDefault()
    }
}
