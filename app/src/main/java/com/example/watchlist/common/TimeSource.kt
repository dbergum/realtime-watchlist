package com.example.watchlist.common

/**
 * Abstracts "what time is it now" so staleness logic can be driven deterministically in tests.
 * A `fun interface` keeps the production binding a one-liner and lets tests pass a lambda.
 */
fun interface TimeSource {
    fun nowMs(): Long
}
