package com.example.campuslostfound

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.campuslostfound.ui.components.*
import com.example.campuslostfound.ui.theme.*

// ---------------------------------------------------------------------------
// PERSONAL INFORMATION SCREEN
// White ScreenHeader over a single white form card: soft filled fields with
// leading line icons, then a full-width PrimaryButton.
// ---------------------------------------------------------------------------
@Composable
fun PersonalInformationScreen(
    viewModel: AuthViewModel,
    onBackClick: () -> Unit = {},
    onSaveClick: () -> Unit = {}
) {
    val user = viewModel.userData.value

    // Keyed on the account so the fields re-seed when the profile finishes
    // loading (userData is null on the first composition), but keep whatever
    // the user has typed while it is still the same account.
    var fullName by remember(user?.uid) { mutableStateOf(user?.fullName ?: "") }
    var email by remember(user?.uid) { mutableStateOf(user?.email ?: "") }
    var studentId by remember(user?.uid) { mutableStateOf(user?.studentId ?: "") }
    var course by remember(user?.uid) { mutableStateOf(user?.course ?: "") }
    var yearLevel by remember(user?.uid) { mutableStateOf(user?.yearLevel ?: "") }
    var phoneNumber by remember(user?.uid) { mutableStateOf(user?.phoneNumber ?: "") }

    val isLoading = viewModel.isLoading.value
    var localErrorMessage by remember { mutableStateOf<String?>(null) } // Added local error state
    val firebaseError = viewModel.errorMessage.value
    val errorMessage = localErrorMessage ?: firebaseError

    LaunchedEffect(Unit) {
        // Drop any stale error left behind by a previous screen.
        viewModel.clearError()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UniFind.Background)
    ) {

        // Header
        ScreenHeader(
            title = "Personal Information",
            onBack = onBackClick,
            subtitle = "View and edit your information"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                // Insets belong on the viewport, outside the scroll container:
                // padding the content instead would hide the gap behind the
                // keyboard instead of shrinking what the user can scroll to.
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Form card
            UniFindCard(cornerRadius = 24.dp) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LabeledField(
                        label = "Full Name",
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            localErrorMessage = null
                        },
                        icon = Icons.Outlined.Person
                    )

                    // Email is usually not editable in Firebase without re-auth,
                    // but it stays editable here with validation.
                    LabeledField(
                        label = "Edu.ph Mail",
                        value = email,
                        onValueChange = {
                            email = it
                            localErrorMessage = null
                        },
                        icon = Icons.Outlined.Email
                    )

                    LabeledField(
                        label = "Student ID",
                        value = studentId,
                        onValueChange = {
                            studentId = it
                            localErrorMessage = null
                        },
                        icon = Icons.Outlined.Badge
                    )
                    LabeledField(
                        label = "Course / Program",
                        value = course,
                        onValueChange = {
                            course = it
                            localErrorMessage = null
                        },
                        icon = Icons.AutoMirrored.Outlined.MenuBook
                    )
                    LabeledField(
                        label = "Year Level",
                        value = yearLevel,
                        onValueChange = {
                            yearLevel = it
                            localErrorMessage = null
                        },
                        icon = Icons.AutoMirrored.Outlined.List
                    )
                    LabeledField(
                        label = "Phone Number",
                        value = phoneNumber,
                        onValueChange = {
                            phoneNumber = it
                            localErrorMessage = null
                        },
                        icon = Icons.Outlined.Phone
                    )
                }
            }

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            PrimaryButton(
                text = "Save",
                onClick = {
                    // Only a *changed* address has to be a school (.edu.ph)
                    // one — an existing account with another address must
                    // still be able to save the rest of the profile.
                    val emailChanged = !email.trim().equals(user?.email ?: "", ignoreCase = true)
                    localErrorMessage = when {
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                            "Please enter a valid email address."
                        emailChanged && !SchoolEmail.isAllowed(email) ->
                            SchoolEmail.REQUIREMENT_MESSAGE
                        else -> null
                    }

                    if (localErrorMessage == null) {
                        viewModel.updateProfile(
                            email = email.trim(),
                            fullName = fullName.trim(),
                            studentId = studentId.trim(),
                            course = course.trim(),
                            yearLevel = yearLevel.trim(),
                            phoneNumber = phoneNumber.trim(),
                            onSuccess = onSaveClick
                        )
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = UniFind.Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (rememberIsLandscape()) 20.dp else 40.dp))
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = UniFind.TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = UniFind.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = UniFind.Text),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = UniFind.SurfaceAlt,
                unfocusedContainerColor = UniFind.SurfaceAlt,
                disabledContainerColor = UniFind.SurfaceAlt,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                cursorColor = UniFind.Primary,
                focusedLeadingIconColor = UniFind.Primary,
                unfocusedLeadingIconColor = UniFind.TextSecondary
            )
        )
    }
}
