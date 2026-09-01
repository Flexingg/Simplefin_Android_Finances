package com.randallengineering.finances.ui.screens.queue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.core.util.CurrencyFormatter
import com.randallengineering.finances.core.util.DateUtils
import com.randallengineering.finances.domain.model.CategoryHierarchy
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.core.theme.FinanceAmber
import com.randallengineering.finances.core.theme.FinanceAmberDark
import com.randallengineering.finances.core.theme.FinanceBlue
import com.randallengineering.finances.core.theme.FinanceBlueDark
import com.randallengineering.finances.core.theme.FinanceCardDark
import com.randallengineering.finances.core.theme.FinanceCardShadow
import com.randallengineering.finances.core.theme.FinanceGreen
import com.randallengineering.finances.core.theme.FinanceGreenDark
import com.randallengineering.finances.core.theme.FinanceRed
import com.randallengineering.finances.core.theme.FinanceRedDark
import com.randallengineering.finances.ui.components.FinanceButton
import org.koin.androidx.compose.koinViewModel
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionQueueScreen(
    viewModel: ActionQueueViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentTx = uiState.currentTransaction

    val isUncategorized = currentTx?.category.isNullOrBlank() || currentTx?.category.equals("Uncategorized", ignoreCase = true)
    var txNote by remember(currentTx?.id) { mutableStateOf(currentTx?.notes.orEmpty()) }
    var searchQuery by remember { mutableStateOf("") }
    var isExpandedAllCategories by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var selectedCategoryForSub by remember { mutableStateOf<CategoryHierarchy?>(null) }
    var isAutoRuleExpanded by remember(currentTx?.id) { mutableStateOf(false) }

    // Clean suggested rule pattern for active transaction
    var customRulePattern by remember(currentTx?.id) {
        mutableStateOf(currentTx?.let { viewModel.extractCleanMerchantPattern(it.originalDesc) } ?: "")
    }

    val liveRuleMatches = remember(customRulePattern, uiState.allTransactions) {
        if (customRulePattern.isNotBlank()) viewModel.calculateMatchesForPattern(customRulePattern) else 0
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        var newSubName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("➕ Add Custom Category", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Main Category (e.g. Pet Care, Hobbies)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newSubName,
                        onValueChange = { newSubName = it },
                        label = { Text("Subcategory (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                FinanceButton(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            viewModel.addCustomCategory(newCatName.trim(), newSubName.trim().ifBlank { null })
                            if (currentTx != null) {
                                viewModel.editCategory(currentTx, newCatName.trim(), newSubName.trim(), txNote)
                            }
                            showAddCategoryDialog = false
                        }
                    },
                    backgroundColor = FinanceGreen,
                    shadowColor = FinanceGreenDark,
                    cornerRadius = 10.dp
                ) {
                    Text("Save & Select", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                FinanceButton(
                    onClick = { showAddCategoryDialog = false },
                    backgroundColor = FinanceCardDark,
                    shadowColor = FinanceCardShadow,
                    cornerRadius = 10.dp
                ) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            // No gamification HUD — the review queue stands on its own.
            androidx.compose.material3.TopAppBar(
                title = { androidx.compose.material3.Text("Review Queue") },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Progress & Combo Status
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reviewing transaction ${uiState.currentCardIndex + 1}/${uiState.pendingTransactions.size}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                LinearProgressIndicator(
                    progress = {
                        if (uiState.pendingTransactions.isNotEmpty()) {
                            (uiState.currentCardIndex.toFloat() / uiState.pendingTransactions.size.toFloat()).coerceIn(0f, 1f)
                        } else 1f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(Shapes.small),
                    color = FinanceGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Rule Created Success Banner
            if (uiState.lastRuleCreatedMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = FinanceGreen.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(uiState.lastRuleCreatedMessage!!, color = FinanceGreenDark, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = FinanceGreenDark,
                            modifier = Modifier.size(16.dp).clickable { viewModel.clearRuleMessage() }
                        )
                    }
                }
            }

            LaunchedEffect(uiState.isSessionComplete) {
                if (uiState.isSessionComplete) {
                    com.randallengineering.finances.core.audio.FinanceSoundEffects.playLevelUpFanfare(context)
                }
            }

            if (uiState.isSessionComplete || currentTx == null) {
                // Celebration Completion State
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    shape = Shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Queue Cleared!", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "All transactions have been reviewed.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(10.dp))

                        FinanceButton(
                            onClick = { viewModel.resetSession() },
                            backgroundColor = FinanceGreen,
                            shadowColor = FinanceGreenDark,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Replay, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Start Over", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Review guidance prompt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isUncategorized) FinanceAmber.copy(alpha = 0.18f) else FinanceGreen.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = if (isUncategorized) "Choose a category or generate an Auto-Rule to categorize this transaction." else "Is this category correct? Confirm, search, or create an Auto-Rule below.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isUncategorized) FinanceAmberDark else FinanceGreenDark,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Active Transaction Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentTx.payee.ifBlank { currentTx.originalDesc },
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = DateUtils.formatDate(currentTx.postedEpochSeconds),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = CurrencyFormatter.format(abs(currentTx.amount)),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        HorizontalDivider()

                        // Quick Note Input for Bonus XP
                        OutlinedTextField(
                            value = txNote,
                            onValueChange = { txNote = it },
                            placeholder = { Text("Add a quick note (optional)", style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Amazon Order History Quick Jump (Conditional)
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val isAmazon = currentTx.originalDesc.contains("Amazon", ignoreCase = true) || currentTx.payee.contains("Amazon", ignoreCase = true)

                        if (isAmazon) {
                            FinanceButton(
                                onClick = {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.amazon.com/gp/your-account/order-history"))
                                    context.startActivity(intent)
                                },
                                backgroundColor = FinanceAmber,
                                shadowColor = FinanceAmberDark,
                                cornerRadius = 8.dp,
                                shadowHeight = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("📦 Jump to Amazon Orders ➔", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        // Current Category Badge & Quick Split Trigger
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                shape = Shapes.small,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUncategorized) FinanceRed.copy(alpha = 0.15f) else FinanceGreen.copy(alpha = 0.15f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isUncategorized) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = FinanceRedDark, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (isUncategorized) "Uncategorized (Required)" else "${currentTx.category}${if (currentTx.subCategory.isNotBlank()) " > ${currentTx.subCategory}" else ""}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUncategorized) FinanceRedDark else FinanceGreenDark,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            // Quick Split Button
                            var showSplitModal by remember { mutableStateOf(false) }
                            if (showSplitModal) {
                                com.randallengineering.finances.ui.components.SplitTransactionModal(
                                    transaction = currentTx,
                                    categories = uiState.availableCategories,
                                    onDismiss = { showSplitModal = false },
                                    onConfirmSplits = { splits ->
                                        viewModel.splitTransaction(currentTx, splits)
                                        showSplitModal = false
                                    }
                                )
                            }

                            FinanceButton(
                                onClick = { showSplitModal = true },
                                backgroundColor = FinanceBlue,
                                shadowColor = FinanceBlueDark,
                                cornerRadius = 8.dp,
                                shadowHeight = 2.dp
                            ) {
                                Text("✂️ Split", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // ⚡ Auto-Rule Generator Card inside Queue
                // -------------------------------------------------------------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.large,
                    colors = CardDefaults.cardColors(containerColor = FinanceCardDark)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAutoRuleExpanded = !isAutoRuleExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = FinanceAmber, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Auto-Rule Generator",
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Icon(
                                if (isAutoRuleExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                        if (!isAutoRuleExpanded) {
                            Text(
                                text = "Always auto-categorize \"$customRulePattern\" in future syncs!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        AnimatedVisibility(visible = isAutoRuleExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = customRulePattern,
                                    onValueChange = { customRulePattern = it },
                                    label = { Text("Match Pattern / Merchant Keyword") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Text(
                                    text = "🔥 Matches $liveRuleMatches existing transactions in database",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FinanceAmberDark,
                                    fontWeight = FontWeight.Bold
                                )

                                if (!isUncategorized) {
                                    FinanceButton(
                                        onClick = {
                                            viewModel.createAutoRuleAndCategorize(
                                                tx = currentTx,
                                                pattern = customRulePattern,
                                                newCategory = currentTx.category,
                                                newSubCategory = currentTx.subCategory,
                                                note = txNote
                                            )
                                        },
                                        backgroundColor = FinanceAmber,
                                        shadowColor = FinanceAmberDark,
                                        cornerRadius = 10.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White)
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Always Assign \"$customRulePattern\" ➔ ${currentTx.category}${if (currentTx.subCategory.isNotBlank()) " > ${currentTx.subCategory}" else ""}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // 🔍 Quick Search Categories & Subcategories (Instant Matching)
                // -------------------------------------------------------------
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("🔍 Quick Search Categories & Subs (e.g. fu, gas, coffee)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FinanceGreen) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Instant Search Results
                if (searchQuery.isNotBlank()) {
                    val searchResults = viewModel.searchCategoriesAndSubCategories(searchQuery, uiState.availableCategories)

                    if (searchResults.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = Shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = FinanceCardDark)
                        ) {
                            Text(
                                text = "No category or subcategory found for \"$searchQuery\". Tap '+ New Category' below to create it!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Search Matches (Tap 1-click to assign):",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            searchResults.forEach { result ->
                                val emoji = getCategoryEmoji(result.mainCategory)
                                FinanceButton(
                                    onClick = {
                                        viewModel.editCategory(currentTx, result.mainCategory, result.subCategory, txNote)
                                        searchQuery = ""
                                    },
                                    backgroundColor = FinanceGreen,
                                    shadowColor = FinanceGreenDark,
                                    cornerRadius = 10.dp,
                                    shadowHeight = 3.dp
                                ) {
                                    Text(
                                        text = "$emoji ${result.fullDisplayName}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }

                // -------------------------------------------------------------
                // Primary / Top Categories Grid
                // -------------------------------------------------------------
                val topCategories = uiState.availableCategories.take(6)
                Text(
                    text = "Select Category from Database:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (i in topCategories.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val cat1 = topCategories[i]
                            val emoji1 = getCategoryEmoji(cat1.mainCategory)
                            val isSelected1 = currentTx.category.equals(cat1.mainCategory, ignoreCase = true)
                            FinanceButton(
                                onClick = {
                                    if (cat1.subCategories.isNotEmpty()) {
                                        selectedCategoryForSub = cat1
                                    } else {
                                        viewModel.editCategory(currentTx, cat1.mainCategory, "", txNote)
                                    }
                                },
                                backgroundColor = if (isSelected1) FinanceGreen else FinanceCardDark,
                                shadowColor = if (isSelected1) FinanceGreenDark else FinanceCardShadow,
                                cornerRadius = 14.dp,
                                shadowHeight = 4.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$emoji1 ${cat1.mainCategory}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }

                            if (i + 1 < topCategories.size) {
                                val cat2 = topCategories[i + 1]
                                val emoji2 = getCategoryEmoji(cat2.mainCategory)
                                val isSelected2 = currentTx.category.equals(cat2.mainCategory, ignoreCase = true)
                                FinanceButton(
                                    onClick = {
                                        if (cat2.subCategories.isNotEmpty()) {
                                            selectedCategoryForSub = cat2
                                        } else {
                                            viewModel.editCategory(currentTx, cat2.mainCategory, "", txNote)
                                        }
                                    },
                                    backgroundColor = if (isSelected2) FinanceGreen else FinanceCardDark,
                                    shadowColor = if (isSelected2) FinanceGreenDark else FinanceCardShadow,
                                    cornerRadius = 14.dp,
                                    shadowHeight = 4.dp,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "$emoji2 ${cat2.mainCategory}",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Expandable Section for All Database Categories
                val remainingCategories = uiState.availableCategories.drop(6)
                if (remainingCategories.isNotEmpty() || true) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(Shapes.medium)
                            .clickable { isExpandedAllCategories = !isExpandedAllCategories },
                        shape = Shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = FinanceCardDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🗂️ All Database Categories (${uiState.availableCategories.size})",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Icon(
                                if (isExpandedAllCategories) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }

                    AnimatedVisibility(visible = isExpandedAllCategories) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.availableCategories.forEach { cat ->
                                    val emoji = getCategoryEmoji(cat.mainCategory)
                                    val isSelected = currentTx.category.equals(cat.mainCategory, ignoreCase = true)
                                    Card(
                                        modifier = Modifier
                                            .clip(Shapes.small)
                                            .clickable {
                                                if (cat.subCategories.isNotEmpty()) {
                                                    selectedCategoryForSub = cat
                                                } else {
                                                    viewModel.editCategory(currentTx, cat.mainCategory, "", txNote)
                                                }
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) FinanceGreen else FinanceCardDark
                                        )
                                    ) {
                                        Text(
                                            text = "$emoji ${cat.mainCategory}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }

                                // ➕ Add New Category Pill
                                Card(
                                    modifier = Modifier
                                        .clip(Shapes.small)
                                        .clickable { showAddCategoryDialog = true },
                                    colors = CardDefaults.cardColors(containerColor = FinanceBlue.copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = FinanceBlue, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("New Category", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = FinanceBlue)
                                    }
                                }
                            }
                        }
                    }
                }

                // Subcategory Selection Popup
                if (selectedCategoryForSub != null) {
                    val cat = selectedCategoryForSub!!
                    AlertDialog(
                        onDismissRequest = { selectedCategoryForSub = null },
                        title = { Text("Select ${cat.mainCategory} Subcategory", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                FinanceButton(
                                    onClick = {
                                        viewModel.editCategory(currentTx, cat.mainCategory, "", txNote)
                                        selectedCategoryForSub = null
                                    },
                                    backgroundColor = FinanceGreen,
                                    shadowColor = FinanceGreenDark,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("General / No Subcategory", color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                cat.subCategories.forEach { sub ->
                                    FinanceButton(
                                        onClick = {
                                            viewModel.editCategory(currentTx, cat.mainCategory, sub, txNote)
                                            selectedCategoryForSub = null
                                        },
                                        backgroundColor = FinanceCardDark,
                                        shadowColor = FinanceCardShadow,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(sub, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            FinanceButton(
                                onClick = { selectedCategoryForSub = null },
                                backgroundColor = FinanceRed,
                                shadowColor = FinanceRedDark,
                                cornerRadius = 10.dp
                            ) {
                                Text("Cancel", color = Color.White)
                            }
                        }
                    )
                }

                // Bottom Confirmation Button (Only enabled if categorized!)
                Spacer(Modifier.height(4.dp))
                val hasNote = txNote.isNotBlank()
                FinanceButton(
                    onClick = {
                        viewModel.confirmCategory(currentTx, txNote)
                    },
                    enabled = !isUncategorized,
                    backgroundColor = FinanceGreen,
                    shadowColor = FinanceGreenDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isUncategorized) {
                        Text("Choose a Category Above to Confirm", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Confirm (${currentTx.category})", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun getCategoryEmoji(name: String): String {
    val lower = name.lowercase()
    return when {
        lower.contains("dining") || lower.contains("food") || lower.contains("restaurant") -> "🍔"
        lower.contains("grocer") -> "🛒"
        lower.contains("auto") || lower.contains("gas") || lower.contains("fuel") || lower.contains("transport") -> "🚗"
        lower.contains("util") || lower.contains("electric") || lower.contains("bill") -> "💡"
        lower.contains("shop") || lower.contains("retail") || lower.contains("amazon") -> "🛍️"
        lower.contains("entertain") || lower.contains("fun") || lower.contains("game") -> "🍿"
        lower.contains("health") || lower.contains("medic") || lower.contains("wellness") -> "🏥"
        lower.contains("income") || lower.contains("salary") || lower.contains("deposit") -> "💼"
        lower.contains("subscript") || lower.contains("netflix") || lower.contains("stream") -> "📱"
        lower.contains("home") || lower.contains("house") || lower.contains("rent") -> "🏠"
        lower.contains("travel") || lower.contains("hotel") || lower.contains("flight") -> "✈️"
        lower.contains("kid") || lower.contains("baby") || lower.contains("toy") -> "🧸"
        else -> "🏷️"
    }
}
