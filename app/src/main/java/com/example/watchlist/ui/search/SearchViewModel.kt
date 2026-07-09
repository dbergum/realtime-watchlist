package com.example.watchlist.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.watchlist.common.toUserMessageRes
import com.example.watchlist.data.repository.InstrumentRepository
import com.example.watchlist.data.repository.WatchlistRepository
import com.example.watchlist.domain.model.Instrument
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the search screen. The reactive query pipeline mirrors the standard debounce →
 * distinctUntilChanged → flatMapLatest pattern:
 * - `debounce` avoids a request per keystroke;
 * - `distinctUntilChanged` ignores no-op changes;
 * - `flatMapLatest` cancels an in-flight search when a newer query arrives (no stale results);
 * - a retry trigger re-runs the current query after an error.
 *
 * [watchlistedSymbols] is exposed separately so each result row can toggle add/remove.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val instrumentRepository: InstrumentRepository,
    private val watchlistRepository: WatchlistRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val retryTrigger = MutableStateFlow(0)

    val watchlistedSymbols: StateFlow<Set<String>> =
        watchlistRepository.watchlist
            .map { list -> list.map { it.symbol }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptySet())

    val uiState: StateFlow<SearchUiState> =
        combine(
            _query.debounce(SEARCH_DEBOUNCE_MS).map { it.trim() }.distinctUntilChanged(),
            retryTrigger,
        ) { query, _ -> query }
            .flatMapLatest { query -> searchResults(query) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = SearchUiState.Idle,
            )

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun retry() {
        retryTrigger.update { it + 1 }
    }

    fun onAdd(instrument: Instrument) {
        viewModelScope.launch { watchlistRepository.add(instrument) }
    }

    fun onRemove(symbol: String) {
        viewModelScope.launch { watchlistRepository.remove(symbol) }
    }

    private fun searchResults(query: String) = flow {
        if (query.isEmpty()) {
            emit(SearchUiState.Idle)
            return@flow
        }
        emit(SearchUiState.Loading)
        val state = instrumentRepository.search(query).fold(
            onSuccess = { instruments ->
                if (instruments.isEmpty()) SearchUiState.Empty
                else SearchUiState.Success(instruments)
            },
            onFailure = { SearchUiState.Error(it.toUserMessageRes()) },
        )
        emit(state)
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
