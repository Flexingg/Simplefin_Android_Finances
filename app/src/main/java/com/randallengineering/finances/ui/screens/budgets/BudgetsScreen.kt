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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Savings
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.randallengineering.finances.domain.model.Goal
import com.randallengineering.finances.domain.model.MainCategoryBudgetGroup
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel = koinViewModel(),
    initialTab: Int = 0,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val calc = uiState.calculationResult
    val mainCategoryGroups = calc?.mainCategoryGroups ?: emptyList()
    val totalMtdIncome = calc?.totalMtdIncome ?: 0.0
    val hasIncomeCategory = uiState.categories.any { it.mainCategory.equals(uiState.incomeCategory, ignoreCase = true) }

    androidx.compose.runtime.LaunchedEffect(initialTab) {
        if (initialTab in 0..3) {
            viewModel.onTabSelect(initialTab)
        }
    }

    // 1. Create / Edit Budget Dialog
    if (uiState.isCreatingBudget || uiState.editingBudget != null) {
        DuolingoSubcategoryBudgetDialog(
            initialBudget = uiState.editingBudget,
            categories = uiState.categories,
            totalMtdIncome = totalMtdIncome,
            onDismiss = { viewModel.closeDialogs() },
            onSave = { viewModel.saveBudget(it) }
        )
    }

    // 2. Create / Edit Goal Dialog
    if (uiState.isCreatingGoal || uiState.editingGoal != null) {
        DuolingoGoalDialog(
            initialGoal = uiState.editingGoal,
            onDismiss = { viewModel.closeDialogs() },
            onSave = { viewModel.saveGoal(it) }
        )
    }

    // 3. Change Income Category Dialog
    if (uiState.isChangingIncomeCategory) {
        DuolingoIncomeCategoryDialog(
            currentIncomeCategory = uiState.incomeCategory,
            categories = uiState.categories,
            onDismiss = { viewModel.closeDialogs() },
            onSelect = { viewModel.setIncomeCategory(it) },
            onCreateIncomeCategory = {
                viewModel.addCategory("Income", "Salary & Wages")
                viewModel.setIncomeCategory("Income")
            }
        )
    }

    // 4. Create Main Category Dialog
    if (uiState.isCreatingCategory) {
        DuolingoCategoryDialog(
            onDismiss = { viewModel.closeDialogs() },
            onSave = { main, sub -> viewModel.addCategory(main, sub) }
        )
    }

    // 5. Edit Main Category Dialog
    if (uiState.editingMainCategory != null) {
        DuolingoRenameCategoryDialog(
            currentName = uiState.editingMainCategory!!,
            title = "✏️ Rename Main Category",
            onDismiss = { viewModel.closeDialogs() },
            onSave = { newName -> viewModel.renameMainCategory(uiState.editingMainCategory!!, newName) }
        )
    }

    // 6. Edit Subcategory Dialog
    if (uiState.editingSubCategory != null) {
        val (mainCat, oldSub) = uiState.editingSubCategory!!
        DuolingoRenameCategoryDialog(
            currentName = oldSub,
            title = "✏️ Rename Subcategory in \"$mainCat\"",
            onDismiss = { viewModel.closeDialogs() },
            onSave = { newSub -> viewModel.renameSubCategory(mainCat, oldSub, newSub) }
        )
    }

    // 7. Add Subcategory Dialog
    if (uiState.selectedMainCategoryForSub != null) {
        DuolingoSubCategoryDialog(
            mainCategory = uiState.selectedMainCategoryForSub!!,
            onDismiss = { viewModel.closeDialogs() },
            onSave = { sub -> viewModel.addSubCategory(uiState.selectedMainCategoryForSub!!, sub) }
        )
    }

    // 8. Create / Edit Rule Dialog
    if (uiState.isCreatingRule || uiState.editingRule != null) {
        DuolingoRuleDialog(
            initialRule = uiState.editingRule,
            categories = uiState.categories,
            nextPriority = uiState.rules.size + 1,
            onCalculateMatches = { pattern, min, max -> viewModel.calculateMatchesForPattern(pattern, min, max) },
            onDismiss = { viewModel.closeDialogs() },
            onSave = { viewModel.saveRule(it) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Income Category Status Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shapes.large)
                .clickable { viewModel.openChangeIncomeCategoryDialog() },
            shape = Shapes.large,
            colors = CardDefaults.cardColors(containerColor = if (hasIncomeCategory) DuoGreen.copy(alpha = 0.15f) else DuoGold.copy(alpha = 0.18f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text("💰", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Income Category: ${uiState.incomeCategory}", fontWeight = FontWeight.Bold, color = if (hasIncomeCategory) DuoGreenDark else DuoGoldDark, style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = if (hasIncomeCategory) "MTD Income: ${CurrencyFormatter.format(totalMtdIncome)}" else "⚠️ Tap to set up your Income category",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DuolingoPressableButton(
                    onClick = { viewModel.openChangeIncomeCategoryDialog() },
                    backgroundColor = if (hasIncomeCategory) DuoGreen else DuoGold,
                    shadowColor = if (hasIncomeCategory) DuoGreenDark else DuoGoldDark,
                    cornerRadius = 8.dp,
                    shadowHeight = 2.dp
                ) {
                    Text(if (hasIncomeCategory) "Change" else "Set Up", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Rule Execution Banner (if any)
        if (uiState.ruleExecutionMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.medium,
                colors = CardDefaults.cardColors(containerColor = DuoBlue.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("⚡ ${uiState.ruleExecutionMessage}", color = DuoBlueDark, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = DuoBlueDark,
                        modifier = Modifier.size(16.dp).clickable { viewModel.closeDialogs() }
                    )
                }
            }
        }

        // Duolingo 4-Tab Bar (Budgets, Goals, Categories, Rules)
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
                text = { Text("📊 Budgets", fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.labelSmall) }
            )
            Tab(
                selected = uiState.selectedTab == 1,
                onClick = { viewModel.onTabSelect(1) },
                text = { Text("🎯 Goals (${uiState.goals.size})", fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.labelSmall) }
            )
            Tab(
                selected = uiState.selectedTab == 2,
                onClick = { viewModel.onTabSelect(2) },
                text = { Text("🗂️ Categories", fontWeight = if (uiState.selectedTab == 2) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.labelSmall) }
            )
            Tab(
                selected = uiState.selectedTab == 3,
                onClick = { viewModel.onTabSelect(3) },
                text = { Text("⚡ Rules (${uiState.rules.size})", fontWeight = if (uiState.selectedTab == 3) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.labelSmall) }
            )
        }

        // Tab Contents
        when (uiState.selectedTab) {
            0 -> SubcategoryBudgetsTabContent(
                mainCategoryGroups = mainCategoryGroups,
                totalMtdIncome = totalMtdIncome,
                onAddBudget = { viewModel.openCreateBudgetDialog() },
                onEditBudget = { viewModel.openEditBudgetDialog(it) },
                onDeleteBudget = { viewModel.deleteBudget(it) }
            )
            1 -> GoalsTabContent(
                goals = uiState.goals,
                onAddGoal = { viewModel.openCreateGoalDialog() },
                onEditGoal = { viewModel.openEditGoalDialog(it) },
                onDeleteGoal = { viewModel.deleteGoal(it) },
                onAddContribution = { id, amount -> viewModel.addGoalContribution(id, amount) }
            )
            2 -> CategoriesTabContent(
                categories = uiState.categories,
                onAddCategory = { viewModel.openCreateCategoryDialog() },
                onEditCategory = { viewModel.openEditMainCategoryDialog(it) },
                onDeleteCategory = { viewModel.deleteMainCategory(it) },
                onAddSubCategory = { viewModel.openAddSubCategoryDialog(it) },
                onEditSubCategory = { main, sub -> viewModel.openEditSubCategoryDialog(main, sub) },
                onDeleteSubCategory = { main, sub -> viewModel.deleteSubCategory(main, sub) }
            )
            3 -> RulesTabContent(
                rules = uiState.rules,
                isAutoRunEnabled = uiState.isAutoRunRulesEnabled,
                onToggleAutoRun = { viewModel.toggleAutoRunRules(it) },
                onRunAllRules = { viewModel.runAllRules() },
                onAddRule = { viewModel.openCreateRuleDialog() },
                onEditRule = { viewModel.openEditRuleDialog(it) },
                onDeleteRule = { viewModel.deleteRule(it) }
            )
        }
    }
}

// -------------------------------------------------------------
// 1. Budgets Tab (Subcategories + Income Percent Conversions)
// -------------------------------------------------------------

@Composable
private fun SubcategoryBudgetsTabContent(
    mainCategoryGroups: List<MainCategoryBudgetGroup>,
    totalMtdIncome: Double,
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
                Text("Add Subcategory Budget ($ or % of Income)", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        if (mainCategoryGroups.isEmpty()) {
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
                        Text("No Subcategory Budgets Set", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Budget against specific subcategories (e.g. Fast Food, Gas, Rent) to automatically calculate main category totals and income percentages!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }

        items(mainCategoryGroups, key = { it.mainCategory }) { group ->
            var isExpanded by remember { mutableStateOf(true) }

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
                    // Main Category Rollup Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${getCategoryEmoji(group.mainCategory)} ${group.mainCategory}",
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "(${group.subBudgets.size} subcategories)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                            val groupPercentIncome = if (totalMtdIncome > 0) (group.totalTargetAmount / totalMtdIncome * 100.0) else 0.0
                            Text(
                                text = "Total Limit: ${CurrencyFormatter.format(group.totalTargetAmount)}/mo (Auto-Sum) • ${String.format("%.1f", groupPercentIncome)}% of Income",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (group.isOverBudget) "⚠️ Over!" else "Health: ${(group.healthPercent * 100).toInt()}%",
                                fontWeight = FontWeight.Bold,
                                color = if (group.isOverBudget) DuoRed else DuoGreen,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }

                    // Main Category Combined Health Bar
                    LinearProgressIndicator(
                        progress = { if (group.totalTargetAmount > 0) (group.totalSpentAmount.toFloat() / group.totalTargetAmount.toFloat()).coerceIn(0f, 1f) else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (group.isOverBudget) DuoRed else DuoGreen,
                        trackColor = Color(0xFF1E1726)
                    )

                    // Subcategory Budgets List
                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                            group.subBudgets.forEach { subBudget ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = Shapes.medium,
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1726))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (subBudget.subCategory.isNotBlank()) subBudget.subCategory else "General / All",
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            // Compute reciprocal metrics: Amount <-> Income %
                                            val dualMetricLabel = if (subBudget.categoryType == BudgetCategoryType.PERCENT_INCOME) {
                                                val percent = subBudget.incomePercentage ?: 0.0
                                                "${percent}% of Income (≈ ${CurrencyFormatter.format(subBudget.targetAmount)}/mo)"
                                            } else {
                                                val percentOfIncome = if (totalMtdIncome > 0) (subBudget.targetAmount / totalMtdIncome * 100.0) else 0.0
                                                "${CurrencyFormatter.format(subBudget.targetAmount)}/mo (${String.format("%.1f", percentOfIncome)}% of Income)"
                                            }
                                            Text(
                                                text = "Spent: ${CurrencyFormatter.format(subBudget.spentAmount)} / $dualMetricLabel",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (subBudget.isOverBudget) DuoRed else Color.White.copy(alpha = 0.7f)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { onEditBudget(subBudget) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = DuoBlue, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(onClick = { onDeleteBudget(subBudget.id) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DuoRed, modifier = Modifier.size(16.dp))
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
    }
}

// -------------------------------------------------------------
// 2. Goals Tab (Duolingo 3D Savings & Wants Vault)
// -------------------------------------------------------------

@Composable
private fun GoalsTabContent(
    goals: List<Goal>,
    onAddGoal: () -> Unit,
    onEditGoal: (Goal) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onAddContribution: (String, Double) -> Unit
) {
    val totalSaved = goals.sumOf { it.currentAmount }
    val totalTarget = goals.sumOf { it.targetAmount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Vault Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.large,
                colors = CardDefaults.cardColors(containerColor = DuoGold.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🏦", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Savings Vault & Targets", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = DuoGoldDark)
                        Text(
                            text = "Total Saved: ${CurrencyFormatter.format(totalSaved)} / ${CurrencyFormatter.format(totalTarget)} (${if (totalTarget > 0) ((totalSaved / totalTarget) * 100).toInt() else 0}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            DuolingoPressableButton(
                onClick = onAddGoal,
                backgroundColor = DuoGold,
                shadowColor = DuoGoldDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Create New Savings / Wants Goal", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        if (goals.isEmpty()) {
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
                        Text("🎯", style = MaterialTheme.typography.displaySmall)
                        Text("No Financial Goals Created", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Set up an Emergency Fund, Vacation, or Major Purchase goal to earn Chapter 1 & 3 quest rewards!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }

        items(goals, key = { it.id }) { goal ->
            val progressPercent = goal.progressPercent.toFloat() / 100f
            val targetDateFormatted = try {
                val instant = Instant.ofEpochSecond(goal.targetEpochSeconds)
                val zdt = instant.atZone(ZoneId.systemDefault())
                zdt.format(DateTimeFormatter.ofPattern("MMM yyyy"))
            } catch (e: Exception) {
                "Target Goal"
            }

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
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🎯 ${goal.title}",
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (goal.isCompleted) {
                                    Spacer(Modifier.width(6.dp))
                                    Text("✅ Completed!", style = MaterialTheme.typography.labelSmall, color = DuoGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = "Category: ${goal.category} • Target Date: $targetDateFormatted",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onEditGoal(goal) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = DuoBlue, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onDeleteGoal(goal.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DuoRed, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Progress Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${CurrencyFormatter.format(goal.currentAmount)} / ${CurrencyFormatter.format(goal.targetAmount)}",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${goal.progressPercent.toInt()}% Saved",
                            fontWeight = FontWeight.Bold,
                            color = if (goal.isCompleted) DuoGreen else DuoGold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progressPercent.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (goal.isCompleted) DuoGreen else DuoGold,
                        trackColor = Color(0xFF1E1726)
                    )

                    // Monthly Pacing Advice
                    val monthlyTarget = goal.calculateMonthlyTargetSaving()
                    if (!goal.isCompleted && monthlyTarget > 0) {
                        Text(
                            text = "💡 Save ≈ ${CurrencyFormatter.format(monthlyTarget)}/month to hit target on time!",
                            style = MaterialTheme.typography.bodySmall,
                            color = DuoGoldDark,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 1-Tap Deposit Shortcuts
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Add Deposit:", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))

                        listOf(25.0, 50.0, 100.0).forEach { amount ->
                            DuolingoPressableButton(
                                onClick = { onAddContribution(goal.id, amount) },
                                backgroundColor = DuoGreen,
                                shadowColor = DuoGreenDark,
                                cornerRadius = 8.dp,
                                shadowHeight = 2.dp
                            ) {
                                Text("+ $${amount.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. Categories Tab (Full Main & Sub Editing & Renaming)
// -------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoriesTabContent(
    categories: List<CategoryHierarchy>,
    onAddCategory: () -> Unit,
    onEditCategory: (String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onAddSubCategory: (String) -> Unit,
    onEditSubCategory: (String, String) -> Unit,
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
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${getCategoryEmoji(cat.mainCategory)} ${cat.mainCategory}",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Spacer(Modifier.width(6.dp))
                            IconButton(onClick = { onEditCategory(cat.mainCategory) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit category", tint = DuoBlue, modifier = Modifier.size(14.dp))
                            }
                            IconButton(onClick = { onDeleteCategory(cat.mainCategory) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete category", tint = DuoRed, modifier = Modifier.size(14.dp))
                            }
                        }

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
                                        Text(
                                            text = sub,
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.clickable { onEditSubCategory(cat.mainCategory, sub) }
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Rename subcategory",
                                            tint = DuoBlue,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { onEditSubCategory(cat.mainCategory, sub) }
                                        )
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

// -------------------------------------------------------------
// 4. Rules Tab (Auto-Run Toggle, Trigger All, Live Matches)
// -------------------------------------------------------------

@Composable
private fun RulesTabContent(
    rules: List<Rule>,
    isAutoRunEnabled: Boolean,
    onToggleAutoRun: (Boolean) -> Unit,
    onRunAllRules: () -> Unit,
    onAddRule: () -> Unit,
    onEditRule: (Rule) -> Unit,
    onDeleteRule: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Auto-Run Settings & Manual Trigger
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.large,
                colors = CardDefaults.cardColors(containerColor = DuoCardDark)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("⚡ Auto-Run Rules", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.bodyLarge)
                            Text("Automatically categorizes transactions on app load & hourly", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                        }
                        Switch(
                            checked = isAutoRunEnabled,
                            onCheckedChange = onToggleAutoRun,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DuoGreen)
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    DuolingoPressableButton(
                        onClick = onRunAllRules,
                        backgroundColor = DuoGreen,
                        shadowColor = DuoGreenDark,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Run All Rules Now", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

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

        if (rules.isEmpty()) {
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
                        Text("⚡", style = MaterialTheme.typography.displaySmall)
                        Text("No Rules Configured", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Create rules (e.g. \"Starbucks\" ➔ Dining > Coffee) to automatically categorize bank syncs!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
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
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("If description contains \"${rule.pattern}\"", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("➔ Assign: ${rule.category}${if (rule.subCategory.isNotBlank()) " > ${rule.subCategory}" else ""}", style = MaterialTheme.typography.bodySmall, color = DuoGreen, fontWeight = FontWeight.Medium)
                        Text("🔥 ${rule.matchCount} transactions currently matched", style = MaterialTheme.typography.labelSmall, color = DuoGoldDark, fontWeight = FontWeight.Bold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onEditRule(rule) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = DuoBlue)
                        }
                        IconButton(onClick = { onDeleteRule(rule.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DuoRed)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Duolingo 3D Dialogs & Goal Editor
// -------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DuolingoGoalDialog(
    initialGoal: Goal?,
    onDismiss: () -> Unit,
    onSave: (Goal) -> Unit
) {
    var title by remember { mutableStateOf(initialGoal?.title ?: "") }
    var targetText by remember { mutableStateOf(initialGoal?.targetAmount?.toString() ?: "1000.00") }
    var currentSavedText by remember { mutableStateOf(initialGoal?.currentAmount?.toString() ?: "0.00") }
    var category by remember { mutableStateOf(initialGoal?.category ?: "Savings") }

    // Target Date State
    var targetEpochSeconds by remember {
        mutableStateOf(initialGoal?.targetEpochSeconds ?: ((System.currentTimeMillis() / 1000) + (365L * 24L * 3600L)))
    }

    val targetDateFormatted = remember(targetEpochSeconds) {
        try {
            val zdt = Instant.ofEpochSecond(targetEpochSeconds).atZone(ZoneId.systemDefault())
            zdt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        } catch (e: Exception) {
            "Target Date"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialGoal != null) "Edit Financial Goal" else "🎯 New Financial Target", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title (e.g. Emergency Fund, Japan Trip)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Target Goal Amount ($)") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = currentSavedText,
                    onValueChange = { currentSavedText = it },
                    label = { Text("Currently Saved / Deposited ($)") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Target Date Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Target Completion Date:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text(targetDateFormatted, fontWeight = FontWeight.Black, color = DuoGoldDark, style = MaterialTheme.typography.bodySmall)
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "- 1 Mo" to -30L * 24 * 3600,
                            "+ 1 Mo" to 30L * 24 * 3600,
                            "+ 3 Mo" to 90L * 24 * 3600,
                            "+ 6 Mo" to 180L * 24 * 3600,
                            "+ 1 Yr" to 365L * 24 * 3600,
                            "+ 2 Yr" to 730L * 24 * 3600,
                            "+ 5 Yr" to 1825L * 24 * 3600
                        ).forEach { (label, delta) ->
                            Card(
                                modifier = Modifier
                                    .clip(Shapes.small)
                                    .clickable {
                                        val now = System.currentTimeMillis() / 1000
                                        targetEpochSeconds = maxOf(now + (30L * 24 * 3600), targetEpochSeconds + delta)
                                    },
                                colors = CardDefaults.cardColors(containerColor = DuoCardDark)
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text("Goal Category:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Savings", "Wants", "Investment").forEach { cat ->
                        val isSelected = category.equals(cat, ignoreCase = true)
                        Card(
                            modifier = Modifier
                                .clip(Shapes.small)
                                .clickable { category = cat },
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) DuoGold else DuoCardDark)
                        ) {
                            Text(
                                text = cat,
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            DuolingoPressableButton(
                onClick = {
                    val target = targetText.toDoubleOrNull() ?: 0.0
                    val current = currentSavedText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && target > 0) {
                        val goal = (initialGoal ?: Goal(
                            id = UUID.randomUUID().toString(),
                            title = title.trim(),
                            targetAmount = target,
                            currentAmount = current,
                            targetEpochSeconds = targetEpochSeconds,
                            category = category
                        )).copy(
                            title = title.trim(),
                            targetAmount = target,
                            currentAmount = current,
                            targetEpochSeconds = targetEpochSeconds,
                            category = category,
                            isCompleted = current >= target
                        )
                        onSave(goal)
                    }
                },
                backgroundColor = DuoGold,
                shadowColor = DuoGoldDark,
                cornerRadius = 10.dp
            ) {
                Text("Save Goal ➔", color = Color.White, fontWeight = FontWeight.Bold)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DuolingoSubcategoryBudgetDialog(
    initialBudget: Budget?,
    categories: List<CategoryHierarchy>,
    totalMtdIncome: Double,
    onDismiss: () -> Unit,
    onSave: (Budget) -> Unit
) {
    var selectedMain by remember { mutableStateOf(initialBudget?.category ?: categories.firstOrNull()?.mainCategory.orEmpty()) }
    val activeCat = categories.find { it.mainCategory.equals(selectedMain, ignoreCase = true) }
    var selectedSub by remember { mutableStateOf(initialBudget?.subCategory ?: activeCat?.subCategories?.firstOrNull().orEmpty()) }

    var budgetType by remember { mutableStateOf(initialBudget?.categoryType ?: BudgetCategoryType.FIXED) }
    var fixedAmountText by remember { mutableStateOf(if (initialBudget?.categoryType == BudgetCategoryType.FIXED) initialBudget.targetAmount.toString() else "150.00") }
    var percentIncomeText by remember { mutableStateOf(initialBudget?.incomePercentage?.toString() ?: "10.0") }

    val calculatedTargetFromPercent = (percentIncomeText.toDoubleOrNull() ?: 0.0) / 100.0 * totalMtdIncome

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialBudget != null) "Edit Subcategory Budget" else "➕ New Subcategory Budget", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 1. Select Main Category
                Text("1. Select Main Category:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedMain.equals(cat.mainCategory, ignoreCase = true)
                        Card(
                            modifier = Modifier
                                .clip(Shapes.small)
                                .clickable {
                                    selectedMain = cat.mainCategory
                                    selectedSub = cat.subCategories.firstOrNull().orEmpty()
                                },
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) DuoGreen else DuoCardDark)
                        ) {
                            Text(
                                text = "${getCategoryEmoji(cat.mainCategory)} ${cat.mainCategory}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // 2. Select Subcategory
                Text("2. Select Subcategory to Budget Against:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                if (activeCat?.subCategories.isNullOrEmpty()) {
                    Text("No subcategories defined for $selectedMain yet. This budget will apply to all $selectedMain expenses.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeCat!!.subCategories.forEach { sub ->
                            val isSelected = selectedSub.equals(sub, ignoreCase = true)
                            Card(
                                modifier = Modifier
                                    .clip(Shapes.small)
                                    .clickable { selectedSub = sub },
                                colors = CardDefaults.cardColors(containerColor = if (isSelected) DuoGreen else DuoCardDark)
                            ) {
                                Text(
                                    text = sub,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // 3. Budget Type (Fixed $ vs % Income)
                Text("3. Choose Budget Limit Type:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DuolingoPressableButton(
                        onClick = { budgetType = BudgetCategoryType.FIXED },
                        backgroundColor = if (budgetType == BudgetCategoryType.FIXED) DuoGreen else DuoCardDark,
                        shadowColor = if (budgetType == BudgetCategoryType.FIXED) DuoGreenDark else DuoCardShadow,
                        cornerRadius = 10.dp,
                        shadowHeight = 3.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("💵 Fixed Monthly ($)", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }

                    DuolingoPressableButton(
                        onClick = { budgetType = BudgetCategoryType.PERCENT_INCOME },
                        backgroundColor = if (budgetType == BudgetCategoryType.PERCENT_INCOME) DuoBlue else DuoCardDark,
                        shadowColor = if (budgetType == BudgetCategoryType.PERCENT_INCOME) DuoBlueDark else DuoCardShadow,
                        cornerRadius = 10.dp,
                        shadowHeight = 3.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📈 % of Income", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (budgetType == BudgetCategoryType.FIXED) {
                    OutlinedTextField(
                        value = fixedAmountText,
                        onValueChange = { fixedAmountText = it },
                        label = { Text("Monthly Limit ($)") },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = percentIncomeText,
                            onValueChange = { percentIncomeText = it },
                            label = { Text("Percentage of Monthly Income (%)") },
                            suffix = { Text("%") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "≈ ${CurrencyFormatter.format(calculatedTargetFromPercent)}/mo based on MTD Income (${CurrencyFormatter.format(totalMtdIncome)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = DuoBlueDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            DuolingoPressableButton(
                onClick = {
                    val target = if (budgetType == BudgetCategoryType.FIXED) {
                        fixedAmountText.toDoubleOrNull() ?: 0.0
                    } else {
                        calculatedTargetFromPercent
                    }

                    val percent = if (budgetType == BudgetCategoryType.PERCENT_INCOME) {
                        percentIncomeText.toDoubleOrNull()
                    } else null

                    if (selectedMain.isNotBlank() && target > 0) {
                        val budget = (initialBudget ?: Budget(
                            id = UUID.randomUUID().toString(),
                            category = selectedMain,
                            subCategory = selectedSub
                        )).copy(
                            category = selectedMain,
                            subCategory = selectedSub,
                            categoryType = budgetType,
                            targetAmount = target,
                            incomePercentage = percent
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DuolingoIncomeCategoryDialog(
    currentIncomeCategory: String,
    categories: List<CategoryHierarchy>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onCreateIncomeCategory: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("💰 Select Income Main Category", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Any transaction assigned to this main category will be treated as Income for your percentage-based budgets and savings calculations.",
                    style = MaterialTheme.typography.bodySmall
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = currentIncomeCategory.equals(cat.mainCategory, ignoreCase = true)
                        Card(
                            modifier = Modifier
                                .clip(Shapes.small)
                                .clickable {
                                    onSelect(cat.mainCategory)
                                },
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) DuoGreen else DuoCardDark)
                        ) {
                            Text(
                                text = "${getCategoryEmoji(cat.mainCategory)} ${cat.mainCategory}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                HorizontalDivider()

                DuolingoPressableButton(
                    onClick = onCreateIncomeCategory,
                    backgroundColor = DuoBlue,
                    shadowColor = DuoBlueDark,
                    cornerRadius = 10.dp,
                    shadowHeight = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Create Default \"Income\" Category", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            DuolingoPressableButton(
                onClick = onDismiss,
                backgroundColor = DuoCardDark,
                shadowColor = DuoCardShadow,
                cornerRadius = 10.dp
            ) {
                Text("Close", color = Color.White)
            }
        }
    )
}

@Composable
fun DuolingoRenameCategoryDialog(
    currentName: String,
    title: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            DuolingoPressableButton(
                onClick = {
                    if (newName.isNotBlank()) {
                        onSave(newName.trim())
                    }
                },
                backgroundColor = DuoGreen,
                shadowColor = DuoGreenDark,
                cornerRadius = 10.dp
            ) {
                Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DuolingoRuleDialog(
    initialRule: Rule?,
    categories: List<CategoryHierarchy>,
    nextPriority: Int,
    onCalculateMatches: (String, Double?, Double?) -> Int,
    onDismiss: () -> Unit,
    onSave: (Rule) -> Unit
) {
    var pattern by remember { mutableStateOf(initialRule?.pattern ?: "") }
    var selectedMain by remember { mutableStateOf(initialRule?.category ?: categories.firstOrNull()?.mainCategory.orEmpty()) }
    val activeCat = categories.find { it.mainCategory.equals(selectedMain, ignoreCase = true) }
    var selectedSub by remember { mutableStateOf(initialRule?.subCategory ?: "") }

    val liveMatches = remember(pattern) {
        onCalculateMatches(pattern, null, null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialRule != null) "Edit Auto-Rule" else "⚡ New Auto-Rule", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("If Description Contains (e.g. Starbucks, Shell)") },
                    placeholder = { Text("e.g. Target, Uber, Amazon") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (pattern.isNotBlank()) {
                    Text(
                        text = "🔥 Matches $liveMatches existing transactions right now!",
                        fontWeight = FontWeight.Bold,
                        color = DuoGoldDark,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text("Assign to Category:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedMain.equals(cat.mainCategory, ignoreCase = true)
                        Card(
                            modifier = Modifier
                                .clip(Shapes.small)
                                .clickable {
                                    selectedMain = cat.mainCategory
                                    selectedSub = cat.subCategories.firstOrNull().orEmpty()
                                },
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) DuoGreen else DuoCardDark)
                        ) {
                            Text(
                                text = "${getCategoryEmoji(cat.mainCategory)} ${cat.mainCategory}",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (!activeCat?.subCategories.isNullOrEmpty()) {
                    Text("Subcategory (Optional):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeCat!!.subCategories.forEach { sub ->
                            val isSelected = selectedSub.equals(sub, ignoreCase = true)
                            Card(
                                modifier = Modifier
                                    .clip(Shapes.small)
                                    .clickable { selectedSub = if (isSelected) "" else sub },
                                colors = CardDefaults.cardColors(containerColor = if (isSelected) DuoGreen else DuoCardDark)
                            ) {
                                Text(
                                    text = sub,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            DuolingoPressableButton(
                onClick = {
                    if (pattern.isNotBlank() && selectedMain.isNotBlank()) {
                        val rule = (initialRule ?: Rule(
                            id = UUID.randomUUID().toString(),
                            name = pattern.trim(),
                            priority = nextPriority,
                            pattern = pattern.trim(),
                            category = selectedMain,
                            subCategory = selectedSub
                        )).copy(
                            pattern = pattern.trim(),
                            category = selectedMain,
                            subCategory = selectedSub
                        )
                        onSave(rule)
                    }
                },
                backgroundColor = DuoGreen,
                shadowColor = DuoGreenDark,
                cornerRadius = 10.dp
            ) {
                Text("Save & Apply Rule ➔", color = Color.White, fontWeight = FontWeight.Bold)
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
