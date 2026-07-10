package com.example.watchlist.data.repository

import com.example.watchlist.data.DemoModeManager
import com.example.watchlist.data.local.WatchlistDao
import com.example.watchlist.data.local.WatchlistEntity
import com.example.watchlist.data.local.toDomain
import com.example.watchlist.data.remote.stream.FakePriceStreamClient
import com.example.watchlist.data.remote.stream.FinnhubPriceStreamClient
import com.example.watchlist.data.remote.stream.PriceStreamClient
import com.example.watchlist.data.remote.stream.StreamEvent
import com.example.watchlist.di.ApplicationScope
import com.example.watchlist.domain.model.ConnectionStatus
import com.example.watchlist.domain.model.Instrument
import com.example.watchlist.domain.model.PriceUpdate
import com.example.watchlist.domain.model.WatchlistInstrument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DefaultWatchlistRepository @Inject constructor(
    private val dao: WatchlistDao,
    private val instrumentRepository: InstrumentRepository,
    private val realStream: FinnhubPriceStreamClient,
    private val fakeStream: FakePriceStreamClient,
    private val demoMode: DemoModeManager,
    @ApplicationScope private val appScope: CoroutineScope,
) : WatchlistRepository {

    override val watchlist: Flow<List<WatchlistInstrument>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    private val symbols: Flow<Set<String>> =
        watchlist.map { list -> list.map { it.symbol }.toSet() }.distinctUntilChanged()

    /**
     * Shared price-stream events. Switching demo mode swaps the underlying client
     * (`flatMapLatest`). `WhileSubscribed` keeps the socket open only while the UI observes, and
     * tears it down shortly after the app is backgrounded.
     */
    private val streamEvents: Flow<StreamEvent> =
        demoMode.enabled
            .flatMapLatest { demo -> activeClient(demo).stream(symbols) }
            .shareIn(appScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), replay = 1)

    override val connectionStatus: Flow<ConnectionStatus> =
        streamEvents
            .filterIsInstance<StreamEvent.Connection>()
            .map { it.status }
            .onStart { emit(ConnectionStatus.CONNECTING) }
            .distinctUntilChanged()

    // Throttles how often each symbol's live price is written back to Room.
    private val lastPersistedAt = HashMap<String, Long>()

    override val livePrices: Flow<Map<String, PriceUpdate>> =
        streamEvents
            .filterIsInstance<StreamEvent.Trade>()
            .onEach { trade -> cacheLivePrice(trade) }
            .scan(emptyMap<String, PriceUpdate>()) { acc, trade ->
                val existing = acc[trade.symbol]
                acc + (trade.symbol to PriceUpdate(
                    symbol = trade.symbol,
                    price = trade.price,
                    previousPrice = existing?.price,
                    // The first tick of the session anchors the change baseline.
                    openPrice = existing?.openPrice ?: trade.price,
                    timestampMs = trade.timestampMs,
                ))
            }

    override suspend fun add(instrument: Instrument) {
        if (dao.exists(instrument.symbol)) return
        val snapshot = instrumentRepository.snapshotPrice(instrument.symbol)
        dao.upsert(
            WatchlistEntity(
                symbol = instrument.symbol,
                displayName = instrument.displayName,
                snapshotPrice = snapshot,
                // Seed the cached price with the snapshot so a just-added row shows a value at once.
                lastPrice = snapshot,
                addedAt = System.currentTimeMillis(),
            ),
        )
    }

    /**
     * Persists the latest live price to Room so it can be shown immediately on the next launch.
     * Rate-limited per symbol to avoid hammering the database on every tick.
     */
    private suspend fun cacheLivePrice(trade: StreamEvent.Trade) {
        val now = System.currentTimeMillis()
        val previous = lastPersistedAt[trade.symbol] ?: 0L
        if (now - previous >= PRICE_PERSIST_INTERVAL_MS) {
            lastPersistedAt[trade.symbol] = now
            runCatching { dao.updateLastPrice(trade.symbol, trade.price) }
        }
    }

    override suspend fun remove(symbol: String) = dao.deleteBySymbol(symbol)

    override suspend fun refreshPrices() {
        dao.allSymbols().forEach { symbol ->
            // Update the displayed (last) price, not the snapshot baseline, so % change stays
            // anchored to the add-time price.
            instrumentRepository.snapshotPrice(symbol)?.let { dao.updateLastPrice(symbol, it) }
        }
    }

    private fun activeClient(demo: Boolean): PriceStreamClient = if (demo) fakeStream else realStream

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val PRICE_PERSIST_INTERVAL_MS = 10_000L
    }
}
