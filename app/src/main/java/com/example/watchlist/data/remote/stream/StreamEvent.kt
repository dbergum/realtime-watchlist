package com.example.watchlist.data.remote.stream

import com.example.watchlist.domain.model.ConnectionStatus

/** Events emitted by a [PriceStreamClient]: either a connection-state change or a trade tick. */
sealed interface StreamEvent {
    data class Connection(val status: ConnectionStatus) : StreamEvent
    data class Trade(val symbol: String, val price: Double, val timestampMs: Long) : StreamEvent
}
