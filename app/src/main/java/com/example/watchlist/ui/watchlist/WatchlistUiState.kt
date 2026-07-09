package com.example.watchlist.ui.watchlist

import androidx.annotation.StringRes
import com.example.watchlist.R
import com.example.watchlist.domain.model.ConnectionStatus
import com.example.watchlist.domain.model.PriceMovement

/** How the watchlist is ordered. */
enum class SortOption(@StringRes val labelRes: Int) {
    SYMBOL(R.string.sort_symbol),
    PRICE(R.string.sort_price),
    CHANGE(R.string.sort_change),
}

/**
 * A fully-resolved row for the watchlist UI.
 *
 * @param price null while waiting for the first price (missing-price state).
 * @param changePercent percent change vs the snapshot baseline; null when either price is missing.
 * @param isStale true when the last update is older than the staleness threshold.
 */
data class WatchlistItem(
    val symbol: String,
    val displayName: String,
    val price: Double?,
    val changePercent: Double?,
    val movement: PriceMovement,
    val isStale: Boolean,
)

sealed interface WatchlistUiState {
    data object Loading : WatchlistUiState
    data object Empty : WatchlistUiState
    data class Content(
        val items: List<WatchlistItem>,
        val connection: ConnectionStatus,
    ) : WatchlistUiState
}
