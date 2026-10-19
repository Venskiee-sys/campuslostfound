package com.example.campuslostfound

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.campuslostfound.ui.components.*
import com.example.campuslostfound.ui.theme.*

// ---------------------------------------------------------------------------
// LOGIN SCREEN
// Dark green header (logo, "Welcome back", subtitle) with a white rounded
// card overlapping it: email + password fields, remember me / forgot password,
// Sign In, "or" divider, Continue with Google, Sign Up link.
// ---------------------------------------------------------------------------
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginClick: () -> Unit = {},
    onCreateAccountClick: () -> Unit = {},
    onGoogleSignInClick: (() -> Unit)? = null
) {


    var studentId by remember {
        mutableStateOf(value = "")
    }

    var password by remember {
        mutableStateOf(value = "")
    }

    var passwordVisible by remember {
        mutableStateOf(value = false)
    }

    var rememberMe by remember {
        mutableStateOf(value = false)
    }

    var localErrorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var infoMessage by remember {
        mutableStateOf<String?>(null)
    }

    val isLoading = viewModel.isLoading.value
    val firebaseError = viewModel.errorMessage.value
    val landscape = rememberIsLandscape()

    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }
    var forgotMessage by remember { mutableStateOf<String?>(null) }
    var isForgotLoading by remember { mutableStateOf(false) }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            containerColor = UniFind.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    "Reset Password",
                    style = MaterialTheme.typography.titleMedium,
                    color = UniFind.Text
                )
            },
            text = {
                Column {
                    Text(
                        "Enter your email address and we'll send you a link to reset your password.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = UniFind.TextSecondary
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                "Email Address",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = "Email"
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = loginFieldColors()
                    )
                    forgotMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (it.contains("sent", true)) {
                                UniFind.Primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isForgotLoading = true
                        viewModel.resetPassword(forgotEmail) { success, msg ->
                            isForgotLoading = false
                            forgotMessage = msg
                        }
                    },
                    enabled = !isForgotLoading && forgotEmail.isNotBlank()
                ) {
                    if (isForgotLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = UniFind.Primary
                        )
                    } else {
                        Text(
                            "SEND RESET LINK",
                            style = MaterialTheme.typography.labelLarge,
                            color = UniFind.Primary
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showForgotDialog = false
                    forgotMessage = null
                    forgotEmail = ""
                }) {
                    Text(
                        "CANCEL",
                        style = MaterialTheme.typography.labelLarge,
                        color = UniFind.TextSecondary
                    )
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.clearError()
        // Load saved credentials
        viewModel.getSavedCredentials()?.let { (email, pass) ->
            studentId = email
            password = pass
            rememberMe = true
        }
    }



    Surface(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // Pushes everything up
        color = UniFind.Background
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                // Bottom inset on the viewport (not the Surface) so the green
                // footer scrolls clear of the gesture bar without shrinking
                // the surface background behind it.
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ---- Dark green header ----------------------------------------
            GreenHeader(modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)) {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    // UniFind logo in a white rounded square
                    Box(
                        modifier = Modifier
                            .size(if (landscape) 48.dp else 64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(UniFind.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.plmunlogo),
                            contentDescription = "UniFind logo",
                            modifier = Modifier.size(if (landscape) 34.dp else 46.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "UniFind",
                            style = MaterialTheme.typography.headlineSmall,
                            color = UniFind.OnPrimary
                        )
                        Text(
                            text = "Campus Lost & Found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = UniFind.OnPrimary.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (landscape) 12.dp else 28.dp))

                Text(
                    text = "Welcome back",
                    style = MaterialTheme.typography.headlineMedium,
                    color = UniFind.OnPrimary
                )

                Spacer(modifier = Modifier.height(if (landscape) 4.dp else 6.dp))

                Text(
                    text = "Find what you've lost. Help return what you've found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = UniFind.OnPrimary.copy(alpha = 0.85f)
                )

                // Room for the white card to overlap the header
                Spacer(modifier = Modifier.height(if (landscape) 20.dp else 36.dp))
            }

            // ---- White card overlapping the header ------------------------
            UniFindCard(
                modifier = Modifier
                    .padding(horizontal = if (landscape) 48.dp else 16.dp)
                    .offset(y = if (landscape) (-16).dp else (-40).dp)
                    .widthIn(max = 560.dp),
                cornerRadius = 28.dp
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 22.dp,
                        end = 22.dp,
                        top = 26.dp,
                        bottom = 24.dp
                    )
                ) {

                    UniFindTextField(
                        value = studentId,
                        onValueChange = {
                            studentId = it
                            localErrorMessage = null
                        },
                        label = "SCHOOL EMAIL",
                        placeholder = "name@school.edu.ph",
                        leadingIcon = Icons.Outlined.Email,
                        leadingIconDescription = "Email"
                    )


                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )


                    UniFindTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            localErrorMessage = null
                        },
                        label = "PASSWORD",
                        placeholder = "Enter your password",
                        leadingIcon = Icons.Outlined.Lock,
                        leadingIconDescription = "Password",
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    passwordVisible = !passwordVisible
                                }
                            ) {
                                Icon(
                                    imageVector =
                                        if (passwordVisible) {
                                            Icons.Outlined.VisibilityOff
                                        } else {
                                            Icons.Outlined.Visibility
                                        },
                                    contentDescription =
                                        if (passwordVisible) {
                                            "Hide password"
                                        } else {
                                            "Show password"
                                        },
                                    tint = UniFind.TextSecondary
                                )
                            }
                        },
                        visualTransformation =
                            if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            }
                    )


                    Spacer(modifier = Modifier.height(4.dp))


                    // The two actions share one row on wider screens and stack on
                    // narrow ones so "Forgot password?" never wraps or clips.
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val rememberRow = @Composable {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = rememberMe,
                                    onCheckedChange = { isChecked ->
                                        rememberMe = isChecked
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = UniFind.Primary
                                    )
                                )
                                Text(
                                    text = "Remember me",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = UniFind.Text,
                                    maxLines = 1
                                )
                            }
                        }
                        val forgotButton = @Composable {
                            TextButton(onClick = { showForgotDialog = true }) {
                                Text(
                                    text = "Forgot password?",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = UniFind.Primary,
                                    maxLines = 1
                                )
                            }
                        }

                        if (maxWidth >= 300.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                rememberRow()
                                forgotButton()
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                rememberRow()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    forgotButton()
                                }
                            }
                        }
                    }


                    val displayError = localErrorMessage ?: firebaseError

                    displayError?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(18.dp)
                    )


                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                color = UniFind.Primary,
                                strokeWidth = 2.5.dp
                            )
                        }
                    } else {
                        PrimaryButton(
                            text = "Sign In",
                            onClick = {
                                localErrorMessage = when {
                                    studentId.isBlank() || password.isBlank() ->
                                        "Please fill in all fields."
                                    !studentId.contains("@") ->
                                        "Please enter a valid email address."
                                    !SchoolEmail.isAllowed(studentId) ->
                                        SchoolEmail.REQUIREMENT_MESSAGE
                                    else -> null
                                }

                                if (localErrorMessage == null) {
                                    // Only persist the credentials once the sign-in
                                    // actually succeeds — otherwise a failed attempt
                                    // leaves a bad password saved for next launch.
                                    viewModel.signIn(
                                        email = studentId.trim(),
                                        password = password,
                                        onSuccess = {
                                            viewModel.saveCredentials(
                                                studentId.trim(),
                                                password,
                                                rememberMe
                                            )
                                            onLoginClick()
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }


                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )


                    // "or" divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(UniFind.Divider)
                        )

                        Text(
                            text = "or",
                            style = MaterialTheme.typography.labelMedium,
                            color = UniFind.TextTertiary,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(UniFind.Divider)
                        )
                    }


                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )


                    SecondaryButton(
                        text = "Continue with Google",
                        onClick = {
                            val googleAction = onGoogleSignInClick
                            when {
                                // No real Web client ID in gradle.properties yet: say so
                                // right under this button — the old message appeared above
                                // "Sign In", so the tap looked like it did nothing.
                                !isGoogleSignInConfigured() -> {
                                    viewModel.clearError()
                                    infoMessage = GOOGLE_SIGN_IN_NOT_SET_UP_MESSAGE
                                }

                                googleAction != null -> {
                                    infoMessage = null
                                    googleAction()
                                }

                                else -> {
                                    infoMessage =
                                        "Google sign-in isn't available yet. Sign in with your email instead."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Outlined.Email
                    )

                    infoMessage?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium,
                            color = UniFind.TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }


                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )


                    // Don't have an account? Sign Up
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Don't have an account?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = UniFind.TextSecondary
                        )
                        TextButton(
                            onClick = onCreateAccountClick,
                            contentPadding = PaddingValues(
                                horizontal = 8.dp,
                                vertical = 0.dp
                            )
                        ) {
                            Text(
                                text = "Sign Up",
                                style = MaterialTheme.typography.labelLarge,
                                color = UniFind.Primary
                            )
                        }
                    }
                }
            }

            // Footer as last item in scroll
            GreenFooter(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


/** UniFind input-field colors: soft fill, outline, green focus border. */
@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = UniFind.Text,
    unfocusedTextColor = UniFind.Text,
    focusedContainerColor = UniFind.White,
    unfocusedContainerColor = UniFind.SurfaceAlt,
    focusedBorderColor = UniFind.Primary,
    unfocusedBorderColor = UniFind.Outline,
    errorBorderColor = MaterialTheme.colorScheme.error,
    cursorColor = UniFind.Primary,
    focusedLabelColor = UniFind.Primary,
    unfocusedLabelColor = UniFind.TextSecondary,
    focusedLeadingIconColor = UniFind.Primary,
    unfocusedLeadingIconColor = UniFind.TextSecondary,
    focusedTrailingIconColor = UniFind.TextSecondary,
    unfocusedTrailingIconColor = UniFind.TextSecondary
)


@Composable
fun GreenFooter(
    modifier: Modifier = Modifier,
) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            // Grows past 115dp at large font scales instead of clipping the footer text.
            .heightIn(min = 115.dp)
    ) {


        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {

            val path = Path().apply {

                moveTo(
                    0f,
                    size.height * 0.35f
                )

                cubicTo(

                    size.width * 0.25f,
                    size.height * 0.05f,

                    size.width * 0.55f,
                    size.height * 0.70f,

                    size.width,
                    size.height * 0.15f
                )

                lineTo(
                    size.width,
                    size.height
                )

                lineTo(
                    0f,
                    size.height
                )

                close()
            }


            drawPath(
                path = path,

                color = UniFind.Primary
            )
        }


        Column(

            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = 24.dp,
                    bottom = 15.dp
                )
        ) {

            Text(
                text = "Pamantasan ng Lungsod ng Muntinlupa",

                style = MaterialTheme.typography.labelMedium,

                color = UniFind.OnPrimary
            )


            Text(
                text = "Campus Lost & Found System",

                style = MaterialTheme.typography.bodySmall,

                color = UniFind.OnPrimary.copy(alpha = 0.9f)
            )
        }
    }
}
