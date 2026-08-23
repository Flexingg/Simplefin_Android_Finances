package com.randallengineering.finances.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.randallengineering.finances.core.theme.FinanceGreen
import com.randallengineering.finances.core.theme.FinanceRed
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.domain.model.Rule
import java.util.UUID

@Composable
fun RuleEditorDialog(
    initialRule: Rule? = null,
    nextPriority: Int = 1,
    onDismiss: () -> Unit,
    onSaveRule: (Rule) -> Unit
) {
    var name by remember { mutableStateOf(initialRule?.name.orEmpty()) }
    var pattern by remember { mutableStateOf(initialRule?.pattern.orEmpty()) }
    var category by remember { mutableStateOf(initialRule?.category.orEmpty()) }
    var subCategory by remember { mutableStateOf(initialRule?.subCategory.orEmpty()) }
    var minAmountText by remember { mutableStateOf(initialRule?.minAmount?.toString().orEmpty()) }
    var maxAmountText by remember { mutableStateOf(initialRule?.maxAmount?.toString().orEmpty()) }
    var isActive by remember { mutableStateOf(initialRule?.isActive ?: true) }

    // Live test preview sandbox
    var testDescription by remember { mutableStateOf("TARGET 0842 PURCHASE") }
    var testAmountText by remember { mutableStateOf("45.50") }

    val testAmount = testAmountText.toDoubleOrNull() ?: 0.0
    val testRule = Rule(
        id = initialRule?.id ?: "",
        name = name,
        priority = initialRule?.priority ?: nextPriority,
        pattern = pattern,
        minAmount = minAmountText.toDoubleOrNull(),
        maxAmount = maxAmountText.toDoubleOrNull(),
        category = category,
        subCategory = subCategory,
        isActive = isActive
    )

    val isRegexValid = remember(pattern) {
        try {
            Regex(pattern, setOf(RegexOption.IGNORE_CASE))
            true
        } catch (e: Exception) {
            false
        }
    }

    val isMatchSuccess = remember(pattern, testDescription, testAmount, minAmountText, maxAmountText) {
        if (!isRegexValid || pattern.isBlank()) false
        else testRule.matches(testDescription, testAmount)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Shapes.large,
        title = {
            Text(
                text = if (initialRule == null) "Create Auto-Categorization Rule" else "Edit Rule",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name") },
                    placeholder = { Text("e.g. Target Groceries") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.small,
                    singleLine = true
                )

                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Regex Pattern (?i enabled)") },
                    placeholder = { Text("e.g. (?i).*target.*") },
                    isError = !isRegexValid && pattern.isNotBlank(),
                    supportingText = {
                        if (!isRegexValid && pattern.isNotBlank()) {
                            Text("Invalid Regex syntax", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.small,
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        placeholder = { Text("Groceries") },
                        modifier = Modifier.weight(1f),
                        shape = Shapes.small,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = subCategory,
                        onValueChange = { subCategory = it },
                        label = { Text("Sub-category") },
                        placeholder = { Text("Household") },
                        modifier = Modifier.weight(1f),
                        shape = Shapes.small,
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minAmountText,
                        onValueChange = { minAmountText = it },
                        label = { Text("Min Amount (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = Shapes.small,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = maxAmountText,
                        onValueChange = { maxAmountText = it },
                        label = { Text("Max Amount (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = Shapes.small,
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Rule", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }

                // Interactive Regex Test Sandbox
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "🧪 Live Regex Test Preview",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = testDescription,
                            onValueChange = { testDescription = it },
                            label = { Text("Sample Transaction Description") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = Shapes.small,
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = testAmountText,
                            onValueChange = { testAmountText = it },
                            label = { Text("Sample Amount ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = Shapes.small,
                            singleLine = true
                        )

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
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isMatchSuccess) "✅ MATCH: Assigns '$category'" else "❌ NO MATCH",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMatchSuccess) FinanceGreen else FinanceRed
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rule = Rule(
                        id = initialRule?.id ?: UUID.randomUUID().toString(),
                        name = name.ifBlank { "Untitled Rule" },
                        priority = initialRule?.priority ?: nextPriority,
                        pattern = pattern,
                        minAmount = minAmountText.toDoubleOrNull(),
                        maxAmount = maxAmountText.toDoubleOrNull(),
                        category = category.ifBlank { "Uncategorized" },
                        subCategory = subCategory,
                        matchCount = initialRule?.matchCount ?: 0L,
                        isActive = isActive
                    )
                    onSaveRule(rule)
                },
                enabled = isRegexValid && pattern.isNotBlank() && category.isNotBlank(),
                shape = Shapes.small
            ) {
                Text("Save Rule")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = Shapes.small) {
                Text("Cancel")
            }
        }
    )
}
