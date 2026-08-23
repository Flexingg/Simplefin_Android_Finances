package com.randallengineering.finances.ui.screens.budgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.randallengineering.finances.core.theme.FinanceGreen
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.core.util.CurrencyFormatter
import com.randallengineering.finances.domain.model.Budget
import com.randallengineering.finances.domain.model.BudgetCategoryType
import com.randallengineering.finances.domain.model.CategoryHierarchy
import com.randallengineering.finances.domain.model.Rule
import com.randallengineering.finances.ui.components.CategoryPickerDialog
import com.randallengineering.finances.ui.components.ExpressiveCard
import com.randallengineering.finances.ui.components.PacingProgressBar
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val calc = uiState.calculationResult

    // Dialogs
    if (uiState.isCreatingBudget || uiState.editingBudget != null) {
        BudgetEditorDialog(
            initialBudget = uiState.editingBudget,
            categories = uiState.categories,
            onDismiss = { viewModel.closeDialogs() },
            onSave = { viewModel.saveBudget(it) }
        )
    }

    if (uiState.isCreatingCategory) {
        CreateCategoryDialog(
            onDismiss = { viewModel.closeDialogs() },
            onSave = { main, sub -> viewModel.addCategory(main, sub) }
        )
    }

    if (uiState.selectedMainCategoryForSub != null) {
        AddSubCategoryDialog(
            mainCategory = uiState.selectedMainCategoryForSub!!,
            onDismiss = { viewModel.closeDialogs() },
            onSave = { sub -> viewModel.addSubCategory(uiState.selectedMainCategoryForSub!!, sub) }
        )
    }

    if (uiState.isCreatingRule || uiState.editingRule != null) {
        RuleEditorDialog(
            initialRule = uiState.editingRule,
            categories = uiState.categories,
            nextPriority = uiState.rules.size + 1,
            onDismiss = { viewModel.closeDialogs() },
            onSave = { viewModel.saveRule(it) }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (uiState.selectedTab) {
                        0 -> viewModel.openCreateBudgetDialog()
                        1 -> viewModel.openCreateCategoryDialog()
                        2 -> viewModel.openCreateRuleDialog()
                    }
                },
                shape = Shapes.medium,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Tab Header: 3-Way Selector
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = 16.dp
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.onTabSelect(0) },
                    text = { Text("Budgets & Pacing", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = null) }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.onTabSelect(1) },
                    text = { Text("Categories", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Category, contentDescription = null) }
                )
                Tab(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.onTabSelect(2) },
                    text = { Text("Auto-Rules", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Rule, contentDescription = null) }
                )
            }

            when (uiState.selectedTab) {
                0 -> BudgetsPacingTab(
                    uiState = uiState,
                    onEditBudget = { viewModel.openEditBudgetDialog(it) },
                    onDeleteBudget = { viewModel.deleteBudget(it.id) }
                )
                1 -> CategoriesManagementTab(
                    categories = uiState.categories,
                    onAddSubCategory = { viewModel.openAddSubCategoryDialog(it) },
                    onDeleteCategory = { viewModel.deleteCategory(it) },
                    onDeleteSubCategory = { main, sub -> viewModel.deleteSubCategory(main, sub) },
                    onCreateCategory = { viewModel.openCreateCategoryDialog() }
                )
                2 -> RulesManagementTab(
                    rules = uiState.rules,
                    onEditRule = { viewModel.openEditRuleDialog(it) },
                    onDeleteRule = { viewModel.deleteRule(it.id) },
                    onMoveUp = { viewModel.moveRuleUp(it) },
                    onMoveDown = { viewModel.moveRuleDown(it) },
                    onCreateRule = { viewModel.openCreateRuleDialog() }
                )
            }
        }
    }
}

