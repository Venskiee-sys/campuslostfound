package com.example.campuslostfound

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.campuslostfound.ui.components.*
import com.example.campuslostfound.ui.theme.*

// ---------------------------------------------------------------------------
// REUSABLE SEARCH BAR — thin wrapper around the UniFind search field
// ---------------------------------------------------------------------------
@Composable
fun CLFSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    // First optional parameter, per the Compose API guidelines.
    modifier: Modifier = Modifier,
    onFilterClick: () -> Unit = {},
    placeholder: String = "Search lost or found items..."
) {
    UniFindSearchBar(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = placeholder
    )
}

// ---------------------------------------------------------------------------
// SEARCH SCREEN — header + live search bar + filter chips + results
// ---------------------------------------------------------------------------
@Composable
fun SearchLostItemsScreen(
    itemViewModel: ItemViewModel,
    onBack: () -> Unit = {},
    onItemClick: (String) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val allLost by itemViewModel.lostItems.collectAsState()
    val allFound by itemViewModel.foundItems.collectAsState()
    val itemsLoaded = itemViewModel.itemsLoaded.value
    val allItems = allLost + allFound

    val filteredItems by remember(query, selectedFilter, allItems) {
        derivedStateOf {
            allItems.filter { item ->
                val matchesType = when (selectedFilter) {
                    "Lost" -> item.type.equals("Lost", ignoreCase = true)
                    "Found" -> item.type.equals("Found", ignoreCase = true)
                    else -> true
                }
                val matchesQuery = query.isBlank() ||
                        item.name.contains(query, ignoreCase = true) ||
                        item.category.contains(query, ignoreCase = true) ||
                        item.location.contains(query, ignoreCase = true)
                matchesType && matchesQuery
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UniFind.Background)
    ) {
        ScreenHeader(
            title = "Search",
            onBack = onBack,
            subtitle = "Find lost or found items"
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
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search lost or found items..."
                )
            }

            item {
                FilterChipRow(
                    options = listOf("All", "Lost", "Found"),
                    selected = selectedFilter,
                    onSelect = { selectedFilter = it }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (query.isBlank()) "All items"
                        else "Results for \"$query\"",
                        style = MaterialTheme.typography.titleMedium,
                        color = UniFind.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (filteredItems.isNotEmpty()) {
                        Spacer(Modifier.size(10.dp))
                        Text(
                            text = if (filteredItems.size == 1) "1 result"
                            else "${filteredItems.size} results",
                            style = MaterialTheme.typography.labelMedium,
                            color = UniFind.TextSecondary
                        )
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                item {
                    if (!itemsLoaded) {
                        // Nothing has loaded yet: a spinner, not "no items posted".
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        EmptyState(
                            icon = Icons.Outlined.Search,
                            title = "No items found",
                            message = when {
                                query.isBlank() && selectedFilter != "All" ->
                                    "Nothing matches the $selectedFilter filter yet."
                                query.isBlank() ->
                                    "No items have been posted yet. Check back soon."
                                else -> "No items match \"$query\". Try another search."
                            }
                        )
                    }
                }
            } else {
                items(filteredItems) { item ->
                    ItemRowCard(
                        name = item.name,
                        type = item.type,
                        location = item.location,
                        date = item.date,
                        category = item.category,
                        onClick = { onItemClick(item.id) }
                    )
                }
            }

            item { Spacer(Modifier.size(8.dp)) }
        }
    }
}
