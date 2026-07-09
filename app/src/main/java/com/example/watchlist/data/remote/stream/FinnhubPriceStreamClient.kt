package com.example.watchlist.data.remote.stream

import com.example.watchlist.BuildConfig
import com.example.watchlist.domain.model.ConnectionStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [PriceStreamClient] backed by Finnhub's trade WebSocket.
 *
 * Responsibilities:
 * - keep a single socket open and (re)subscribe the desired symbol set on open;
 * - apply incremental subscribe/unsubscribe frames as the watchlist changes mid-connection;
 * - detect drops and reconnect with capped exponential backoff, surfacing [ConnectionStatus]
 *   so the UI can show a reconnecting banner.
 */
@Singleton
class FinnhubPriceStreamClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) : PriceStreamClient {

    private val apiKey: String = BuildConfig.FINNHUB_API_KEY

    override fun stream(symbols: Flow<Set<String>>): Flow<StreamEvent> = channelFlow {
        val desired = MutableStateFlow<Set<String>>(emptySet())
        launch { symbols.collect { desired.value = it } }

        val lock = Any()
        val subscribed = mutableSetOf<String>()
        // AtomicReference because it is read/written from both the coroutine and OkHttp's
        // callback thread (@Volatile isn't allowed on a captured local variable).
        val activeSocket = AtomicReference<WebSocket?>(null)

        // Apply subscription diffs to whichever socket is currently open. On reconnect the socket
        // is null briefly; onOpen re-subscribes the full set, so nothing is lost.
        launch {
            desired.collect { want ->
                val socket = activeSocket.get() ?: return@collect
                synchronized(lock) {
                    (want - subscribed).forEach { socket.send(subscribeFrame(it)) }
                    (subscribed - want).forEach { socket.send(unsubscribeFrame(it)) }
                    subscribed.clear()
                    subscribed.addAll(want)
                }
            }
        }

        trySend(StreamEvent.Connection(ConnectionStatus.CONNECTING))
        var backoffMs = INITIAL_BACKOFF_MS

        try {
            while (isActive) {
                val closed = CompletableDeferred<Unit>()
                val listener = object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        backoffMs = INITIAL_BACKOFF_MS
                        trySend(StreamEvent.Connection(ConnectionStatus.CONNECTED))
                        synchronized(lock) {
                            subscribed.clear()
                            desired.value.forEach {
                                webSocket.send(subscribeFrame(it))
                                subscribed.add(it)
                            }
                        }
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        parseTradeMessage(json, text).forEach { trySend(it) }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        closed.complete(Unit)
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        webSocket.close(NORMAL_CLOSE, null)
                        closed.complete(Unit)
                    }
                }

                val socket = client.newWebSocket(buildRequest(), listener)
                activeSocket.set(socket)
                closed.await()
                activeSocket.set(null)
                socket.cancel()

                if (!isActive) break
                trySend(StreamEvent.Connection(ConnectionStatus.RECONNECTING))
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
            }
        } finally {
            // Reached when collection is cancelled (delay/await throw); ensure the socket closes.
            activeSocket.get()?.cancel()
        }
    }

    private fun buildRequest(): Request =
        Request.Builder().url("$WS_URL?token=$apiKey").build()

    private companion object {
        const val WS_URL = "wss://ws.finnhub.io"
        const val NORMAL_CLOSE = 1000
        const val INITIAL_BACKOFF_MS = 1_000L
        const val MAX_BACKOFF_MS = 30_000L

        fun subscribeFrame(symbol: String) = """{"type":"subscribe","symbol":"$symbol"}"""
        fun unsubscribeFrame(symbol: String) = """{"type":"unsubscribe","symbol":"$symbol"}"""
    }
}
