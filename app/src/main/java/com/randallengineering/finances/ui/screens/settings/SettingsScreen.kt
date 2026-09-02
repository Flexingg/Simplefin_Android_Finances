package com.randallengineering.finances.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.randallengineering.finances.core.theme.FinanceGreen
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.core.util.DateUtils
import com.randallengineering.finances.ui.components.BackupExportSection
import com.randallengineering.finances.ui.components.ExpressiveCard
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private val AmazonAmber = Color(0xFFFF9900)
private val AmazonDark = Color(0xFF131A22)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.initSecurityState(context)
    }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Integrations", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // -------------------------------------------------------------
            // Account Section
            // -------------------------------------------------------------
            if (uiState.accountEmail != null) {
                ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Account",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = uiState.accountDisplayName ?: "Synced account",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        uiState.accountEmail?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { viewModel.signOut() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign Out", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // Backup & Export Section
            // -------------------------------------------------------------
            BackupExportSection(
                driveSignInIntent = { viewModel.driveSignInIntent() },
                csvProvider = { viewModel.buildBackupCsv() },
                onMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
            )

            // -------------------------------------------------------------
            // SimpleFIN Sync Section
            // -------------------------------------------------------------
            ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = "Bank",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "SimpleFIN Bank Connection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Connection status badge
                    Card(
                        shape = Shapes.small,
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.isConnected) FinanceGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (uiState.isConnected) Icons.Default.CheckCircle else Icons.Default.Link,
                                contentDescription = null,
                                tint = if (uiState.isConnected) FinanceGreen else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isConnected) "Status: Connected to SimpleFIN Bridge" else "Status: Not Connected",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (uiState.isConnected) FinanceGreen else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (uiState.isConnected) {
                        uiState.config?.let { cfg ->
                            if (cfg.lastSyncTimestamp > 0) {
                                Text(
                                    text = "Last synced: ${DateUtils.formatDateTime(cfg.lastSyncTimestamp)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider()

                        Text(
                            text = "Sync History Range: ${uiState.syncDaysBack} Days",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )

                        Slider(
                            value = uiState.syncDaysBack.toFloat(),
                            onValueChange = { viewModel.onSyncDaysBackChange(it.toInt()) },
                            valueRange = 7f..365f,
                            steps = 11
                        )

                        Button(
                            onClick = { viewModel.triggerSync() },
                            enabled = !uiState.isSyncing,
                            modifier = Modifier.fillMaxWidth(),
                            shape = Shapes.small
                        ) {
                            if (uiState.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Syncing via 89-day batches...")
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Sync Transactions Now")
                            }
                        }

                        uiState.lastSync?.let { sync ->
                            val failed = sync.succeeded == false
                            Text(
                                text = buildString {
                                    append("Last sync: ${sync.syncedAgoText}")
                                    if (sync.accountCount > 0) {
                                        append(" · ${sync.accountCount} account${if (sync.accountCount == 1) "" else "s"}")
                                    }
                                    if (failed) append(" · FAILED")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (failed) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            if (failed) {
                                Text(
                                    text = sync.errorMessage ?: "Couldn't reach your bank. Check your connection.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Paste your Setup Token from bridge.simplefin.org to connect your bank accounts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = uiState.tokenInput,
                            onValueChange = { viewModel.onTokenInputChange(it) },
                            label = { Text("Setup Token") },
                            placeholder = { Text("Base64 encoded SimpleFIN token") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = { viewModel.claimToken() },
                            enabled = !uiState.isClaimingToken && uiState.tokenInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = Shapes.small
                        ) {
                            if (uiState.isClaimingToken) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Claiming Token...")
                            } else {
                                Text("Claim Token & Connect")
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // Biometrics & App Security Section
            // -------------------------------------------------------------
            ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Security",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Biometric App Lock",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Require Fingerprint, Face, or PIN to open app",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        androidx.compose.material3.Switch(
                            checked = uiState.isBiometricEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.setBiometricEnabled(context, enabled)
                            }
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // Notifications (opt-in, default off) Section
            // -------------------------------------------------------------
            NotificationSettingsCard(
                budgetAlerts = uiState.budgetAlertsEnabled,
                reviewAlerts = uiState.reviewAlertsEnabled,
                onToggleBudget = { on -> viewModel.setBudgetAlerts(on) },
                onToggleReview = { on -> viewModel.setReviewAlerts(on) }
            )

            // -------------------------------------------------------------
            // Amazon Order History Launcher Section
            // -------------------------------------------------------------
            ExpressiveCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ShoppingBag,
                            contentDescription = "Amazon",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Amazon Order History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    Text(
                        text = "Instantly open the Amazon App or browser directly to your recent order history to check purchased items, totals, and receipts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                    )

                    Button(
                        onClick = { viewModel.openAmazonOrderHistory(context) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmazonAmber,
                            contentColor = AmazonDark
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open Amazon Orders ➔", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // -------------------------------------------------------------
            // App Info Section
            // -------------------------------------------------------------
            ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("About Randall Finances", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Randall Finances — Personal Finance & Budgeting Engine.\nVersion 2.0 • Build Debug",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NotificationSettingsCard(
    budgetAlerts: Boolean,
    reviewAlerts: Boolean,
    onToggleBudget: (Boolean) -> Unit,
    onToggleReview: (Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var pendingType by remember { mutableStateOf<String?>(null) }
    var showConfirm by remember { mutableStateOf(false) }
    var permissionNote by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val type = pendingType
        pendingType = null
        if (granted) {
            permissionNote = null
            if (type == "budget") onToggleBudget(true) else onToggleReview(true)
        } else {
            permissionNote = if (type == "budget")
                "Notification permission was denied, so budget alerts stay off. You can enable it later in system Settings."
            else
                "Notification permission was denied, so review reminders stay off. You can enable it later in system Settings."
        }
    }

    // Called when the user confirms they want a category on.
    fun confirmEnable(type: String) {
        val needsPermission = android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingType = type
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            pendingType = null
            if (type == "budget") onToggleBudget(true) else onToggleReview(true)
        }
    }

    fun requestEnable(type: String) {
        pendingType = type
        showConfirm = true
    }

    if (showConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Enable notifications?") },
            text = {
                Text(
                    if (pendingType == "budget")
                        "Turn on budget alerts? You'll be notified when a category reaches 90% of its monthly limit."
                    else
                        "Turn on review reminders? You'll be notified when new transactions arrive that need categorization in your review queue."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showConfirm = false
                    confirmEnable(pendingType ?: return@TextButton)
                }) { Text("Enable") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showConfirm = false }) { Text("Not now") }
            }
        )
    }

    ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Alerts are off by default. Only categories you enable will ever be sent.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            NotificationSwitchRow("Budget alerts", "Notify at 90% of a monthly category budget", budgetAlerts) {
                if (it) requestEnable("budget") else onToggleBudget(false)
            }
            NotificationSwitchRow("Review reminders", "New transactions needing review after a sync", reviewAlerts) {
                if (it) requestEnable("review") else onToggleReview(false)
            }

            permissionNote?.let { note ->
                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun NotificationSwitchRow(title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onToggle)
    }
}
