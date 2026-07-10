package com.example.watchlist.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// A dark green reads on the bright neon primary better than white, so it's used for onPrimary.
private val LightColors = lightColorScheme(
    primary = NeonGreenDark,
    onPrimary = OnNeonGreenContainer,
    primaryContainer = NeonGreenContainer,
    onPrimaryContainer = OnNeonGreenContainer,
)

private val DarkColors = darkColorScheme(
    primary = NeonGreen,
    onPrimary = OnNeonGreenContainer,
    primaryContainer = NeonGreenDark,
    onPrimaryContainer = NeonGreenContainer,
)

@Composable
fun WatchlistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
