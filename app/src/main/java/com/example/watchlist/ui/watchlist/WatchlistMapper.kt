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
        val price = update?.price
        val baseline = instrument.snapshotPrice
        val changePercent = if (price != null && baseline != null && baseline != 0.0) {
            (price - baseline) / baseline * 100.0
        } else {
            null
        }
        val isStale = update != null && (nowMs - update.timestampMs) > staleThresholdMs
        return WatchlistItem(
            symbol = instrument.symbol,
            displayName = instrument.displayName,
            price = price,
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
