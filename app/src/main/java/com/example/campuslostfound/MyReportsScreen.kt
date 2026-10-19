package com.example.campuslostfound

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.campuslostfound.model.Item
import com.example.campuslostfound.ui.components.*
import com.example.campuslostfound.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen(
    authViewModel: AuthViewModel,
    itemViewModel: ItemViewModel,
    onBackClick: () -> Unit,
    onReportClick: (String) -> Unit,
    onReportLostClick: () -> Unit = {},
    onReportFoundClick: () -> Unit = {}
) {
    val user = authViewModel.userData.value
    // StateFlow, not compose State: reading .value directly in composition
    // would not subscribe to it (and lint errors on it). Collect it instead so
    // the sign-in check reacts when the session arrives.
    val isAuthenticated by authViewModel.isUserAuthenticated.collectAsState()
    var showResolveDialog by remember { mutableStateOf<String?>(null) }
    var showReportSheet by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }

    LaunchedEffect(user) {
        user?.uid?.let { uid ->
            itemViewModel.fetchMyReports(uid)
        }
    }

    val myReports by itemViewModel.myReports.collectAsState()

    val filteredReports = when (selectedFilter) {
        "Lost" -> myReports.filter { it.type.equals("Lost", ignoreCase = true) }
        "Found" -> myReports.filter { it.type.equals("Found", ignoreCase = true) }
        "Resolved" -> myReports.filter { it.status.equals("Resolved", ignoreCase = true) }
        else -> myReports
    }

    val filterBadges = mapOf(
        "All" to myReports.size,
        "Lost" to myReports.count { it.type.equals("Lost", ignoreCase = true) },
        "Found" to myReports.count { it.type.equals("Found", ignoreCase = true) },
        "Resolved" to myReports.count { it.status.equals("Resolved", ignoreCase = true) }
    )

    // Resolve confirmation dialog
    if (showResolveDialog != null) {
        AlertDialog(
            onDismissRequest = { showResolveDialog = null },
            containerColor = UniFind.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Mark as Resolved",
                    style = MaterialTheme.typography.titleLarge,
                    color = UniFind.Text
                )
            },
            text = {
                Column {
                    Text(
                        text = "Has this item been returned? It will no longer be visible to other users.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniFind.TextSecondary
                    )
                    itemViewModel.errorMessage.value?.let { error ->
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = error,
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
                        showResolveDialog?.let { id ->
                            // Close only after the update succeeds, otherwise the
                            // item silently stays "Open" with no explanation.
                            itemViewModel.resolveItem(
                                itemId = id,
                                onSuccess = { showResolveDialog = null }
                            )
                        }
                    }
                ) {
                    Text(
                        text = "YES, RESOLVED",
                        color = UniFind.Primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResolveDialog = null }) {
                    Text(
                        text = "CANCEL",
                        color = UniFind.TextSecondary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        )
    }

    // Report picker — "I lost something" / "I found something"
    if (showReportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showReportSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = UniFind.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp)
            ) {
                Text(
                    text = "Create a report",
                    style = MaterialTheme.typography.titleMedium,
                    color = UniFind.Text
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "What would you like to post?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = UniFind.TextSecondary
                )
                Spacer(Modifier.height(16.dp))
                ReportOptionRow(
                    icon = Icons.AutoMirrored.Outlined.Help,
                    iconBackground = UniFind.LostBackground,
                    iconTint = UniFind.LostText,
                    title = "I lost something",
                    message = "Post an item that is missing",
                    onClick = {
                        showReportSheet = false
                        onReportLostClick()
                    }
                )
                Spacer(Modifier.height(12.dp))
                ReportOptionRow(
                    icon = Icons.Outlined.Inventory2,
                    iconBackground = UniFind.FoundBackground,
                    iconTint = UniFind.FoundText,
                    title = "I found something",
                    message = "Post an item you found on campus",
                    onClick = {
                        showReportSheet = false
                        onReportFoundClick()
                    }
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UniFind.Background)
    ) {
        ScreenHeader(
            title = "My Reports",
            onBack = onBackClick,
            subtitle = "Track your lost and found reports",
            actions = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(UniFind.Primary)
                        .clickable { showReportSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Report an item",
                        tint = UniFind.OnPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        )

        if (user == null) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isAuthenticated) {
                    CircularProgressIndicator(color = UniFind.Primary)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Please sign in to see your reports.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = UniFind.TextSecondary
                        )
                    }
                }
            }
        } else {
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
                    FilterChipRow(
                        options = listOf("All", "Lost", "Found", "Resolved"),
                        selected = selectedFilter,
                        onSelect = { selectedFilter = it },
                        badges = filterBadges
                    )
                }

                item {
                    Text(
                        text = if (filteredReports.size == 1) "1 report"
                        else "${filteredReports.size} reports",
                        style = MaterialTheme.typography.labelMedium,
                        color = UniFind.TextSecondary
                    )
                }

                if (filteredReports.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.AutoMirrored.Outlined.Assignment,
                            title = "No reports yet",
                            message = when (selectedFilter) {
                                "All" -> "Your reported items will appear here."
                                "Lost" -> "You have not reported any lost items yet."
                                "Found" -> "You have not reported any found items yet."
                                else -> "Reports you mark as Resolved will appear here."
                            }
                        )
                    }
                } else {
                    items(filteredReports) { report ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ItemRowCard(
                                name = report.name,
                                type = report.type,
                                location = report.location,
                                date = report.date,
                                category = report.category,
                                onClick = { onReportClick(report.id) },
                                modifier = Modifier.weight(1f),
                                statusLabel = reportStatusPill(report.status)
                            )
                            if (report.status == "Open") {
                                Spacer(Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(UniFind.SurfaceAlt)
                                        .clickable {
                                            itemViewModel.clearError()
                                            showResolveDialog = report.id
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = "Mark as resolved",
                                        tint = UniFind.Primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

/**
 * Maps an item status onto one of the UniFind status pill labels
 * (Searching / Possible match / Claimed / Returned / Resolved / …).
 */
private fun reportStatusPill(status: String): String = when (status.trim().lowercase()) {
    "", "open" -> "Searching"
    "resolved" -> "Resolved"
    "closed" -> "Closed"
    else -> status.trim()
}

/** One tappable option inside the report picker bottom sheet. */
@Composable
private fun ReportOptionRow(
    icon: ImageVector,
    iconBackground: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    message: String,
    onClick: () -> Unit
) {
    UniFindCard(onClick = onClick, containerColor = UniFind.SurfaceAlt) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(
                icon = icon,
                size = 46.dp,
                cornerRadius = 14.dp,
                containerColor = iconBackground,
                iconTint = iconTint,
                iconSize = 24.dp
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = UniFind.Text
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = UniFind.TextSecondary
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = UniFind.TextTertiary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// MY REPORT CARD — kept for compatibility; the list uses ItemRowCard
// ---------------------------------------------------------------------------

@Composable
fun MyReportCard(
    report: Item,
    onClick: () -> Unit,
    onResolveClick: () -> Unit
) {
    UniFindCard(onClick = onClick) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIconTile(
                    category = report.category,
                    size = 54.dp,
                    cornerRadius = 16.dp
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = UniFind.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = report.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = UniFind.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(10.dp))
                StatusTag(type = report.type)
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = report.description,
                style = MaterialTheme.typography.bodySmall,
                color = UniFind.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = UniFind.Divider)
            Spacer(Modifier.height(4.dp))

            InfoRow(
                icon = Icons.Outlined.LocationOn,
                label = "Location",
                value = report.location
            )
            InfoRow(
                icon = Icons.Outlined.DateRange,
                label = "Date",
                value = report.date
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(reportStatusPill(report.status))
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClick) {
                    Text(
                        text = "Details",
                        style = MaterialTheme.typography.labelLarge,
                        color = UniFind.Secondary
                    )
                }
                if (report.status == "Open") {
                    TextButton(onClick = onResolveClick) {
                        Text(
                            text = "Mark resolved",
                            style = MaterialTheme.typography.labelLarge,
                            color = UniFind.Primary
                        )
                    }
                }
            }
        }
    }
}
