package com.example.watchlist.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from Finnhub `/quote`. Unlike `/crypto/candle` (premium), this endpoint works on the
 * free plan for crypto and returns the current price directly, so it can seed a price immediately
 * without waiting for the WebSocket.
 */
@Serializable
data class QuoteDto(
    @SerialName("c") val current: Double = 0.0,   // current price
    @SerialName("o") val open: Double = 0.0,       // today's open
    @SerialName("pc") val previousClose: Double = 0.0,
    @SerialName("t") val timestamp: Long = 0,      // unix seconds
) {
    /** Current price, or null when the symbol is unknown (Finnhub returns 0 for invalid symbols). */
    val price: Double?
        get() = if (current > 0.0) current else null
}
