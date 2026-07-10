package com.example.watchlist.data.repository

import com.example.watchlist.data.DemoModeManager
import com.example.watchlist.data.remote.FinnhubApi
import com.example.watchlist.data.remote.dto.CryptoSymbolDto
import com.example.watchlist.data.remote.dto.QuoteDto
import com.example.watchlist.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultInstrumentRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeFinnhubApi(private val symbols: List<CryptoSymbolDto>) : FinnhubApi {
        override suspend fun cryptoSymbols(exchange: String, token: String) = symbols
        override suspend fun quote(symbol: String, token: String) = QuoteDto()
    }

    private fun repository(symbols: List<CryptoSymbolDto>): DefaultInstrumentRepository {
        val demo = DemoModeManager().apply { setEnabled(false) }
        return DefaultInstrumentRepository(FakeFinnhubApi(symbols), demo, mainDispatcherRule.testDispatcher)
    }

    @Test
    fun `search ranks the liquid exact-base USDT pair first`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val symbols = listOf(
                CryptoSymbolDto("BINANCE:ENGBTC", "ENG/BTC"),
                CryptoSymbolDto("BINANCE:BTCUSDC", "BTC/USDC"),
                CryptoSymbolDto("BINANCE:1000BTCUSDT", "1000BTC/USDT"),
                CryptoSymbolDto("BINANCE:BTCUSDT", "BTC/USDT"),
            )

            val results = repository(symbols).search("BTC").getOrThrow()

            assertEquals("BINANCE:BTCUSDT", results.first().symbol)
            // Every result at least contains the query.
            assert(results.all { it.symbol.contains("BTC", ignoreCase = true) })
        }

    @Test
    fun `search excludes non-matches`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val symbols = listOf(
                CryptoSymbolDto("BINANCE:ETHUSDT", "ETH/USDT"),
                CryptoSymbolDto("BINANCE:SOLUSDT", "SOL/USDT"),
            )

            val results = repository(symbols).search("ETH").getOrThrow()

            assertEquals(listOf("BINANCE:ETHUSDT"), results.map { it.symbol })
        }
}
