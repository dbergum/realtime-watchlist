package com.example.watchlist.domain.model

/**
 * The latest known price for a symbol, carrying enough context to render movement.
 *
 * @param previousPrice the price from the prior tick (null on the very first update), used to
 *   derive the tick-to-tick [PriceMovement].
 * @param timestampMs when this price was observed (epoch millis); drives stale-data detection.
 */
data class PriceUpdate(
    val symbol: String,
    val price: Double,
    val previousPrice: Double?,
    val timestampMs: Long,
) {
    val movement: PriceMovement
        get() = when {
            previousPrice == null || price == previousPrice -> PriceMovement.FLAT
            price > previousPrice -> PriceMovement.UP
            else -> PriceMovement.DOWN
        }
}

enum class PriceMovement { UP, DOWN, FLAT }
