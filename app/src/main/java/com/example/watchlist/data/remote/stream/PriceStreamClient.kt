package com.example.watchlist.data.remote.stream

import kotlinx.coroutines.flow.Flow

/**
 * A live price stream.
 *
 * [stream] takes a *flow* of the currently desired symbol set so the implementation can adjust
 * its subscriptions as the watchlist changes, without reopening the connection. The returned flow
 * is cold: collecting it opens the stream; cancelling collection tears it down.
 */
interface PriceStreamClient {
    fun stream(symbols: Flow<Set<String>>): Flow<StreamEvent>
}
