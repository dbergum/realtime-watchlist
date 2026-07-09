package com.example.watchlist.di

import com.example.watchlist.common.TimeSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        // SupervisorJob so a failure in one stream collector doesn't tear the whole scope down.
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    fun provideTimeSource(): TimeSource = TimeSource { System.currentTimeMillis() }
}
