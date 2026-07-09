package com.example.watchlist.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.watchlist.R
import com.example.watchlist.domain.model.Instrument
import com.example.watchlist.ui.components.ErrorState
import com.example.watchlist.ui.components.LoadingState
import com.example.watchlist.ui.components.MessageState

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val watchlisted by viewModel.watchlistedSymbols.collectAsStateWithLifecycle()

    SearchScreenContent(
        query = query,
        uiState = uiState,
        watchlistedSymbols = watchlisted,
        onQueryChange = viewModel::onQueryChange,
        onRetry = viewModel::retry,
        onAdd = viewModel::onAdd,
        onRemove = viewModel::onRemove,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreenContent(
    query: String,
    uiState: SearchUiState,
    watchlistedSymbols: Set<String>,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onAdd: (Instrument) -> Unit,
    onRemove: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (uiState) {
                    SearchUiState.Idle -> MessageState(stringResource(R.string.search_prompt))
                    SearchUiState.Loading -> LoadingState()
                    SearchUiState.Empty -> MessageState(stringResource(R.string.search_empty))
                    is SearchUiState.Error -> ErrorState(
                        message = stringResource(uiState.messageRes),
                        onRetry = onRetry,
                    )

                    is SearchUiState.Success -> SearchResults(
                        instruments = uiState.instruments,
                        watchlistedSymbols = watchlistedSymbols,
                        onAdd = onAdd,
                        onRemove = onRemove,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        singleLine = true,
        label = { Text(stringResource(R.string.search_label)) },
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.clear_search),
                    )
                }
            }
        },
    )
}

@Composable
private fun SearchResults(
    instruments: List<Instrument>,
    watchlistedSymbols: Set<String>,
    onAdd: (Instrument) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(items = instruments, key = { it.symbol }) { instrument ->
            val inWatchlist = instrument.symbol in watchlistedSymbols
            ListItem(
                headlineContent = { Text(instrument.displayName) },
                supportingContent = { Text(instrument.symbol) },
                trailingContent = {
                    if (inWatchlist) {
                        IconButton(onClick = { onRemove(instrument.symbol) }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.remove_from_watchlist),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        IconButton(onClick = { onAdd(instrument) }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.add_to_watchlist),
                            )
                        }
                    }
                },
            )
        }
    }
}
