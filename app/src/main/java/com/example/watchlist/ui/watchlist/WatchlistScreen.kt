package com.example.watchlist.ui.watchlist

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.watchlist.R
import com.example.watchlist.common.PriceFormat
import com.example.watchlist.domain.model.PriceMovement
import com.example.watchlist.ui.components.ConnectionBanner
import com.example.watchlist.ui.components.DemoModeBanner
import com.example.watchlist.ui.components.LoadingState
import com.example.watchlist.ui.components.MessageState
import com.example.watchlist.ui.theme.GainGreen
import com.example.watchlist.ui.theme.GainGreenDark
import com.example.watchlist.ui.theme.LossRed
import com.example.watchlist.ui.theme.LossRedDark

@Composable
fun WatchlistScreen(
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val demoMode by viewModel.demoMode.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    WatchlistScreenContent(
        uiState = uiState,
        demoMode = demoMode,
        demoToggleLocked = viewModel.demoToggleLocked,
        sort = sort,
        filter = filter,
        isRefreshing = isRefreshing,
        onSortChange = viewModel::onSortChange,
        onFilterChange = viewModel::onFilterChange,
        onRemove = viewModel::onRemove,
        onRefresh = viewModel::onRefresh,
        onToggleDemoMode = viewModel::onToggleDemoMode,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchlistScreenContent(
    uiState: WatchlistUiState,
    demoMode: Boolean,
    demoToggleLocked: Boolean,
    sort: SortOption,
    filter: String,
    isRefreshing: Boolean,
    onSortChange: (SortOption) -> Unit,
    onFilterChange: (String) -> Unit,
    onRemove: (String) -> Unit,
    onRefresh: () -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.watchlist_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.demo_mode),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Switch(
                            checked = demoMode,
                            onCheckedChange = onToggleDemoMode,
                            // Locked on when no API key is configured (live mode impossible).
                            enabled = !demoToggleLocked,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (demoMode) DemoModeBanner()

            when (uiState) {
                WatchlistUiState.Loading -> LoadingState()
                WatchlistUiState.Empty -> MessageState(stringResource(R.string.watchlist_empty))
                is WatchlistUiState.Content -> WatchlistContent(
                    state = uiState,
                    sort = sort,
                    filter = filter,
                    isRefreshing = isRefreshing,
                    onSortChange = onSortChange,
                    onFilterChange = onFilterChange,
                    onRemove = onRemove,
                    onRefresh = onRefresh,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchlistContent(
    state: WatchlistUiState.Content,
    sort: SortOption,
    filter: String,
    isRefreshing: Boolean,
    onSortChange: (SortOption) -> Unit,
    onFilterChange: (String) -> Unit,
    onRemove: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ConnectionBanner(status = state.connection)

        OutlinedTextField(
            value = filter,
            onValueChange = onFilterChange,
            singleLine = true,
            label = { Text(stringResource(R.string.filter_hint)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.sort_label),
                style = MaterialTheme.typography.labelLarge,
            )
            SortOption.entries.forEach { option ->
                FilterChip(
                    selected = sort == option,
                    onClick = { onSortChange(option) },
                    label = { Text(stringResource(option.labelRes)) },
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            if (state.items.isEmpty()) {
                MessageState(stringResource(R.string.search_empty))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(items = state.items, key = { it.symbol }) { item ->
                        PriceRow(item = item, onRemove = { onRemove(item.symbol) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceRow(
    item: WatchlistItem,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = item.symbol,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            if (item.price == null) {
                Text(
                    text = stringResource(R.string.waiting_for_price),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MovementIcon(movement = item.movement, dark = dark, stale = item.isStale)
                    Text(
                        text = PriceFormat.price(item.price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.isStale) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                if (item.changePercent != null) {
                    Text(
                        text = PriceFormat.percent(item.changePercent),
                        style = MaterialTheme.typography.bodySmall,
                        color = movementColor(item.movementFromChange(), dark),
                    )
                }
                if (item.isStale) {
                    Text(
                        text = stringResource(R.string.stale_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.remove),
            )
        }
    }
}

@Composable
private fun MovementIcon(movement: PriceMovement, dark: Boolean, stale: Boolean) {
    // FLAT shows no arrow; UP/DOWN use core Material icons (no material-icons-extended dependency).
    val icon = when (movement) {
        PriceMovement.UP -> Icons.Default.KeyboardArrowUp
        PriceMovement.DOWN -> Icons.Default.KeyboardArrowDown
        PriceMovement.FLAT -> return
    }
    val tint = if (stale) MaterialTheme.colorScheme.onSurfaceVariant else movementColor(movement, dark)
    Icon(imageVector = icon, contentDescription = null, tint = tint)
}

private fun movementColor(movement: PriceMovement, dark: Boolean): Color = when (movement) {
    PriceMovement.UP -> if (dark) GainGreenDark else GainGreen
    PriceMovement.DOWN -> if (dark) LossRedDark else LossRed
    PriceMovement.FLAT -> Color.Gray
}

/** Colours the percent-change text by the sign of the change rather than the last tick. */
private fun WatchlistItem.movementFromChange(): PriceMovement = when {
    changePercent == null || changePercent == 0.0 -> PriceMovement.FLAT
    changePercent > 0 -> PriceMovement.UP
    else -> PriceMovement.DOWN
}
