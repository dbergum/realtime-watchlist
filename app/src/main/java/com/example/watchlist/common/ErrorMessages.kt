package com.example.watchlist.common

import androidx.annotation.StringRes
import com.example.watchlist.R
import java.io.IOException

/**
 * Maps a [Throwable] to a user-facing string *resource* (not a literal string). Returning a
 * resource id keeps the ViewModel free of any Android [android.content.Context], so the actual
 * text is resolved in the Compose layer and stays localizable.
 */
@StringRes
fun Throwable.toUserMessageRes(): Int = when (this) {
    is IOException -> R.string.error_network
    else -> R.string.error_generic
}
