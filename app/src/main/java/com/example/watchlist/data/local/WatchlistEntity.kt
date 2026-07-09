package com.example.watchlist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.watchlist.domain.model.WatchlistInstrument

/** Room row for a watchlisted instrument. The Finnhub symbol is the natural primary key. */
@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val symbol: String,
    val displayName: String,
    val snapshotPrice: Double?,
    val addedAt: Long,
)

fun WatchlistEntity.toDomain(): WatchlistInstrument = WatchlistInstrument(
    symbol = symbol,
    displayName = displayName,
    snapshotPrice = snapshotPrice,
    addedAt = addedAt,
)
