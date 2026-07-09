package com.example.watchlist.data.remote.stream

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Finnhub trade-stream message envelope. Non-trade frames (notably `{"type":"ping"}`) decode with
 * an empty [data] list and are ignored.
 */
@Serializable
data class WsMessage(
    val type: String = "",
    val data: List<WsTrade> = emptyList(),
)

@Serializable
data class WsTrade(
    val s: String,   // symbol
    val p: Double,   // price
    val t: Long,     // trade timestamp (epoch millis)
)

/**
 * Parses a raw WebSocket text frame into [StreamEvent.Trade]s. Returns an empty list for pings,
 * unknown types, or malformed JSON — the stream never crashes on a bad frame.
 */
fun parseTradeMessage(json: Json, text: String): List<StreamEvent.Trade> =
    runCatching {
        val message = json.decodeFromString<WsMessage>(text)
        if (message.type != "trade") emptyList()
        else message.data.map { StreamEvent.Trade(it.s, it.p, it.t) }
    }.getOrDefault(emptyList())
