package com.example.campuslostfound.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// UniFind brand palette — STATIC (identical in light and dark mode)
// ---------------------------------------------------------------------------
val UniFindPrimary = Color(0xFF336A29)       // primary dark green
val UniFindSecondary = Color(0xFF498428)     // secondary green
val UniFindMidGreen = Color(0xFF80B155)      // mid green
val UniFindLime = Color(0xFFC1D95C)          // lime
val UniFindPaleLime = Color(0xFFEAEF9D)      // pale lime

// ---------------------------------------------------------------------------
// Status tags — orange is reserved for LOST only (static)
// ---------------------------------------------------------------------------
val LostTagText = Color(0xFFB4410B)
val LostTagBackground = Color(0xFFFDEBDD)
val FoundTagText = Color(0xFF336A29)
val FoundTagBackground = Color(0xFFEAEF9D)

// ---------------------------------------------------------------------------
// Soft tiles for category / item line icons (static brand accents)
// ---------------------------------------------------------------------------
val TilePaleLime = Color(0xFFEAEF9D)
val TileSoftGreen = Color(0xFFDCEBCF)
val TileSoftSage = Color(0xFFE7EFDD)
val TileSoftMint = Color(0xFFD8ECD4)
val TileSoftOlive = Color(0xFFF0F3D6)
val TileWhite = Color(0xFFFFFFFF)

// ---------------------------------------------------------------------------
// Neutral (surfaces / text) values for each mode
// ---------------------------------------------------------------------------
// Light mode
private val LightBackground = Color(0xFFF7F9EE)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceAlt = Color(0xFFF2F5E6)
private val LightText = Color(0xFF1E2B19)
private val LightTextSecondary = Color(0xFF6D7B66)
private val LightTextTertiary = Color(0xFF96A18F)
private val LightDivider = Color(0xFFE4E9D8)
private val LightOutline = Color(0xFFD9E0C9)
private val LightHighlight = Color(0xFFEAEF9D)

// Dark mode (paired with the dark color scheme in Theme.kt)
private val DarkBackground = Color(0xFF131A10)
private val DarkSurface = Color(0xFF1B2417)
private val DarkSurfaceAlt = Color(0xFF26301F)
private val DarkText = Color(0xFFE8EEDF)
private val DarkTextSecondary = Color(0xFFB6C2AB)
private val DarkTextTertiary = Color(0xFF8FA086)
private val DarkDivider = Color(0xFF2E3A28)
private val DarkOutline = Color(0xFF3A4634)
private val DarkHighlight = Color(0xFF2A3A1E)

/**
 * Holds the active mode so the neutral UniFind colors can follow it.
 *
 * The state is written by [CampusLostFoundTheme] during composition; every
 * composable that reads a neutral color (background, surface, text, …)
 * subscribes to it and recomposes when the mode flips.
 */
object UniFindPalette {
    var isDark by mutableStateOf(false)
        internal set

    val background: Color get() = if (isDark) DarkBackground else LightBackground
    /** Cards, sheets, panels, inputs — anything that reads as "white" in light mode. */
    val surface: Color get() = if (isDark) DarkSurface else LightSurface
    val surfaceAlt: Color get() = if (isDark) DarkSurfaceAlt else LightSurfaceAlt
    val text: Color get() = if (isDark) DarkText else LightText
    val textSecondary: Color get() = if (isDark) DarkTextSecondary else LightTextSecondary
    val textTertiary: Color get() = if (isDark) DarkTextTertiary else LightTextTertiary
    val divider: Color get() = if (isDark) DarkDivider else LightDivider
    val outline: Color get() = if (isDark) DarkOutline else LightOutline
    /** Soft "highlight" panels (info boxes, unread rows, avatars). */
    val highlight: Color get() = if (isDark) DarkHighlight else LightHighlight
    /** Content painted on top of brand greens — always white. */
    val onPrimary: Color get() = Color.White
}

// ---------------------------------------------------------------------------
// Theme-reactive neutrals — flip automatically with the active mode
// ---------------------------------------------------------------------------
val UniFindBackground: Color get() = UniFindPalette.background
val UniFindWhite: Color get() = UniFindPalette.surface       // surfaces read as "white" in light mode
val UniFindSurface: Color get() = UniFindPalette.surface
val UniFindSurfaceAlt: Color get() = UniFindPalette.surfaceAlt
val UniFindText: Color get() = UniFindPalette.text
val UniFindTextSecondary: Color get() = UniFindPalette.textSecondary
val UniFindTextTertiary: Color get() = UniFindPalette.textTertiary
val UniFindDivider: Color get() = UniFindPalette.divider
val UniFindOutline: Color get() = UniFindPalette.outline
val UniFindHighlight: Color get() = UniFindPalette.highlight
/** Always white — for text/icons sitting on green/brand fills. */
val UniFindOnPrimary: Color get() = UniFindPalette.onPrimary

// ---------------------------------------------------------------------------
// Legacy aliases — kept so existing references stay meaningful.
// ---------------------------------------------------------------------------
val PLMUNGreen = UniFindPrimary
val PLMUNGreenLight = UniFindPaleLime

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = UniFindPrimary
val PurpleGrey40 = UniFindSecondary
val Pink40 = UniFindMidGreen

val AppHeaderGreen = UniFindPrimary
val AppBackground: Color get() = UniFindBackground
val AppTextPrimary: Color get() = UniFindText
val AppTextSecondary: Color get() = UniFindTextSecondary
val AppIconSquareGreen = UniFindPrimary
val AppSearchFieldBg: Color get() = UniFindSurfaceAlt
val AppDividerColor: Color get() = UniFindDivider
val AppAccentGreen = UniFindSecondary
