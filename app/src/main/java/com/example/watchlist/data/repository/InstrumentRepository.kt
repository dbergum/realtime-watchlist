package com.example.watchlist.data.repository

import com.example.watchlist.domain.model.Instrument

/** Searches for instruments and fetches point-in-time snapshot prices over REST. */
interface InstrumentRepository {

    /**
     * Returns instruments matching [query]. An empty/blank query returns an empty list so the UI
     * can show a "search prompt" state rather than firing a request.
     */
    suspend fun search(query: String): Result<List<Instrument>>

    /**
     * Best-effort snapshot price for [symbol], or null when unavailable (free-plan limits, no
     * candle history, etc.). Callers treat null as "waiting for the first live tick".
     */
    suspend fun snapshotPrice(symbol: String): Double?
}
