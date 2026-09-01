package com.randallengineering.finances.core.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.randallengineering.finances.core.theme.FinanceGreen
import com.randallengineering.finances.core.theme.FinanceGreenDark

private enum class AuthMode { Login, Create }

/**
 * Full-screen auth gate: email/password create-account or login, plus Google
 * sign-in. A brand-new account is created by default when the email has no
 * existing user (create mode) or via Google first-time sign-in, and the user's
 * Firestore record is auto-provisioned.
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onGoogleSignIn: () -> Unit,
    onSkip: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var mode by rememberSaveable { mutableStateOf(AuthMode.Login) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

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
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text("🪙", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Randall Finances",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Sign in or create an account to sync transactions, budgets, and insights across your Android, web, and desktop apps.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            // Mode toggle
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { mode = AuthMode.Login; viewModel.clearError() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Log In", fontWeight = if (mode == AuthMode.Login) FontWeight.Bold else FontWeight.Normal)
                }
                TextButton(
                    onClick = { mode = AuthMode.Create; viewModel.clearError() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Create Account", fontWeight = if (mode == AuthMode.Create) FontWeight.Bold else FontWeight.Normal)
                }
            }
            Spacer(Modifier.height(12.dp))

            if (mode == AuthMode.Create) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                enabled = !uiState.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                enabled = !uiState.isLoading,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (mode == AuthMode.Login) {
                TextButton(
                    onClick = { if (email.isNotBlank()) viewModel.login(email, password) },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Use Create Account if you're new here", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(Modifier.height(16.dp))

            uiState.error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    if (mode == AuthMode.Create) viewModel.createAccount(email, password, displayName)
                    else viewModel.login(email, password)
                },
                enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinanceGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text(if (mode == AuthMode.Create) "Create Account" else "Log In", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                androidx.compose.material3.HorizontalDivider(modifier = Modifier.weight(1f))
                Text("  or  ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.material3.HorizontalDivider(modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onGoogleSignIn,
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Continue with Google", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick = onSkip,
                enabled = !uiState.isLoading
            ) {
                Text("Continue without syncing", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
