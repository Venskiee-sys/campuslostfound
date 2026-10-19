package com.example.campuslostfound.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

// UniFind shapes — friendly, rounded (16-28px on cards, buttons, sheets)
val UniFindShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// NOTE: scheme values are literals on purpose — the UniFind* neutrals are
// theme-reactive state and must not be captured while these are initialized.

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3F8033),
    secondary = Color(0xFF6FA84E),
    tertiary = UniFindMidGreen,
    background = Color(0xFF131A10),
    surface = Color(0xFF1B2417),
    onPrimary = Color.White,
    onSecondary = Color(0xFF12200E),
    onTertiary = Color(0xFF12200E),
    onBackground = Color(0xFFE8EEDF),
    onSurface = Color(0xFFE8EEDF),
    outline = Color(0xFF3A4634),
    outlineVariant = Color(0xFF2E3A28),
    primaryContainer = Color(0xFF2A3A1E),
    onPrimaryContainer = Color(0xFFD7E8C9),
    secondaryContainer = Color(0xFF2A3A1E),
    onSecondaryContainer = Color(0xFFE3EFC9),
    surfaceVariant = Color(0xFF26301F),
    onSurfaceVariant = Color(0xFFB6C2AB),
    error = Color(0xFFE5735A)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF336A29),
    secondary = Color(0xFF498428),
    tertiary = UniFindMidGreen,
    background = Color(0xFFF7F9EE),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1E2B19),
    onSurface = Color(0xFF1E2B19),
    outline = Color(0xFFD9E0C9),
    outlineVariant = Color(0xFFE4E9D8),
    primaryContainer = Color(0xFFEAEF9D),
    onPrimaryContainer = Color(0xFF336A29),
    secondaryContainer = Color(0xFFC1D95C),
    onSecondaryContainer = Color(0xFF1E2B19),
    surfaceVariant = Color(0xFFF2F5E6),
    onSurfaceVariant = Color(0xFF6D7B66),
    error = Color(0xFFB4410B),
    onError = Color.White
)

@Composable
fun CampusLostFoundTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled to maintain brand identity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Keep the UniFind neutral palette in sync before children compose, so
    // every screen renders with the correct mode on the very first frame.
    if (UniFindPalette.isDark != darkTheme) {
        UniFindPalette.isDark = darkTheme
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = UniFindShapes,
        content = content
    )
}

/**
 * Paints the status bar to match a screen's header color (used by screens
 * with a dark green header that runs behind the status bar).
 *
 * `Window.statusBarColor` is deprecated (API 35 enforces edge-to-edge and
 * ignores it), but it is still the only way to tint the bar on API 24-34,
 * which is what the app's minSdk/targetSdk range actually targets. Suppressing
 * rather than deleting: removing the call would visibly change every header.
 */
@Suppress("DEPRECATION")
@Composable
fun SetStatusBarColor(color: Color) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = color.toArgb()
        }
    }
}

/**
 * Picks light or dark status-bar icons to match the header drawn behind them.
 *
 * `enableEdgeToEdge()` decides this once from the *system theme*, so in light
 * mode the dark green headers end up with unreadable dark icons — and dark
 * mode does the opposite to the white headers. Screens therefore declare the
 * icon colour their header actually needs instead of trusting the theme.
 */
@Composable
fun SetStatusBarIconAppearance(lightIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    androidx.compose.runtime.SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        androidx.core.view.WindowCompat.getInsetsController(window, view)
            .isAppearanceLightStatusBars = !lightIcons
    }
}
