package com.example.watchlist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.watchlist.R
import com.example.watchlist.domain.model.ConnectionStatus

/**
 * A thin banner surfaced above the watchlist while the live stream is anything other than healthy.
 * When [ConnectionStatus.CONNECTED] nothing is shown, so the banner is quiet in the happy path.
 */
@Composable
fun ConnectionBanner(
    status: ConnectionStatus,
    modifier: Modifier = Modifier,
) {
    val message = when (status) {
        ConnectionStatus.CONNECTING -> stringResource(R.string.status_connecting)
        ConnectionStatus.RECONNECTING -> stringResource(R.string.status_reconnecting)
        ConnectionStatus.DISCONNECTED -> stringResource(R.string.status_disconnected)
        ConnectionStatus.CONNECTED -> return
    }

    val background = when (status) {
        ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val content = when (status) {
        ConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (status != ConnectionStatus.DISCONNECTED) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = content,
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = content,
        )
    }
}

/** Demo-mode notice, shown when simulated data is in use. */
@Composable
fun DemoModeBanner(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.demo_banner),
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFF3D2E00),
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFFE6B3))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
