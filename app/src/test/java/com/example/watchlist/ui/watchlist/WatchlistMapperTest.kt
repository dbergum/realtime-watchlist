package com.example.watchlist.ui.watchlist

import com.example.watchlist.domain.model.PriceMovement
import com.example.watchlist.domain.model.PriceUpdate
import com.example.watchlist.domain.model.WatchlistInstrument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchlistMapperTest {

    private fun instrument(symbol: String, snapshot: Double? = 100.0) =
        WatchlistInstrument(symbol, symbol, snapshot, addedAt = 0L)

    @Test
    fun `buildItem computes percent change against the snapshot baseline`() {
        val item = WatchlistMapper.buildItem(
            instrument = instrument("A", snapshot = 100.0),
            update = PriceUpdate("A", price = 110.0, previousPrice = 105.0, timestampMs = 0L),
            nowMs = 0L,
        )
        assertEquals(110.0, item.price)
        assertEquals(10.0, item.changePercent!!, 0.0001)
        assertEquals(PriceMovement.UP, item.movement)
    }

    @Test
    fun `buildItem leaves price and change null when there is no update`() {
        val item = WatchlistMapper.buildItem(instrument("A"), update = null, nowMs = 0L)
        assertNull(item.price)
        assertNull(item.changePercent)
        assertFalse(item.isStale)
    }

    @Test
    fun `buildItem flags stale when the update is older than the threshold`() {
        val update = PriceUpdate("A", price = 1.0, previousPrice = null, timestampMs = 0L)
        val fresh = WatchlistMapper.buildItem(instrument("A"), update, nowMs = 10_000L, staleThresholdMs = 15_000L)
        val stale = WatchlistMapper.buildItem(instrument("A"), update, nowMs = 20_000L, staleThresholdMs = 15_000L)
        assertFalse(fresh.isStale)
        assertTrue(stale.isStale)
    }

    @Test
    fun `sort by change orders descending with missing values last`() {
        val items = listOf(
            item(symbol = "LOW", change = -5.0),
            item(symbol = "NONE", change = null),
            item(symbol = "HIGH", change = 8.0),
        )
        val sorted = WatchlistMapper.sort(items, SortOption.CHANGE).map { it.symbol }
        assertEquals(listOf("HIGH", "LOW", "NONE"), sorted)
    }

    @Test
    fun `filter matches symbol or display name case-insensitively`() {
        val items = listOf(item("BINANCE:BTCUSDT"), item("BINANCE:ETHUSDT"))
        assertEquals(listOf("BINANCE:BTCUSDT"), WatchlistMapper.filter(items, "btc").map { it.symbol })
        assertEquals(2, WatchlistMapper.filter(items, "  ").size)
    }

    private fun item(symbol: String, change: Double? = null) = WatchlistItem(
        symbol = symbol,
        displayName = symbol,
        price = 1.0,
        changePercent = change,
        movement = PriceMovement.FLAT,
        isStale = false,
    )
}
