package com.example.watchlist.domain.model

/**
 * A tradable instrument the user can search for and add to the watchlist.
 *
 * @param symbol the Finnhub symbol used for both REST and WebSocket calls, e.g.
 *   `"BINANCE:BTCUSDT"`.
 * @param displayName a human-friendly label, e.g. `"Binance BTC/USDT"`.
 */
data class Instrument(
    val symbol: String,
    val displayName: String,
)
