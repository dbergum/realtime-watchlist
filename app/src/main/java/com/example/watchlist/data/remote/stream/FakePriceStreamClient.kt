package com.example.watchlist.data.remote.stream

import com.example.watchlist.data.demo.DemoCatalog
import com.example.watchlist.domain.model.ConnectionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Demo-mode [PriceStreamClient] that emits synthetic trades so the full experience — live ticks,
 * movement colors, staleness, and reconnection — works offline with no API key. Prices random-walk
 * from the [DemoCatalog] seed for each symbol, and the stream periodically simulates a brief
 * reconnect so that state is demonstrable too.
 */
@Singleton
class FakePriceStreamClient @Inject constructor() : PriceStreamClient {

    override fun stream(symbols: Flow<Set<String>>): Flow<StreamEvent> = channelFlow {
        val desired = MutableStateFlow<Set<String>>(emptySet())
        launch { symbols.collect { desired.value = it } }

        val lastPrices = mutableMapOf<String, Double>()

        trySend(StreamEvent.Connection(ConnectionStatus.CONNECTING))
        delay(300)
        trySend(StreamEvent.Connection(ConnectionStatus.CONNECTED))

        var tick = 0
        while (isActive) {
            if (tick > 0 && tick % RECONNECT_EVERY_TICKS == 0) {
                trySend(StreamEvent.Connection(ConnectionStatus.RECONNECTING))
                delay(RECONNECT_PAUSE_MS)
                trySend(StreamEvent.Connection(ConnectionStatus.CONNECTED))
            }

            for (symbol in desired.value) {
                val base = lastPrices[symbol] ?: DemoCatalog.seedPrice(symbol)
                val next = randomWalk(base)
                lastPrices[symbol] = next
                trySend(StreamEvent.Trade(symbol, next, System.currentTimeMillis()))
            }

            tick++
            delay(TICK_INTERVAL_MS)
        }
    }

    private fun randomWalk(base: Double): Double {
        val drift = Random.nextDouble(-MAX_STEP_FRACTION, MAX_STEP_FRACTION)
        return base * (1.0 + drift)
    }

    private companion object {
        const val TICK_INTERVAL_MS = 900L
        const val MAX_STEP_FRACTION = 0.002 // ±0.2% per tick
        const val RECONNECT_EVERY_TICKS = 25
        const val RECONNECT_PAUSE_MS = 1_500L
    }
}
