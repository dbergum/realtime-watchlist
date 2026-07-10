package com.example.watchlist.util

import com.example.watchlist.data.repository.WatchlistRepository
import com.example.watchlist.domain.model.ConnectionStatus
import com.example.watchlist.domain.model.Instrument
import com.example.watchlist.domain.model.PriceUpdate
import com.example.watchlist.domain.model.WatchlistInstrument
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [WatchlistRepository] whose flows are directly mutable, so tests can push watchlist
 * changes, price ticks, and connection-state changes and observe the ViewModel react.
 */
class FakeWatchlistRepository : WatchlistRepository {

    val watchlistFlow = MutableStateFlow<List<WatchlistInstrument>>(emptyList())
    val livePricesFlow = MutableStateFlow<Map<String, PriceUpdate>>(emptyMap())
    val connectionFlow = MutableStateFlow(ConnectionStatus.CONNECTING)

    override val watchlist = watchlistFlow
    override val livePrices = livePricesFlow
    override val connectionStatus = connectionFlow

    val added = mutableListOf<Instrument>()
    val removed = mutableListOf<String>()
    var refreshCount = 0

    override suspend fun add(instrument: Instrument) {
        added += instrument
    }

    override suspend fun remove(symbol: String) {
        removed += symbol
        watchlistFlow.value = watchlistFlow.value.filterNot { it.symbol == symbol }
    }

    override suspend fun refreshPrices() {
        refreshCount++
    }
}
