package com.example.watchlist.data.remote.dto

import kotlinx.serialization.Serializable

/** One entry from Finnhub `/crypto/symbol?exchange=BINANCE`. */
@Serializable
data class CryptoSymbolDto(
    val symbol: String,          // e.g. "BINANCE:BTCUSDT" — used for REST + WebSocket
    val displaySymbol: String = "", // e.g. "BTC/USDT"
    val description: String = "",
)
