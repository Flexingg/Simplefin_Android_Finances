package com.randallengineering.finances.ui.screens.transactions

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.randallengineering.finances.core.theme.FinanceGreen
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.core.util.CurrencyFormatter
import com.randallengineering.finances.core.util.DateUtils
import com.randallengineering.finances.ui.components.CategoryPickerDialog
import com.randallengineering.finances.ui.components.ExpressiveCard
import com.randallengineering.finances.ui.components.GenerateRuleFromTransactionDialog
import com.randallengineering.finances.ui.components.SplitTransactionDialog
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: String,
    onNavigateBack: () -> Unit,
    viewModel: TransactionViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val transaction = remember(uiState.transactions, transactionId) {
        uiState.transactions.find { it.id == transactionId }
    }

    // Receipt File Picker Launcher with ML Kit OCR
    val receiptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && transaction != null) {
            viewModel.processAndScanReceipt(
                context = context,
                transactionId = transaction.id,
                uri = uri
            )
        }
    }

    // ML Kit Scanned Receipt Confirmation Dialog
    if (uiState.scannedReceipt != null && transaction != null) {
        val parsed = uiState.scannedReceipt!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissScannedReceipt() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚡", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(8.dp))
                    Text("AI Scanned Receipt", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Merchant: ${parsed.merchantName.ifBlank { "Not detected" }}", fontWeight = FontWeight.Bold)
                    if (parsed.totalAmount > 0) {
                        Text("Detected Total: ${CurrencyFormatter.format(parsed.totalAmount)}", fontWeight = FontWeight.Bold, color = FinanceGreen)
                    }
                    Text("Auto-fill this merchant name into the transaction?", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.applyScannedReceiptToTransaction(transaction) },
                    colors = ButtonDefaults.buttonColors(containerColor = FinanceGreen)
                ) {
                    Text("Apply Info ➔", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissScannedReceipt() }) {
                    Text("Keep Current")
                }
            }
        )
    }

    // Split modal dialog
    if (uiState.selectedTransactionForSplit != null) {
        SplitTransactionDialog(
            transaction = uiState.selectedTransactionForSplit!!,
            onDismiss = { viewModel.closeSplitDialog() },
            onConfirmSplits = { splits ->
                viewModel.saveTransactionSplits(uiState.selectedTransactionForSplit!!, splits)
            }
        )
    }

    // Category Picker Dialog
    if (uiState.selectedTransactionForCategoryPicker != null) {
        CategoryPickerDialog(
            categories = uiState.categories,
            initialMainCategory = uiState.selectedTransactionForCategoryPicker!!.category,
            initialSubCategory = uiState.selectedTransactionForCategoryPicker!!.subCategory,
            onDismiss = { viewModel.closeCategoryPicker() },
            onCategorySelected = { main, sub ->
                viewModel.updateTransactionCategory(uiState.selectedTransactionForCategoryPicker!!, main, sub)
            },
            onAddNewCategory = { main, sub ->
                viewModel.addCustomCategory(main, sub)
            }
        )
    }

    // Rule Generation Dialog
    if (uiState.selectedTransactionForRuleGen != null) {
        GenerateRuleFromTransactionDialog(
            transaction = uiState.selectedTransactionForRuleGen!!,
            categories = uiState.categories,
            nextPriority = uiState.rules.size + 1,
            onDismiss = { viewModel.closeRuleGenDialog() },
            onSaveRule = { rule ->
                viewModel.saveGeneratedRule(rule)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (transaction == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Transaction not found", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Amount & Description Card
                ExpressiveCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = CurrencyFormatter.formatWithSign(transaction.amount),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (transaction.isIncome) FinanceGreen else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = transaction.originalDesc,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = DateUtils.formatDateTime(transaction.postedEpochSeconds),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                // Amazon Order Jump Card (Conditional)
                val isAmazon = transaction.originalDesc.contains("Amazon", ignoreCase = true) ||
                        transaction.payee.contains("Amazon", ignoreCase = true)

                if (isAmazon) {
                    ExpressiveCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Default.ShoppingBag,
                                    contentDescription = "Amazon",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Amazon Purchase Detected",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "Jump directly to Amazon order history",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.openAmazonOrders(context) },
                                shape = Shapes.small,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Text("Jump to Orders ➔", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Categorization & Subcategories Section
                ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Categorization & Splits",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedButton(
                                onClick = { viewModel.openCategoryPicker(transaction) },
                                shape = Shapes.small
                            ) {
                                Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Change Category")
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        if (transaction.isSplit) {
                            transaction.splits.forEachIndexed { index, split ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${index + 1}. ${split.category}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (split.notes.isNotBlank()) {
                                            Text(
                                                text = split.notes,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = CurrencyFormatter.format(split.amount),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (index < transaction.splits.size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(Shapes.small)
                                    .clickable { viewModel.openCategoryPicker(transaction) }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Main Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(transaction.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }

                                if (transaction.subCategory.isNotBlank()) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Subcategory", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(transaction.subCategory, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            if (transaction.matchedRuleId != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "⚡ Categorized by active regex rule",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.openSplitDialog(transaction) },
                                modifier = Modifier.weight(1f),
                                shape = Shapes.small
                            ) {
                                Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (transaction.isSplit) "Edit Splits" else "Split Amount")
                            }

                            Button(
                                onClick = { viewModel.openRuleGenDialog(transaction) },
                                modifier = Modifier.weight(1.2f),
                                shape = Shapes.small,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Create Auto-Rule")
                            }
                        }
                    }
                }

                // Receipts & Attachments Section
                ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Receipts & Attachments",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Button(
                                onClick = { receiptPickerLauncher.launch("image/*") },
                                enabled = !uiState.isUploadingReceipt,
                                shape = Shapes.small
                            ) {
                                if (uiState.isUploadingReceipt) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                } else {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text("Upload Receipt")
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (transaction.receiptUrls.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        Shapes.medium
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "No receipts attached yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                transaction.receiptUrls.forEach { url ->
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(Shapes.medium)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = "Receipt",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
