package com.randallengineering.finances.ui.screens.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.domain.model.Rule
import com.randallengineering.finances.ui.components.ExpressiveCard
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.core.util.CurrencyFormatter
import com.randallengineering.finances.core.util.DateUtils
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import com.randallengineering.finances.ui.components.RuleEditorDialog
import org.koin.androidx.compose.koinViewModel

@Composable
fun RulesManagementScreen(
    viewModel: RulesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var expandedRuleId by remember { mutableStateOf<String?>(null) }

    if (uiState.isCreatingNewRule || uiState.editingRule != null) {
        RuleEditorDialog(
            initialRule = uiState.editingRule,
            nextPriority = uiState.rules.size + 1,
            onDismiss = { viewModel.closeDialog() },
            onSaveRule = { viewModel.saveRule(it) }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateRuleDialog() },
                shape = Shapes.medium,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Header Info Card
            ExpressiveCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = Shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Rule, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Regex Auto-Categorization Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Rules are evaluated sequentially ascending by priority. The first matching pattern assigns category and stops further rule evaluation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                    )
                }
            }

            if (uiState.isLoading && uiState.rules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.rules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No auto-categorization rules created yet.\nTap '+' below to define your first regex rule!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(
                        items = uiState.rules,
                        key = { _, rule -> rule.id }
                    ) { index, rule ->
                        RuleItemCard(
                            rule = rule,
                            index = index,
                            totalRules = uiState.rules.size,
                            onMoveUp = { viewModel.moveRuleUp(index) },
                            onMoveDown = { viewModel.moveRuleDown(index) },
                            onEdit = { viewModel.openEditRuleDialog(rule) },
                            onDelete = { viewModel.deleteRule(rule.id) }
                        )
                        val matched = uiState.ruleMatches[rule.id].orEmpty()
                        if (expandedRuleId == rule.id) {
                            RuleMatchedCard(
                                matched = matched,
                                onCollapse = { expandedRuleId = null }
                            )
                        } else {
                            OutlinedButton(
                                onClick = { expandedRuleId = rule.id },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (rule.matchCount > 0)
                                        "▾ Show ${rule.matchCount} matched transactions"
                                    else "No matched transactions yet"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RuleItemCard(
    rule: Rule,
    index: Int,
    totalRules: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ExpressiveCard(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Card(
                        shape = Shapes.extraSmall,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(
                            text = "#${rule.priority}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = rule.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalRules - 1,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Regex Pattern
            Card(
                shape = Shapes.small,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text(
                    text = rule.pattern,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Assigns: ${rule.category}${if (rule.subCategory.isNotBlank()) " > ${rule.subCategory}" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "${rule.matchCount} matches",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Expands to show the real transactions this rule matches / has applied to. */
@Composable
private fun RuleMatchedCard(matched: List<Transaction>, onCollapse: () -> Unit) {
    ExpressiveCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Matched transactions (${matched.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                OutlinedButton(onClick = onCollapse) { Text("Collapse") }
            }

            if (matched.isEmpty()) {
                Text(
                    "No transactions currently match this rule. It will apply automatically to new ones during sync.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                matched.forEach { tx ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                tx.originalDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${DateUtils.formatDate(tx.postedEpochSeconds)} · ${tx.category.ifBlank { "Uncategorized" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            CurrencyFormatter.formatWithSign(tx.amount),
                            fontWeight = FontWeight.SemiBold,
                            color = if (tx.amount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
