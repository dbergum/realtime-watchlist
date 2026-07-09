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
 * offline. Snapshots come from the last close of a short recent candle window.
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
            val matches = all
                .filter {
                    it.symbol.contains(trimmed, ignoreCase = true) ||
                        it.displayName.contains(trimmed, ignoreCase = true)
                }
                .take(MAX_RESULTS)
            Result.success(matches)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun snapshotPrice(symbol: String): Double? {
        if (demoMode.enabled.value) return DemoCatalog.seedPrice(symbol)

        return try {
            withContext(ioDispatcher) {
                val nowSec = System.currentTimeMillis() / 1000
                api.cryptoCandle(
                    symbol = symbol,
                    resolution = SNAPSHOT_RESOLUTION,
                    from = nowSec - SNAPSHOT_WINDOW_SEC,
                    to = nowSec,
                    token = apiKey,
                ).lastClose
            }
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
        const val SNAPSHOT_RESOLUTION = "1"
        const val SNAPSHOT_WINDOW_SEC = 3_600L
    }
}

private fun CryptoSymbolDto.toInstrument(): Instrument = Instrument(
    symbol = symbol,
    displayName = displaySymbol.ifBlank { description.ifBlank { symbol } },
)
