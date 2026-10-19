package com.example.campuslostfound

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.campuslostfound.model.Item
import com.example.campuslostfound.ui.components.*
import com.example.campuslostfound.ui.theme.*

@Composable
fun LostItemsScreen(
    authViewModel: AuthViewModel,
    chatViewModel: ChatViewModel,
    itemViewModel: ItemViewModel,
    onBackClick: () -> Unit = {},
    onItemClick: (String) -> Unit = {},
    onMessageClick: (String, String) -> Unit = { _, _ -> }
) {
    var searchText by remember {
        mutableStateOf("")
    }

    val lostItems by itemViewModel.lostItems.collectAsState()
    val itemsLoaded = itemViewModel.itemsLoaded.value

    val filteredItems = lostItems.filter {
        it.name.contains(searchText, ignoreCase = true) ||
                it.category.contains(searchText, ignoreCase = true) ||
                it.location.contains(searchText, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UniFind.Background)
    ) {
        ScreenHeader(
            title = "Lost Items",
            onBack = onBackClick,
            subtitle = "Find items reported missing"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                UniFindSearchBar(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = "Search item, category, or location"
                )
            }

            if (filteredItems.isEmpty()) {
                item {
                    if (!itemsLoaded) {
                        // Feeds start empty — show progress instead of claiming
                        // nothing has been reported yet.
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        EmptyState(
                            icon = Icons.Outlined.Search,
                            title = "No lost items found",
                            message = if (searchText.isBlank())
                                "No lost items have been reported yet. Check back soon."
                            else "Nothing matches \"$searchText\". Try a different search."
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = if (filteredItems.size == 1) "1 lost item"
                        else "${filteredItems.size} lost items",
                        style = MaterialTheme.typography.labelMedium,
                        color = UniFind.TextSecondary
                    )
                }

                items(filteredItems) { item ->
                    LostItemCard(
                        item = item,
                        onClick = {
                            onItemClick(item.id)
                        },
                        onMessageClick = {
                            val currentUser = authViewModel.userData.value
                            if (currentUser != null && item.reportedBy != currentUser.uid) {
                                chatViewModel.startChat(
                                    myId = currentUser.uid,
                                    myName = currentUser.fullName,
                                    otherId = item.reportedBy,
                                    otherName = item.reporterName,
                                    onReady = { chatId ->
                                        onMessageClick(chatId, item.reporterName)
                                    }
                                )
                            }
                        },
                        isMine = item.reportedBy == authViewModel.userData.value?.uid
                    )
                }
            }

            item { Spacer(Modifier.size(8.dp)) }
        }
    }
}

@Composable
fun LostItemCard(
    item: Item,
    onClick: () -> Unit,
    onMessageClick: () -> Unit,
    isMine: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ItemRowCard(
            name = item.name,
            type = item.type,
            location = item.location,
            date = item.date,
            category = item.category,
            onClick = onClick,
            modifier = Modifier.weight(1f)
        )

        if (!isMine) {
            Spacer(Modifier.width(10.dp))
            MessageIconButton(onClick = onMessageClick)
        }
    }
}

/** Small circular line-icon button used for the "message" action on list rows. */
@Composable
private fun MessageIconButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(UniFind.White)
            .border(width = 1.dp, color = UniFind.Outline, shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Chat,
            contentDescription = "Message reporter",
            tint = UniFind.Primary,
            modifier = Modifier.size(22.dp)
        )
    }
}
