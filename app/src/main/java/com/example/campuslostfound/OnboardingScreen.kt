package com.example.campuslostfound

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campuslostfound.ui.components.PrimaryButton
import com.example.campuslostfound.ui.theme.*

/**
 * Onboarding: pale-lime hero panel with the search mascot moment and floating
 * item tiles, then the wordmark, description, page dots and CTA.
 */
@Composable
@Suppress("ConfigurationScreenWidthHeight")
fun OnboardingScreen(
    onGetStartedClick: () -> Unit = {},
    onSignInClick: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    // Hero panel shrinks in landscape/short windows so the copy, dots and CTA stay reachable.
    val heroHeight =
        if (isLandscape)
            (configuration.screenHeightDp * 0.34f).dp.coerceIn(180.dp, 240.dp)
        else
            (configuration.screenHeightDp * 0.46f).dp.coerceIn(300.dp, 430.dp)

    // Pale lime hero sits behind the status bar: keep the icons dark.
    SetStatusBarIconAppearance(lightIcons = false)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UniFindBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Keeps the CTA and the sign-in link clear of the gesture bar
                // on short portrait screens where the content just scrolls.
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {

            // ---- Hero panel: pale lime, rounded bottom edge ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                    .background(UniFindPaleLime)
            ) {
                // Floating tilted item tiles, positioned proportionally so they
                // never run off the edge on narrow portrait screens.
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val heroWidth = maxWidth
                    val heroHeightPx = maxHeight
                    FloatingTile(Icons.Outlined.AccountBalanceWallet, Modifier.offset(x = heroWidth * 0.10f, y = heroHeightPx * 0.15f).rotate(-10f))
                    FloatingTile(Icons.Outlined.Key, Modifier.offset(x = heroWidth * 0.70f, y = heroHeightPx * 0.12f).rotate(9f))
                    FloatingTile(Icons.Outlined.Smartphone, Modifier.offset(x = heroWidth * 0.07f, y = heroHeightPx * 0.58f).rotate(7f))
                    FloatingTile(Icons.Outlined.Backpack, Modifier.offset(x = heroWidth * 0.72f, y = heroHeightPx * 0.55f).rotate(-8f))
                    FloatingTile(Icons.Outlined.Umbrella, Modifier.offset(x = heroWidth * 0.40f, y = heroHeightPx * 0.77f).rotate(5f))
                }

                // Center: big green circle with a magnifying glass
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-10).dp)
                        .size(168.dp)
                        .clip(CircleShape)
                        .background(UniFindPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(116.dp)
                            .clip(CircleShape)
                            .background(UniFindSecondary.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }

            // ---- Content ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(34.dp))

                // Logo + wordmark
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(UniFindPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "UniFind",
                        style = MaterialTheme.typography.headlineSmall,
                        color = UniFindText
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Report lost items, browse what others have found, and get matched automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = UniFindTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.9f)
                )

                Spacer(Modifier.height(22.dp))

                // Page dots
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(width = 22.dp, height = 8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(UniFindPrimary)
                    )
                    Spacer(Modifier.width(7.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(UniFindOutline)
                    )
                    Spacer(Modifier.width(7.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(UniFindOutline)
                    )
                }

                Spacer(Modifier.height(26.dp))

                PrimaryButton(
                    text = "Get Started",
                    onClick = onGetStartedClick,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "I already have an account",
                    style = MaterialTheme.typography.labelLarge,
                    color = UniFindSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onSignInClick)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )

                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

/** Small floating rounded tile holding a line icon, used in the hero panel. */
@Composable
private fun FloatingTile(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = UniFindPrimary,
            modifier = Modifier.size(30.dp)
        )
    }
}
