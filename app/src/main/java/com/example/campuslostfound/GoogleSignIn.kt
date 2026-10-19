package com.example.campuslostfound

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.MessageDigest
import java.util.UUID

/** What the account picker came back with. */
sealed interface GoogleSignInResult {

    /** The user picked an account; [idToken] is ready to exchange with Supabase. */
    data class Success(
        val idToken: String,
        val rawNonce: String,
        val email: String?,
    ) : GoogleSignInResult

    /** The user closed the picker without choosing an account — not an error. */
    data object Cancelled : GoogleSignInResult

    /** The device has no Google account to choose from. */
    data object NoAccount : GoogleSignInResult

    /** Anything else (misconfigured project, Play services missing, …). */
    data class Failure(val message: String) : GoogleSignInResult
}

private const val TAG = "GoogleSignIn"

/** Shown while `google.webClientId` is still the placeholder — see SUPABASE_SETUP.md § 5. */
const val GOOGLE_SIGN_IN_NOT_SET_UP_MESSAGE =
    "Google sign-in isn't set up yet — see the “Google sign-in” section of " +
        "SUPABASE_SETUP.md, then try again."

/**
 * True once `google.webClientId` holds a real Web client ID. Until then
 * "Continue with Google" says so right under the button instead of opening an
 * account picker Google would reject.
 */
fun isGoogleSignInConfigured(): Boolean {
    val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
    return webClientId.isNotBlank() && !webClientId.startsWith("YOUR-")
}

/**
 * Opens the system Google account picker (Credential Manager's bottom sheet)
 * and returns the ID token for the account the user tapped.
 *
 * [context] must be the current Activity so the sheet attaches to it. The
 * token is *not* trusted by itself: Supabase verifies it (including the nonce)
 * when AuthViewModel exchanges it for a session, and the `before-user-created`
 * hook re-checks the address server-side.
 */
suspend fun requestGoogleIdToken(context: Context): GoogleSignInResult {
    val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
    if (!isGoogleSignInConfigured()) {
        return GoogleSignInResult.Failure(GOOGLE_SIGN_IN_NOT_SET_UP_MESSAGE)
    }

    // Nonce: Supabase Auth checks that the ID token carries the SHA-256 of the
    // value sent alongside it, which stops a captured token from being
    // replayed. Google receives the hash, Supabase receives the original.
    val rawNonce = UUID.randomUUID().toString()
    val hashedNonce = MessageDigest.getInstance("SHA-256")
        .digest(rawNonce.toByteArray())
        .joinToString("") { "%02x".format(it.toInt() and 0xFF) }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(webClientId)
        // Show every Google account on the device (authorized-only would hide
        // first-time users). The .edu.ph rule is applied to whatever is picked
        // — here for a friendly message, and by Supabase for real.
        .setFilterByAuthorizedAccounts(false)
        .setAutoSelectEnabled(false)
        .setNonce(hashedNonce)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    val credentialManager = CredentialManager.create(context)
    return try {
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            GoogleSignInResult.Success(
                idToken = googleCredential.idToken,
                rawNonce = rawNonce,
                email = googleCredential.id.takeIf { it.isNotBlank() },
            )
        } else {
            GoogleSignInResult.Failure("Unexpected sign-in response from Google. Please try again.")
        }
    } catch (e: GetCredentialCancellationException) {
        GoogleSignInResult.Cancelled
    } catch (e: NoCredentialException) {
        GoogleSignInResult.NoAccount
    } catch (e: GetCredentialException) {
        Log.e(TAG, "Credential Manager failed", e)
        GoogleSignInResult.Failure(
            "Couldn't open Google sign-in. Check that Google Play services is " +
                "up to date, then try again."
        )
    } catch (e: GoogleIdTokenParsingException) {
        Log.e(TAG, "Google returned an unreadable ID token", e)
        GoogleSignInResult.Failure("Google returned an invalid sign-in response. Please try again.")
    }
}
