package com.example.watchlist.util

import com.example.watchlist.data.repository.InstrumentRepository
import com.example.watchlist.domain.model.Instrument

/** Configurable in-memory [InstrumentRepository] for driving the search ViewModel in tests. */
class FakeInstrumentRepository : InstrumentRepository {

    var searchResult: Result<List<Instrument>> = Result.success(emptyList())
    var snapshot: Double? = null

    override suspend fun search(query: String): Result<List<Instrument>> = searchResult

    override suspend fun snapshotPrice(symbol: String): Double? = snapshot
}
