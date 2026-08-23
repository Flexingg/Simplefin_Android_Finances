package com.randallengineering.finances.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.randallengineering.finances.core.theme.FinanceGreen
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.core.util.DateUtils
import com.randallengineering.finances.ui.components.AmazonOAuthWebViewSheet
import com.randallengineering.finances.ui.components.ExpressiveCard
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

    // CSV File Picker
    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importAmazonCsv(uri)
        }
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

    // In-App Amazon OAuth WebView BottomSheet
    if (uiState.authUrlForSheet != null) {
        AmazonOAuthWebViewSheet(
            authUrl = uiState.authUrlForSheet!!,
            onCodeReceived = { authCode ->
                viewModel.onOAuthCodeReceived(authCode)
            },
            onDismiss = { viewModel.closeOAuthSheet() }
        )
    }

    // Raw API Data Dialog
    if (uiState.rawApiDataToDisplay != null) {
        AlertDialog(
            onDismissRequest = { viewModel.closeRawApiDataDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Amazon Raw API Response", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(Shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = uiState.rawApiDataToDisplay!!,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Amazon Raw API Data", uiState.rawApiDataToDisplay!!))
                        Toast.makeText(context, "Copied API JSON to clipboard", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copy JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeRawApiDataDialog() }) {
                    Text("Close")
                }
            }
        )
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
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ===============================================================
            // Section 1: Login with Amazon (LWA) & SP-API
            // ===============================================================
            Text(
                text = "Amazon Integration (Login with Amazon)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            ExpressiveCard(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header & Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(Shapes.small)
                                    .background(AmazonAmber),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ShoppingBag,
                                    contentDescription = "Amazon",
                                    tint = AmazonDark,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Login with Amazon (LWA)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "SP-API Order & Item Breakdown",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Card(
                            shape = Shapes.small,
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.isAmazonConnected) FinanceGreen.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (uiState.isAmazonConnected) Icons.Default.CheckCircle else Icons.Default.LinkOff,
                                    contentDescription = null,
                                    tint = if (uiState.isAmazonConnected) FinanceGreen else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.isAmazonConnected) "Authorized" else "Not Linked",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isAmazonConnected) FinanceGreen else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    // Display Linked Amazon Account Profile Details
                    if (uiState.isAmazonConnected && uiState.amazonUserEmail.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = Shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = FinanceGreen.copy(alpha = 0.12f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = FinanceGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    if (uiState.amazonUserName.isNotBlank()) {
                                        Text(
                                            text = uiState.amazonUserName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "Account: ${uiState.amazonUserEmail}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Main Action: Login with Amazon Button
                    Button(
                        onClick = { viewModel.startAmazonConnect() },
                        enabled = !uiState.isFetchingAmazonAuthUrl && !uiState.isExchangingToken,
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmazonAmber,
                            contentColor = AmazonDark
                        )
                    ) {
                        if (uiState.isFetchingAmazonAuthUrl || uiState.isExchangingToken) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AmazonDark)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (uiState.isExchangingToken) "Authorizing Account..." else "Opening Amazon Login...",
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (uiState.isAmazonConnected) "Re-Authorize Amazon Account" else "Login with Amazon",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Raw API Data Button
                        OutlinedButton(
                            onClick = { viewModel.viewRawApiData() },
                            modifier = Modifier.weight(1f),
                            shape = Shapes.small
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Raw API Data")
                        }

                        if (uiState.isAmazonConnected) {
                            OutlinedButton(
                                onClick = { viewModel.disconnectAmazon() },
                                modifier = Modifier.weight(1f),
                                shape = Shapes.small,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Disconnect")
                            }
                        }
                    }

                    HorizontalDivider()

                    // Amazon Order Ingestion Methods (Live AI Scanner & CSV)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Real Amazon Orders Ingestion", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (uiState.importedOrdersCount > 0) "${uiState.importedOrdersCount} real items loaded" else "Scan or Import your real orders",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (uiState.importedOrdersCount > 0) {
                            IconButton(onClick = { viewModel.clearImportedOrders() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // 1-Tap Live AI Scanner Button
                    var isShowingSettingsScanner by remember { mutableStateOf(false) }
                    if (isShowingSettingsScanner) {
                        com.randallengineering.finances.ui.components.AmazonOrdersBrowserSheet(
                            onOrdersImported = { isShowingSettingsScanner = false },
                            onDismiss = { isShowingSettingsScanner = false }
                        )
                    }

                    Button(
                        onClick = { isShowingSettingsScanner = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmazonAmber,
                            contentColor = AmazonDark
                        )
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("⚡ Live AI Scan Amazon Orders", fontWeight = FontWeight.Bold)
                    }

                    // CSV File Picker Button
                    OutlinedButton(
                        onClick = { csvPickerLauncher.launch("*/*") },
                        enabled = !uiState.isImportingCsv,
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.small
                    ) {
                        if (uiState.isImportingCsv) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Parsing Orders...")
                        } else {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Import Order Report (CSV)")
                        }
                    }

                    HorizontalDivider()

                    // Amazon Developer Security Profile Configuration Section
                    Text(
                        text = "Amazon LWA Developer Credentials",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // 1. Client ID
                    OutlinedTextField(
                        value = uiState.amazonClientIdInput,
                        onValueChange = { viewModel.onAmazonClientIdChange(it) },
                        label = { Text("LWA Client ID") },
                        placeholder = { Text("amzn1.application-oa2-client.xxxx...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium,
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) }
                    )

                    // 2. Client Secret
                    OutlinedTextField(
                        value = uiState.amazonClientSecretInput,
                        onValueChange = { viewModel.onAmazonClientSecretChange(it) },
                        label = { Text("LWA Client Secret") },
                        placeholder = { Text("amzn1.oa2-cs.v1.xxxx...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium,
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Button(
                        onClick = { viewModel.saveAmazonCredentials() },
                        enabled = uiState.amazonClientIdInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.small
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save Credentials")
                    }

                    // Amazon Developer Console Links & URLs to copy
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("LWA Console Setup URLs", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://developer.amazon.com/loginwithamazon/console/site/lwa/overview.html"))
                                        context.startActivity(intent)
                                    },
                                    shape = Shapes.small
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("LWA Console", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            // URL 1: Privacy Notice URL
                            UrlCopyRow(
                                label = "1. Privacy Notice URL",
                                url = "https://randall-finances.web.app/privacy",
                                context = context
                            )

                            // URL 2: Data Collection Policy URL
                            UrlCopyRow(
                                label = "2. Data Collection Policy URL",
                                url = "https://randall-finances.web.app/terms",
                                context = context
                            )

                            // URL 3: Allowed Return URL (Redirect URI)
                            UrlCopyRow(
                                label = "3. Allowed Return URL (Redirect URI)",
                                url = "https://randall-finances.web.app/amazonOAuthCallback",
                                context = context
                            )
                        }
                    }
                }
            }

            // ===============================================================
            // Section 2: SimpleFIN Banking Bridge
            // ===============================================================
            Text(
                text = "SimpleFIN Banking Bridge",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            ExpressiveCard(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(Shapes.small)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "SimpleFIN Direct Sync",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (uiState.config?.lastSyncTimestamp != null && uiState.config!!.lastSyncTimestamp > 0) {
                                    Text(
                                        text = "Last synced: ${DateUtils.formatDateTime(uiState.config!!.lastSyncTimestamp / 1000L)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Card(
                            shape = Shapes.small,
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.isConnected) FinanceGreen.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (uiState.isConnected) Icons.Default.CheckCircle else Icons.Default.LinkOff,
                                    contentDescription = null,
                                    tint = if (uiState.isConnected) FinanceGreen else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.isConnected) "Connected" else "Not Configured",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isConnected) FinanceGreen else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    // Sync Days Back Slider
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sync Timeframe (89-Day Batches)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Past ${uiState.syncDaysBack} Days (~${((uiState.syncDaysBack + 88) / 89)} batches)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = uiState.syncDaysBack.toFloat(),
                            onValueChange = { viewModel.onSyncDaysBackChange(it.toInt()) },
                            valueRange = 7f..1000f,
                            steps = 33
                        )
                    }

                    // Sync Now Button
                    Button(
                        onClick = { viewModel.triggerSync() },
                        enabled = uiState.isConnected && !uiState.isSyncing,
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.small
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Syncing Transactions in 89-Day Batches...")
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Sync Past ${uiState.syncDaysBack} Days Now")
                        }
                    }

                    HorizontalDivider()

                    // Update Setup Token Input
                    Text("Update SimpleFIN Setup Token", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = uiState.tokenInput,
                        onValueChange = { viewModel.onTokenInputChange(it) },
                        placeholder = { Text("Paste Base64 setup token...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium,
                        singleLine = false,
                        maxLines = 3
                    )

                    Button(
                        onClick = { viewModel.claimToken() },
                        enabled = uiState.tokenInput.isNotBlank() && !uiState.isClaimingToken,
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.small
                    ) {
                        if (uiState.isClaimingToken) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Connecting...")
                        } else {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Connect New Setup Token")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UrlCopyRow(
    label: String,
    url: String,
    context: Context
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shapes.small)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), Shapes.small)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(label, url))
                    Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
