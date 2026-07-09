package com.example.watchlist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {

    /** Observes the full watchlist. Room emits a fresh list on every change (single source of truth). */
    @Query("SELECT * FROM watchlist ORDER BY addedAt ASC")
    fun observeAll(): Flow<List<WatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WatchlistEntity)

    @Query("UPDATE watchlist SET snapshotPrice = :price WHERE symbol = :symbol")
    suspend fun updateSnapshotPrice(symbol: String, price: Double)

    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun deleteBySymbol(symbol: String)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE symbol = :symbol)")
    suspend fun exists(symbol: String): Boolean

    @Query("SELECT symbol FROM watchlist")
    suspend fun allSymbols(): List<String>
}
