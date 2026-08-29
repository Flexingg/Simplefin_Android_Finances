package com.randallengineering.finances.core.auth

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.randallengineering.finances.core.network.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Cross-platform auth + sync coordinator. Signing in with the same Google account on
 * Android, web, or desktop yields the same Firebase `uid`, so all data is scoped and
 * shared under `users/{uid}/...` in Firestore.
 */
class SessionManager(private val context: Context) {

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
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Google sign-in failed")
        }
    }

    suspend fun signOut() {
        auth.signOut()
        try { googleSignInClient.signOut().await() } catch (_: Exception) {}
    }
}

// Minimal Task -> suspend await helper
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWith(Result.failure(it)) }
}
