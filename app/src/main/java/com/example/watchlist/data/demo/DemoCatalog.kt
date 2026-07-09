package com.example.watchlist.data.demo

import com.example.watchlist.domain.model.Instrument

/** A demo instrument plus a plausible seed price used to generate simulated ticks. */
data class DemoInstrument(
    val symbol: String,
    val displayName: String,
    val seedPrice: Double,
) {
    fun toInstrument(): Instrument = Instrument(symbol, displayName)
}

/**
 * A small, fixed catalogue of well-known crypto pairs used by demo mode for both search and the
 * fake price stream. Seed prices are only starting points — the fake stream random-walks from
 * them so movement, staleness, and reconnection states are all observable offline.
 */
object DemoCatalog {

    val instruments: List<DemoInstrument> = listOf(
        DemoInstrument("BINANCE:BTCUSDT", "Bitcoin / USDT", 68_000.0),
        DemoInstrument("BINANCE:ETHUSDT", "Ethereum / USDT", 3_500.0),
        DemoInstrument("BINANCE:SOLUSDT", "Solana / USDT", 172.0),
        DemoInstrument("BINANCE:BNBUSDT", "BNB / USDT", 585.0),
        DemoInstrument("BINANCE:XRPUSDT", "XRP / USDT", 0.62),
        DemoInstrument("BINANCE:ADAUSDT", "Cardano / USDT", 0.45),
        DemoInstrument("BINANCE:DOGEUSDT", "Dogecoin / USDT", 0.16),
        DemoInstrument("BINANCE:LTCUSDT", "Litecoin / USDT", 84.0),
        DemoInstrument("BINANCE:AVAXUSDT", "Avalanche / USDT", 37.0),
        DemoInstrument("BINANCE:LINKUSDT", "Chainlink / USDT", 18.0),
    )

    private val bySymbol = instruments.associateBy { it.symbol }

    fun seedPrice(symbol: String): Double = bySymbol[symbol]?.seedPrice ?: 100.0

    /** Case-insensitive match on symbol or display name. Blank query returns the full catalogue. */
    fun search(query: String): List<Instrument> {
        val q = query.trim()
        if (q.isEmpty()) return instruments.map { it.toInstrument() }
        return instruments
            .filter { it.symbol.contains(q, ignoreCase = true) || it.displayName.contains(q, ignoreCase = true) }
            .map { it.toInstrument() }
    }
}
