package com.example.campuslostfound

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.campuslostfound.ui.components.*
import com.example.campuslostfound.ui.theme.*

// ---------------------------------------------------------------------------
// SIGN UP SCREEN
// Same visual system as Login: dark green header (logo, "Create your account",
// subtitle) with a white rounded card overlapping it — full name, email,
// password, confirm password, Create Account, link back to Sign In.
// ---------------------------------------------------------------------------
@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    onCreateAccountClick: () -> Unit = {},
    onBackToLoginClick: () -> Unit = {},
) {

    var fullName by remember {
        mutableStateOf(value = "")
    }

    var email by remember {
        mutableStateOf(value = "")
    }

    var studentId by remember {
        mutableStateOf(value = "")
    }

    var password by remember {
        mutableStateOf(value = "")
    }

    var confirmPassword by remember {
        mutableStateOf(value = "")
    }

    var passwordVisible by remember {
        mutableStateOf(value = false)
    }

    var confirmPasswordVisible by remember {
        mutableStateOf(value = false)
    }

    var localErrorMessage by remember {
        mutableStateOf<String?>(value = null)
    }

    val isLoading = viewModel.isLoading.value
    val landscape = rememberIsLandscape()
    val firebaseError = viewModel.errorMessage.value

    LaunchedEffect(Unit) {
        viewModel.clearError()
    }


    Surface(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        color = UniFind.Background
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                // Bottom inset on the viewport so the green footer never ends
                // up underneath the gesture/navigation bar at full scroll.
                .navigationBarsPadding()
                .verticalScroll(
                    rememberScrollState()
                ),

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
                    text = "Create your account",
                    style = MaterialTheme.typography.headlineMedium,
                    color = UniFind.OnPrimary
                )

                Spacer(modifier = Modifier.height(if (landscape) 4.dp else 6.dp))

                Text(
                    text = "Join us to report and recover lost items.",
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
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            localErrorMessage = null
                        },
                        label = "FULL NAME",
                        placeholder = "Enter your full name",
                        leadingIcon = Icons.Outlined.Person,
                        leadingIconDescription = "Full Name"
                    )


                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )


                    UniFindTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            localErrorMessage = null
                        },
                        label = "SCHOOL EMAIL",
                        placeholder = "name@school.edu.ph",
                        leadingIcon = Icons.Outlined.Email,
                        leadingIconDescription = "Email",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        )
                    )


                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )


                    UniFindTextField(
                        value = studentId,
                        onValueChange = {
                            studentId = it
                            localErrorMessage = null
                        },
                        label = "STUDENT ID",
                        placeholder = "Enter your student ID",
                        leadingIcon = Icons.Outlined.Person,
                        leadingIconDescription = "Student ID"
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


                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )


                    UniFindTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            localErrorMessage = null
                        },
                        label = "CONFIRM PASSWORD",
                        placeholder = "Confirm your password",
                        leadingIcon = Icons.Outlined.Lock,
                        leadingIconDescription = "Confirm Password",
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    confirmPasswordVisible = !confirmPasswordVisible
                                }
                            ) {
                                Icon(

                                    imageVector =
                                        if (confirmPasswordVisible) {
                                            Icons.Outlined.VisibilityOff
                                        } else {
                                            Icons.Outlined.Visibility
                                        },

                                    contentDescription =
                                        if (confirmPasswordVisible) {
                                            "Hide password"
                                        } else {
                                            "Show password"
                                        },

                                    tint = UniFind.TextSecondary
                                )
                            }
                        },
                        visualTransformation =
                            if (confirmPasswordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            }
                    )


                    val displayError = localErrorMessage ?: firebaseError

                    displayError?.let {

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

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
                        modifier = Modifier.height(20.dp)
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
                            text = "Create Account",
                            onClick = {

                                localErrorMessage = when {
                                    fullName.isBlank() || email.isBlank() ||
                                        studentId.isBlank() || password.isBlank() ->
                                        "Please fill in all fields."

                                    // Email format validation
                                    !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                                        "Please enter a valid email address."

                                    // Accounts are reserved for school addresses.
                                    !SchoolEmail.isAllowed(email) ->
                                        SchoolEmail.REQUIREMENT_MESSAGE

                                    password != confirmPassword ->
                                        "Passwords do not match."

                                    password.length < 6 ->
                                        "Password must be at least 6 characters."

                                    else -> null
                                }

                                if (localErrorMessage == null) {
                                    viewModel.signUp(
                                        email = email.trim(),
                                        password = password,
                                        fullName = fullName.trim(),
                                        studentId = studentId.trim(),
                                        onSuccess = onCreateAccountClick
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }


                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )


                    // Already have an account? Sign In
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Already have an account?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = UniFind.TextSecondary
                        )
                        TextButton(
                            onClick = onBackToLoginClick,
                            contentPadding = PaddingValues(
                                horizontal = 8.dp,
                                vertical = 0.dp
                            )
                        ) {
                            Text(
                                text = "Sign In",
                                style = MaterialTheme.typography.labelLarge,
                                color = UniFind.Primary
                            )
                        }
                    }
                }
            }

            // Footer as last item in scroll
            GreenFooter(
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
