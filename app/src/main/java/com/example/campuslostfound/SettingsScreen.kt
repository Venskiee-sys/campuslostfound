package com.example.campuslostfound

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.campuslostfound.ui.components.*
import com.example.campuslostfound.ui.theme.*

// ---------------------------------------------------------------------------
// SETTINGS SCREEN
// White ScreenHeader over grouped white cards with section labels: appearance
// toggle, preferences, help, and destructive account actions.
// ---------------------------------------------------------------------------
@Composable
fun SettingsScreen(
    viewModel: AuthViewModel,
    onThemeChange: (Boolean) -> Unit = {},
    onBackClick: () -> Unit = {},
    onLogoutClick: (() -> Unit)? = null,
    onDeleteAccountClick: (() -> Unit)? = null
) {
    var darkMode by remember { mutableStateOf(viewModel.isDarkMode()) }
    var selectedLanguage by remember { mutableStateOf(viewModel.getLanguage()) }

    // Dialog States
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var changePasswordMessage by remember { mutableStateOf<String?>(null) }
    var isChangeLoading by remember { mutableStateOf(false) }

    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangePasswordDialog = false
                changePasswordMessage = null
                newPassword = ""
                confirmPassword = ""
                newPasswordVisible = false
                confirmPasswordVisible = false
            },
            containerColor = UniFind.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Change Password", style = MaterialTheme.typography.titleMedium, color = UniFind.Text)
            },
            text = {
                // Scrollable + IME-aware: on short screens or with the keyboard up
                // the second field and status message would otherwise be clipped.
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                ) {
                    Text(
                        "Enter your new password below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniFind.TextSecondary
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("New Password") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(
                                    imageVector = if (newPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (newPasswordVisible) "Hide password" else "Show password",
                                    tint = UniFind.TextSecondary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = UniFind.Primary,
                            unfocusedBorderColor = UniFind.Outline,
                            cursorColor = UniFind.Primary
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Confirm New Password") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                    tint = UniFind.TextSecondary
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = UniFind.Primary,
                            unfocusedBorderColor = UniFind.Outline,
                            cursorColor = UniFind.Primary
                        )
                    )
                    changePasswordMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            it,
                            color = if (it.contains("success", ignoreCase = true)) {
                                UniFind.Primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPassword != confirmPassword) {
                            changePasswordMessage = "Passwords do not match."
                            return@TextButton
                        }
                        if (newPassword.length < 6) {
                            changePasswordMessage = "Password must be at least 6 characters."
                            return@TextButton
                        }

                        isChangeLoading = true
                        viewModel.changePassword(newPassword) { success, msg ->
                            isChangeLoading = false
                            changePasswordMessage = msg
                            if (success) {
                                // Close the dialog automatically on success
                                showChangePasswordDialog = false
                                newPassword = ""
                                confirmPassword = ""
                                newPasswordVisible = false
                                confirmPasswordVisible = false
                                changePasswordMessage = null
                            }
                        }
                    },
                    enabled = !isChangeLoading && newPassword.isNotBlank() && confirmPassword.isNotBlank()
                ) {
                    if (isChangeLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = UniFind.Primary
                        )
                    } else {
                        Text("UPDATE", fontWeight = FontWeight.Bold, color = UniFind.Primary)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showChangePasswordDialog = false
                        changePasswordMessage = null
                        newPassword = ""
                        confirmPassword = ""
                    }
                ) {
                    Text("CANCEL", color = UniFind.TextSecondary)
                }
            }
        )
    }

    val languages = listOf(
        "English (US)",
        "Tagalog",
        "Spanish (Español)",
        "Chinese (中文)",
        "Japanese (日本語)",
        "Korean (한국어)",
        "French (Français)",
        "German (Deutsch)"
    )

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = UniFind.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Select Language", style = MaterialTheme.typography.titleMedium, color = UniFind.Text)
            },
            text = {
                // 8 rows exceed the dialog height on short portrait screens.
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    languages.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLanguage = lang
                                    viewModel.setLanguage(lang)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (lang == selectedLanguage),
                                onClick = {
                                    selectedLanguage = lang
                                    viewModel.setLanguage(lang)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                lang,
                                style = MaterialTheme.typography.bodyMedium,
                                color = UniFind.Text
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("CANCEL", color = UniFind.TextSecondary)
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor = UniFind.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Privacy Policy", style = MaterialTheme.typography.titleMedium, color = UniFind.Text)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Your privacy is important to us. PLMUN Lost & Found collects your name, student ID, and email to facilitate item recovery.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniFind.TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Data is stored securely in Google Firebase and is only used within the app for communication between students.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniFind.TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("OK", color = UniFind.Primary)
                }
            }
        )
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            containerColor = UniFind.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Terms of Service", style = MaterialTheme.typography.titleMedium, color = UniFind.Text)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "1. Be honest: Do not report fake items.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniFind.TextSecondary
                    )
                    Text(
                        "2. Respect others: Use the chat only for item recovery.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniFind.TextSecondary
                    )
                    Text(
                        "3. Safety: Meet in public places or the Security Office.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniFind.TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text("AGREE", color = UniFind.Primary)
                }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = UniFind.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Help & Support", style = MaterialTheme.typography.titleMedium, color = UniFind.Text)
            },
            text = {
                Column {
                    Text(
                        "Need help? Contact the PLMUN App Support Team.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniFind.TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Email: support.lostfound@plmun.edu.ph",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniFind.Text
                    )
                    Text(
                        "Location: IT Department, 3rd Floor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniFind.TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("CLOSE", color = UniFind.Primary)
                }
            }
        )
    }

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
                    "Are you sure you want to log out of this device?",
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

    if (showDeleteAccountDialog) {
        val canDelete = onDeleteAccountClick != null
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            containerColor = UniFind.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("Delete account", style = MaterialTheme.typography.titleMedium, color = UniFind.Text)
            },
            text = {
                Text(
                    if (canDelete) {
                        "This permanently removes your account and everything you have posted. This action cannot be undone."
                    } else {
                        "Account deletion is handled by the PLMUN support team. Send a request to support.lostfound@plmun.edu.ph and we will process it within 3 working days."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = UniFind.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        if (canDelete) onDeleteAccountClick?.invoke()
                    }
                ) {
                    Text(
                        if (canDelete) "Delete" else "OK",
                        color = if (canDelete) MaterialTheme.colorScheme.error else UniFind.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel", color = UniFind.TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UniFind.Background)
    ) {

        // Header
        ScreenHeader(
            title = "Settings",
            onBack = onBackClick,
            subtitle = "App preferences"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Appearance ---------------------------------------------------
            SectionHeader(title = "Appearance")
            Spacer(modifier = Modifier.height(10.dp))

            UniFindCard(cornerRadius = 24.dp) {
                SettingsToggleRow(
                    icon = Icons.Outlined.Tune,
                    title = "Dark Mode",
                    subtitle = "Switch between light and dark theme",
                    checked = darkMode,
                    onCheckedChange = {
                        darkMode = it
                        viewModel.setDarkMode(it)
                        onThemeChange(it)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Preferences --------------------------------------------------
            SectionHeader(title = "Preferences")
            Spacer(modifier = Modifier.height(10.dp))

            UniFindCard(cornerRadius = 24.dp) {
                Column {
                    SettingsNavRow(
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        title = "Language",
                        subtitle = selectedLanguage,
                        onClick = { showLanguageDialog = true }
                    )
                    HorizontalDivider(color = UniFind.Divider, thickness = 1.dp)

                    SettingsNavRow(
                        icon = Icons.Outlined.Lock,
                        title = "Change Password",
                        subtitle = "Update your account security",
                        onClick = { showChangePasswordDialog = true }
                    )
                    HorizontalDivider(color = UniFind.Divider, thickness = 1.dp)

                    SettingsNavRow(
                        icon = Icons.Outlined.Shield,
                        title = "Privacy Policy",
                        subtitle = "How we handle your data",
                        onClick = { showPrivacyDialog = true }
                    )
                    HorizontalDivider(color = UniFind.Divider, thickness = 1.dp)

                    SettingsNavRow(
                        icon = Icons.Outlined.Description,
                        title = "Terms of Service",
                        subtitle = "Rules for using this app",
                        onClick = { showTermsDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Help ---------------------------------------------------------
            SectionHeader(title = "Help")
            Spacer(modifier = Modifier.height(10.dp))

            UniFindCard(cornerRadius = 24.dp) {
                SettingsNavRow(
                    icon = Icons.AutoMirrored.Outlined.Help,
                    title = "Help & Support",
                    subtitle = "Get help or send feedback",
                    onClick = { showHelpDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Account (destructive actions) --------------------------------
            SectionHeader(title = "Account")
            Spacer(modifier = Modifier.height(10.dp))

            UniFindCard(cornerRadius = 24.dp) {
                Column {
                    SettingsNavRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "Log out",
                        subtitle = "Sign out of this device",
                        onClick = { showLogoutDialog = true },
                        iconTint = MaterialTheme.colorScheme.error,
                        tileColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                        titleColor = MaterialTheme.colorScheme.error
                    )
                    HorizontalDivider(color = UniFind.Divider, thickness = 1.dp)

                    SettingsNavRow(
                        icon = Icons.Outlined.Delete,
                        title = "Delete account",
                        subtitle = "Permanently remove your account",
                        onClick = { showDeleteAccountDialog = true },
                        iconTint = MaterialTheme.colorScheme.error,
                        tileColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                        titleColor = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/** Soft icon tile used by the settings rows. */
@Composable
private fun SettingsIconBox(
    icon: ImageVector,
    containerColor: Color = TileSoftSage,
    iconTint: Color = UniFind.Primary
) {
    IconTile(
        icon = icon,
        size = 44.dp,
        cornerRadius = 14.dp,
        containerColor = containerColor,
        iconTint = iconTint,
        iconSize = 21.dp
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconBox(icon)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = UniFind.Text
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = UniFind.TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = UniFind.OnPrimary,
                checkedTrackColor = UniFind.Primary,
                checkedBorderColor = UniFind.Primary,
                uncheckedThumbColor = UniFind.White,
                uncheckedTrackColor = UniFind.SurfaceAlt,
                uncheckedBorderColor = UniFind.Outline
            )
        )
    }
}

@Composable
private fun SettingsNavRow(
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
        SettingsIconBox(icon, containerColor = tileColor, iconTint = iconTint)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = titleColor
            )
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
