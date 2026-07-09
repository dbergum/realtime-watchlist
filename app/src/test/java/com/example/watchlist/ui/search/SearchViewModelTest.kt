package com.example.watchlist.ui.search

import app.cash.turbine.test
import com.example.watchlist.R
import com.example.watchlist.domain.model.Instrument
import com.example.watchlist.util.FakeInstrumentRepository
import com.example.watchlist.util.FakeWatchlistRepository
import com.example.watchlist.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val instrumentRepository = FakeInstrumentRepository()
    private val watchlistRepository = FakeWatchlistRepository()

    private val btc = Instrument("BINANCE:BTCUSDT", "BTC/USDT")

    private fun viewModel() = SearchViewModel(instrumentRepository, watchlistRepository)

    @Test
    fun `starts idle for a blank query`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModel().uiState.test {
            assertEquals(SearchUiState.Idle, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Loading then Success when instruments are returned`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModel()
            viewModel.uiState.test {
                assertEquals(SearchUiState.Idle, awaitItem())

                instrumentRepository.searchResult = Result.success(listOf(btc))
                viewModel.onQueryChange("btc")

                assertEquals(SearchUiState.Loading, awaitItem())
                assertEquals(SearchUiState.Success(listOf(btc)), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emits Empty when no instruments match`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModel()
            viewModel.uiState.test {
                assertEquals(SearchUiState.Idle, awaitItem())

                instrumentRepository.searchResult = Result.success(emptyList())
                viewModel.onQueryChange("zzz")

                assertEquals(SearchUiState.Loading, awaitItem())
                assertEquals(SearchUiState.Empty, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `emits Error with network message on IOException`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = viewModel()
            viewModel.uiState.test {
                assertEquals(SearchUiState.Idle, awaitItem())

                instrumentRepository.searchResult = Result.failure(IOException())
                viewModel.onQueryChange("btc")

                assertEquals(SearchUiState.Loading, awaitItem())
                assertEquals(SearchUiState.Error(R.string.error_network), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onAdd delegates to the watchlist repository`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel().onAdd(btc)
            testScheduler.advanceUntilIdle()
            assertEquals(listOf(btc), watchlistRepository.added)
        }
}
