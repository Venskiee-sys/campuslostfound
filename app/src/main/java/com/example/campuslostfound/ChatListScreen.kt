package com.example.campuslostfound

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campuslostfound.model.Chat
import com.example.campuslostfound.ui.components.*
import com.example.campuslostfound.ui.theme.*
import io.github.jan.supabase.auth.auth

/** One conversation row: the chat itself, who we show as the other person, unread count. */
private data class ConversationRow(
    val chat: Chat,
    val name: String,
    val unreadCount: Int
)

/** Up to two uppercase initials for the round green avatar. */
private fun initialsFor(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

@Composable
fun ChatListScreen(
    authViewModel: AuthViewModel,
    chatViewModel: ChatViewModel,
    notifViewModel: NotificationViewModel, // New
    onBackClick: () -> Unit,
    onChatClick: (String, String) -> Unit,
    onStartChattingClick: () -> Unit // New
) {
    val user = authViewModel.userData.value
    // The profile row can lag behind the session (it loads asynchronously), so
    // fall back to the auth record — otherwise "the other participant" resolves
    // to yourself while userData is still null.
    val myId = user?.uid ?: SupabaseProvider.client.auth.currentUserOrNull()?.id
    val chats by chatViewModel.chats.collectAsState()
    val notifications by notifViewModel.notifications.collectAsState()

    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("All") }

    LaunchedEffect(myId) {
        myId?.let {
            chatViewModel.fetchChats(it)
            notifViewModel.fetchNotifications(it)
        }
    }

    val conversations = chats.map { chat ->
        val otherId = chat.participantIds.firstOrNull { it != myId }
            ?: chat.participantIds.firstOrNull()
            ?: ""
        // Try to get name from map, or from a notification if map is empty (old chats)
        val nameFromMap = chat.participantNames[otherId]
        val nameFromNotif = notifications.find { it.relatedId == chat.id }?.senderName
        val name = nameFromMap ?: nameFromNotif ?: "User"

        val unreadCount = notifications.count {
            !it.seen && it.relatedId == chat.id && it.type == "Message"
        }
        ConversationRow(chat = chat, name = name, unreadCount = unreadCount)
    }

    val filtered = conversations
        .filter { row ->
            when (activeFilter) {
                "Unread" -> row.unreadCount > 0
                "Read" -> row.unreadCount == 0
                else -> true
            }
        }
        .filter { row ->
            searchQuery.isBlank() ||
                row.name.contains(searchQuery, ignoreCase = true) ||
                row.chat.lastMessage.contains(searchQuery, ignoreCase = true)
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UniFindBackground)
    ) {
        ScreenHeader(
            title = "Messages",
            onBack = onBackClick,
            actions = {
                // Circular search toggle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (searchVisible) UniFindPrimary else UniFindWhite)
                        .border(
                            width = 1.dp,
                            color = if (searchVisible) UniFindPrimary else UniFindOutline,
                            shape = CircleShape
                        )
                        .clickable {
                            searchVisible = !searchVisible
                            if (!searchVisible) searchQuery = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search messages",
                        tint = if (searchVisible) UniFindOnPrimary else UniFindText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        if (searchVisible) {
            UniFindSearchBar(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.padding(horizontal = 20.dp),
                placeholder = "Search conversations..."
            )
            Spacer(Modifier.height(14.dp))
        }

        FilterChipRow(
            options = listOf("All", "Unread", "Read"),
            selected = activeFilter,
            onSelect = { activeFilter = it },
            modifier = Modifier.padding(horizontal = 20.dp),
            badges = mapOf("All" to conversations.size)
        )

        Spacer(Modifier.height(16.dp))

        when {
            conversations.isEmpty() -> Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                EmptyState(
                    icon = Icons.AutoMirrored.Outlined.Chat,
                    title = "No messages yet",
                    message = "Start a conversation with someone who found an item!"
                )
                PrimaryButton(
                    text = "Browse items",
                    onClick = onStartChattingClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp)
                )
            }

            filtered.isEmpty() -> Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Outlined.Search,
                    title = "Nothing here yet",
                    message = "No conversations match this filter or search."
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    UniFindCard(cornerRadius = 24.dp) {
                        filtered.forEachIndexed { index, row ->
                            ChatRow(
                                chat = row.chat,
                                otherName = row.name,
                                hasUnread = row.unreadCount > 0,
                                onClick = { onChatClick(row.chat.id, row.name) },
                                unreadCount = row.unreadCount
                            )
                            if (index < filtered.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 82.dp, end = 20.dp),
                                    thickness = 1.dp,
                                    color = UniFindDivider
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatRow(
    chat: Chat,
    otherName: String,
    hasUnread: Boolean,
    onClick: () -> Unit,
    unreadCount: Int = 0
) {
    val badgeCount = when {
        unreadCount > 0 -> unreadCount
        hasUnread -> 1
        else -> 0
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Round green avatar with initials + unread badge
        Box(modifier = Modifier.size(52.dp)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(UniFindPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initialsFor(otherName),
                    style = MaterialTheme.typography.titleMedium,
                    color = UniFindOnPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-4).dp)
                        .height(20.dp)
                        .widthIn(min = 20.dp)
                        .clip(CircleShape)
                        .background(UniFindPrimary)
                        .border(width = 2.dp, color = UniFindWhite, shape = CircleShape)
                        .padding(horizontal = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        color = UniFindOnPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = otherName,
                    style = MaterialTheme.typography.titleMedium,
                    color = UniFindText,
                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = timeAgo(chat.lastTimestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasUnread) UniFindPrimary else UniFindTextTertiary,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = chat.lastMessage,
                style = MaterialTheme.typography.bodySmall,
                color = if (hasUnread) UniFindText else UniFindTextSecondary,
                fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
