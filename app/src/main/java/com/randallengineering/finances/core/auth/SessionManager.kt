package com.randallengineering.finances.core.auth

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.randallengineering.finances.core.network.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Cross-platform auth + sync coordinator. Signing in with the same Google account on
 * Android, web, or desktop yields the same Firebase `uid`, so all data is scoped and
 * shared under `users/{uid}/...` in Firestore. Email/password accounts are supported
 * too, and every first sign-in auto-provisions a `users/{uid}` profile record.
 */
class SessionManager(
    private val context: Context,
    private val firestore: FirebaseFirestore
) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _isSignedIn = MutableStateFlow(auth.currentUser != null)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    init {
        auth.addAuthStateListener {
            SyncScope.uid = it.currentUser?.uid
            _isSignedIn.value = it.currentUser != null
        }
    }

    val uid: String?
        get() = auth.currentUser?.uid

    val displayName: String?
        get() = auth.currentUser?.displayName

    val email: String?
        get() = auth.currentUser?.email

    /** Scoped Firestore collection path for the signed-in user, e.g. "users/abc/transactions". */
    fun scopedCollection(collection: String): String {
        val u = requireNotNull(uid) { "Not signed in" }
        return "users/$u/$collection"
    }

    val googleSignInClient: GoogleSignInClient by lazy {
        val webClientId = try {
            context.getString(context.resources.getIdentifier("default_web_client_id", "string", context.packageName))
        } catch (e: Exception) { "" }
        val opts = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .apply { if (webClientId.isNotBlank()) requestIdToken(webClientId) }
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, opts)
    }

    /** Exchange the Google sign-in result for a Firebase Auth session. */
    suspend fun handleSignInResult(task: Task<GoogleSignInAccount>): Resource<Unit> {
        return try {
            val account = task.getResult(ApiException::class.java) ?: return Resource.Error("Sign-in cancelled")
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                return Resource.Error("Google returned no ID token. Check that the Firebase console has the Google sign-in provider enabled and your SHA-1 is registered.")
            }
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            provisionUser()
            Resource.Success(Unit)
        } catch (e: ApiException) {
            Resource.Error(googleSignInErrorMessage(e))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Google sign-in failed")
        }
    }

    /** Create a new email/password account, then auto-provision the user record. */
    suspend fun createAccount(email: String, password: String, displayName: String): Resource<Unit> {
        if (!isValidEmail(email)) return Resource.Error("Please enter a valid email address.")
        if (password.length < 6) return Resource.Error("Password must be at least 6 characters.")
        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            result.user?.let { user ->
                try {
                    user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(displayName.trim().ifBlank { email.substringBefore('@') }).build()).await()
                } catch (e: Exception) { Log.w("SessionManager", "set displayName failed", e) }
            }
            provisionUser()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(friendlyAuthError(e))
        }
    }

    /** Log in with an existing email/password account; provisions if the record is missing. */
    suspend fun login(email: String, password: String): Resource<Unit> {
        if (!isValidEmail(email)) return Resource.Error("Please enter a valid email address.")
        return try {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            provisionUser()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(friendlyAuthError(e))
        }
    }

    suspend fun resetPassword(email: String): Resource<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Could not send reset email")
        }
    }

    suspend fun signOut() {
        auth.signOut()
        try { googleSignInClient.signOut().await() } catch (_: Exception) {}
    }

    /**
     * Ensure a `users/{uid}` profile record exists (create by default if it doesn't).
     * Called after every successful sign-in so new accounts are provisioned immediately.
     */
    private suspend fun provisionUser() {
        val user = auth.currentUser ?: return
        try {
            firestore.collection("users").document(user.uid).set(
                mapOf(
                    "uid" to user.uid,
                    "email" to (user.email ?: ""),
                    "displayName" to (user.displayName ?: user.email?.substringBefore('@') ?: ""),
                    "provider" to (user.providerData.firstOrNull()?.providerId ?: ""),
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            Log.w("SessionManager", "provisionUser failed", e)
        }
    }

    private fun isValidEmail(e: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(e.trim()).matches()

    private fun googleSignInErrorMessage(e: ApiException): String = when (e.statusCode) {
        10 -> "Developer console is not configured correctly. Rebuild with the SHA-1 registered in Firebase and enable the Google sign-in provider."
        12500, 12501 -> "Google Sign-In failed or was cancelled. Please try again."
        else -> e.localizedMessage ?: "Google sign-in failed (${e.statusCode})."
    }

    private fun friendlyAuthError(e: Exception): String {
        val m = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode ?: ""
        return when {
            m == "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists. Try logging in instead."
            m == "ERROR_INVALID_CREDENTIAL" || m == "ERROR_WRONG_PASSWORD" || m == "ERROR_USER_NOT_FOUND" ->
                "Incorrect email or password."
            m == "ERROR_USER_DISABLED" -> "This account has been disabled."
            m == "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please try again later."
            m == "ERROR_INVALID_EMAIL" -> "That email address is not valid."
            else -> e.localizedMessage ?: "Authentication failed."
        }
    }
}

// Minimal Task -> suspend await helper
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWith(Result.failure(it)) }
}
