package com.example.campuslostfound

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuslostfound.model.User
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Auth + profile handling on Supabase (was FirebaseAuth + Firestore "users"
 * collection). The public API is identical to the old Firebase version so none
 * of the screens needed to change.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val supabase get() = SupabaseProvider.client

    private val TAG = "AuthViewModel"

    private val prefs = application.getSharedPreferences("clf_prefs", Context.MODE_PRIVATE)

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _isUserAuthenticated = MutableStateFlow(
        SupabaseProvider.client.auth.currentSessionOrNull() != null
    )
    val isUserAuthenticated: StateFlow<Boolean> = _isUserAuthenticated

    private val _userData = mutableStateOf<User?>(null)
    val userData: State<User?> = _userData

    init {
        // Restore the user's language pick before anything formats a date.
        applyLanguagePreference()

        viewModelScope.launch {
            // The Auth plugin restores the persisted session asynchronously, so a
            // one-shot read at construction time would always report "logged out"
            // and return users to onboarding on every cold start.
            try {
                supabase.auth.awaitInitialization()
            } catch (e: Exception) {
                Log.e(TAG, "Auth initialization failed", e)
            }
            syncAuthState()

            // Keep the flag (and the cached profile) in sync with the session for
            // the rest of the process lifetime: sign-in, sign-out, token refresh.
            supabase.auth.sessionStatus.collect { status ->
                val authenticated = status is SessionStatus.Authenticated
                if (authenticated) {
                    _isUserAuthenticated.value = true
                    if (_userData.value == null) fetchUserData()
                } else if (status is SessionStatus.NotAuthenticated) {
                    // Signed out (or the session was cleared): drop the profile so
                    // the next account never sees the previous one's data.
                    _isUserAuthenticated.value = false
                    _userData.value = null
                }
            }
        }
    }

    private fun syncAuthState() {
        val authenticated = SupabaseProvider.client.auth.currentSessionOrNull() != null
        _isUserAuthenticated.value = authenticated
        if (authenticated && _userData.value == null) fetchUserData()
    }

    private fun fetchUserData() {
        val uid = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            try {
                val user = supabase.postgrest["users"].select {
                    filter {
                        eq("uid", uid)
                    }
                    limit(1)
                }.decodeSingleOrNull<User>()
                if (user != null) {
                    _userData.value = user
                } else if (_userData.value?.uid != uid) {
                    // No profile row (trigger/RLS) — fall back to the auth record so
                    // screens keyed on userData (chats, my reports) still work.
                    val sessionUser = SupabaseProvider.client.auth.currentUserOrNull()
                    val email = sessionUser?.email ?: ""
                    _userData.value = User(
                        uid = uid,
                        fullName = email.substringBefore('@'),
                        email = email
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load profile for $uid", e)
            }
        }
    }

    fun signUp(email: String, password: String, fullName: String, studentId: String, onSuccess: () -> Unit) {
        // Backstop for the form's own check: an account may only be created
        // with a school (.edu.ph) address.
        if (!SchoolEmail.isAllowed(email)) {
            _errorMessage.value = SchoolEmail.REQUIREMENT_MESSAGE
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                // If "Confirm email" is enabled in Supabase there is no session yet —
                // try to sign in right away; if that fails, ask the user to confirm.
                if (SupabaseProvider.client.auth.currentSessionOrNull() == null) {
                    try {
                        supabase.auth.signInWith(Email) {
                            this.email = email
                            this.password = password
                        }
                    } catch (e: Exception) {
                        throw AuthException("Check your inbox to confirm your email, then sign in.")
                    }
                }

                val uid = SupabaseProvider.client.auth.currentUserOrNull()?.id
                    ?: throw AuthException("Sign up failed. Please try again.")

                val user = User(uid, fullName, studentId, email)
                // Upsert so the row created by the on_auth_user_created trigger (if any)
                // gets the full profile.
                supabase.postgrest["users"].upsert(user)

                _userData.value = user
                _isUserAuthenticated.value = true
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = readableAuthError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        // The app only issues accounts to school (.edu.ph) addresses, so there
        // is nothing to sign in to with anything else.
        if (!SchoolEmail.isAllowed(email)) {
            _errorMessage.value = SchoolEmail.REQUIREMENT_MESSAGE
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                _isUserAuthenticated.value = true
                fetchUserData()
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = readableAuthError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Completes "Continue with Google": the ID token from the Credential
     * Manager account picker is exchanged for Supabase session tokens, and the
     * account's address is checked against the school (.edu.ph) policy — first
     * from the token so a personal account never becomes a junk Supabase user,
     * then against what Supabase itself accepted so a tampered client can't
     * skip it.
     */
    fun signInWithGoogle(
        idToken: String,
        rawNonce: String,
        pickedEmail: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                if (pickedEmail != null && !SchoolEmail.isAllowed(pickedEmail)) {
                    throw AuthException(SchoolEmail.REQUIREMENT_MESSAGE)
                }

                supabase.auth.signInWith(IDToken) {
                    this.idToken = idToken
                    provider = Google
                    nonce = rawNonce
                }

                val signedInEmail = supabase.auth.currentUserOrNull()?.email.orEmpty()
                if (!SchoolEmail.isAllowed(signedInEmail)) {
                    // Never leave a non-school session behind.
                    runCatching { supabase.auth.signOut() }
                    throw AuthException(SchoolEmail.REQUIREMENT_MESSAGE)
                }

                _isUserAuthenticated.value = true
                fetchUserData()
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = readableAuthError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        _isUserAuthenticated.value = false
        // Drop the cached profile immediately so no screen can render (or query
        // with) the previous account's identity while the sign-out is in flight.
        _userData.value = null
        _errorMessage.value = null
        viewModelScope.launch {
            // Tell Credential Manager the Google session ended, so the next
            // sign-in offers every account instead of quietly reusing this one.
            runCatching {
                CredentialManager.create(getApplication<Application>())
                    .clearCredentialState(ClearCredentialStateRequest())
            }.onFailure { Log.w(TAG, "clearCredentialState failed", it) }

            runCatching { supabase.auth.signOut() }
                .onFailure { Log.e(TAG, "signOut failed", it) }
        }
    }

    fun getSavedCredentials(): Pair<String, String>? {
        val email = prefs.getString("saved_email", null)
        val password = prefs.getString("saved_password", null)
        return if (email != null && password != null) email to password else null
    }

    fun saveCredentials(email: String, password: String, remember: Boolean) {
        if (remember) {
            prefs.edit {
                putString("saved_email", email)
                putString("saved_password", password)
            }
        } else {
            prefs.edit {
                remove("saved_email")
                remove("saved_password")
            }
        }
    }

    fun resetPassword(email: String, onComplete: (Boolean, String?) -> Unit) {
        if (email.isBlank()) {
            onComplete(false, "Please enter your email address.")
            return
        }

        viewModelScope.launch {
            try {
                supabase.auth.resetPasswordForEmail(email)
                onComplete(true, "Reset link sent to your email.")
            } catch (e: Exception) {
                onComplete(false, readableAuthError(e))
            }
        }
    }

    fun isDarkMode(): Boolean {
        return prefs.getBoolean("dark_mode", false)
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit { putBoolean("dark_mode", enabled) }
    }

    fun getLanguage(): String {
        return prefs.getString("app_language", "English (US)") ?: "English (US)"
    }

    fun setLanguage(lang: String) {
        prefs.edit { putString("app_language", lang) }
        applyLanguagePreference()
    }

    /**
     * BCP-47 tag for every language offered in Settings → Language.
     */
    private fun languageTag(lang: String): String? = when (lang) {
        "English (US)" -> "en-US"
        "Tagalog" -> "fil-PH"
        "Spanish (Español)" -> "es-ES"
        "Chinese (中文)" -> "zh-CN"
        "Japanese (日本語)" -> "ja-JP"
        "Korean (한국어)" -> "ko-KR"
        "French (Français)" -> "fr-FR"
        "German (Deutsch)" -> "de-DE"
        else -> null
    }

    /**
     * Applies the stored language choice to this process's default locale.
     *
     * The pick used to be written to prefs and nothing else, so the setting
     * could never have any effect. UI copy is still English-only (the project
     * ships a single `values/` folder), but everything derived from the locale
     * — dates, numbers, casing, collation — now follows the selection, on this
     * change and on every cold start.
     */
    fun applyLanguagePreference() {
        val tag = languageTag(getLanguage()) ?: return
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
    }

    /** True until the user has left onboarding, so it shows exactly once. */
    fun hasSeenOnboarding(): Boolean = prefs.getBoolean("onboarding_done", false)

    fun setOnboardingSeen() {
        prefs.edit { putBoolean("onboarding_done", true) }
    }

    fun changePassword(newPassword: String, onComplete: (Boolean, String?) -> Unit) {
        if (SupabaseProvider.client.auth.currentSessionOrNull() == null) {
            onComplete(false, "User not logged in.")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                supabase.auth.updateUser {
                    password = newPassword
                }
                onComplete(true, "Password updated successfully.")
            } catch (e: Exception) {
                onComplete(false, readableAuthError(e))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /** Shows a message from outside this view model (the Google picker, MainActivity). */
    fun postError(message: String) {
        _errorMessage.value = message
    }

    fun updateProfile(
        email: String,
        fullName: String,
        studentId: String,
        course: String,
        yearLevel: String,
        phoneNumber: String,
        onSuccess: () -> Unit
    ) {
        val user = SupabaseProvider.client.auth.currentUserOrNull()
        if (user == null) {
            // Never return silently: the caller waits for onSuccess or errorMessage
            // to leave the loading state.
            _errorMessage.value = "Your session has expired. Please sign in again."
            return
        }
        val uid = user.id
        val newEmail = email.trim().ifBlank { user.email ?: "" }
        val currentEmail = user.email ?: ""
        // A changed address must stay a school (.edu.ph) address — same rule as
        // sign-up, checked again because the login credential is pushed here.
        if (!newEmail.equals(currentEmail, ignoreCase = true) && !SchoolEmail.isAllowed(newEmail)) {
            _errorMessage.value = SchoolEmail.REQUIREMENT_MESSAGE
            return
        }
        val updatedUser = User(uid, fullName, studentId, newEmail, course, yearLevel, phoneNumber)

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // The e-mail field is editable, so a changed address must also be
                // pushed to Supabase Auth — otherwise the profile row and the login
                // credential drift apart and the user can never sign in with it.
                if (!newEmail.equals(currentEmail, ignoreCase = true)) {
                    supabase.auth.updateUser { this.email = newEmail }
                }
                supabase.postgrest["users"].upsert(updatedUser)
                _userData.value = updatedUser
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

/** Used to surface a friendly, already-formatted message through the error channel. */
private class AuthException(message: String) : Exception(message)

/** Maps raw Supabase Auth errors to the friendly copy the screens expect. */
private fun readableAuthError(e: Exception): String {
    // Messages raised by this app are already written for the user — never
    // rewrite them (the "confirm email" branch below would otherwise swallow
    // the actionable instructions we just produced).
    if (e is AuthException) return e.message.orEmpty()
    val msg = e.message.orEmpty()
    return when {
        msg.contains("already registered", true) ||
            msg.contains("already been registered", true) ||
            msg.contains("user_already_exists", true) ->
            "An account with this email already exists."

        msg.contains("invalid login credentials", true) ->
            "Incorrect email or password."

        msg.contains("password should be at least", true) ||
            msg.contains("weak_password", true) ->
            "Password must be at least 6 characters."

        msg.contains("rate limit", true) ->
            "Too many attempts. Please wait a moment and try again."

        // The database has not been initialised yet (schema.sql never ran) —
        // PostgREST answers PGRST205 for every table. Say so plainly instead of
        // leaking "Could not find the table 'public.users' in the schema cache".
        msg.contains("PGRST205", true) ||
            msg.contains("Could not find the table", true) ||
            msg.contains("relation does not exist", true) ->
            "Setup incomplete: the database tables don't exist yet. In Supabase " +
                "open SQL Editor → New query, paste supabase/schema.sql and press " +
                "Run, then try again."

        // Offline / Supabase not configured: the raw message ("Unable to resolve
        // host \"your-project-ref.supabase.co\"") means nothing to a student.
        msg.contains("Unable to resolve host", true) ||
            msg.contains("Failed to connect", true) ||
            msg.contains("timeout", true) ||
            msg.contains("Connection reset", true) ||
            e is java.net.UnknownHostException ->
            "Can't reach the server. Check your internet connection — and if this " +
                "is a fresh checkout, fill in supabase.url / supabase.anonKey in " +
                "gradle.properties (see SUPABASE_SETUP.md)."

        // A sign-up the Supabase hook (supabase/restrict_edu_ph.sql) rejected.
        msg.contains(".edu.ph", true) ||
            msg.contains("school email", true) ->
            SchoolEmail.REQUIREMENT_MESSAGE

        // Google hands the nonce back hashed; a mismatch means the sign-in
        // attempt cannot be trusted, so it fails instead of continuing.
        msg.contains("nonce", true) ->
            "Google sign-in couldn't be verified. Please try again."

        msg.contains("confirm", true) && msg.contains("email", true) ->
            "Please confirm your email first."

        msg.isBlank() -> "Something went wrong. Please try again."
        else -> msg
    }
}
