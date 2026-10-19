package com.example.campuslostfound

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.campuslostfound.ui.components.ReportChoiceSheet
import com.example.campuslostfound.ui.components.UniFindNavBar
import com.example.campuslostfound.ui.components.UniFindNavRail
import com.example.campuslostfound.ui.theme.CampusLostFoundTheme
import com.example.campuslostfound.ui.theme.rememberIsLandscape
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val isDark by remember { mutableStateOf(authViewModel.isDarkMode()) }
            var currentDarkTheme by remember { mutableStateOf(isDark) }

            CampusLostFoundTheme(darkTheme = currentDarkTheme) {

                val itemViewModel: ItemViewModel = viewModel()
                val chatViewModel: ChatViewModel = viewModel()
                val notifViewModel: NotificationViewModel = viewModel()
                val isUserAuthenticated by authViewModel.isUserAuthenticated.collectAsState()
                val navController = rememberNavController()
                // Read once for this cold start: onboarding is a first-run screen,
                // not something logged-out users should have to sit through again.
                val hasSeenOnboarding = remember { authViewModel.hasSeenOnboarding() }

                // Screens sign out via authViewModel; this also drops everything
                // those view models cached for the previous account.
                val clearSessionData = {
                    chatViewModel.clear()
                    notifViewModel.clear()
                    itemViewModel.clear()
                }

                // The map picker used to throw its result away — it is parked here
                // until the report form consumes it.
                var pendingLocation by remember { mutableStateOf<String?>(null) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    // Global state for the stable bottom navigation / report sheet.
                    var showReportChoiceSheet by remember { mutableStateOf(false) }
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route ?: ""
                    val showGlobalNavBar = currentRoute in setOf(
                        "home", "lost_items", "found_items", "item_detail/{itemId}",
                        "my_reports", "chat_list", "notifications", "profile",
                        "settings", "personal_info", "search"
                    )
                    val currentTab = when (currentRoute) {
                        "home" -> "home"
                        "my_reports", "lost_items", "found_items", "item_detail/{itemId}" -> "reports"
                        "chat_list" -> "messages"
                        "notifications" -> "alerts"
                        else -> ""
                    }
                    val allNotifications by notifViewModel.notifications.collectAsState()
                    val unreadMessages = allNotifications.count { !it.seen && it.type == "Message" }
                    val unreadAlerts = allNotifications.count { !it.seen && it.type != "Message" }

                    val landscape = rememberIsLandscape()

                    // In landscape the rail owns the left edge, so the NavHost
                    // only needs to be kept clear of the bottom nav in portrait.
                    Row(modifier = Modifier.fillMaxSize()) {
                        if (showGlobalNavBar && landscape) {
                            UniFindNavRail(
                                current = currentTab,
                                onHome = {
                                    navController.navigate("home") { launchSingleTop = true }
                                },
                                onReports = {
                                    navController.navigate("my_reports") { launchSingleTop = true }
                                },
                                onReportClick = { showReportChoiceSheet = true },
                                onMessages = {
                                    navController.navigate("chat_list") { launchSingleTop = true }
                                },
                                onAlerts = {
                                    navController.navigate("notifications") { launchSingleTop = true }
                                },
                                modifier = Modifier
                                    .fillMaxHeight(),
                                messageBadge = unreadMessages,
                                alertBadge = unreadAlerts
                            )
                        }

                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        modifier = Modifier
                            .fillMaxSize()
                            // Keep the last content row clear of the fixed nav bar.
                            .padding(bottom = if (showGlobalNavBar && !landscape) 96.dp else 0.dp)
                    ) {

                        // Splash → Home / Onboarding / Sign in
                        composable("splash") {
                            SplashScreen(
                                onFinish = {
                                    val next = when {
                                        isUserAuthenticated -> "home"
                                        hasSeenOnboarding -> "login"
                                        else -> "onboarding"
                                    }
                                    navController.navigate(next) {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Onboarding (first run only)
                        composable("onboarding") {
                            OnboardingScreen(
                                onGetStartedClick = {
                                    authViewModel.setOnboardingSeen()
                                    navController.navigate("login") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                },
                                onSignInClick = {
                                    authViewModel.setOnboardingSeen()
                                    navController.navigate("login") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Login Screen]
                        composable("login") {
                            val context = LocalContext.current
                            val scope = rememberCoroutineScope()
                            LoginScreen(
                                viewModel = authViewModel,
                                onLoginClick = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onCreateAccountClick = {
                                    navController.navigate("signup")
                                },
                                onGoogleSignInClick = {
                                    authViewModel.clearError()
                                    scope.launch {
                                        when (val result = requestGoogleIdToken(context)) {
                                            is GoogleSignInResult.Success ->
                                                // The picker succeeded; exchange the
                                                // Google ID token for a session in
                                                // Supabase (and enforce .edu.ph).
                                                authViewModel.signInWithGoogle(
                                                    idToken = result.idToken,
                                                    rawNonce = result.rawNonce,
                                                    pickedEmail = result.email
                                                ) {
                                                    navController.navigate("home") {
                                                        popUpTo("login") { inclusive = true }
                                                    }
                                                }

                                            // Dismissing the sheet is a normal change
                                            // of mind — stay silent.
                                            GoogleSignInResult.Cancelled -> Unit

                                            GoogleSignInResult.NoAccount ->
                                                authViewModel.postError(
                                                    "No Google account on this device. Add your " +
                                                        "school account in Android Settings → " +
                                                        "Passwords & accounts, or sign in with your email."
                                                )

                                            is GoogleSignInResult.Failure ->
                                                authViewModel.postError(result.message)
                                        }
                                    }
                                }
                            )
                        }

                        // Sign Up Screen
                        composable("signup") {
                            SignUpScreen(
                                viewModel = authViewModel,
                                onBackToLoginClick = {
                                    navController.popBackStack()
                                },
                                onCreateAccountClick = {
                                    navController.navigate("home") {
                                        popUpTo("signup") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Home Screen
                        composable("home") {
                            HomeScreen(
                                authViewModel = authViewModel,
                                itemViewModel = itemViewModel,
                                notifViewModel = notifViewModel,
                                onSearchClick = {
                                    navController.navigate("search")
                                },
                                onLostItemsClick = {
                                    navController.navigate("lost_items")
                                },
                                onFoundItemsClick = {
                                    navController.navigate("found_items")
                                },
                                onReportLostClick = {
                                    navController.navigate("report_lost")
                                },
                                onReportFoundClick = {
                                    navController.navigate("report_found")
                                },
                                onMyReportsClick = {
                                    navController.navigate("my_reports")
                                },
                                onChatClick = {
                                    navController.navigate("chat_list")
                                },
                                onNotificationsClick = {
                                    navController.navigate("notifications")
                                },
                                onProfileClick = {
                                    navController.navigate("profile")
                                },
                                onSettingsClick = {
                                    navController.navigate("settings")
                                },
                                onLogoutClick = {
                                    clearSessionData()
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onMapClick = {
                                    navController.navigate("map_screen")
                                },
                                onItemClick = { itemId ->
                                    navController.navigate("item_detail/$itemId")
                                }
                            )
                        }

                        // Chat List
                        composable("chat_list") {
                            ChatListScreen(
                                authViewModel = authViewModel,
                                chatViewModel = chatViewModel,
                                notifViewModel = notifViewModel,
                                onBackClick = { navController.popBackStack() },
                                onChatClick = { chatId, name ->
                                    navController.navigate("chat_detail/$chatId/${Uri.encode(name)}")
                                },
                                onStartChattingClick = {
                                    navController.navigate("lost_items")
                                }
                            )
                        }

                        // Chat Detail
                        composable("chat_detail/{chatId}/{name}") { backStackEntry ->
                            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                            val name = backStackEntry.arguments?.getString("name") ?: ""
                            ChatDetailScreen(
                                chatId = chatId,
                                otherName = name,
                                authViewModel = authViewModel,
                                chatViewModel = chatViewModel,
                                notifViewModel = notifViewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        // Item Detail
                        composable("item_detail/{itemId}") { backStackEntry ->
                            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                            ItemDetailScreen(
                                itemId = itemId,
                                authViewModel = authViewModel,
                                itemViewModel = itemViewModel,
                                chatViewModel = chatViewModel,
                                onBackClick = { navController.popBackStack() },
                                onMessageClick = { chatId, name ->
                                    navController.navigate("chat_detail/$chatId/${Uri.encode(name)}")
                                },
                                onClaimClick = {
                                    navController.navigate("claim_item/$itemId")
                                }
                            )
                        }

                        // Search
                        composable("search") {
                            SearchLostItemsScreen(
                                itemViewModel = itemViewModel,
                                onBack = {
                                    navController.popBackStack()
                                },
                                onItemClick = { itemId ->
                                    navController.navigate("item_detail/$itemId")
                                }
                            )
                        }

                        // Profile
                        composable("profile") {
                            ProfileScreen(
                                viewModel = authViewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onEditProfileClick = {
                                    navController.navigate("personal_info")
                                },
                                onNotificationsClick = {
                                    navController.navigate("notifications")
                                },
                                onSettingsClick = {
                                    navController.navigate("settings")
                                },
                                onLogoutClick = {
                                    clearSessionData()
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        // Personal Information
                        composable("personal_info") {
                            PersonalInformationScreen(
                                viewModel = authViewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onSaveClick = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // Notifications
                        composable("notifications") {
                            NotificationsScreen(
                                authViewModel = authViewModel,
                                viewModel = notifViewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onNotificationClick = { type, id, name ->
                                    if (type == "Message" && id != null) {
                                        val displayName = name ?: "User"
                                        navController.navigate("chat_detail/$id/${Uri.encode(displayName)}")
                                    } else if (id != null) {
                                        navController.navigate("item_detail/$id")
                                    }
                                }
                            )
                        }

                        // Settings
                        composable("settings") {
                            SettingsScreen(
                                viewModel = authViewModel,
                                onThemeChange = { currentDarkTheme = it },
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onLogoutClick = {
                                    clearSessionData()
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        // My Reports
                        composable("my_reports") {
                            MyReportsScreen(
                                authViewModel = authViewModel,
                                itemViewModel = itemViewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onReportClick = { itemId ->
                                    navController.navigate("item_detail/$itemId")
                                },
                                onReportLostClick = {
                                    navController.navigate("report_lost")
                                },
                                onReportFoundClick = {
                                    navController.navigate("report_found")
                                }
                            )
                        }

                        // Claim Item
                        composable("claim_item/{itemId}") { backStackEntry ->
                            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                            ClaimItemScreen(
                                itemId = itemId,
                                itemViewModel = itemViewModel,
                                onBackClick = { navController.popBackStack() },
                                onSubmitClick = {
                                    navController.navigate("claim_status/$itemId") {
                                        popUpTo("claim_item/$itemId") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Claim Status
                        composable("claim_status/{itemId}") { backStackEntry ->
                            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                            ClaimStatusScreen(
                                itemId = itemId,
                                itemViewModel = itemViewModel,
                                authViewModel = authViewModel,
                                chatViewModel = chatViewModel,
                                onBackClick = {
                                    navController.navigate("home") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                },
                                onMessageFinderClick = { chatId, name ->
                                    navController.navigate("chat_detail/$chatId/${Uri.encode(name)}")
                                }
                            )
                        }

                        // Report Lost
                        composable("report_lost") {
                            ReportLostItemScreen(
                                authViewModel = authViewModel,
                                itemViewModel = itemViewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onSubmitClick = {
                                    navController.popBackStack()
                                },
                                onSwitchTypeClick = {
                                    navController.navigate("report_found") {
                                        popUpTo("report_lost") { inclusive = true }
                                    }
                                },
                                onPickLocationClick = {
                                    navController.navigate("map_screen")
                                },
                                pickedLocation = pendingLocation,
                                onLocationConsumed = { pendingLocation = null }
                            )
                        }

                        // Report Found
                        composable("report_found") {
                            ReportFoundItemScreen(
                                authViewModel = authViewModel,
                                itemViewModel = itemViewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onSubmitClick = {
                                    navController.popBackStack()
                                },
                                onSwitchTypeClick = {
                                    navController.navigate("report_lost") {
                                        popUpTo("report_found") { inclusive = true }
                                    }
                                },
                                onPickLocationClick = {
                                    navController.navigate("map_screen")
                                },
                                pickedLocation = pendingLocation,
                                onLocationConsumed = { pendingLocation = null }
                            )
                        }

                        // Lost Items
                        composable("lost_items") {
                            LostItemsScreen(
                                authViewModel = authViewModel,
                                chatViewModel = chatViewModel,
                                itemViewModel = itemViewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onItemClick = { itemId ->
                                    navController.navigate("item_detail/$itemId")
                                },
                                onMessageClick = { chatId, name ->
                                    navController.navigate("chat_detail/$chatId/${Uri.encode(name)}")
                                }
                            )
                        }

                        // Found Items
                        composable("found_items") {
                            FoundItemsScreen(
                                authViewModel = authViewModel,
                                chatViewModel = chatViewModel,
                                itemViewModel = itemViewModel,
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onItemClick = { itemId ->
                                    navController.navigate("item_detail/$itemId")
                                },
                                onMessageClick = { chatId, name ->
                                    navController.navigate("chat_detail/$chatId/${Uri.encode(name)}")
                                }
                            )
                        }

                        // Live Map
                        composable("map_screen") {
                            MapScreen(
                                onBackClick = {
                                    navController.popBackStack()
                                },
                                onLocationConfirmed = { place ->
                                    pendingLocation = place
                                    navController.popBackStack()
                                }
                            )
                        }
                    }

                    // Stable bottom navigation: rendered outside the scrolling
                    // NavHost content so it never scrolls, shrinks, or moves.
                    if (showGlobalNavBar && !landscape) {
                        UniFindNavBar(
                            current = currentTab,
                            onHome = {
                                navController.navigate("home") { launchSingleTop = true }
                            },
                            onReports = {
                                navController.navigate("my_reports") { launchSingleTop = true }
                            },
                            onReportClick = { showReportChoiceSheet = true },
                            onMessages = {
                                navController.navigate("chat_list") { launchSingleTop = true }
                            },
                            onAlerts = {
                                navController.navigate("notifications") { launchSingleTop = true }
                            },
                            modifier = Modifier
                                .align(androidx.compose.ui.Alignment.BottomCenter)
                                .navigationBarsPadding(),
                            messageBadge = unreadMessages,
                            alertBadge = unreadAlerts
                        )
                    }
                    }
                    }

                    if (showReportChoiceSheet) {
                        ReportChoiceSheet(
                            onDismissRequest = { showReportChoiceSheet = false },
                            onLost = {
                                showReportChoiceSheet = false
                                navController.navigate("report_lost")
                            },
                            onFound = {
                                showReportChoiceSheet = false
                                navController.navigate("report_found")
                            }
                        )
                    }
                }
            }
        }
    }
}