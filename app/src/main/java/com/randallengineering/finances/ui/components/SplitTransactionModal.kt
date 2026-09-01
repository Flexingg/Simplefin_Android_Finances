package com.randallengineering.finances.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.randallengineering.finances.core.theme.*
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.core.util.CurrencyFormatter
import com.randallengineering.finances.domain.model.CategoryHierarchy
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.domain.model.TransactionSplit
import java.util.UUID
import kotlin.math.abs

data class QuickSplitRow(
    val id: String = UUID.randomUUID().toString(),
    var category: String = "Dining",
    var subCategory: String = "",
    var amountText: String = "",
    var note: String = ""
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SplitTransactionModal(
    transaction: Transaction,
    categories: List<CategoryHierarchy>,
    onDismiss: () -> Unit,
    onConfirmSplits: (List<TransactionSplit>) -> Unit
) {
    val totalAmount = abs(transaction.amount)
    val defaultCat = if (transaction.category.isNotBlank() && !transaction.category.equals("Uncategorized", ignoreCase = true)) transaction.category else "Dining"
    val defaultSub = transaction.subCategory

    val splitRows = remember {
        mutableStateListOf<QuickSplitRow>().apply {
            if (transaction.splits.isNotEmpty()) {
                addAll(transaction.splits.map {
                    QuickSplitRow(
                        id = it.id,
                        category = it.category,
                        subCategory = it.subCategory,
                        amountText = String.format("%.2f", it.amount),
                        note = it.notes
                    )
                })
            } else {
                val half = totalAmount / 2.0
                add(QuickSplitRow(category = defaultCat, subCategory = defaultSub, amountText = String.format("%.2f", half)))
                add(QuickSplitRow(category = "Groceries", subCategory = "", amountText = String.format("%.2f", totalAmount - half)))
            }
        }
    }

    var selectedRowForCategory by remember { mutableStateOf<Int?>(null) }
    var selectedCategoryForSub by remember { mutableStateOf<CategoryHierarchy?>(null) }
    var showAddCustomCategoryDialog by remember { mutableStateOf(false) }

    val currentAllocated = splitRows.sumOf { it.amountText.toDoubleOrNull() ?: 0.0 }
    val remaining = totalAmount - currentAllocated
    val isBalanced = abs(remaining) < 0.05

    // 1. Subcategory Selection Dialog for Split Line
    if (selectedCategoryForSub != null && selectedRowForCategory != null) {
        val cat = selectedCategoryForSub!!
        val rowIndex = selectedRowForCategory!!

        AlertDialog(
            onDismissRequest = { selectedCategoryForSub = null },
            title = { Text("Select Subcategory for ${cat.mainCategory}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Option to choose main category only
                    FinanceButton(
                        onClick = {
                            if (rowIndex in splitRows.indices) {
                                splitRows[rowIndex] = splitRows[rowIndex].copy(
                                    category = cat.mainCategory,
                                    subCategory = ""
                                )
                            }
                            selectedCategoryForSub = null
                            selectedRowForCategory = null
                        },
                        backgroundColor = FinanceCardDark,
                        shadowColor = FinanceCardShadow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("General / All ${cat.mainCategory}", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (cat.subCategories.isNotEmpty()) {
                        Text("Specific Subcategories:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            cat.subCategories.forEach { sub ->
                                FinanceButton(
                                    onClick = {
                                        if (rowIndex in splitRows.indices) {
                                            splitRows[rowIndex] = splitRows[rowIndex].copy(
                                                category = cat.mainCategory,
                                                subCategory = sub
                                            )
                                        }
                                        selectedCategoryForSub = null
                                        selectedRowForCategory = null
                                    },
                                    backgroundColor = FinanceGreen,
                                    shadowColor = FinanceGreenDark,
                                    cornerRadius = 8.dp,
                                    shadowHeight = 2.dp
                                ) {
                                    Text(sub, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                FinanceButton(
                    onClick = { selectedCategoryForSub = null },
                    backgroundColor = FinanceCardDark,
                    shadowColor = FinanceCardShadow,
                    cornerRadius = 10.dp
                ) {
                    Text("Back", color = Color.White)
                }
            }
        )
    }

    // 2. Full Main Category Selection Modal for Split Line
    if (selectedRowForCategory != null && selectedCategoryForSub == null) {
        val rowIndex = selectedRowForCategory!!

        AlertDialog(
            onDismissRequest = { selectedRowForCategory = null },
            title = { Text("Choose Category for Split Line #${rowIndex + 1}", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("Select Main Category or Subcategory:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    items(categories.size) { idx ->
                        val cat = categories[idx]
                        val emoji = getCategoryEmoji(cat.mainCategory)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(Shapes.medium)
                                .clickable {
                                    if (cat.subCategories.isNotEmpty()) {
                                        selectedCategoryForSub = cat
                                    } else {
                                        if (rowIndex in splitRows.indices) {
                                            splitRows[rowIndex] = splitRows[rowIndex].copy(
                                                category = cat.mainCategory,
                                                subCategory = ""
                                            )
                                        }
                                        selectedRowForCategory = null
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = FinanceCardDark)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$emoji ${cat.mainCategory}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                if (cat.subCategories.isNotEmpty()) {
                                    Text(
                                        text = "${cat.subCategories.size} subs ➔",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FinanceGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                FinanceButton(
                    onClick = { selectedRowForCategory = null },
                    backgroundColor = FinanceCardDark,
                    shadowColor = FinanceCardShadow,
                    cornerRadius = 10.dp
                ) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    // Main Split Modal
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = null, tint = FinanceGreen, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Quick Split (${CurrencyFormatter.format(totalAmount)})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Preset Quick Split Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FinanceButton(
                        onClick = {
                            val half = totalAmount / 2.0
                            splitRows.clear()
                            splitRows.add(QuickSplitRow(category = defaultCat, subCategory = defaultSub, amountText = String.format("%.2f", half)))
                            splitRows.add(QuickSplitRow(category = "Groceries", subCategory = "", amountText = String.format("%.2f", totalAmount - half)))
                        },
                        backgroundColor = FinanceCardDark,
                        shadowColor = FinanceCardShadow,
                        cornerRadius = 10.dp,
                        shadowHeight = 3.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("½ 50/50 Split", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }

                    FinanceButton(
                        onClick = {
                            val third = totalAmount / 3.0
                            splitRows.clear()
                            splitRows.add(QuickSplitRow(category = defaultCat, subCategory = defaultSub, amountText = String.format("%.2f", third)))
                            splitRows.add(QuickSplitRow(category = "Groceries", subCategory = "", amountText = String.format("%.2f", third)))
                            splitRows.add(QuickSplitRow(category = "Shopping", subCategory = "", amountText = String.format("%.2f", totalAmount - (third * 2))))
                        },
                        backgroundColor = FinanceCardDark,
                        shadowColor = FinanceCardShadow,
                        cornerRadius = 10.dp,
                        shadowHeight = 3.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("⅓ 3-Way Split", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                }

                HorizontalDivider()

                // Split Rows List
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    splitRows.forEachIndexed { index, row ->
                        val emoji = getCategoryEmoji(row.category)
                        val displayCatLabel = if (row.subCategory.isNotBlank()) "$emoji ${row.category} > ${row.subCategory}" else "$emoji ${row.category}"

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(Shapes.small)
                                .background(FinanceCardDark.copy(alpha = 0.5f))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Category Picker Chip (Tap to pick category & subcategory)
                                Card(
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .clip(Shapes.small)
                                        .clickable { selectedRowForCategory = index },
                                    colors = CardDefaults.cardColors(containerColor = FinanceCardDark)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = displayCatLabel,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // Amount Field
                                OutlinedTextField(
                                    value = row.amountText,
                                    onValueChange = { newText ->
                                        splitRows[index] = row.copy(amountText = newText)
                                    },
                                    prefix = { Text("$") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                // Delete Row
                                if (splitRows.size > 2) {
                                    IconButton(
                                        onClick = { splitRows.removeAt(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Row", tint = FinanceRed)
                                    }
                                }
                            }

                            // Optional Note per Split Line with Note Bonus XP indicator
                            OutlinedTextField(
                                value = row.note,
                                onValueChange = { newNote ->
                                    splitRows[index] = row.copy(note = newNote)
                                },
                                placeholder = { Text("Item note (optional)", style = MaterialTheme.typography.labelSmall) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Add Custom Line & Auto Balance Remaining
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FinanceButton(
                        onClick = {
                            val rem = if (remaining > 0) String.format("%.2f", remaining) else "0.00"
                            splitRows.add(QuickSplitRow(category = "Shopping", subCategory = "", amountText = rem))
                        },
                        backgroundColor = FinanceCardDark,
                        shadowColor = FinanceCardShadow,
                        cornerRadius = 8.dp,
                        shadowHeight = 2.dp
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Line", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }

                    if (remaining > 0.01) {
                        FinanceButton(
                            onClick = {
                                if (splitRows.isNotEmpty()) {
                                    val last = splitRows.last()
                                    val lastVal = last.amountText.toDoubleOrNull() ?: 0.0
                                    splitRows[splitRows.lastIndex] = last.copy(amountText = String.format("%.2f", lastVal + remaining))
                                }
                            },
                            backgroundColor = FinanceAmber,
                            shadowColor = FinanceAmberDark,
                            cornerRadius = 8.dp,
                            shadowHeight = 2.dp
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Balance +${CurrencyFormatter.format(remaining)}", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Balance Status Bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isBalanced) FinanceGreen.copy(alpha = 0.15f) else FinanceRed.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBalanced) "✅ Fully Balanced ($${String.format("%.2f", currentAllocated)})" else "Remaining: ${CurrencyFormatter.format(remaining)}",
                            fontWeight = FontWeight.Bold,
                            color = if (isBalanced) FinanceGreenDark else FinanceRedDark,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            FinanceButton(
                onClick = {
                    if (isBalanced) {
                        val splits = splitRows.mapNotNull { row ->
                            val amount = row.amountText.toDoubleOrNull() ?: 0.0
                            if (amount > 0) {
                                TransactionSplit(
                                    id = row.id,
                                    category = row.category,
                                    subCategory = row.subCategory,
                                    amount = amount,
                                    notes = row.note
                                )
                            } else null
                        }
                        if (splits.isNotEmpty()) {
                            onConfirmSplits(splits)
                        }
                    }
                },
                enabled = isBalanced,
                backgroundColor = if (isBalanced) FinanceGreen else Color.Gray,
                shadowColor = if (isBalanced) FinanceGreenDark else Color.DarkGray,
                cornerRadius = 10.dp
            ) {
                Text("Confirm Split", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            FinanceButton(
                onClick = onDismiss,
                backgroundColor = FinanceCardDark,
                shadowColor = FinanceCardShadow,
                cornerRadius = 10.dp
            ) {
                Text("Cancel", color = Color.White)
            }
        }
    )
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
        lower.contains("home") || lower.contains("house") || lower.contains("rent") || lower.contains("mortgage") -> "🏠"
        lower.contains("travel") || lower.contains("hotel") || lower.contains("flight") -> "✈️"
        lower.contains("kid") || lower.contains("baby") || lower.contains("toy") -> "🧸"
        else -> "🏷️"
    }
}
