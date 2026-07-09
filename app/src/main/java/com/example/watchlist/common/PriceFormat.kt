package com.example.watchlist.common

import java.util.Locale
import kotlin.math.abs

/** Formatting helpers shared by the watchlist rows. Pure functions so they are trivially testable. */
object PriceFormat {

    /**
     * Formats a price with a sensible number of decimals for its magnitude: large prices
     * (e.g. BTC) show 2 decimals, sub-dollar prices (e.g. some tokens) show more precision.
     */
    fun price(value: Double, locale: Locale = Locale.US): String {
        val decimals = when {
            abs(value) >= 1.0 -> 2
            abs(value) >= 0.01 -> 4
            else -> 6
        }
        return "$" + String.format(locale, "%,.${decimals}f", value)
    }

    /** Formats a percentage change with an explicit sign, e.g. `"+1.24%"` / `"-0.80%"`. */
    fun percent(value: Double, locale: Locale = Locale.US): String {
        val sign = if (value >= 0) "+" else ""
        return sign + String.format(locale, "%.2f", value) + "%"
    }
}
