package com.randallengineering.finances.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.randallengineering.finances.core.theme.FinanceGreen
import com.randallengineering.finances.core.theme.FinanceRed
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.core.util.CurrencyFormatter
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.domain.model.TransactionSplit
import java.util.UUID
import kotlin.math.abs

data class SplitItemState(
    val id: String = UUID.randomUUID().toString(),
    var category: String = "",
    var subCategory: String = "",
    var amountText: String = "",
    var notes: String = ""
)

@Composable
fun SplitTransactionDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onConfirmSplits: (List<TransactionSplit>) -> Unit
) {
    val splits = remember {
        mutableStateListOf<SplitItemState>().apply {
            if (transaction.splits.isNotEmpty()) {
                addAll(transaction.splits.map {
                    SplitItemState(
                        id = it.id,
                        category = it.category,
                        subCategory = it.subCategory,
                        amountText = it.amount.toString(),
                        notes = it.notes
                    )
                })
            } else {
                // Initialize with two split slots
                val half = (transaction.amount / 2.0)
                add(SplitItemState(category = transaction.category, amountText = String.format("%.2f", half)))
                add(SplitItemState(category = "General", amountText = String.format("%.2f", transaction.amount - half)))
            }
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currentTotal = splits.sumOf { it.amountText.toDoubleOrNull() ?: 0.0 }
    val remainingToAllocate = transaction.amount - currentTotal
    val isBalanced = abs(remainingToAllocate) < 0.01

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Shapes.large,
        title = {
            Column {
                Text(
                    text = "Split Transaction",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${transaction.originalDesc} • Total: ${CurrencyFormatter.format(transaction.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Balance Status Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.small,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isBalanced) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isBalanced) "Allocations Balanced" else "Remaining to allocate:",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isBalanced) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = if (isBalanced) CurrencyFormatter.format(currentTotal) else CurrencyFormatter.format(remainingToAllocate),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isBalanced) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        Button(
                            onClick = {
                                splits.add(
                                    SplitItemState(
                                        category = "New Split",
                                        amountText = if (remainingToAllocate > 0) String.format("%.2f", remainingToAllocate) else "0.00"
                                    )
                                )
                            },
                            shape = Shapes.small
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Split")
                            Spacer(Modifier.width(4.dp))
                            Text("Add Split")
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Splits List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(splits) { index, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = Shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Allocation #${index + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (splits.size > 1) {
                                        IconButton(onClick = { splits.removeAt(index) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Remove Split",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = item.category,
                                        onValueChange = { item.category = it; splits[index] = item.copy(category = it) },
                                        label = { Text("Category") },
                                        modifier = Modifier.weight(1.2f),
                                        shape = Shapes.small,
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = item.amountText,
                                        onValueChange = {
                                            item.amountText = it
                                            splits[index] = item.copy(amountText = it)
                                        },
                                        label = { Text("Amount ($)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(0.8f),
                                        shape = Shapes.small,
                                        singleLine = true
                                    )
                                }

                                Spacer(Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = item.notes,
                                    onValueChange = { item.notes = it; splits[index] = item.copy(notes = it) },
                                    label = { Text("Notes / Sub-category") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = Shapes.small,
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isBalanced) {
                        errorMessage = "Splits must sum up exactly to ${CurrencyFormatter.format(transaction.amount)}"
                        return@Button
                    }
                    val domainSplits = splits.map {
                        TransactionSplit(
                            id = it.id,
                            category = it.category.ifBlank { "General" },
                            subCategory = it.subCategory,
                            amount = it.amountText.toDoubleOrNull() ?: 0.0,
                            notes = it.notes
                        )
                    }
                    onConfirmSplits(domainSplits)
                },
                enabled = isBalanced && splits.isNotEmpty(),
                shape = Shapes.small
            ) {
                Text("Save Splits")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = Shapes.small) {
                Text("Cancel")
            }
        }
    )
}
