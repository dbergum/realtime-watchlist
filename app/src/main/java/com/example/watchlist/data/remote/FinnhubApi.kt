package com.example.watchlist.data.remote

import com.example.watchlist.data.remote.dto.CryptoSymbolDto
import com.example.watchlist.data.remote.dto.QuoteDto
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

    /**
     * Current quote for a symbol. Works for crypto on the free plan (the `/crypto/candle` endpoint
     * is premium), so it's used to pull a price directly without waiting for the WebSocket.
     */
    @GET("api/v1/quote")
    suspend fun quote(
        @Query("symbol") symbol: String,
        @Query("token") token: String,
    ): QuoteDto

    companion object {
        const val BASE_URL = "https://finnhub.io/"
        const val DEFAULT_EXCHANGE = "BINANCE"
    }
}
