package com.randallengineering.finances

import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.randallengineering.finances.core.auth.AuthScreen
import com.randallengineering.finances.core.auth.AuthViewModel
import com.randallengineering.finances.core.auth.SessionManager
import com.randallengineering.finances.core.security.BiometricAuthManager
import com.randallengineering.finances.core.theme.RandallFinancesTheme
import com.randallengineering.finances.core.theme.*
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.core.notifications.NotificationHelper
import com.randallengineering.finances.ui.components.*
import com.randallengineering.finances.ui.navigation.FinanceNavHost
import com.randallengineering.finances.ui.navigation.Screen
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val sessionManager: SessionManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RandallFinancesTheme {
                // Firebase auth gate (cross-platform sync): email/password + Google
                val signedIn by sessionManager.isSignedIn.collectAsState()
                var skippedSync by remember { mutableStateOf(false) }
                val authViewModel: AuthViewModel = koinViewModel()
                val deepLinkRoute = remember {
                    deepLinkRouteFor(intent.getStringExtra("deep_link_target"))
                }

                val googleLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    // Always inspect the result — a Google error or cancel still
                    // carries a diagnosable intent, and we must surface it instead
                    // of silently doing nothing.
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    if (task != null) {
                        authViewModel.handleGoogleResult(task)
                    } else {
                        authViewModel.setError("Google sign-in was cancelled or returned no result.")
                    }
                }

                if (!signedIn && !skippedSync) {
                    AuthScreen(
                        viewModel = authViewModel,
                        onGoogleSignIn = {
                            try {
                                googleLauncher.launch(sessionManager.googleSignInClient.signInIntent)
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "Google sign-in launch failed: ${e.message}")
                            }
                        },
                        onSkip = { skippedSync = true }
                    )
                } else {
                    val isBiometricEnabled = remember { BiometricAuthManager.isBiometricEnabled(this) }
                    var isAuthenticated by remember { mutableStateOf(!isBiometricEnabled) }
                    var authErrorMessage by remember { mutableStateOf<String?>(null) }

                    fun triggerUnlock() {
                        authErrorMessage = null
                        BiometricAuthManager.promptBiometricUnlock(
                            activity = this,
                            onSuccess = { isAuthenticated = true },
                            onError = { err -> authErrorMessage = err }
                        )
                    }

                    LaunchedEffect(isBiometricEnabled) {
                        if (isBiometricEnabled && !isAuthenticated) {
                            triggerUnlock()
                        }
                    }

                    if (isAuthenticated) {
                        FinanceNavHost(deepLinkRoute = deepLinkRoute)
                    } else {
                        BiometricLockScreen(
                            errorMessage = authErrorMessage,
                            onUnlockClick = { triggerUnlock() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BiometricLockScreen(
    errorMessage: String?,
    onUnlockClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(FinanceGreen.copy(alpha = 0.15f), shape = Shapes.large),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = FinanceGreen,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Randall Finances Locked",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Your financial data and bank credentials are securely encrypted.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            FinanceButton(
                onClick = onUnlockClick,
                backgroundColor = FinanceGreen,
                shadowColor = FinanceGreenDark,
                cornerRadius = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text("Unlock with Biometrics", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Maps a notification deep-link target to a navigation route (null = stay on Dashboard). */
private fun deepLinkRouteFor(target: String?): String? = when (target) {
    NotificationHelper.DeepLinkTarget.BUDGETS -> Screen.Budgets.route
    NotificationHelper.DeepLinkTarget.REVIEW_QUEUE -> Screen.ActionQueue.route
    NotificationHelper.DeepLinkTarget.SYNC -> Screen.Settings.route
    else -> null
}
