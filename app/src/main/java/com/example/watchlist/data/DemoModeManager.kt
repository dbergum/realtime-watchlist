package com.example.watchlist.data

import com.example.watchlist.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for whether the app is in demo (fake-data) mode.
 *
 * Demo mode is ON by default whenever no Finnhub API key is configured, so a reviewer can run
 * the full experience with zero setup. It is also runtime-toggleable from the UI so live and
 * demo data can be compared without a rebuild. The repositories and the price-stream layer
 * observe [enabled] and switch data sources reactively.
 */
@Singleton
class DemoModeManager @Inject constructor() {

    private val _enabled = MutableStateFlow(BuildConfig.FINNHUB_API_KEY.isBlank())
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    /** True when no API key was provided, so live mode isn't even possible. */
    val forcedByMissingKey: Boolean = BuildConfig.FINNHUB_API_KEY.isBlank()

    fun setEnabled(value: Boolean) {
        _enabled.value = value
    }
}
