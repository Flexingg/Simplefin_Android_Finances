package com.randallengineering.finances.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.randallengineering.finances.core.util.CurrencyFormatter
import com.randallengineering.finances.domain.model.DashboardCardType
import com.randallengineering.finances.domain.model.Transaction
import org.koin.androidx.compose.koinViewModel
import java.util.Locale
import kotlin.math.abs

@Composable
fun DashboardScreen(
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToQueue: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scheme = MaterialTheme.colorScheme
    var editMode by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your money at a glance",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { editMode = !editMode }) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (editMode) "Done" else "Customize")
                }
            }
        }

        if (editMode) {
            item {
                DashboardCardEditor(
                    layout = state.fullLayout,
                    onToggle = { viewModel.toggleCard(it) },
                    onMove = { type, up -> viewModel.moveCard(type, up) },
                    onReset = { viewModel.resetLayout() }
                )
            }
        }

        items(state.enabledCards, key = { it.name }) { cardType ->
            DashboardCard(
                type = cardType,
                state = state,
                onNavigateToTransactions = onNavigateToTransactions,
                onNavigateToBudgets = onNavigateToBudgets,
                onNavigateToInsights = onNavigateToInsights,
                onNavigateToQueue = onNavigateToQueue,
                onNavigateToSettings = onNavigateToSettings
            )
        }

        if (!state.isLoading && !state.hasTransactions) {
            item { EmptyState(onNavigateToSettings) }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

/** Renders one dashboard card by type, honoring the user's order/enabled config. */
@Composable
private fun DashboardCard(
    type: DashboardCardType,
    state: DashboardUiState,
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToQueue: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    when (type) {
        DashboardCardType.TOTAL_BALANCE -> HeroBalanceCard(state.totalBalance)
        DashboardCardType.MONTH_INCOME -> StatCard("Income this month", CurrencyFormatter.format(state.monthIncome), scheme.primary)
        DashboardCardType.MONTH_EXPENSE -> StatCard("Spending this month", CurrencyFormatter.format(state.monthExpenses), MaterialTheme.colorScheme.error)
        DashboardCardType.MONTH_NET -> StatCard("Net this month", CurrencyFormatter.format(state.monthNet), if (state.monthNet >= 0) scheme.primary else MaterialTheme.colorScheme.error)
        DashboardCardType.SAVINGS_RATE -> StatCard("Savings rate", String.format(Locale.US, "%.0f%%", state.savingsRate), scheme.primary)
        DashboardCardType.DAILY_ALLOWANCE -> StatCard("Daily allowance", "${CurrencyFormatter.format(state.dailyAllowance)}/day", scheme.primary)
        DashboardCardType.BUDGET_ALERTS -> BudgetAlertsCard(state.budgetAlertCount, onNavigateToBudgets)
        DashboardCardType.TOP_CATEGORIES -> TopCategoriesCard(state.topCategories)
        DashboardCardType.RECENT_TRANSACTIONS -> RecentTransactionsCard(state.recentTransactions, onNavigateToTransactions)
        DashboardCardType.NEEDS_REVIEW -> NeedsReviewCard(state.uncategorizedCount, onNavigateToQueue)
        DashboardCardType.QUICK_ACTIONS -> QuickActionsCard(onNavigateToQueue, onNavigateToBudgets, onNavigateToInsights)
    }
}

@Composable
private fun HeroBalanceCard(balance: Double) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = scheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Total Balance", style = MaterialTheme.typography.labelLarge, color = scheme.onPrimaryContainer)
            Text(
                CurrencyFormatter.format(balance),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = scheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color) {
    Card(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun BudgetAlertsCard(count: Int, onNavigateToBudgets: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val hasAlerts = count > 0
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (hasAlerts) scheme.errorContainer.copy(alpha = 0.5f) else scheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        onClick = onNavigateToBudgets,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (hasAlerts) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (hasAlerts) scheme.onErrorContainer else scheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasAlerts) "$count budget(s) need attention" else "Budgets on track",
                    fontWeight = FontWeight.Bold,
                    color = if (hasAlerts) scheme.onErrorContainer else scheme.onSurface
                )
                Text(
                    text = "Tap to review budgets",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasAlerts) scheme.onErrorContainer else scheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TopCategoriesCard(items: List<TopCategoryItem>) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Top categories this month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (items.isEmpty()) {
                Text(
                    "No spending this month yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
            items.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${CurrencyFormatter.format(item.total)} · ${item.percentage.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (item.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = scheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionsCard(recent: List<Transaction>, onSeeAll: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "See all",
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.primary,
                    modifier = Modifier.clickable(onClick = onSeeAll)
                )
            }
            if (recent.isEmpty()) {
                Text(
                    "No transactions yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            recent.take(5).forEach { tx ->
                TransactionRow(tx, onSeeAll)
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.3f))
            }
        }
    }
}

@Composable
private fun NeedsReviewCard(count: Int, onNavigateToQueue: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val needs = count > 0
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (needs) scheme.errorContainer.copy(alpha = 0.6f) else scheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        onClick = onNavigateToQueue,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (needs) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (needs) scheme.onErrorContainer else scheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (needs) "$count transaction(s) need review" else "All transactions categorized",
                    fontWeight = FontWeight.Bold,
                    color = if (needs) scheme.onErrorContainer else scheme.onSurface
                )
                Text(
                    text = "Tap to review",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (needs) scheme.onErrorContainer else scheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickActionsCard(onQueue: () -> Unit, onBudgets: () -> Unit, onInsights: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickAction("Review", Icons.Default.ReceiptLong, onQueue, Modifier.weight(1f))
        QuickAction("Budgets", Icons.Default.Sync, onBudgets, Modifier.weight(1f))
        QuickAction("Insights", Icons.Default.AutoAwesome, onInsights, Modifier.weight(1f))
    }
}

@Composable
private fun DashboardCardEditor(
    layout: List<Pair<DashboardCardType, Boolean>>,
    onToggle: (DashboardCardType) -> Unit,
    onMove: (DashboardCardType, Boolean) -> Unit,
    onReset: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Card(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Customize dashboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Reorder and show/hide cards. Your changes save automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            layout.forEachIndexed { index, (type, enabled) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(type.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(type.subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                    IconButton(
                        onClick = { onMove(type, true) },
                        enabled = index > 0
                    ) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move up") }
                    IconButton(
                        onClick = { onMove(type, false) },
                        enabled = index < layout.lastIndex
                    ) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move down") }
                    Switch(checked = enabled, onCheckedChange = { onToggle(type) })
                }
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onReset) { Text("Reset to default") }
        }
    }
}

@Composable
private fun TransactionRow(tx: Transaction, onClick: () -> Unit) {
    val isIncome = tx.amount > 0
    Card(
        shape = MaterialTheme.shapes.medium,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isIncome) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (tx.payee.ifBlank { tx.category }.take(1).uppercase()),
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.payee.ifBlank { tx.originalDesc },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tx.category.ifBlank { "Uncategorized" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isIncome) "+${CurrencyFormatter.format(abs(tx.amount))}" else CurrencyFormatter.format(tx.amount),
                fontWeight = FontWeight.Bold,
                color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        shape = MaterialTheme.shapes.large,
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EmptyState(onNavigateToSettings: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("No transactions yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Connect a bank account to start importing your real transactions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
