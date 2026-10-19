package com.example.campuslostfound

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campuslostfound.ui.components.*
import com.example.campuslostfound.ui.theme.*

// ---------------------------------------------------------------------------
// PROFILE SCREEN
// Dark green header (avatar, name, verified line, edit affordance) over
// white rounded cards with icon-tile menu rows.
// ---------------------------------------------------------------------------
@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onEditProfileClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogoutClick: (() -> Unit)? = null
) {
    val user = viewModel.userData.value

    var showLogoutDialog by remember { mutableStateOf(false) }

    val displayName = user?.fullName?.takeIf { it.isNotBlank() } ?: "Student Name"
    val initials = displayName
        .split(" ")
        .filter { it.isNotBlank() }
        .mapNotNull { it.firstOrNull() }
        .take(2)
        .joinToString("") { it.uppercaseChar().toString() }
        .ifBlank { "S" }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = UniFind.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Log out", style = MaterialTheme.typography.titleMedium, color = UniFind.Text)
            },
            text = {
                Text(
                    "Are you sure you want to log out of your account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = UniFind.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.signOut()
                        onLogoutClick?.invoke()
                    }
                ) {
                    Text("Log out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = UniFind.Primary)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(UniFind.Background)
            .navigationBarsPadding()
    ) {

        // ---- Green header -------------------------------------------------
        item {
            GreenHeader(modifier = Modifier.fillMaxWidth()) {

                CircleBackButton(onClick = onBackClick, tint = UniFind.Primary)

                Spacer(modifier = Modifier.height(18.dp))

                // Avatar — initials in a soft pale-lime circle
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(UniFind.PaleLime),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.headlineMedium,
                        color = UniFind.Primary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = UniFind.OnPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = null,
                        tint = UniFind.PaleLime,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Verified user",
                        style = MaterialTheme.typography.labelMedium,
                        color = UniFind.OnPrimary.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Edit-profile affordance — white pill on the green header
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(UniFind.White)
                        .clickable(onClick = onEditProfileClick)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = UniFind.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit profile",
                        style = MaterialTheme.typography.labelLarge,
                        color = UniFind.Primary
                    )
                }
            }
        }

        // ---- Cards --------------------------------------------------------
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                SectionHeader(title = "Account details")
                Spacer(modifier = Modifier.height(10.dp))

                UniFindCard(cornerRadius = 24.dp) {
                    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                        InfoRow(
                            icon = Icons.Outlined.Badge,
                            label = "Student ID",
                            value = user?.studentId ?: "2026-XXXXX"
                        )

                        val course = user?.course
                        if (!course.isNullOrBlank()) {
                            InfoRow(
                                icon = Icons.AutoMirrored.Outlined.MenuBook,
                                label = "Course / Program",
                                value = course
                            )
                        }

                        val yearLevel = user?.yearLevel
                        if (!yearLevel.isNullOrBlank()) {
                            InfoRow(
                                icon = Icons.AutoMirrored.Outlined.List,
                                label = "Year Level",
                                value = yearLevel
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                SectionHeader(title = "Account")
                Spacer(modifier = Modifier.height(10.dp))

                UniFindCard(cornerRadius = 24.dp) {
                    Column {
                        ProfileMenuItem(
                            icon = Icons.Outlined.Person,
                            title = "Personal information",
                            subtitle = "View and edit your information",
                            onClick = onEditProfileClick
                        )

                        HorizontalDivider(color = UniFind.Divider, thickness = 1.dp)

                        ProfileMenuItem(
                            icon = Icons.Outlined.Notifications,
                            title = "Notifications",
                            subtitle = "Manage your notifications",
                            onClick = onNotificationsClick
                        )

                        HorizontalDivider(color = UniFind.Divider, thickness = 1.dp)

                        ProfileMenuItem(
                            icon = Icons.Outlined.Settings,
                            title = "Settings",
                            subtitle = "App preferences",
                            onClick = onSettingsClick
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                UniFindCard(cornerRadius = 24.dp) {
                    ProfileMenuItem(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "Log out",
                        subtitle = "Sign out of this device",
                        onClick = { showLogoutDialog = true },
                        iconTint = MaterialTheme.colorScheme.error,
                        tileColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                        titleColor = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}


/**
 * White-card menu row: soft icon tile + title/subtitle + chevron.
 * [iconTint] / [tileColor] / [titleColor] let destructive rows (log out)
 * paint in the error color.
 */
@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = UniFind.Primary,
    tileColor: Color = TileSoftSage,
    titleColor: Color = UniFind.Text
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconTile(
            icon = icon,
            size = 46.dp,
            cornerRadius = 14.dp,
            containerColor = tileColor,
            iconTint = iconTint,
            iconSize = 22.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = titleColor
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = UniFind.TextSecondary
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = UniFind.TextTertiary,
            modifier = Modifier.size(22.dp)
        )
    }
}
