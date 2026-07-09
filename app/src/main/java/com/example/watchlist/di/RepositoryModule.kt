package com.example.watchlist.di

import com.example.watchlist.data.repository.DefaultInstrumentRepository
import com.example.watchlist.data.repository.DefaultWatchlistRepository
import com.example.watchlist.data.repository.InstrumentRepository
import com.example.watchlist.data.repository.WatchlistRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Consumed by Hilt's annotation processor at build time.
@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindInstrumentRepository(impl: DefaultInstrumentRepository): InstrumentRepository

    @Binds
    @Singleton
    abstract fun bindWatchlistRepository(impl: DefaultWatchlistRepository): WatchlistRepository
}
