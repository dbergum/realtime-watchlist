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

    override val livePrices: Flow<Map<String, PriceUpdate>> =
        streamEvents
            .filterIsInstance<StreamEvent.Trade>()
            .scan(emptyMap<String, PriceUpdate>()) { acc, trade ->
                val previous = acc[trade.symbol]?.price
                acc + (trade.symbol to PriceUpdate(
                    symbol = trade.symbol,
                    price = trade.price,
                    previousPrice = previous,
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
                addedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun remove(symbol: String) = dao.deleteBySymbol(symbol)

    override suspend fun refreshSnapshots() {
        dao.allSymbols().forEach { symbol ->
            instrumentRepository.snapshotPrice(symbol)?.let { dao.updateSnapshotPrice(symbol, it) }
        }
    }

    private fun activeClient(demo: Boolean): PriceStreamClient = if (demo) fakeStream else realStream

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
