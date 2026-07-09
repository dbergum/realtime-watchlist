package com.example.watchlist.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from Finnhub `/crypto/candle`. Finnhub uses terse single-letter keys; we only need the
 * close series (`c`) and the status (`s`, "ok" or "no_data").
 */
@Serializable
data class CandleDto(
    @SerialName("c") val close: List<Double> = emptyList(),
    @SerialName("s") val status: String = "no_data",
) {
    /** The most recent close, or null when the response carried no usable data. */
    val lastClose: Double?
        get() = if (status == "ok") close.lastOrNull() else null
}
