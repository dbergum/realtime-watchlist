package com.example.watchlist.data.remote.stream

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WsMessagesTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `parses a trade frame into trade events`() {
        val frame =
            """{"type":"trade","data":[{"s":"BINANCE:BTCUSDT","p":68000.5,"t":1700000000000,"v":0.01}]}"""
        val events = parseTradeMessage(json, frame)
        assertEquals(1, events.size)
        assertEquals(StreamEvent.Trade("BINANCE:BTCUSDT", 68000.5, 1700000000000), events.single())
    }

    @Test
    fun `parses multiple trades in one frame`() {
        val frame = """{"type":"trade","data":[
            {"s":"A","p":1.0,"t":1,"v":1},
            {"s":"B","p":2.0,"t":2,"v":1}
        ]}"""
        assertEquals(listOf("A", "B"), parseTradeMessage(json, frame).map { it.symbol })
    }

    @Test
    fun `ignores ping frames`() {
        assertTrue(parseTradeMessage(json, """{"type":"ping"}""").isEmpty())
    }

    @Test
    fun `ignores malformed json`() {
        assertTrue(parseTradeMessage(json, "definitely not json").isEmpty())
    }
}
