package com.example.watchlist.ui.watchlist

import app.cash.turbine.test
import com.example.watchlist.common.TimeSource
import com.example.watchlist.data.DemoModeManager
import com.example.watchlist.domain.model.ConnectionStatus
import com.example.watchlist.domain.model.PriceMovement
import com.example.watchlist.domain.model.PriceUpdate
import com.example.watchlist.domain.model.WatchlistInstrument
import com.example.watchlist.util.FakeWatchlistRepository
import com.example.watchlist.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WatchlistViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeWatchlistRepository()
    private var now = 1_000L

    private val symbol = "BINANCE:BTCUSDT"
    private val instrument = WatchlistInstrument(
        symbol = symbol,
        displayName = "BTC/USDT",
        snapshotPrice = 90.0,
        addedAt = 0L,
    )

    private fun viewModel() =
        WatchlistViewModel(repository, DemoModeManager(), TimeSource { now })

    @Test
    fun `emits Empty when the watchlist has no items`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel().uiState.test {
                assertEquals(WatchlistUiState.Loading, awaitItem())
                assertEquals(WatchlistUiState.Empty, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `missing price renders a waiting state, then a tick fills it with Up movement`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.watchlistFlow.value = listOf(instrument)
            repository.connectionFlow.value = ConnectionStatus.CONNECTED

            viewModel().uiState.test {
                assertEquals(WatchlistUiState.Loading, awaitItem())

                val waiting = awaitItem() as WatchlistUiState.Content
                assertNull(waiting.items.single().price)

                now = 2_000L
                repository.livePricesFlow.value =
                    mapOf(symbol to PriceUpdate(symbol, price = 100.0, previousPrice = 90.0, timestampMs = 2_000L))

                val ticked = awaitItem() as WatchlistUiState.Content
                val item = ticked.items.single()
                assertEquals(100.0, item.price)
                assertEquals(PriceMovement.UP, item.movement)
                assertEquals(false, item.isStale)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `marks the row stale once the last update is older than the threshold`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.watchlistFlow.value = listOf(instrument)
            repository.connectionFlow.value = ConnectionStatus.CONNECTED
            now = 1_000_000L

            viewModel().uiState.test {
                assertEquals(WatchlistUiState.Loading, awaitItem())
                awaitItem() // initial Content with no price

                repository.livePricesFlow.value =
                    mapOf(symbol to PriceUpdate(symbol, price = 50.0, previousPrice = null, timestampMs = 0L))

                val stale = awaitItem() as WatchlistUiState.Content
                assertTrue(stale.items.single().isStale)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `surfaces the reconnecting connection status`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.watchlistFlow.value = listOf(instrument)
            repository.connectionFlow.value = ConnectionStatus.CONNECTED

            viewModel().uiState.test {
                assertEquals(WatchlistUiState.Loading, awaitItem())
                assertEquals(
                    ConnectionStatus.CONNECTED,
                    (awaitItem() as WatchlistUiState.Content).connection,
                )

                repository.connectionFlow.value = ConnectionStatus.RECONNECTING

                assertEquals(
                    ConnectionStatus.RECONNECTING,
                    (awaitItem() as WatchlistUiState.Content).connection,
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onRemove delegates to the repository`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModel()
            viewModel.onRemove(symbol)
            testScheduler.advanceUntilIdle()
            assertEquals(listOf(symbol), repository.removed)
        }
}
