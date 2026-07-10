package com.example.watchlist.ui.watchlist

import com.example.watchlist.domain.model.PriceMovement
import com.example.watchlist.domain.model.PriceUpdate
import com.example.watchlist.domain.model.WatchlistInstrument

/**
 * Pure functions that turn persisted instruments + live prices into sorted, filtered UI rows.
 * Kept separate from the ViewModel so the (interesting) logic is unit-testable in isolation.
 */
object WatchlistMapper {

    const val STALE_THRESHOLD_MS = 15_000L

    fun buildItem(
        instrument: WatchlistInstrument,
        update: PriceUpdate?,
        nowMs: Long,
        staleThresholdMs: Long = STALE_THRESHOLD_MS,
    ): WatchlistItem {
        val livePrice = update?.price
        // Show the live price if we have one; otherwise fall back to the price in Room (a REST
        // snapshot pulled at startup, or the last cached tick) so a value appears immediately
        // instead of "waiting for price".
        val displayPrice = livePrice ?: instrument.lastPrice
        // Baseline for % change is the add-time snapshot; fall back to the session-open price from
        // the live stream if no snapshot was captured.
        val baseline = instrument.snapshotPrice ?: update?.openPrice
        val changePercent = if (displayPrice != null && baseline != null && baseline != 0.0) {
            (displayPrice - baseline) / baseline * 100.0
        } else {
            null
        }
        // Staleness reflects the live stream: a tick is stale once it's older than the threshold.
        // A REST/cached price shown before the socket connects isn't flagged (the connection
        // banner communicates that live data isn't flowing yet).
        val isStale = update != null && (nowMs - update.timestampMs) > staleThresholdMs
        return WatchlistItem(
            symbol = instrument.symbol,
            displayName = instrument.displayName,
            price = displayPrice,
            changePercent = changePercent,
            movement = update?.movement ?: PriceMovement.FLAT,
            isStale = isStale,
        )
    }

    fun filter(items: List<WatchlistItem>, query: String): List<WatchlistItem> {
        val q = query.trim()
        if (q.isEmpty()) return items
        return items.filter {
            it.symbol.contains(q, ignoreCase = true) || it.displayName.contains(q, ignoreCase = true)
        }
    }

    fun sort(items: List<WatchlistItem>, option: SortOption): List<WatchlistItem> = when (option) {
        SortOption.SYMBOL -> items.sortedBy { it.symbol }
        // Rows without a price yet sort to the bottom.
        SortOption.PRICE -> items.sortedByDescending { it.price ?: Double.NEGATIVE_INFINITY }
        SortOption.CHANGE -> items.sortedByDescending { it.changePercent ?: Double.NEGATIVE_INFINITY }
    }
}
