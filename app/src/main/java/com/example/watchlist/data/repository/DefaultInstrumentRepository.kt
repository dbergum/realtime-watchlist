package com.example.watchlist.data.repository

import com.example.watchlist.BuildConfig
import com.example.watchlist.data.DemoModeManager
import com.example.watchlist.data.demo.DemoCatalog
import com.example.watchlist.data.remote.FinnhubApi
import com.example.watchlist.data.remote.dto.CryptoSymbolDto
import com.example.watchlist.di.IoDispatcher
import com.example.watchlist.domain.model.Instrument
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Default [InstrumentRepository].
 *
 * In demo mode it serves the fixed [DemoCatalog]. In live mode it fetches the full Binance crypto
 * symbol list from Finnhub **once**, caches it, and filters client-side — this is more reliable
 * for crypto than the generic `/search` endpoint and keeps subsequent searches instant and
 * offline. Snapshot prices come from `/quote` (the current price), which works for crypto on the
 * free plan.
 */
@Singleton
class DefaultInstrumentRepository @Inject constructor(
    private val api: FinnhubApi,
    private val demoMode: DemoModeManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : InstrumentRepository {

    private val apiKey: String = BuildConfig.FINNHUB_API_KEY

    private val cacheMutex = Mutex()
    @Volatile
    private var cachedSymbols: List<Instrument>? = null

    override suspend fun search(query: String): Result<List<Instrument>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return Result.success(emptyList())

        if (demoMode.enabled.value) {
            return Result.success(DemoCatalog.search(trimmed))
        }

        return try {
            val all = loadSymbols()
            val query = trimmed.uppercase()
            val matches = all
                .mapNotNull { instrument -> rankOf(instrument, query)?.let { instrument to it } }
                // Rank first, then prefer shorter symbols (BTCUSDT over 1000BTCUSDT), then alpha.
                .sortedWith(compareBy({ it.second }, { it.first.symbol.length }, { it.first.symbol }))
                .map { it.first }
                .take(MAX_RESULTS)
            Result.success(matches)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Scores how well an instrument matches [query] (lower is better), or null if it doesn't match.
     * Exact and prefix matches on the base asset rank above substring matches, and USD/USDT quote
     * pairs (the liquid ones that actually stream trades) are preferred within each tier — so
     * searching "BTC" surfaces "BTC/USDT" at the top instead of obscure pairs like "ENGBTC".
     */
    private fun rankOf(instrument: Instrument, query: String): Int? {
        val display = instrument.displayName.uppercase()      // e.g. "BTC/USDT"
        val base = display.substringBefore("/")               // e.g. "BTC"
        val quote = display.substringAfter("/", "")           // e.g. "USDT"
        val symbol = instrument.symbol.substringAfter(":").uppercase() // e.g. "BTCUSDT"
        val liquidQuote = quote == "USDT" || quote == "USD"
        val quoteBoost = if (liquidQuote) 0 else 1

        val tier = when {
            base == query -> 0                                // exact base match
            base.startsWith(query) -> 2                        // base prefix, e.g. "ETH" -> "ETHFI"
            display.contains(query) || symbol.contains(query) -> 4 // anywhere
            else -> return null
        }
        return tier + quoteBoost
    }

    override suspend fun snapshotPrice(symbol: String): Double? {
        if (demoMode.enabled.value) return DemoCatalog.seedPrice(symbol)

        return try {
            withContext(ioDispatcher) { api.quote(symbol, apiKey).price }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A missing snapshot is not fatal: the row waits for the first live tick.
            null
        }
    }

    private suspend fun loadSymbols(): List<Instrument> {
        cachedSymbols?.let { return it }
        return cacheMutex.withLock {
            cachedSymbols ?: withContext(ioDispatcher) {
                api.cryptoSymbols(FinnhubApi.DEFAULT_EXCHANGE, apiKey)
                    .map { it.toInstrument() }
                    .also { cachedSymbols = it }
            }
        }
    }

    private companion object {
        const val MAX_RESULTS = 50
    }
}

private fun CryptoSymbolDto.toInstrument(): Instrument = Instrument(
    symbol = symbol,
    displayName = displaySymbol.ifBlank { description.ifBlank { symbol } },
)
