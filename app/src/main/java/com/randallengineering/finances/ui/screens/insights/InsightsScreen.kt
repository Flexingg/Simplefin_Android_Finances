package com.randallengineering.finances.ui.screens.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.randallengineering.finances.core.theme.FinanceGreen
import com.randallengineering.finances.core.theme.FinanceRed
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.core.util.CurrencyFormatter
import com.randallengineering.finances.ui.components.*
import org.koin.androidx.compose.koinViewModel
import kotlin.math.max

private val ChartColors = listOf(
    Color(0xFF6750A4),
    Color(0xFF006C4C),
    Color(0xFF00639B),
    Color(0xFFB3261E),
    Color(0xFFE28743),
    Color(0xFF006A6A),
    Color(0xFF7D5260),
    Color(0xFF825500),
    Color(0xFF4A6572),
    Color(0xFF9E4770)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = DuoGreen)
                        Spacer(Modifier.width(8.dp))
                        Text("Insights & Analytics", fontWeight = FontWeight.Black)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(
                selectedTabIndex = uiState.selectedInsightsTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = uiState.selectedInsightsTab == 0,
                    onClick = { viewModel.selectInsightsTab(0) },
                    text = { Text("📊 Spending Trends", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = uiState.selectedInsightsTab == 1,
                    onClick = { viewModel.selectInsightsTab(1) },
                    text = { Text("🗓️ Heatmap", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = uiState.selectedInsightsTab == 2,
                    onClick = { viewModel.selectInsightsTab(2) },
                    text = { Text("💰 Net Worth & Debt", fontWeight = FontWeight.Bold) }
                )
            }

            when (uiState.selectedInsightsTab) {
                0 -> TrendsTabContent(uiState, viewModel)
                1 -> HeatmapTabContent(uiState)
                2 -> NetWorthAndDebtTabContent(uiState, viewModel)
            }
        }
    }
}

// -------------------------------------------------------------
// 1. Spending Trends & Category Breakdown
// -------------------------------------------------------------

