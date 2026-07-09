package com.example.watchlist.data.remote

import com.example.watchlist.data.remote.dto.CandleDto
import com.example.watchlist.data.remote.dto.CryptoSymbolDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Finnhub REST endpoints used by the app. The API token is passed per-request as a query
 * parameter (Finnhub also accepts it this way, which keeps the client construction simple).
 */
interface FinnhubApi {

    /** All symbols supported on an exchange, e.g. `exchange = "BINANCE"`. Fetched once and cached. */
    @GET("api/v1/crypto/symbol")
    suspend fun cryptoSymbols(
        @Query("exchange") exchange: String,
        @Query("token") token: String,
    ): List<CryptoSymbolDto>

    /** OHLC candles; we request a short recent window and use the last close as the snapshot price. */
    @GET("api/v1/crypto/candle")
    suspend fun cryptoCandle(
        @Query("symbol") symbol: String,
        @Query("resolution") resolution: String,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("token") token: String,
    ): CandleDto

    companion object {
        const val BASE_URL = "https://finnhub.io/"
        const val DEFAULT_EXCHANGE = "BINANCE"
    }
}
