package com.randallengineering.finances.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallSplit
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
    var amountText: String = ""
)

@Composable
fun DuolingoSplitModal(
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
                        amountText = String.format("%.2f", it.amount)
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

    val currentAllocated = splitRows.sumOf { it.amountText.toDoubleOrNull() ?: 0.0 }
    val remaining = totalAmount - currentAllocated
    val isBalanced = abs(remaining) < 0.05

    // Category Selector Popup for Row
    if (selectedRowForCategory != null) {
        val rowIndex = selectedRowForCategory!!
        AlertDialog(
            onDismissRequest = { selectedRowForCategory = null },
            title = { Text("Choose Category for Split Line #${rowIndex + 1}", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories.size) { idx ->
                        val cat = categories[idx]
                        DuolingoPressableButton(
                            onClick = {
                                if (rowIndex in splitRows.indices) {
                                    splitRows[rowIndex] = splitRows[rowIndex].copy(category = cat.mainCategory, subCategory = cat.subCategories.firstOrNull().orEmpty())
                                }
                                selectedRowForCategory = null
                            },
                            backgroundColor = DuoCardDark,
                            shadowColor = DuoCardShadow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(cat.mainCategory, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                DuolingoPressableButton(
                    onClick = { selectedRowForCategory = null },
                    backgroundColor = DuoRed,
                    shadowColor = DuoRedDark,
                    cornerRadius = 10.dp
                ) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CallSplit, contentDescription = null, tint = DuoGreen, modifier = Modifier.size(24.dp))
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
                    DuolingoPressableButton(
                        onClick = {
                            val half = totalAmount / 2.0
                            splitRows.clear()
                            splitRows.add(QuickSplitRow(category = defaultCat, amountText = String.format("%.2f", half)))
                            splitRows.add(QuickSplitRow(category = "Groceries", amountText = String.format("%.2f", totalAmount - half)))
                        },
                        backgroundColor = DuoCardDark,
                        shadowColor = DuoCardShadow,
                        cornerRadius = 10.dp,
                        shadowHeight = 3.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("½ 50/50 Split", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }

                    DuolingoPressableButton(
                        onClick = {
                            val third = totalAmount / 3.0
                            splitRows.clear()
                            splitRows.add(QuickSplitRow(category = defaultCat, amountText = String.format("%.2f", third)))
                            splitRows.add(QuickSplitRow(category = "Groceries", amountText = String.format("%.2f", third)))
                            splitRows.add(QuickSplitRow(category = "Shopping", amountText = String.format("%.2f", totalAmount - (third * 2))))
                        },
                        backgroundColor = DuoCardDark,
                        shadowColor = DuoCardShadow,
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Category Picker Chip
                            Card(
                                modifier = Modifier
                                    .weight(1.3f)
                                    .clip(Shapes.small)
                                    .clickable { selectedRowForCategory = index },
                                colors = CardDefaults.cardColors(containerColor = DuoCardDark)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = row.category,
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

                            // Delete Button
                            if (splitRows.size > 2) {
                                IconButton(
                                    onClick = { splitRows.removeAt(index) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DuoRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // Add Split Row Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DuolingoPressableButton(
                        onClick = {
                            val autoAmount = if (remaining > 0) String.format("%.2f", remaining) else "0.00"
                            splitRows.add(QuickSplitRow(category = "Shopping", amountText = autoAmount))
                        },
                        backgroundColor = DuoBlue,
                        shadowColor = DuoBlueDark,
                        cornerRadius = 10.dp,
                        shadowHeight = 3.dp
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Line", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }

                    // Balance Status Badge
                    Card(
                        shape = Shapes.small,
                        colors = CardDefaults.cardColors(containerColor = if (isBalanced) DuoGreen.copy(alpha = 0.2f) else DuoRed.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = if (isBalanced) "✅ Exact Match!" else if (remaining > 0) "Remaining: ${CurrencyFormatter.format(remaining)}" else "Over by: ${CurrencyFormatter.format(abs(remaining))}",
                            fontWeight = FontWeight.Bold,
                            color = if (isBalanced) DuoGreenDark else DuoRedDark,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            DuolingoPressableButton(
                onClick = {
                    if (isBalanced) {
                        val validSplits = splitRows.mapNotNull { row ->
                            val amt = row.amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                TransactionSplit(
                                    id = row.id,
                                    category = row.category,
                                    subCategory = row.subCategory,
                                    amount = amt,
                                    notes = "${row.category} Split"
                                )
                            } else null
                        }
                        if (validSplits.isNotEmpty()) {
                            onConfirmSplits(validSplits)
                        }
                    }
                },
                enabled = isBalanced,
                backgroundColor = DuoGreen,
                shadowColor = DuoGreenDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("Confirm Split & Earn +35 XP", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}