@Composable
private fun TrendsTabContent(
    uiState: InsightsUiState,
    viewModel: InsightsViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Time Range Filter Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeRange.entries.forEach { range ->
                    val isSelected = range == uiState.selectedTimeRange
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectTimeRange(range) },
                        label = { Text(range.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DuoGreen.copy(alpha = 0.2f),
                            selectedLabelColor = DuoGreenDark
                        )
                    )
                }
            }
        }

        // Summary KPI Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExpressiveCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = FinanceGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Total Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(CurrencyFormatter.format(uiState.totalIncome), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = FinanceGreen)
                    }
                }

                ExpressiveCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = FinanceRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Total Outflows", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(CurrencyFormatter.format(uiState.totalExpenses), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = FinanceRed)
                    }
                }
            }
        }

        // Multi-Month Trends Comparison (Last 6 Months)
        item {
            ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = DuoBlue)
                        Spacer(Modifier.width(8.dp))
                        Text("Month-over-Month Trends (6 Mo)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    uiState.monthlyTrends.forEach { trend ->
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(trend.monthLabel, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "Spent ${CurrencyFormatter.format(trend.totalExpenses)} / Saved ${CurrencyFormatter.format(trend.netSavings)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (trend.netSavings >= 0) FinanceGreen else FinanceRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            val maxVal = max(1.0, uiState.monthlyTrends.maxOf { max(it.totalIncome, it.totalExpenses) })
                            LinearProgressIndicator(
                                progress = { (trend.totalExpenses / maxVal).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = if (trend.netSavings >= 0) DuoBlue else DuoRed,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Category Breakdown List
        item {
            Text("Spending by Category", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        items(uiState.categoryBreakdown) { cat ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cat.category, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("${cat.count} transactions • ${String.format("%.1f", cat.percentageOfTotal)}% of total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(CurrencyFormatter.format(cat.totalAmount), fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. Interactive Spending Heatmap Calendar
// -------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeatmapTabContent(uiState: InsightsUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = DuoGreen)
                        Spacer(Modifier.width(8.dp))
                        Text("Current Month Spending Intensity", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    Text(
                        text = "Visual heatmap of daily cash outflows. Darker green indicates lower/zero spending, while gold/red highlights high expenditure days.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Heatmap Grid
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        maxItemsInEachRow = 7
                    ) {
                        uiState.currentMonthHeatmap.forEach { day ->
                            val color = when (day.intensityLevel) {
                                0 -> DuoGreen.copy(alpha = 0.85f)      // $0 (Duolingo Green Success)
                                1 -> Color(0xFF66BB6A)                  // <$25 (Light Green)
                                2 -> DuoGold                            // <$75 (Amber)
                                3 -> Color(0xFFFF7043)                  // <$150 (Orange)
                                else -> DuoRed                          // $150+ (Red Peak)
                            }

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(Shapes.small)
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${day.dayOfMonth}",
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                    if (day.spendAmount > 0) {
                                        Text(
                                            text = "\$${day.spendAmount.toInt()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    } else {
                                        Text("🛡️", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Intensity:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(12.dp).clip(CircleShape).background(DuoGreen))
                            Text("\$0", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.width(4.dp))
                            Box(Modifier.size(12.dp).clip(CircleShape).background(DuoGold))
                            Text("<\$75", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.width(4.dp))
                            Box(Modifier.size(12.dp).clip(CircleShape).background(DuoRed))
                            Text("\$150+", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. Net Worth & Debt Payoff Tracker
// -------------------------------------------------------------

@Composable
private fun NetWorthAndDebtTabContent(
    uiState: InsightsUiState,
    viewModel: InsightsViewModel
) {
    val sim = uiState.debtSimulation

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Net Worth Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.large,
                colors = CardDefaults.cardColors(containerColor = DuoGold.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Savings, contentDescription = null, tint = DuoGoldDark, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Estimated Net Cash Flow", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = DuoGoldDark)
                        Text(
                            text = CurrencyFormatter.format(uiState.estimatedNetWorth),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.headlineMedium,
                            color = FinanceGreen
                        )
                        Text("Cumulative liquidity calculated across connected SimpleFIN accounts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Debt Payoff Simulator
        item {
            ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = DuoBlue)
                        Spacer(Modifier.width(8.dp))
                        Text("Debt Payoff Accelerator", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    Text("Simulate debt-free timelines by adjusting your monthly debt acceleration payment.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // Strategy Selector (Snowball vs Avalanche)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DuolingoPressableButton(
                            onClick = { viewModel.setDebtStrategy("Snowball") },
                            backgroundColor = if (sim.strategy == "Snowball") DuoGreen else DuoCardDark,
                            shadowColor = if (sim.strategy == "Snowball") DuoGreenDark else DuoCardShadow,
                            cornerRadius = 8.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("❄️ Snowball (Smallest First)", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }

                        DuolingoPressableButton(
                            onClick = { viewModel.setDebtStrategy("Avalanche") },
                            backgroundColor = if (sim.strategy == "Avalanche") DuoBlue else DuoCardDark,
                            shadowColor = if (sim.strategy == "Avalanche") DuoBlueDark else DuoCardShadow,
                            cornerRadius = 8.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("⚡ Avalanche (Highest APR)", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Text("Monthly Debt Payment: ${CurrencyFormatter.format(sim.monthlyPaymentAmount)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                    Slider(
                        value = sim.monthlyPaymentAmount.toFloat(),
                        onValueChange = { viewModel.updateDebtPayoffMonthlyPayment(it.toDouble()) },
                        valueRange = 100f..1500f,
                        steps = 13
                    )

                    // Results Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = Shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Time to Debt-Free", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${sim.estimatedMonthsToDebtFree} Months", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = DuoBlueDark)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Est. Interest Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CurrencyFormatter.format(sim.totalInterestPaid), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = DuoRedDark)
                            }
                        }
                    }
                }
            }
        }
    }
}