@Composable
fun BudgetsPacingTab(
    uiState: BudgetsUiState,
    onEditBudget: (Budget) -> Unit,
    onDeleteBudget: (Budget) -> Unit
) {
    val calc = uiState.calculationResult

    Column(modifier = Modifier.fillMaxSize()) {
        ExpressiveCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = Shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Target Daily Allowance",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = CurrencyFormatter.format(calc?.targetDailyAllowance ?: 0.0),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Card(
                        shape = Shapes.small,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${calc?.daysRemaining ?: 0} days left",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Monthly Variable Target", style = MaterialTheme.typography.labelSmall)
                        Text(CurrencyFormatter.format(calc?.monthlyVariableTarget ?: 0.0), fontWeight = FontWeight.SemiBold)
                    }
                    Column {
                        Text("MTD Variable Spent", style = MaterialTheme.typography.labelSmall)
                        Text(CurrencyFormatter.format(calc?.mtdVariableSpent ?: 0.0), fontWeight = FontWeight.SemiBold)
                    }
                    Column {
                        Text("MTD Total Income", style = MaterialTheme.typography.labelSmall)
                        Text(CurrencyFormatter.format(calc?.totalMtdIncome ?: 0.0), fontWeight = FontWeight.SemiBold, color = FinanceGreen)
                    }
                }
            }
        }

        if (calc?.anomalies?.isNotEmpty() == true) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = Shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Anomaly", tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${calc.anomalies.size} categories currently over 120% pacing!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        if (calc?.calculatedBudgets.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No budgets created yet.\nTap '+' to assign target budgets or switch to 'Categories' to set up your taxonomy!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = calc?.calculatedBudgets.orEmpty(),
                    key = { it.id }
                ) { budget ->
                    BudgetItemCard(
                        budget = budget,
                        onEdit = { onEditBudget(budget) },
                        onDelete = { onDeleteBudget(budget) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesManagementTab(
    categories: List<CategoryHierarchy>,
    onAddSubCategory: (mainCategory: String) -> Unit,
    onDeleteCategory: (mainCategory: String) -> Unit,
    onDeleteSubCategory: (mainCategory: String, subCategory: String) -> Unit,
    onCreateCategory: () -> Unit
) {
    if (categories.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "No Categories Created Yet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Start by creating your own main categories (e.g. Home, Food & Dining, Transportation) and subcategories to categorize your transactions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onCreateCategory,
                    shape = Shapes.medium
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Create First Category")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = categories,
                key = { it.mainCategory }
            ) { category ->
                ExpressiveCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = category.mainCategory,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = { onAddSubCategory(category.mainCategory) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add Subcategory",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteCategory(category.mainCategory) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Main Category",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        if (category.subCategories.isEmpty()) {
                            Text(
                                text = "No subcategories yet. Tap '+' to add subcategories (e.g. Mortgage, Utilities, Groceries).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                category.subCategories.forEach { sub ->
                                    Card(
                                        shape = Shapes.small,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(start = 10.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(sub, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                            Spacer(Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { onDeleteSubCategory(category.mainCategory, sub) },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    modifier = Modifier.size(12.dp)
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
    }
}

@Composable
fun RulesManagementTab(
    rules: List<Rule>,
    onEditRule: (Rule) -> Unit,
    onDeleteRule: (Rule) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onCreateRule: () -> Unit
) {
    if (rules.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Rule,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "No Auto-Rules Created Yet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Auto-rules automatically categorize imported transactions based on regex patterns (e.g. (?i).*shell.* -> Transportation > Gas).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onCreateRule,
                    shape = Shapes.medium
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Create First Rule")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(
                items = rules,
                key = { _, rule -> rule.id }
            ) { index, rule ->
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Card(
                                    shape = Shapes.small,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Text(
                                        text = "#${rule.priority}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(rule.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${rule.category}${if (rule.subCategory.isNotBlank()) " > ${rule.subCategory}" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = { onMoveUp(index) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { onMoveDown(index) },
                                    enabled = index < rules.size - 1,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { onEditRule(rule) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { onDeleteRule(rule) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Card(
                            shape = Shapes.small,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = rule.pattern,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Matched ${rule.matchCount} transactions",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (rule.minAmount != null || rule.maxAmount != null) {
                                Text(
                                    text = "Range: $${rule.minAmount ?: 0.0} - $${rule.maxAmount ?: "∞"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
fun RuleEditorDialog(
    initialRule: Rule? = null,
    categories: List<CategoryHierarchy>,
    nextPriority: Int = 1,
    onDismiss: () -> Unit,
    onSave: (Rule) -> Unit
) {
    var name by remember { mutableStateOf(initialRule?.name.orEmpty()) }
    var pattern by remember { mutableStateOf(initialRule?.pattern ?: "(?i).*") }
    var category by remember { mutableStateOf(initialRule?.category ?: categories.firstOrNull()?.mainCategory.orEmpty()) }
    var subCategory by remember { mutableStateOf(initialRule?.subCategory.orEmpty()) }
    var minAmountText by remember { mutableStateOf(initialRule?.minAmount?.toString().orEmpty()) }
    var maxAmountText by remember { mutableStateOf(initialRule?.maxAmount?.toString().orEmpty()) }

    var isCategoryPickerOpen by remember { mutableStateOf(false) }

    val isRegexValid = remember(pattern) {
        try {
            Regex(pattern, setOf(RegexOption.IGNORE_CASE))
            true
        } catch (e: Exception) {
            false
        }
    }

    if (isCategoryPickerOpen) {
        CategoryPickerDialog(
            categories = categories,
            initialMainCategory = category,
            initialSubCategory = subCategory,
            onDismiss = { isCategoryPickerOpen = false },
            onCategorySelected = { main, sub ->
                category = main
                subCategory = sub
                isCategoryPickerOpen = false
            },
            onAddNewCategory = { _, _ -> }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Shapes.large,
        title = {
            Text(
                text = if (initialRule == null) "Create Auto-Rule" else "Edit Auto-Rule",
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
                    placeholder = { Text("e.g. Starbucks Coffee") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.small,
                    singleLine = true
                )

                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Regex Pattern") },
                    placeholder = { Text("e.g. (?i).*starbucks.*") },
                    isError = !isRegexValid,
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.small,
                    singleLine = true
                )

                // Category Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.small,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Assigned Category", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "$category${if (subCategory.isNotBlank()) " > $subCategory" else ""}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = { isCategoryPickerOpen = true },
                            shape = Shapes.small
                        ) {
                            Text("Change")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minAmountText,
                        onValueChange = { minAmountText = it },
                        label = { Text("Min $ (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = Shapes.small,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = maxAmountText,
                        onValueChange = { maxAmountText = it },
                        label = { Text("Max $ (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = Shapes.small,
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rule = Rule(
                        id = initialRule?.id ?: UUID.randomUUID().toString(),
                        name = name,
                        priority = initialRule?.priority ?: nextPriority,
                        pattern = pattern,
                        category = category,
                        subCategory = subCategory,
                        minAmount = minAmountText.toDoubleOrNull(),
                        maxAmount = maxAmountText.toDoubleOrNull(),
                        isActive = initialRule?.isActive ?: true
                    )
                    onSave(rule)
                },
                enabled = name.isNotBlank() && isRegexValid && category.isNotBlank(),
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

@Composable
fun CreateCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (mainCategory: String, subCategory: String?) -> Unit
) {
    var mainName by remember { mutableStateOf("") }
    var subName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Shapes.large,
        title = {
            Text("Create Category Hierarchy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = mainName,
                    onValueChange = { mainName = it },
                    label = { Text("Main Category Name") },
                    placeholder = { Text("e.g. Home, Food & Dining, Vehicle") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.small,
                    singleLine = true
                )

                OutlinedTextField(
                    value = subName,
                    onValueChange = { subName = it },
                    label = { Text("Initial Subcategory (Optional)") },
                    placeholder = { Text("e.g. Utilities, Groceries, Gas") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.small,
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (mainName.isNotBlank()) {
                        onSave(mainName.trim(), subName.trim().ifBlank { null })
                    }
                },
                enabled = mainName.isNotBlank(),
                shape = Shapes.small
            ) {
                Text("Create Category")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = Shapes.small) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddSubCategoryDialog(
    mainCategory: String,
    onDismiss: () -> Unit,
    onSave: (subCategory: String) -> Unit
) {
    var subName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Shapes.large,
        title = {
            Text("Add Subcategory to $mainCategory", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = subName,
                onValueChange = { subName = it },
                label = { Text("Subcategory Name") },
                placeholder = { Text("e.g. Maintenance, Coffee, Internet") },
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.small,
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subName.isNotBlank()) {
                        onSave(subName.trim())
                    }
                },
                enabled = subName.isNotBlank(),
                shape = Shapes.small
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = Shapes.small) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BudgetItemCard(
    budget: Budget,
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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = budget.category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (budget.categoryType == BudgetCategoryType.PERCENT_INCOME) {
                        Spacer(Modifier.width(6.dp))
                        Card(
                            shape = Shapes.extraSmall,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = "${budget.incomePercentage ?: 0.0}% of Income",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${CurrencyFormatter.format(budget.spentAmount)} spent of ${CurrencyFormatter.format(budget.targetAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = CurrencyFormatter.formatPacing(budget.pacingPercent),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (budget.isAnomalyOverpacing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(8.dp))

            PacingProgressBar(pacingPercent = budget.pacingPercent)
        }
    }
}

@Composable
fun BudgetEditorDialog(
    initialBudget: Budget? = null,
    categories: List<CategoryHierarchy>,
    onDismiss: () -> Unit,
    onSave: (Budget) -> Unit
) {
    var category by remember { mutableStateOf(initialBudget?.category.orEmpty()) }
    var categoryType by remember { mutableStateOf(initialBudget?.categoryType ?: BudgetCategoryType.VARIABLE) }
    var targetAmountText by remember { mutableStateOf(initialBudget?.targetAmount?.toString().orEmpty()) }
    var incomePercentText by remember { mutableStateOf(initialBudget?.incomePercentage?.toString().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Shapes.large,
        title = {
            Text(
                text = if (initialBudget == null) "Create Budget Target" else "Edit Budget",
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
                if (categories.isNotEmpty()) {
                    Text("Select Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.take(4).forEach { cat ->
                            Card(
                                shape = Shapes.small,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (category.equals(cat.mainCategory, ignoreCase = true)) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.clickable { category = cat.mainCategory }
                            ) {
                                Text(
                                    text = cat.mainCategory,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Home, Food & Dining, Tithe") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.small,
                    singleLine = true
                )

                Text("Budget Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = categoryType == BudgetCategoryType.VARIABLE,
                        onClick = { categoryType = BudgetCategoryType.VARIABLE }
                    )
                    Text("Variable (Counts toward Daily Allowance)")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = categoryType == BudgetCategoryType.FIXED,
                        onClick = { categoryType = BudgetCategoryType.FIXED }
                    )
                    Text("Fixed (Mortgage, Insurance)")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = categoryType == BudgetCategoryType.PERCENT_INCOME,
                        onClick = { categoryType = BudgetCategoryType.PERCENT_INCOME }
                    )
                    Text("Formula (% of Monthly Income)")
                }

                if (categoryType == BudgetCategoryType.PERCENT_INCOME) {
                    OutlinedTextField(
                        value = incomePercentText,
                        onValueChange = { incomePercentText = it },
                        label = { Text("Income Percentage (%)") },
                        placeholder = { Text("e.g. 10.0 for Tithe") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.small,
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = targetAmountText,
                        onValueChange = { targetAmountText = it },
                        label = { Text("Monthly Budget Target ($)") },
                        placeholder = { Text("e.g. 600.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.small,
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val budget = Budget(
                        id = initialBudget?.id ?: UUID.randomUUID().toString(),
                        category = category.ifBlank { "Uncategorized" },
                        categoryType = categoryType,
                        targetAmount = targetAmountText.toDoubleOrNull() ?: 0.0,
                        incomePercentage = if (categoryType == BudgetCategoryType.PERCENT_INCOME) incomePercentText.toDoubleOrNull() ?: 0.0 else null,
                        spentAmount = initialBudget?.spentAmount ?: 0.0,
                        pacingPercent = initialBudget?.pacingPercent ?: 0.0
                    )
                    onSave(budget)
                },
                enabled = category.isNotBlank() &&
                        (categoryType == BudgetCategoryType.PERCENT_INCOME && incomePercentText.toDoubleOrNull() != null ||
                                categoryType != BudgetCategoryType.PERCENT_INCOME && targetAmountText.toDoubleOrNull() != null),
                shape = Shapes.small
            ) {
                Text("Save Budget")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = Shapes.small) {
                Text("Cancel")
            }
        }
    )
}
