package com.randallengineering.finances.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.randallengineering.finances.core.theme.FinanceGreen
import com.randallengineering.finances.core.theme.FinanceRed
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.core.util.CurrencyFormatter
import com.randallengineering.finances.domain.model.CategoryHierarchy
import com.randallengineering.finances.domain.model.Rule
import com.randallengineering.finances.domain.model.Transaction
import java.util.UUID

@Composable
fun GenerateRuleFromTransactionDialog(
    transaction: Transaction,
    categories: List<CategoryHierarchy>,
    nextPriority: Int = 1,
    onDismiss: () -> Unit,
    onSaveRule: (Rule) -> Unit
) {
    // Smart keyword extraction
    val cleanedDesc = remember(transaction) {
        val raw = if (transaction.payee.isNotBlank()) transaction.payee else transaction.originalDesc
        // Clean out numbers, store IDs, punctuation
        val cleaned = raw.replace(Regex("[#0-9]+"), "")
            .replace(Regex("[^a-zA-Z\\s]"), "")
            .trim()
            .split("\\s+".toRegex())
            .filter { it.length > 2 }
            .take(2)
            .joinToString(" ")
        if (cleaned.isNotBlank()) cleaned else raw
    }

    var ruleName by remember { mutableStateOf("${cleanedDesc.ifBlank { "Auto-Rule" }} Rule") }
    var pattern by remember { mutableStateOf("(?i).*${cleanedDesc.trim()}.*") }
    var selectedCategory by remember { mutableStateOf(transaction.category.ifBlank { "Home" }) }
    var selectedSubCategory by remember { mutableStateOf(transaction.subCategory) }

    var isCategoryPickerOpen by remember { mutableStateOf(false) }

    // Amount constraints
    var applyAmountFilter by remember { mutableStateOf(false) }
    var minAmountText by remember { mutableStateOf("") }
    var maxAmountText by remember { mutableStateOf("") }

    val isRegexValid = remember(pattern) {
        try {
            Regex(pattern, setOf(RegexOption.IGNORE_CASE))
            true
        } catch (e: Exception) {
            false
        }
    }

    val rulePreview = Rule(
        id = UUID.randomUUID().toString(),
        name = ruleName,
        priority = nextPriority,
        pattern = pattern,
        minAmount = if (applyAmountFilter) minAmountText.toDoubleOrNull() else null,
        maxAmount = if (applyAmountFilter) maxAmountText.toDoubleOrNull() else null,
        category = selectedCategory,
        subCategory = selectedSubCategory,
        isActive = true
    )

    val isMatchSuccess = remember(pattern, selectedCategory, applyAmountFilter, minAmountText, maxAmountText) {
        if (!isRegexValid || pattern.isBlank()) false
        else rulePreview.matches(transaction.originalDesc, transaction.amount)
    }

    if (isCategoryPickerOpen) {
        CategoryPickerDialog(
            categories = categories,
            initialMainCategory = selectedCategory,
            initialSubCategory = selectedSubCategory,
            onDismiss = { isCategoryPickerOpen = false },
            onCategorySelected = { main, sub ->
                selectedCategory = main
                selectedSubCategory = sub
                isCategoryPickerOpen = false
            },
            onAddNewCategory = { _, _ -> }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Shapes.large,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Generate Auto-Categorization Rule",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Create a rule based on '${transaction.originalDesc}' to automatically categorize past and future transactions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    label = { Text("Rule Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.small,
                    singleLine = true
                )

                // Pattern Templates
                Text("Regex Pattern Templates", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = pattern == "(?i).*${cleanedDesc.trim()}.*",
                        onClick = { pattern = "(?i).*${cleanedDesc.trim()}.*" },
                        label = { Text("Contains '${cleanedDesc.take(12)}'") },
                        shape = Shapes.small
                    )
                    FilterChip(
                        selected = pattern == "(?i)^${cleanedDesc.trim()}.*",
                        onClick = { pattern = "(?i)^${cleanedDesc.trim()}.*" },
                        label = { Text("Starts with") },
                        shape = Shapes.small
                    )
                }

                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Regex Pattern") },
                    isError = !isRegexValid,
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.small,
                    singleLine = true
                )

                // Category & Subcategory Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.small,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Assigned Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "$selectedCategory${if (selectedSubCategory.isNotBlank()) " > $selectedSubCategory" else ""}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = { isCategoryPickerOpen = true },
                            shape = Shapes.small
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Change")
                        }
                    }
                }

                // Amount constraint toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = !applyAmountFilter,
                        onClick = { applyAmountFilter = false },
                        label = { Text("Any Amount") },
                        shape = Shapes.small
                    )
                    FilterChip(
                        selected = applyAmountFilter,
                        onClick = {
                            applyAmountFilter = true
                            minAmountText = (transaction.amount - 5.0).coerceAtLeast(0.0).toString()
                            maxAmountText = (transaction.amount + 5.0).toString()
                        },
                        label = { Text("Limit by Amount Range") },
                        shape = Shapes.small
                    )
                }

                AnimatedVisibility(visible = applyAmountFilter) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = minAmountText,
                            onValueChange = { minAmountText = it },
                            label = { Text("Min Amount ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = Shapes.small,
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = maxAmountText,
                            onValueChange = { maxAmountText = it },
                            label = { Text("Max Amount ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            shape = Shapes.small,
                            singleLine = true
                        )
                    }
                }

                // Match Status Indicator
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.small,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMatchSuccess) FinanceGreen.copy(alpha = 0.15f) else FinanceRed.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isMatchSuccess) "✅ Confirmed: Matches this transaction!" else "❌ Pattern does not match this transaction",
                            fontWeight = FontWeight.Bold,
                            color = if (isMatchSuccess) FinanceGreen else FinanceRed
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveRule(rulePreview)
                },
                enabled = isRegexValid && pattern.isNotBlank() && selectedCategory.isNotBlank(),
                shape = Shapes.small
            ) {
                Text("Save & Apply Rule")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = Shapes.small) {
                Text("Cancel")
            }
        }
    )
}
