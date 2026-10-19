package com.example.campuslostfound

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campuslostfound.ui.theme.PlusJakartaSans
import com.example.campuslostfound.ui.theme.SetStatusBarIconAppearance
import com.example.campuslostfound.ui.theme.UniFindPaleLime
import com.example.campuslostfound.ui.theme.UniFindPrimary
import kotlinx.coroutines.delay

/**
 * UniFind splash: full dark green screen with the logo, wordmark and tagline.
 * Calls [onFinish] after a short beat so navigation can move on.
 */
@Composable
fun SplashScreen(
    onFinish: () -> Unit = {}
) {
    var started by remember { mutableStateOf(false) }
    val logoAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "splashLogo"
    )
    val logoScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.85f,
        animationSpec = tween(durationMillis = 700),
        label = "splashScale"
    )

    LaunchedEffect(Unit) {
        started = true
        delay(1900)
        onFinish()
    }

    // Dark green canvas: light icons, whatever the system theme says.
    SetStatusBarIconAppearance(lightIcons = true)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UniFindPrimary)
    ) {
        // Two faint translucent circles for depth
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-70).dp, y = (-60).dp)
                .size(220.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 90.dp)
                .size(260.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .alpha(logoAlpha)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // White rounded-square logo with a location pin
            Box(
                modifier = Modifier
                    .size(96.dp)
                    // logoScale is the pop-in half of the splash animation —
                    // without it the value animated and was simply dropped.
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                    }
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = UniFindPrimary,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "UniFind",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PlusJakartaSans,
                letterSpacing = (-0.5).sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Lost it? Let's bring it back.",
                color = UniFindPaleLime,
                fontSize = 16.sp,
                fontFamily = PlusJakartaSans,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = "Campus Lost & Found",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp,
            fontFamily = PlusJakartaSans,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // Nav inset first so the wordmark sits 16dp above the gesture
                // bar (and clears 3-button nav, which is taller than 40dp).
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .alpha(logoAlpha)
        )
    }
}
