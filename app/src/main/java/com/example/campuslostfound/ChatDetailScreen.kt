package com.example.campuslostfound

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.campuslostfound.model.Message
import com.example.campuslostfound.ui.components.*
import com.example.campuslostfound.ui.theme.*
import io.github.jan.supabase.auth.auth
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun ChatDetailScreen(
    chatId: String,
    otherName: String,
    authViewModel: AuthViewModel,
    chatViewModel: ChatViewModel,
    notifViewModel: NotificationViewModel, // New parameter
    onBackClick: () -> Unit
) {
    val user = authViewModel.userData.value
    val messages by chatViewModel.messages.collectAsState()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Resolve the other participant's name reactively: it is looked up from the
    // chat list, which may arrive after this screen (e.g. opened from a
    // notification), so a one-shot lookup could leave the header stuck on "User".
    val chats by chatViewModel.chats.collectAsState()
    val displayName = remember(chats, chatId, otherName) {
        val chat = chats.find { it.id == chatId }
        val otherId = chat?.participantIds?.find { it != user?.uid }
        val resolved = otherId?.let { chat?.participantNames?.get(it) }
        when {
            !resolved.isNullOrBlank() -> resolved
            !otherName.isNullOrBlank() && otherName != "User" -> otherName
            else -> resolved ?: otherName.ifBlank { "User" }
        }
    }

    LaunchedEffect(chatId) {
        chatViewModel.fetchMessages(chatId)
        // Name resolution (and the unread badge) need the conversation list;
        // make sure it exists when the screen was opened from a notification.
        val uid = user?.uid ?: SupabaseProvider.client.auth.currentUserOrNull()?.id
        if (uid != null && chatViewModel.chats.value.isEmpty()) {
            chatViewModel.fetchChats(uid)
        }
    }

    // Mark messages as read when entering or receiving new ones. The uid has to
    // be a key: it is usually null on the very first composition (the profile
    // loads asynchronously), and the effect must re-run once it arrives.
    LaunchedEffect(chatId, user?.uid, messages.size) {
        val uid = user?.uid ?: return@LaunchedEffect
        if (chatId.isNotBlank()) notifViewModel.markChatMessagesAsRead(uid, chatId)
    }

    // Scroll to the newest bubble. Keyed on the last message rather than the
    // count, so opening a chat with the same number of messages as the previous
    // one still lands at the bottom.
    val lastMessageId = messages.lastOrNull()?.id
    LaunchedEffect(chatId, lastMessageId) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UniFindBackground)
    ) {
        // Green header strip: back button + other user's name + subtitle
        GreenHeader {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleBackButton(onClick = onBackClick)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = UniFindOnPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.VerifiedUser,
                            contentDescription = null,
                            tint = UniFindPaleLime,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Verified user",
                            style = MaterialTheme.typography.labelMedium,
                            color = UniFindOnPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Message thread
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message, message.senderId == user?.uid)
            }
        }

        // Input bar: white rounded pill field + circular green send button
        Surface(
            color = UniFindBackground,
            shadowElevation = 10.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                chatViewModel.errorMessage.value?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
                    )
                }
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding() // Pushes the input bar up when keyboard opens
                    .navigationBarsPadding() // Ensures it doesn't overlap with system nav bar
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(UniFindWhite)
                        .border(
                            width = 1.dp,
                            color = UniFindOutline,
                            shape = RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 13.dp)
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = "Type a message...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = UniFindTextTertiary
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = {
                            text = it
                            chatViewModel.clearError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = UniFindText),
                        cursorBrush = SolidColor(UniFindPrimary),
                        maxLines = 3
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(UniFindPrimary)
                        .clickable {
                            val draft = text
                            val uid = user?.uid
                            when {
                                draft.isBlank() -> Unit
                                uid == null -> chatViewModel.notifyError(
                                    "Please sign in again to send messages."
                                )
                                else -> chatViewModel.sendMessage(uid, draft) { sent ->
                                    // Only drop the draft once the message is on the
                                    // server — clearing it first threw the text away
                                    // on every failed send.
                                    if (sent) text = ""
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = UniFindOnPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

}

@Composable
fun MessageBubble(message: Message, isMine: Boolean) {
    // Observable locale (lint: NonObservableLocale) — recreated only when it changes
    val locale = LocalConfiguration.current.locales[0]
    val timeFormat = remember(locale) { SimpleDateFormat("hh:mm a", locale) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isMine) UniFindPrimary else UniFindWhite,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                // Smaller corner on the sender (tail) side
                bottomStart = if (isMine) 20.dp else 6.dp,
                bottomEnd = if (isMine) 6.dp else 20.dp
            ),
            shadowElevation = 2.dp
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                color = if (isMine) UniFindOnPrimary else UniFindText,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = timeFormat.format(Date(message.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = UniFindTextTertiary
        )
    }
}
