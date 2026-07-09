package com.example.watchlist.common

import org.junit.Assert.assertEquals
import org.junit.Test

class PriceFormatTest {

    @Test
    fun `large prices use two decimals with grouping`() {
        assertEquals("$68,000.50", PriceFormat.price(68_000.5))
    }

    @Test
    fun `sub-dollar prices use more precision`() {
        assertEquals("$0.6200", PriceFormat.price(0.62))
    }

    @Test
    fun `percent carries an explicit sign`() {
        assertEquals("+1.24%", PriceFormat.percent(1.2373))
        assertEquals("-0.80%", PriceFormat.percent(-0.8))
    }
}
