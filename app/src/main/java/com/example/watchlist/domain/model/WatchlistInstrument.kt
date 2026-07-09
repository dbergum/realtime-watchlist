package com.example.watchlist.domain.model

/**
 * An instrument the user has saved to their watchlist.
 *
 * @param snapshotPrice the REST price captured when the item was added (or last refreshed).
 *   Used as the baseline for the percentage-change indicator. Null when no snapshot was
 *   available at add time (the row then waits for the first live tick).
 * @param addedAt epoch millis of when the item was added; used for stable default ordering.
 */
data class WatchlistInstrument(
    val symbol: String,
    val displayName: String,
    val snapshotPrice: Double?,
    val addedAt: Long,
)
