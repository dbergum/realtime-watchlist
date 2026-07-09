package com.example.watchlist.domain.model

/** State of the live price stream, surfaced to the user as a connection banner. */
enum class ConnectionStatus {
    /** Opening the socket for the first time (or no symbols yet). */
    CONNECTING,

    /** Socket is open and receiving trades. */
    CONNECTED,

    /** Socket dropped; a backoff retry is scheduled. */
    RECONNECTING,

    /** Streaming is unavailable (e.g. demo mode disabled with no connectivity). */
    DISCONNECTED,
}
