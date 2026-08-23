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
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.core.util.CurrencyFormatter
import com.randallengineering.finances.core.util.DateUtils
import com.randallengineering.finances.domain.model.CategoryHierarchy
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.ui.components.DuoBlue
import com.randallengineering.finances.ui.components.DuoBlueDark
import com.randallengineering.finances.ui.components.DuoCardDark
import com.randallengineering.finances.ui.components.DuoCardShadow
import com.randallengineering.finances.ui.components.DuoGold
import com.randallengineering.finances.ui.components.DuoGoldDark
import com.randallengineering.finances.ui.components.DuoGreen
import com.randallengineering.finances.ui.components.DuoGreenDark
import com.randallengineering.finances.ui.components.DuoRed
import com.randallengineering.finances.ui.components.DuoRedDark
import com.randallengineering.finances.ui.components.DuolingoPressableButton
import com.randallengineering.finances.ui.components.GamificationHud
import org.koin.androidx.compose.koinViewModel
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionQueueScreen(
    viewModel: ActionQueueViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTx = uiState.currentTransaction

    val isUncategorized = currentTx?.category.isNullOrBlank() || currentTx?.category.equals("Uncategorized", ignoreCase = true)
    var txNote by remember(currentTx?.id) { mutableStateOf(currentTx?.notes.orEmpty()) }
    var isExpandedAllCategories by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var selectedCategoryForSub by remember { mutableStateOf<CategoryHierarchy?>(null) }

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
                DuolingoPressableButton(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            viewModel.addNewCategory(newCatName.trim(), newSubName.trim().ifBlank { null })
                            if (currentTx != null) {
                                viewModel.editCategory(currentTx, newCatName.trim(), newSubName.trim())
                            }
                            showAddCategoryDialog = false
                        }
                    },
                    backgroundColor = DuoGreen,
                    shadowColor = DuoGreenDark,
                    cornerRadius = 10.dp
                ) {
                    Text("Save & Select", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                DuolingoPressableButton(
                    onClick = { showAddCategoryDialog = false },
                    backgroundColor = DuoCardDark,
                    shadowColor = DuoCardShadow,
                    cornerRadius = 10.dp
                ) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            GamificationHud()
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
                        text = "Daily Habit Review (${uiState.currentCardIndex}/${uiState.pendingTransactions.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )

                    if (uiState.comboMultiplier > 1) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(Shapes.small)
                                .background(DuoGold.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = DuoGoldDark, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("${uiState.comboMultiplier}x Combo!", fontWeight = FontWeight.Black, color = DuoGoldDark, style = MaterialTheme.typography.labelSmall)
                        }
                    }
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
                    color = DuoGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
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
                        Text("🦉", style = MaterialTheme.typography.displayLarge)
                        Text("Queue Cleared!", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "All transactions categorized and verified! +${uiState.totalXpEarnedInSession} XP earned today.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(10.dp))

                        DuolingoPressableButton(
                            onClick = { viewModel.resetSession() },
                            backgroundColor = DuoGreen,
                            shadowColor = DuoGreenDark,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Replay, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Practice Again", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Duolingo Mascot Prompt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🦉", style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.width(10.dp))
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isUncategorized) DuoGold.copy(alpha = 0.18f) else DuoGreen.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = if (isUncategorized) "Choose a category below to earn +25 XP and avoid heart loss!" else "Is this category correct? Confirm or change it below!",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isUncategorized) DuoGoldDark else DuoGreenDark,
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
                            placeholder = { Text("💬 Add a quick note (+10 XP bonus!)", style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Current Category Badge & Quick Split Trigger
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                shape = Shapes.small,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUncategorized) DuoRed.copy(alpha = 0.15f) else DuoGreen.copy(alpha = 0.15f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isUncategorized) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = DuoRedDark, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (isUncategorized) "Uncategorized (Required)" else "${currentTx.category}${if (currentTx.subCategory.isNotBlank()) " > ${currentTx.subCategory}" else ""}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUncategorized) DuoRedDark else DuoGreenDark,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            // Quick Split Button
                            var showSplitModal by remember { mutableStateOf(false) }
                            if (showSplitModal) {
                                com.randallengineering.finances.ui.components.DuolingoSplitModal(
                                    transaction = currentTx,
                                    categories = uiState.availableCategories,
                                    onDismiss = { showSplitModal = false },
                                    onConfirmSplits = { splits ->
                                        viewModel.splitTransaction(currentTx, splits)
                                        showSplitModal = false
                                    }
                                )
                            }

                            DuolingoPressableButton(
                                onClick = { showSplitModal = true },
                                backgroundColor = DuoBlue,
                                shadowColor = DuoBlueDark,
                                cornerRadius = 8.dp,
                                shadowHeight = 2.dp
                            ) {
                                Text("✂️ Split", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Primary / Top Categories Grid
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
                            DuolingoPressableButton(
                                onClick = {
                                    if (cat1.subCategories.isNotEmpty()) {
                                        selectedCategoryForSub = cat1
                                    } else {
                                        viewModel.editCategory(currentTx, cat1.mainCategory, "", txNote)
                                    }
                                },
                                backgroundColor = if (isSelected1) DuoGreen else DuoCardDark,
                                shadowColor = if (isSelected1) DuoGreenDark else DuoCardShadow,
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
                                DuolingoPressableButton(
                                    onClick = {
                                        if (cat2.subCategories.isNotEmpty()) {
                                            selectedCategoryForSub = cat2
                                        } else {
                                            viewModel.editCategory(currentTx, cat2.mainCategory, "", txNote)
                                        }
                                    },
                                    backgroundColor = if (isSelected2) DuoGreen else DuoCardDark,
                                    shadowColor = if (isSelected2) DuoGreenDark else DuoCardShadow,
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
                        colors = CardDefaults.cardColors(containerColor = DuoCardDark)
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
                                                    viewModel.editCategory(currentTx, cat.mainCategory, "")
                                                }
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) DuoGreen else DuoCardDark
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
                                    colors = CardDefaults.cardColors(containerColor = DuoBlue.copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = DuoBlue, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("New Category", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = DuoBlue)
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
                                DuolingoPressableButton(
                                    onClick = {
                                        viewModel.editCategory(currentTx, cat.mainCategory, "")
                                        selectedCategoryForSub = null
                                    },
                                    backgroundColor = DuoGreen,
                                    shadowColor = DuoGreenDark,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("General / No Subcategory", color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                cat.subCategories.forEach { sub ->
                                    DuolingoPressableButton(
                                        onClick = {
                                            viewModel.editCategory(currentTx, cat.mainCategory, sub)
                                            selectedCategoryForSub = null
                                        },
                                        backgroundColor = DuoCardDark,
                                        shadowColor = DuoCardShadow,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(sub, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            DuolingoPressableButton(
                                onClick = { selectedCategoryForSub = null },
                                backgroundColor = DuoRed,
                                shadowColor = DuoRedDark,
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
                val totalConfirmXp = (15 * uiState.comboMultiplier) + (if (hasNote) 10 else 0)
                val bonusNoteText = if (hasNote) " (+10 XP Note Bonus!)" else ""

                DuolingoPressableButton(
                    onClick = { viewModel.confirmCategory(currentTx, txNote) },
                    enabled = !isUncategorized,
                    backgroundColor = DuoGreen,
                    shadowColor = DuoGreenDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isUncategorized) {
                        Text("⚠️ Choose Category Above to Confirm", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Confirm (${currentTx.category}) +${totalConfirmXp} XP$bonusNoteText", color = Color.White, fontWeight = FontWeight.Bold)
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
        lower.contains("auto") || lower.contains("gas") || lower.contains("transport") -> "🚗"
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
