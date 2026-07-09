package com.example.watchlist.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.watchlist.common.TimeSource
import com.example.watchlist.data.DemoModeManager
import com.example.watchlist.data.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the watchlist screen by combining five reactive inputs into a single observable state:
 * the persisted watchlist, the live price map, the stream connection status, the user's
 * sort/filter choices, and a 1 Hz ticker that re-evaluates staleness. Exposed as a [StateFlow]
 * so Compose can collect it safely with lifecycle awareness.
 */
@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
    private val demoModeManager: DemoModeManager,
    private val timeSource: TimeSource,
) : ViewModel() {

    private val sortOption = MutableStateFlow(SortOption.SYMBOL)
    private val filterQuery = MutableStateFlow("")

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val sort: StateFlow<SortOption> = sortOption.asStateFlow()
    val filter: StateFlow<String> = filterQuery.asStateFlow()

    val demoMode: StateFlow<Boolean> = demoModeManager.enabled
    val demoToggleLocked: Boolean = demoModeManager.forcedByMissingKey

    private val viewOptions = combine(sortOption, filterQuery) { sort, filter -> sort to filter }

    // Re-emits every second so stale rows update without any per-row timers.
    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(TICK_INTERVAL_MS)
        }
    }

    val uiState: StateFlow<WatchlistUiState> = combine(
        watchlistRepository.watchlist,
        watchlistRepository.livePrices,
        watchlistRepository.connectionStatus,
        viewOptions,
        ticker,
    ) { watchlist, prices, connection, options, _ ->
        if (watchlist.isEmpty()) {
            WatchlistUiState.Empty
        } else {
            val (sortOption, filter) = options
            val now = timeSource.nowMs()
            val items = watchlist
                .map { WatchlistMapper.buildItem(it, prices[it.symbol], now) }
                .let { WatchlistMapper.filter(it, filter) }
                .let { WatchlistMapper.sort(it, sortOption) }
            WatchlistUiState.Content(items = items, connection = connection)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = WatchlistUiState.Loading,
    )

    fun onSortChange(option: SortOption) {
        sortOption.value = option
    }

    fun onFilterChange(query: String) {
        filterQuery.value = query
    }

    fun onRemove(symbol: String) {
        viewModelScope.launch { watchlistRepository.remove(symbol) }
    }

    fun onRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                watchlistRepository.refreshSnapshots()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun onToggleDemoMode(enabled: Boolean) {
        demoModeManager.setEnabled(enabled)
    }

    private companion object {
        const val TICK_INTERVAL_MS = 1_000L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
