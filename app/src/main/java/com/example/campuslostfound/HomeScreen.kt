package com.example.campuslostfound

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.campuslostfound.ui.components.*
import com.example.campuslostfound.ui.theme.SetStatusBarColor
import com.example.campuslostfound.ui.theme.rememberIsLandscape
import com.example.campuslostfound.ui.theme.cardsPerRow
import com.example.campuslostfound.ui.theme.rememberWindowWidth
import com.example.campuslostfound.ui.theme.WindowWidth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Quick category shortcuts shown on the home screen. */
private val HomeCategories = listOf("Gadgets", "Wallets", "IDs", "Bags", "Keys")

/** Keywords that tie a category tile to the free-form `category` of a report. */
private val HomeCategoryKeywords = mapOf(
    "Gadgets" to listOf("gadget", "phone", "electronic", "laptop", "charger", "camera", "device", "tablet", "earphone", "headset"),
    "Wallets" to listOf("wallet", "purse", "billfold"),
    "IDs" to listOf("id", "identification", "document", "card", "license"),
    "Bags" to listOf("bag", "backpack", "satchel", "tote"),
    "Keys" to listOf("key")
)

/** True when an item's category matches a tapped category tile. */
private fun matchesHomeCategory(itemCategory: String, tile: String): Boolean {
    val raw = itemCategory.trim().lowercase()
    if (raw.isEmpty()) return false
    val stem = tile.trim().lowercase().removeSuffix("s")
    if (raw == stem || raw.removeSuffix("s") == stem) return true
    val keywords = HomeCategoryKeywords[tile] ?: return false
    return keywords.any { raw == it || raw.startsWith(it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("ConfigurationScreenWidthHeight"
, "UNUSED_VARIABLE")
fun HomeScreen(
    authViewModel: AuthViewModel,
    itemViewModel: ItemViewModel,
    notifViewModel: NotificationViewModel,
    onSearchClick: () -> Unit = {},
    onLostItemsClick: () -> Unit = {},
    onFoundItemsClick: () -> Unit = {},
    onReportLostClick: () -> Unit = {},
    onReportFoundClick: () -> Unit = {},
    onMyReportsClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onMapClick: () -> Unit = {},
    onItemClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val landscape = rememberIsLandscape()

    // A drawer left open while rotating can get stuck half-drawn on the new
    // window size; pruning it keeps the edge swipe of the rail clean.
    LaunchedEffect(landscape) { drawerState.close() }

    // The dark green header runs behind the status bar
    SetStatusBarColor(UniFind.Primary)

    // UI States
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showMapDialog by remember { mutableStateOf(false) }
    var showLeaderboardDialog by remember { mutableStateOf(false) }
    var showDraftsDialog by remember { mutableStateOf(false) }
    var showQRDialog by remember { mutableStateOf(false) }
    // rememberSaveable so rotation (activity recreation) keeps the user's
    // filter, category and search instead of resetting the feed.
    var selectedFilter by rememberSaveable { mutableStateOf("All") }
    // Tapped category tile (Wallets, Keys, …) — null means no category filter
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Dialogs
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "Are you sure you want to log out?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    authViewModel.signOut()
                    onLogoutClick()
                }) {
                    Text(
                        "LOGOUT",
                        color = UniFind.LostText,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("CANCEL", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }

    if (showSecurityDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityDialog = false },
            title = { Text("Campus Security", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text(
                        "Physical items should be surrendered to the Security Office at the Main Gate.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Office Hours: 8:00 AM - 5:00 PM",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Contact: (02) 8861-1234",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSecurityDialog = false }) {
                    Text("OK", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("How it Works", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Report items you lost or found.", style = MaterialTheme.typography.bodyMedium)
                    Text("2. Chat with finders/owners.", style = MaterialTheme.typography.bodyMedium)
                    Text("3. Meet at the Security Office for safety.", style = MaterialTheme.typography.bodyMedium)
                    Text("4. Mark reports as 'Resolved' once returned.", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("GOT IT", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }

    if (showMapDialog) {
        AlertDialog(
            onDismissRequest = { showMapDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = UniFind.Primary)
                    Spacer(Modifier.width(8.dp))
                    Text("University Map", style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = UniFind.Secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Security Office - Main Gate", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = UniFind.Secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Admin Hub - Library Bldg", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().height(if (landscape) 100.dp else 160.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = UniFind.SurfaceAlt)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.plmunlogo),
                            contentDescription = "Campus Map",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Inside,
                            alpha = 0.3f
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    SecondaryButton(
                        text = "OPEN IN GOOGLE MAPS",
                        onClick = {
                            val mapUri =
                                "geo:14.3916,121.0450?q=Pamantasan+ng+Lungsod+ng+Muntinlupa".toUri()
                            val mapsIntent =
                                Intent(Intent.ACTION_VIEW, mapUri)
                                    .setPackage("com.google.android.apps.maps")
                            val canHandleMaps = mapsIntent.resolveActivity(
                                context.packageManager
                            ) != null
                            // ActivityNotFoundException (device without Google
                            // Maps) used to crash the app straight from here.
                            runCatching {
                                context.startActivity(
                                    if (canHandleMaps) mapsIntent
                                    else Intent(Intent.ACTION_VIEW, mapUri)
                                )
                            }.onFailure {
                                Toast.makeText(
                                    context,
                                    "No map app found on this device.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.AutoMirrored.Filled.OpenInNew
                    )
                    PrimaryButton(
                        text = "VIEW LIVE IN-APP MAP",
                        onClick = {
                            showMapDialog = false
                            onMapClick()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Default.Map
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showMapDialog = false }) {
                    Text("CLOSE", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }

    if (showLeaderboardDialog) {
        AlertDialog(
            onDismissRequest = { showLeaderboardDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = UniFind.Secondary)
                    Spacer(Modifier.width(8.dp))
                    Text("Top Returners", style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Current Community Heroes:", style = MaterialTheme.typography.bodyMedium)
                    Text("1. Raven Kyle - 12 items returned", style = MaterialTheme.typography.titleSmall)
                    Text("2. Mark Anthony - 8 items returned", style = MaterialTheme.typography.bodyMedium)
                    Text("3. Charles Green - 5 items returned", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { showLeaderboardDialog = false }) {
                    Text("AWESOME", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }

    if (showQRDialog) {
        AlertDialog(
            onDismissRequest = { showQRDialog = false },
            title = { Text("Return QR Code", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Show this QR to the finder to confirm your item was returned safely.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Icon(
                        Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(140.dp),
                        tint = UniFind.Primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQRDialog = false }) {
                    Text("DONE", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }

    if (showDraftsDialog) {
        AlertDialog(
            onDismissRequest = { showDraftsDialog = false },
            title = { Text("My Drafts", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "You currently have 0 saved drafts. You can save reports here to finish them later.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showDraftsDialog = false }) {
                    Text("OK", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }

    // rememberSaveable so rotation (activity recreation) keeps the search text.
    var searchText by rememberSaveable { mutableStateOf("") }
    // Reset the scroll offset whenever orientation changes: landscape/portrait
    // have different item layouts, so a saved item index from one orientation
    // can otherwise land on a different section in the other and make items
    // look "missing". Search text, filter and category survive rotation.
    val homeListState = remember(landscape) { androidx.compose.foundation.lazy.LazyListState() }
    val userData = authViewModel.userData.value
    val userName = userData?.fullName?.split(" ")?.firstOrNull() ?: "Student"

    LaunchedEffect(userData) {
        userData?.uid?.let { notifViewModel.fetchNotifications(it) }
    }

    val lostItems by itemViewModel.lostItems.collectAsState()
    val foundItems by itemViewModel.foundItems.collectAsState()
    val itemsLoaded = itemViewModel.itemsLoaded.value
    val notifications by notifViewModel.notifications.collectAsState()
    val unreadCount = notifications.count { !it.seen }
    val unreadMessages = notifications.count { !it.seen && it.type == "Message" }
    val unreadAlerts = notifications.count { !it.seen && it.type != "Message" }

    val recentItems = (lostItems + foundItems)
        .filter {
            it.name.contains(searchText, ignoreCase = true) ||
                it.category.contains(searchText, ignoreCase = true) ||
                it.location.contains(searchText, ignoreCase = true)
        }
        .filter {
            selectedFilter.equals("All", ignoreCase = true) ||
                it.type.equals(selectedFilter, ignoreCase = true)
        }
        .filter { item ->
            val category = selectedCategory
            category == null || matchesHomeCategory(item.category, category)
        }
        .sortedByDescending { it.timestamp }
        .take(6)

    // Mobile 1 column, large phones/tablets 2, laptops/desktops 4.
    val gridColumns = cardsPerRow()

    val homeContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(UniFind.Background)
                .then(
                    // Keep content clear of left/right cutouts and the bottom
                    // gesture bar in landscape. Top stays with the green header.
                    if (landscape) Modifier.windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Left + WindowInsetsSides.Right +
                                WindowInsetsSides.Bottom
                        )
                    ) else Modifier
                )
        ) {
            if (landscape) {
                // Compact pinned bar: hamburger, greeting, bell. The big title
                // and search bar live in the scrolling list below.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(UniFind.Primary)
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = UniFind.OnPrimary
                            )
                        }
                        Text(
                            text = "Hello, $userName!",
                            style = MaterialTheme.typography.titleSmall,
                            color = UniFind.OnPrimary.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(UniFind.White)
                                .clickable(onClick = onNotificationsClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = UniFind.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                            if (unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 5.dp, end = 5.dp)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(UniFind.LostText)
                                )
                            }
                        }
                    }
                }
            } else {
                // Dark green header: greeting + bell, title, white search bar
                GreenHeader {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = UniFind.OnPrimary
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Hello, $userName!",
                            style = MaterialTheme.typography.titleMedium,
                            color = UniFind.OnPrimary.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(UniFind.White)
                                .clickable(onClick = onNotificationsClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = UniFind.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            if (unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 5.dp, end = 5.dp)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(UniFind.LostText)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Find what's missing",
                        style = MaterialTheme.typography.headlineMedium,
                        color = UniFind.OnPrimary
                    )
                    Spacer(Modifier.height(18.dp))
                    UniFindSearchBar(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = "Search lost or found items...",
                        onClick = onSearchClick
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        userData?.uid?.let { notifViewModel.fetchNotifications(it) }
                        itemViewModel.refreshItems()
                        delay(1000)
                        isRefreshing = false
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                LazyColumn(
                    state = homeListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = if (landscape) 12.dp else 18.dp,
                        bottom = if (landscape) 24.dp else 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // In landscape the header/search scroll with the content so
                    // the pinned bar is the only fixed chrome up top.
                    if (landscape) {
                        item {
                            Text(
                                text = "Find what's missing",
                                style = MaterialTheme.typography.headlineSmall,
                                color = UniFind.Text
                            )
                        }
                        item {
                            UniFindSearchBar(
                                value = searchText,
                                onValueChange = { searchText = it },
                                placeholder = "Search lost or found items...",
                                onClick = onSearchClick
                            )
                        }
                    }

                    // Filters — All / Lost / Found
                    item {
                        FilterChipRow(
                            options = listOf("All", "Lost", "Found"),
                            selected = selectedFilter,
                            onSelect = { selectedFilter = it },
                            badges = mapOf("Lost" to lostItems.size, "Found" to foundItems.size)
                        )
                    }

                    // Category shortcuts
                    item {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            // Shrink the icon tiles on narrow portrait screens so
                            // all five categories still fit without clipping.
                            val iconSize = ((maxWidth / 5f) - 20.dp).coerceIn(40.dp, 54.dp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                HomeCategories.forEach { category ->
                                    val isSelected = selectedCategory == category
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isSelected) UniFind.PaleLime
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                // Tap again to clear the category filter
                                                selectedCategory =
                                                    if (isSelected) null else category
                                                onCategoryClick(category)
                                            }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        CategoryIconTile(
                                            category = category,
                                            size = iconSize,
                                            cornerRadius = 16.dp,
                                            modifier = Modifier.then(
                                                if (isSelected) Modifier.border(
                                                    width = 2.dp,
                                                    color = UniFind.Primary,
                                                    shape = RoundedCornerShape(16.dp)
                                                ) else Modifier
                                            )
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) UniFind.Primary
                                            else UniFind.TextSecondary,
                                            fontWeight = if (isSelected) FontWeight.Bold
                                            else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Recent posts
                    item {
                        SectionHeader(
                            title = if (selectedCategory != null)
                                "Recent posts · $selectedCategory" else "Recent posts",
                            actionLabel = if (selectedCategory != null) "Clear" else "See all",
                            onAction = {
                                if (selectedCategory != null) selectedCategory = null
                                // "See all" heads a list of *everyone's* posts, so it
                                // must open the full browse list — not "My reports",
                                // which only contains the current user's items.
                                else onSearchClick()
                            }
                        )
                    }

                    if (recentItems.isEmpty()) {
                        item {
                            if (!itemsLoaded) {
                                // The feeds start empty: don't claim "no posts yet"
                                // before the first result has arrived.
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = UniFind.Primary)
                                }
                            } else {
                                EmptyState(
                                    icon = Icons.Default.Inventory2,
                                    title = if (selectedCategory != null)
                                        "No $selectedCategory reports yet" else "No recent posts",
                                    message = if (selectedCategory != null)
                                        "Nothing matches this category right now. Tap the tile again to see everything."
                                    else "Lost and found reports from students will appear here."
                                )
                            }
                        }
                    } else {
                        items(recentItems.chunked(gridColumns)) { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                rowItems.forEach { item ->
                                    ItemGridCard(
                                        name = item.name,
                                        type = item.type,
                                        location = item.location,
                                        timePosted = timeAgo(item.timestamp),
                                        category = item.category,
                                        onClick = { onItemClick(item.id) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowItems.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // Browse lost / found lists
                    item { SectionHeader(title = "Browse items") }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            BrowseCard(
                                "Lost Items",
                                "View all lost items",
                                Icons.Default.Warning,
                                UniFind.LostBackground,
                                UniFind.LostText,
                                onClick = onLostItemsClick,
                                modifier = Modifier.weight(1f)
                            )
                            BrowseCard(
                                "Found Items",
                                "View all found items",
                                Icons.Default.Inventory2,
                                UniFind.FoundBackground,
                                UniFind.FoundText,
                                onClick = onFoundItemsClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Quick reporting actions
                    item { SectionHeader(title = "Quick actions") }
                    item {
                        if (landscape) {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                ActionCard(
                                    title = "Report Lost Item",
                                    description = "Let others know what you lost",
                                    icon = Icons.Default.Warning,
                                    iconColor = UniFind.LostText,
                                    iconBackground = UniFind.LostBackground,
                                    onClick = onReportLostClick,
                                    modifier = Modifier.weight(1f)
                                )
                                ActionCard(
                                    title = "Report Found Item",
                                    description = "Help return lost items",
                                    icon = Icons.Default.Inventory2,
                                    iconColor = UniFind.FoundText,
                                    iconBackground = UniFind.FoundBackground,
                                    onClick = onReportFoundClick,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                ActionCard(
                                    title = "Report Lost Item",
                                    description = "Let others know what you lost",
                                    icon = Icons.Default.Warning,
                                    iconColor = UniFind.LostText,
                                    iconBackground = UniFind.LostBackground,
                                    onClick = onReportLostClick
                                )
                                ActionCard(
                                    title = "Report Found Item",
                                    description = "Help return lost items",
                                    icon = Icons.Default.Inventory2,
                                    iconColor = UniFind.FoundText,
                                    iconBackground = UniFind.FoundBackground,
                                    onClick = onReportFoundClick
                                )
                            }
                        }
                    }
                }
            }

        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = UniFind.White,
                        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    modifier = Modifier.width(
                        // Keep the drawer inside the screen on narrow phones so a slice of
                        // the page stays visible behind it (85% of width, capped at 300dp).
                        (LocalConfiguration.current.screenWidthDp.dp * 0.85f).coerceAtMost(300.dp)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(UniFind.Primary)
                            .padding(24.dp)
                    ) {
                        Column {
                            Image(
                                painter = painterResource(id = R.drawable.plmunlogo),
                                contentDescription = null,
                                modifier = Modifier.size(65.dp).clip(CircleShape).background(UniFind.White).padding(4.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                userData?.fullName ?: "User",
                                style = MaterialTheme.typography.titleMedium,
                                color = UniFind.OnPrimary
                            )
                            Text(
                                userData?.email ?: "student@plmun.edu.ph",
                                style = MaterialTheme.typography.bodySmall,
                                color = UniFind.OnPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Scrollable so every entry stays reachable on short portrait screens.
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        NavigationDrawerItem(
                            label = { Text("University Map", style = MaterialTheme.typography.bodyLarge) },
                            selected = false,
                            onClick = { scope.launch { drawerState.close() }; showMapDialog = true },
                            icon = { Icon(Icons.Default.Map, contentDescription = null) },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        NavigationDrawerItem(
                            label = { Text("My Profile", style = MaterialTheme.typography.bodyLarge) },
                            selected = false,
                            onClick = { scope.launch { drawerState.close() }; onProfileClick() },
                            icon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        NavigationDrawerItem(
                            label = { Text("Settings", style = MaterialTheme.typography.bodyLarge) },
                            selected = false,
                            onClick = { scope.launch { drawerState.close() }; onSettingsClick() },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        NavigationDrawerItem(
                            label = { Text("Top Returners", style = MaterialTheme.typography.bodyLarge) },
                            selected = false,
                            onClick = { scope.launch { drawerState.close() }; showLeaderboardDialog = true },
                            icon = { Icon(Icons.Default.Star, contentDescription = null) },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        NavigationDrawerItem(
                            label = { Text("My Drafts", style = MaterialTheme.typography.bodyLarge) },
                            selected = false,
                            onClick = { scope.launch { drawerState.close() }; showDraftsDialog = true },
                            icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        NavigationDrawerItem(
                            label = { Text("Return QR Code", style = MaterialTheme.typography.bodyLarge) },
                            selected = false,
                            onClick = { scope.launch { drawerState.close() }; showQRDialog = true },
                            icon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

                        NavigationDrawerItem(
                            label = { Text("Campus Security", style = MaterialTheme.typography.bodyLarge) },
                            selected = false,
                            onClick = { scope.launch { drawerState.close() }; showSecurityDialog = true },
                            icon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        NavigationDrawerItem(
                            label = { Text("How it Works", style = MaterialTheme.typography.bodyLarge) },
                            selected = false,
                            onClick = { scope.launch { drawerState.close() }; showHelpDialog = true },
                            icon = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null) },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        NavigationDrawerItem(
                            label = { Text("Log Out", style = MaterialTheme.typography.bodyLarge, color = UniFind.LostText) },
                            selected = false,
                            onClick = { scope.launch { drawerState.close() }; showLogoutDialog = true },
                            icon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = null,
                                    tint = UniFind.LostText
                                )
                            },
                            modifier = Modifier.padding(12.dp)
                        )
                    } // end scrollable drawer list
                    Spacer(Modifier.height(12.dp))
                }
            }
        ) {
            homeContent()
        }
}

/** One tappable option inside the "+" report bottom sheet. */
@Composable
private fun ReportSheetOption(
    title: String,
    message: String,
    icon: ImageVector,
    containerColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    UniFindCard(
        containerColor = UniFind.SurfaceAlt,
        cornerRadius = 20.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(
                icon = icon,
                size = 48.dp,
                cornerRadius = 14.dp,
                containerColor = containerColor,
                iconTint = iconTint,
                iconSize = 24.dp
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = UniFind.Text)
                Spacer(Modifier.height(2.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = UniFind.TextSecondary)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = UniFind.TextTertiary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun BrowseCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconBackground: Color,
    iconColor: Color,
    // First optional parameter, per the Compose API guidelines.
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: () -> Unit = {}
) {
    UniFindCard(
        modifier = modifier.heightIn(min = 160.dp),
        containerColor = containerColor,
        cornerRadius = 20.dp,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            IconTile(
                icon = icon,
                size = 48.dp,
                cornerRadius = 14.dp,
                containerColor = iconBackground,
                iconTint = iconColor,
                iconSize = 24.dp
            )
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = UniFind.Text)
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = UniFind.TextSecondary)
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = UniFind.TextTertiary,
                modifier = Modifier.align(Alignment.End).size(22.dp)
            )
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    iconBackground: Color,
    // First optional parameter, per the Compose API guidelines.
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    UniFindCard(
        modifier = modifier,
        cornerRadius = 20.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(
                icon = icon,
                size = 52.dp,
                cornerRadius = 16.dp,
                containerColor = iconBackground,
                iconTint = iconColor,
                iconSize = 26.dp
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = UniFind.Text)
                Spacer(Modifier.height(2.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = UniFind.TextSecondary)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = UniFind.TextTertiary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

data class Report(val id: String = "", val name: String, val location: String, val status: String)

@Composable
fun RecentReportCard(report: Report, onClick: () -> Unit) {
    val isLost = report.status.equals("Lost", ignoreCase = true)
    UniFindCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(
                icon = statusIcon(report.status),
                size = 52.dp,
                cornerRadius = 14.dp,
                containerColor = if (isLost) UniFind.LostBackground else UniFind.FoundBackground,
                iconTint = if (isLost) UniFind.LostText else UniFind.FoundText,
                iconSize = 26.dp
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    report.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = UniFind.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = UniFind.Secondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        report.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = UniFind.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    "Recent report",
                    style = MaterialTheme.typography.labelSmall,
                    color = UniFind.TextTertiary
                )
            }
            Spacer(Modifier.width(10.dp))
            StatusTag(type = report.status)
        }
    }
}
