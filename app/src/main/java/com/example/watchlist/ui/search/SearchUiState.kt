package com.example.watchlist.ui.search

import androidx.annotation.StringRes
import com.example.watchlist.domain.model.Instrument

sealed interface SearchUiState {
    /** Blank query: prompt the user to type instead of firing a request. */
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val instruments: List<Instrument>) : SearchUiState
    data object Empty : SearchUiState
    data class Error(@StringRes val messageRes: Int) : SearchUiState
}
