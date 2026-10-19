package com.example.campuslostfound

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campuslostfound.model.Notification
import com.example.campuslostfound.ui.components.*
import com.example.campuslostfound.ui.theme.*
import java.util.Calendar
import java.util.Date

/** True when both timestamps fall on the same calendar day. */
private fun isSameDay(first: Date, second: Date): Boolean {
    val a = Calendar.getInstance().apply { time = first }
    val b = Calendar.getInstance().apply { time = second }
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}

/** A notification describing an item match (the featured "Possible match" card). */
private fun isMatchNotification(notif: Notification): Boolean =
    notif.relatedId != null && (
        notif.type.contains("match", ignoreCase = true) ||
            notif.title.contains("match", ignoreCase = true) ||
            notif.message.contains("match", ignoreCase = true)
        )

/** Soft line icon per notification type. */
private fun notificationIcon(type: String): ImageVector = when {
    type.equals("Message", ignoreCase = true) -> Icons.AutoMirrored.Outlined.Chat
    type.contains("match", ignoreCase = true) -> Icons.Outlined.AutoAwesome
    else -> Icons.Outlined.Notifications
}

@Composable
fun NotificationsScreen(
    authViewModel: AuthViewModel,
    viewModel: NotificationViewModel,
    onBackClick: () -> Unit = {},
    onNotificationClick: (String, String?, String?) -> Unit = { _, _, _ -> }
) {
    val user = authViewModel.userData.value
    val notifications by viewModel.notifications.collectAsState()

    // Locally dismissed "Possible match" card (marked read, no longer featured)
    var dismissedMatchId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user) {
        user?.uid?.let { viewModel.fetchNotifications(it) }
    }

    val matchPool = notifications.filter {
        it.id != dismissedMatchId && isMatchNotification(it)
    }
    // Only unseen matches get the big card. Falling back to any match meant a
    // dismissal simply promoted the next *read* one — so the card looked like
    // it refused to go away (and duplicated a row already shown below).
    val featuredMatch = matchPool.firstOrNull { !it.seen }

    val grouped = notifications.filterNot { it.id == featuredMatch?.id }
    val now = Date()
    val (todayList, earlierList) = grouped.partition { isSameDay(Date(it.timestamp), now) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UniFindBackground)
    ) {
        ScreenHeader(
            title = "Alerts",
            onBack = onBackClick,
            subtitle = "Your recent activity",
            actions = {
                Text(
                    text = "Mark all read",
                    style = MaterialTheme.typography.labelLarge,
                    color = UniFindSecondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { user?.uid?.let { viewModel.markAllAsRead(it) } }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        )

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Outlined.Notifications,
                    title = "No alerts yet",
                    message = "Messages and item matches will show up here."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Featured dark green "Possible match" card
                featuredMatch?.let { match ->
                    item {
                        FeaturedMatchCard(
                            notif = match,
                            onViewItem = {
                                dismissedMatchId = match.id
                                viewModel.markAsRead(match.id)
                                onNotificationClick(match.type, match.relatedId, match.senderName)
                            },
                            onDismissMatch = {
                                dismissedMatchId = match.id
                                viewModel.markAsRead(match.id)
                            }
                        )
                    }
                }

                if (todayList.isNotEmpty()) {
                    item { SectionHeader("Today") }
                    item {
                        NotificationGroupCard(
                            notifs = todayList,
                            onNotificationClick = { notif ->
                                viewModel.markAsRead(notif.id)
                                onNotificationClick(notif.type, notif.relatedId, notif.senderName)
                            }
                        )
                    }
                }

                if (earlierList.isNotEmpty()) {
                    item { SectionHeader("Earlier") }
                    item {
                        NotificationGroupCard(
                            notifs = earlierList,
                            onNotificationClick = { notif ->
                                viewModel.markAsRead(notif.id)
                                onNotificationClick(notif.type, notif.relatedId, notif.senderName)
                            }
                        )
                    }
                }
            }
        }
    }
}

/** White rounded panel holding a group of notification rows with soft dividers. */
@Composable
private fun NotificationGroupCard(
    notifs: List<Notification>,
    onNotificationClick: (Notification) -> Unit
) {
    UniFindCard(cornerRadius = 24.dp) {
        notifs.forEachIndexed { index, notif ->
            NotificationRow(notif = notif, onClick = { onNotificationClick(notif) })
            if (index < notifs.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 86.dp, end = 16.dp),
                    thickness = 1.dp,
                    color = UniFindDivider
                )
            }
        }
    }
}

/** Featured dark green match card: sparkle + item tile + message + two actions. */
@Composable
private fun FeaturedMatchCard(
    notif: Notification,
    onViewItem: () -> Unit,
    onDismissMatch: () -> Unit
) {
    UniFindCard(containerColor = UniFindPrimary, cornerRadius = 24.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(UniFindPaleLime),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = UniFindPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                IconTile(
                    icon = Icons.Outlined.Inventory2,
                    size = 48.dp,
                    cornerRadius = 14.dp,
                    containerColor = UniFindWhite,
                    iconTint = UniFindPrimary,
                    iconSize = 24.dp
                )
                Spacer(Modifier.width(14.dp))
                // Weighted: without it the subtitle is measured against the
                // full row width and the card's text overflows on 360dp.
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Possible match",
                        style = MaterialTheme.typography.titleMedium,
                        color = UniFindOnPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "A found item may match your report",
                        style = MaterialTheme.typography.bodySmall,
                        color = UniFindOnPrimary.copy(alpha = 0.75f)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = notif.message,
                style = MaterialTheme.typography.bodyMedium,
                color = UniFindOnPrimary.copy(alpha = 0.92f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = timeAgo(notif.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = UniFindOnPrimary.copy(alpha = 0.65f)
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onViewItem,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UniFindOnPrimary,
                        contentColor = UniFindPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "View Item",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Button(
                    onClick = onDismissMatch,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UniFindWhite.copy(alpha = 0.14f),
                        contentColor = UniFindOnPrimary
                    ),
                    border = BorderStroke(1.dp, UniFindWhite.copy(alpha = 0.45f)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "Not mine",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notif: Notification,
    onClick: () -> Unit
) {
    val unread = !notif.seen
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .then(if (unread) Modifier.background(UniFindSurfaceAlt) else Modifier)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pale lime left accent marks unread rows
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (unread) UniFindHighlight else Color.Transparent)
        )
        Spacer(Modifier.width(10.dp))

        IconTile(
            icon = notificationIcon(notif.type),
            size = 44.dp,
            cornerRadius = 14.dp,
            containerColor = if (unread) UniFindWhite else TileSoftSage,
            iconTint = UniFindPrimary,
            iconSize = 22.dp
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notif.title,
                style = MaterialTheme.typography.titleSmall,
                color = UniFindText,
                fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = notif.message,
                style = MaterialTheme.typography.bodySmall,
                color = UniFindTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = timeAgo(notif.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = UniFindTextTertiary
            )
        }
    }
}
