package com.example.watchlist.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WatchlistEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        const val NAME = "watchlist.db"
    }
}
