package com.randallengineering.finances.ui.screens.budgets

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Rule
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.core.util.CurrencyFormatter
import com.randallengineering.finances.domain.model.Budget
import com.randallengineering.finances.domain.model.BudgetCategoryType
import com.randallengineering.finances.domain.model.CategoryHierarchy
import com.randallengineering.finances.domain.model.Rule
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
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val calculatedBudgets = uiState.calculationResult?.calculatedBudgets ?: emptyList()

    // 1. Create / Edit Budget Dialog
    if (uiState.isCreatingBudget || uiState.editingBudget != null) {
        DuolingoBudgetDialog(
            initialBudget = uiState.editingBudget,
            categories = uiState.categories,
            onDismiss = { viewModel.closeDialogs() },
            onSave = { viewModel.saveBudget(it) }
        )
    }

    // 2. Create Category Dialog
    if (uiState.isCreatingCategory) {
        DuolingoCategoryDialog(
            onDismiss = { viewModel.closeDialogs() },
            onSave = { main, sub -> viewModel.addCategory(main, sub) }
        )
    }

    // 3. Add Subcategory Dialog
    if (uiState.selectedMainCategoryForSub != null) {
        DuolingoSubCategoryDialog(
            mainCategory = uiState.selectedMainCategoryForSub!!,
            onDismiss = { viewModel.closeDialogs() },
            onSave = { sub -> viewModel.addSubCategory(uiState.selectedMainCategoryForSub!!, sub) }
        )
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Mascot Coach Prompt
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.large,
                colors = CardDefaults.cardColors(containerColor = DuoGreen.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🦉", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Category & Budget HQ", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = DuoGreenDark)
                        Text(
                            text = "Set limits on your top categories and build your 25+ subcategories hierarchy to unlock Chapter 1 & 2 quest rewards!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Duolingo 3-Tab Bar
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Shapes.medium),
                containerColor = DuoCardDark,
                contentColor = Color.White
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.onTabSelect(0) },
                    text = { Text("📊 Budgets (${calculatedBudgets.size})", fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.onTabSelect(1) },
                    text = { Text("🗂️ Categories (${uiState.categories.size})", fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.onTabSelect(2) },
                    text = { Text("⚡ Rules (${uiState.rules.size})", fontWeight = if (uiState.selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            // Tab Contents
            when (uiState.selectedTab) {
                0 -> BudgetsTabContent(
                    budgets = calculatedBudgets,
                    onAddBudget = { viewModel.openCreateBudgetDialog() },
                    onEditBudget = { viewModel.openEditBudgetDialog(it) },
                    onDeleteBudget = { viewModel.deleteBudget(it) }
                )
                1 -> CategoriesTabContent(
                    categories = uiState.categories,
                    onAddCategory = { viewModel.openCreateCategoryDialog() },
                    onAddSubCategory = { viewModel.openAddSubCategoryDialog(it) },
                    onDeleteSubCategory = { main, sub -> viewModel.deleteSubCategory(main, sub) }
                )
                2 -> RulesTabContent(
                    rules = uiState.rules,
                    onAddRule = { viewModel.openCreateRuleDialog() },
                    onDeleteRule = { viewModel.deleteRule(it) }
                )
            }
        }
    }
}

@Composable
private fun BudgetsTabContent(
    budgets: List<Budget>,
    onAddBudget: () -> Unit,
    onEditBudget: (Budget) -> Unit,
    onDeleteBudget: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DuolingoPressableButton(
                onClick = onAddBudget,
                backgroundColor = DuoGreen,
                shadowColor = DuoGreenDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Add New Category Budget", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        if (budgets.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    shape = Shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📊", style = MaterialTheme.typography.displaySmall)
                        Text("No Budgets Configured", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Create at least 3 category budgets to complete Chapter 1 quest milestones!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        items(budgets, key = { it.id }) { budget ->
            val isOver = budget.spentAmount > budget.targetAmount && budget.targetAmount > 0
            val healthPercent = if (budget.targetAmount > 0) {
                ((budget.targetAmount - budget.spentAmount) / budget.targetAmount).toFloat().coerceIn(0f, 1f)
            } else 1f

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.large,
                colors = CardDefaults.cardColors(containerColor = DuoCardDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(budget.category, fontWeight = FontWeight.Black, color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Text("Limit: ${CurrencyFormatter.format(budget.targetAmount)}/month", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onEditBudget(budget) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = DuoBlue, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { onDeleteBudget(budget.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DuoRed, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // Budget Health Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Spent: ${CurrencyFormatter.format(budget.spentAmount)}", fontWeight = FontWeight.Bold, color = if (isOver) DuoRed else Color.White, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = if (isOver) "⚠️ Over Budget!" else "Health: ${(healthPercent * 100).toInt()}%",
                            fontWeight = FontWeight.Bold,
                            color = if (isOver) DuoRedDark else DuoGreen,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    LinearProgressIndicator(
                        progress = { if (budget.targetAmount > 0) (budget.spentAmount.toFloat() / budget.targetAmount.toFloat()).coerceIn(0f, 1f) else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (isOver) DuoRed else DuoGreen,
                        trackColor = Color(0xFF1E1726)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoriesTabContent(
    categories: List<CategoryHierarchy>,
    onAddCategory: () -> Unit,
    onAddSubCategory: (String) -> Unit,
    onDeleteSubCategory: (String, String) -> Unit
) {
    val totalSubs = categories.sumOf { it.subCategories.size }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DuolingoPressableButton(
                    onClick = onAddCategory,
                    backgroundColor = DuoBlue,
                    shadowColor = DuoBlueDark,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("New Main Category", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quest Progress:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Text("${categories.size}/6 Main  •  $totalSubs/25 Subs", fontWeight = FontWeight.Bold, color = if (categories.size >= 6 && totalSubs >= 25) DuoGreenDark else DuoBlue)
                }
            }
        }

        items(categories, key = { it.mainCategory }) { cat ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.large,
                colors = CardDefaults.cardColors(containerColor = DuoCardDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cat.mainCategory,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )

                        DuolingoPressableButton(
                            onClick = { onAddSubCategory(cat.mainCategory) },
                            backgroundColor = DuoGreen,
                            shadowColor = DuoGreenDark,
                            cornerRadius = 8.dp,
                            shadowHeight = 2.dp
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Sub", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (cat.subCategories.isEmpty()) {
                        Text("No subcategories yet. Tap '+ Add Sub' to add specific items!", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            cat.subCategories.forEach { sub ->
                                Card(
                                    shape = Shapes.small,
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1726))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(sub, color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Delete subcategory",
                                            tint = DuoRed,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { onDeleteSubCategory(cat.mainCategory, sub) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RulesTabContent(
    rules: List<Rule>,
    onAddRule: () -> Unit,
    onDeleteRule: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DuolingoPressableButton(
                onClick = onAddRule,
                backgroundColor = DuoBlue,
                shadowColor = DuoBlueDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Create Auto-Categorization Rule", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        items(rules, key = { it.id }) { rule ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.large,
                colors = CardDefaults.cardColors(containerColor = DuoCardDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("If payee contains \"${rule.pattern}\"", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("➔ Assign ${rule.category}${if (rule.subCategory.isNotBlank()) " > ${rule.subCategory}" else ""}", style = MaterialTheme.typography.bodySmall, color = DuoGreen)
                    }

                    IconButton(onClick = { onDeleteRule(rule.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DuoRed)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Duolingo 3D Dialogs for Budgets & Categories
// -------------------------------------------------------------

@Composable
fun DuolingoBudgetDialog(
    initialBudget: Budget?,
    categories: List<CategoryHierarchy>,
    onDismiss: () -> Unit,
    onSave: (Budget) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(initialBudget?.category ?: categories.firstOrNull()?.mainCategory.orEmpty()) }
    var limitText by remember { mutableStateOf(initialBudget?.targetAmount?.toString() ?: "300.00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialBudget != null) "Edit Budget" else "➕ New Category Budget", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = { selectedCategory = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Monthly Limit ($)") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            DuolingoPressableButton(
                onClick = {
                    val limit = limitText.toDoubleOrNull() ?: 0.0
                    if (selectedCategory.isNotBlank() && limit > 0) {
                        val budget = (initialBudget ?: Budget(id = UUID.randomUUID().toString(), category = selectedCategory, targetAmount = limit)).copy(
                            category = selectedCategory,
                            targetAmount = limit
                        )
                        onSave(budget)
                    }
                },
                backgroundColor = DuoGreen,
                shadowColor = DuoGreenDark,
                cornerRadius = 10.dp
            ) {
                Text("Save Budget", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            DuolingoPressableButton(
                onClick = onDismiss,
                backgroundColor = DuoCardDark,
                shadowColor = DuoCardShadow,
                cornerRadius = 10.dp
            ) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

@Composable
fun DuolingoCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit
) {
    var mainName by remember { mutableStateOf("") }
    var subName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("➕ Add Main Category", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = mainName,
                    onValueChange = { mainName = it },
                    label = { Text("Main Category Name (e.g. Dining, Auto)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subName,
                    onValueChange = { subName = it },
                    label = { Text("Initial Subcategory (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            DuolingoPressableButton(
                onClick = {
                    if (mainName.isNotBlank()) {
                        onSave(mainName.trim(), subName.trim().ifBlank { null })
                    }
                },
                backgroundColor = DuoGreen,
                shadowColor = DuoGreenDark,
                cornerRadius = 10.dp
            ) {
                Text("Create Category", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            DuolingoPressableButton(
                onClick = onDismiss,
                backgroundColor = DuoCardDark,
                shadowColor = DuoCardShadow,
                cornerRadius = 10.dp
            ) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

@Composable
fun DuolingoSubCategoryDialog(
    mainCategory: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var subName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("➕ Add Subcategory to \"$mainCategory\"", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = subName,
                onValueChange = { subName = it },
                label = { Text("Subcategory Name (e.g. Coffee, Gas, Electric)") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            DuolingoPressableButton(
                onClick = {
                    if (subName.isNotBlank()) {
                        onSave(subName.trim())
                    }
                },
                backgroundColor = DuoGreen,
                shadowColor = DuoGreenDark,
                cornerRadius = 10.dp
            ) {
                Text("Add Subcategory", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            DuolingoPressableButton(
                onClick = onDismiss,
                backgroundColor = DuoCardDark,
                shadowColor = DuoCardShadow,
                cornerRadius = 10.dp
            ) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}
