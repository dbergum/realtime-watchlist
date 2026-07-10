package com.example.watchlist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.watchlist.domain.model.WatchlistInstrument

/**
 * Room row for a watchlisted instrument. The Finnhub symbol is the natural primary key.
 *
 * @param snapshotPrice the REST snapshot captured at add time; the percentage-change baseline.
 * @param lastPrice the most recently observed live price, cached here so it can be shown
 *   immediately on the next app launch without waiting for the WebSocket.
 */
@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val symbol: String,
    val displayName: String,
    val snapshotPrice: Double?,
    val lastPrice: Double?,
    val addedAt: Long,
)

fun WatchlistEntity.toDomain(): WatchlistInstrument = WatchlistInstrument(
    symbol = symbol,
    displayName = displayName,
    snapshotPrice = snapshotPrice,
    lastPrice = lastPrice,
    addedAt = addedAt,
)
