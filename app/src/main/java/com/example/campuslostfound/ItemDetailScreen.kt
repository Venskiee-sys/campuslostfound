package com.example.campuslostfound

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.campuslostfound.ui.components.*
import com.example.campuslostfound.ui.theme.*

@Composable
@Suppress("ConfigurationScreenWidthHeight")
fun ItemDetailScreen(
    itemId: String,
    authViewModel: AuthViewModel,
    itemViewModel: ItemViewModel,
    chatViewModel: ChatViewModel,
    onBackClick: () -> Unit,
    onMessageClick: (String, String) -> Unit,
    onClaimClick: () -> Unit = {}
) {
    // Collect the feeds so this screen actually recomposes when they load, and
    // fall back to the on-demand fetch for deep links / resolved items that are
    // not in any of the in-memory lists.
    val lostItems by itemViewModel.lostItems.collectAsState()
    val foundItems by itemViewModel.foundItems.collectAsState()
    val myReports by itemViewModel.myReports.collectAsState()
    val selectedItem by itemViewModel.selectedItem.collectAsState()

    LaunchedEffect(itemId) { itemViewModel.fetchItem(itemId) }

    val item = (lostItems + foundItems + myReports).firstOrNull { it.id == itemId }
        ?: selectedItem?.takeIf { it.id == itemId }

    val currentUser = authViewModel.userData.value
    val isMine = currentUser != null && item?.reportedBy == currentUser.uid

    if (item == null) {
        // Never bail out of composition: on cold start (or when the query fails)
        // this used to render a blank screen with no back button, forever.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(UniFindBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Not-found only after the on-demand fetch and the feeds have
                // both come back — otherwise a deep link flashes "not found".
                if (!itemViewModel.itemLoading.value && itemViewModel.itemsLoaded.value) {
                    Text(
                        text = "Item not found",
                        style = MaterialTheme.typography.titleMedium,
                        color = UniFindText
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "It may have been removed or is no longer available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniFindTextSecondary
                    )
                    Spacer(Modifier.height(16.dp))
                    SecondaryButton(text = "Go back", onClick = onBackClick)
                } else {
                    CircularProgressIndicator(color = UniFindPrimary)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Loading item…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniFindTextSecondary
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 20.dp, top = 12.dp)
            ) {
                CircleBackButton(onClick = onBackClick)
            }
        }
        return
    }

    var showResolveDialog by remember { mutableStateOf(false) }
    var bookmarked by remember { mutableStateOf(false) }

    val images = listOfNotNull(item.imageUrl?.takeIf { it.isNotBlank() })

    val startChat: () -> Unit = {
        currentUser?.let { me ->
            chatViewModel.startChat(
                myId = me.uid,
                myName = me.fullName,
                otherId = item.reportedBy,
                otherName = item.reporterName,
                onReady = { chatId ->
                    onMessageClick(chatId, item.reporterName)
                }
            )
        }
    }

    val reporterInitials = remember(item.reporterName) {
        item.reporterName.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifBlank { "U" }
    }

    if (showResolveDialog) {
        val resolveError = itemViewModel.errorMessage.value
        AlertDialog(
            onDismissRequest = { showResolveDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = UniFindWhite,
            title = {
                Text(
                    "Resolve item",
                    style = MaterialTheme.typography.titleMedium,
                    color = UniFindText
                )
            },
            text = {
                Column {
                    Text(
                        "Are you sure you want to mark this item as resolved? It will be hidden from public lists.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniFindTextSecondary
                    )
                    if (resolveError != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = resolveError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !itemViewModel.isLoading.value,
                    onClick = {
                        // Only leave the screen when the update actually landed —
                        // a failed resolve used to look like a success.
                        itemViewModel.resolveItem(
                            itemId = item.id,
                            onSuccess = {
                                showResolveDialog = false
                                onBackClick()
                            }
                        )
                    }
                ) {
                    Text("RESOLVE", color = UniFindPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResolveDialog = false }) {
                    Text("CANCEL", color = UniFindTextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UniFindBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // ------------------------------------------------------------------
            // Pale-lime media area with overlaid back / bookmark buttons
            // ------------------------------------------------------------------
            // Media band scales with the viewport so short portrait screens still
            // show item info above the fold; the sheet overlap derives from it.
            val landscape = rememberIsLandscape()
            val mediaHeight =
                if (landscape)
                    (LocalConfiguration.current.screenHeightDp.dp * 0.34f).coerceIn(140.dp, 220.dp)
                else
                    (LocalConfiguration.current.screenHeightDp.dp * 0.42f).coerceIn(240.dp, 320.dp)
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(mediaHeight)
                        .background(UniFindPaleLime)
                ) {
                    if (images.isEmpty()) {
                        CategoryIconTile(
                            category = item.category,
                            modifier = Modifier.align(Alignment.Center),
                            size = 112.dp,
                            cornerRadius = 28.dp
                        )
                    } else {
                        val pagerState = rememberPagerState(pageCount = { images.size })
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            AsyncImage(
                                model = images[page],
                                contentDescription = "Item Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Page dots (only when there is more than one photo)
                        if (images.size > 1) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 48.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                repeat(images.size) { index ->
                                    val active = index == pagerState.currentPage
                                    Box(
                                        modifier = Modifier
                                            .size(if (active) 8.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (active) UniFindPrimary
                                                else UniFindWhite.copy(alpha = 0.7f)
                                            )
                                    )
                                }
                            }
                        }
                    }

                    // Overlaid controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircleBackButton(onClick = onBackClick)
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(UniFindWhite)
                                .clickable { bookmarked = !bookmarked },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.BookmarkBorder,
                                contentDescription = if (bookmarked) "Saved" else "Save item",
                                tint = if (bookmarked) UniFindPrimary else UniFindTextTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // ------------------------------------------------------------------
                // White sheet with rounded top corners, sliding over the media area
                // ------------------------------------------------------------------
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Derived from the media height so the 28dp overlap never drifts.
                        .padding(top = mediaHeight - 28.dp),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = UniFindWhite,
                    shadowElevation = 10.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusTag(type = item.type)
                            Spacer(Modifier.width(10.dp))
                            // Same wording the reports list uses for an open item.
                            StatusPill(
                                label = if (item.status.equals("Open", ignoreCase = true))
                                    "Searching" else item.status
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = UniFindText
                        )

                        Spacer(Modifier.height(16.dp))

                        InfoRow(icon = Icons.Outlined.LocationOn, label = "Location", value = item.location)
                        InfoRow(icon = Icons.Outlined.DateRange, label = "Date ${item.type}", value = item.date)
                        InfoRow(icon = categoryIcon(item.category), label = "Category", value = item.category)

                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = UniFindDivider)
                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium,
                            color = UniFindText
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = UniFindTextSecondary,
                            lineHeight = 22.sp
                        )

                        Spacer(Modifier.height(18.dp))

                        // ------------------------------------------------------------------
                        // Finder / reporter card
                        // ------------------------------------------------------------------
                        UniFindCard(containerColor = UniFindSurfaceAlt, cornerRadius = 20.dp) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(UniFindPaleLime),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = reporterInitials,
                                        color = UniFindPrimary,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.reporterName.ifBlank { "Campus member" },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = UniFindText,
                                        maxLines = 1
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.VerifiedUser,
                                            contentDescription = null,
                                            tint = UniFindSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "Verified user",
                                            color = UniFindTextTertiary,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                if (!isMine && item.status == "Open") {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(UniFindWhite)
                                            .clickable(onClick = startChat),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Outlined.Chat,
                                            contentDescription = "Message reporter",
                                            tint = UniFindPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Claiming and messaging only make sense on someone else's
                        // item while it is still open — never on your own report
                        // and never on an already resolved item.
                        if (!isMine && item.status == "Open") {
                            PrimaryButton(
                                text = "This is mine, Claim Item",
                                onClick = onClaimClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            SecondaryButton(
                                text = "Message finder",
                                onClick = startChat,
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.AutoMirrored.Outlined.Chat
                            )
                        } else if (isMine && item.status == "Open") {
                            PrimaryButton(
                                text = "Mark as resolved",
                                onClick = { itemViewModel.clearError(); showResolveDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Legacy label/value row kept for compatibility — now delegates to the
 * UniFind [InfoRow] design-system component.
 */
@Composable
fun DetailInfoRow(icon: ImageVector, label: String, value: String) {
    InfoRow(icon = icon, label = label, value = value)
}
