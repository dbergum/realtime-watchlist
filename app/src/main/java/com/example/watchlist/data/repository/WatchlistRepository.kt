package com.example.watchlist.data.repository

import com.example.watchlist.domain.model.ConnectionStatus
import com.example.watchlist.domain.model.Instrument
import com.example.watchlist.domain.model.PriceUpdate
import com.example.watchlist.domain.model.WatchlistInstrument
import kotlinx.coroutines.flow.Flow

/**
 * Owns the user's watchlist (persisted) and the derived live-price/connection streams for the
 * symbols on it. Everything is exposed as [Flow] so the UI observes reactively.
 */
interface WatchlistRepository {

    /** The persisted watchlist; Room re-emits on every change. */
    val watchlist: Flow<List<WatchlistInstrument>>

    /** Latest price per symbol, accumulated from the live stream. */
    val livePrices: Flow<Map<String, PriceUpdate>>

    /** Live-stream connection state, for the reconnecting banner. */
    val connectionStatus: Flow<ConnectionStatus>

    /** Adds an instrument, capturing a REST snapshot price as the change baseline. No-op if present. */
    suspend fun add(instrument: Instrument)

    suspend fun remove(symbol: String)

    /**
     * Pulls the current price for every watchlist symbol over REST (`/quote`) and caches it, so a
     * price appears without waiting for the WebSocket. Called on startup and on pull-to-refresh.
     */
    suspend fun refreshPrices()
}
