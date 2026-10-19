package com.example.campuslostfound.ui.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * True when the device is in landscape orientation. Use it to shrink
 * decorative heights / hero panels so content stays reachable when the
 * window becomes wide and short instead of tall and narrow.
 */
@Composable
fun rememberIsLandscape(): Boolean {
    val config = LocalConfiguration.current
    return config.orientation == Configuration.ORIENTATION_LANDSCAPE
}

/** Shorter of the two screen spacings on landscape; the full one in portrait. */
@Composable
fun adaptiveHeight(portrait: Dp, landscape: Dp): Dp =
    if (rememberIsLandscape()) landscape else portrait

/**
 * Compose equivalent of the web breakpoints requested:
 *  - COMPACT:  phones (320–480px)
 *  - MEDIUM:   large phones / small tablets (481–1024px)
 *  - EXPANDED: tablets landscape / laptops & desktops (1025px+)
 */
enum class WindowWidth { COMPACT, MEDIUM, EXPANDED }

@Suppress("ConfigurationScreenWidthHeight")
@Composable
fun rememberWindowWidth(): WindowWidth {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp >= 1025 -> WindowWidth.EXPANDED
        widthDp >= 481 -> WindowWidth.MEDIUM
        else -> WindowWidth.COMPACT
    }
}

/** 1 column on phones, 2 on large phones/tablets, 3–4 on desktop. */
@Composable
fun cardsPerRow(): Int = when (rememberWindowWidth()) {
    WindowWidth.COMPACT -> 1
    WindowWidth.MEDIUM -> 2
    WindowWidth.EXPANDED -> 4
}
