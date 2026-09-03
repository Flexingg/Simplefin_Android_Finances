package com.randallengineering.finances.ui.screens.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Flag
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.randallengineering.finances.data.repository.NetWorthPoint
import com.randallengineering.finances.core.util.DateUtils
import com.randallengineering.finances.domain.model.Transaction
import java.util.Locale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.randallengineering.finances.core.theme.*
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
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = FinanceGreen)
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
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedInsightsTab,
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = 16.dp
            ) {
                Tab(
                    selected = uiState.selectedInsightsTab == 0,
                    onClick = { viewModel.selectInsightsTab(0) },
                    text = { Text("ðŸ“Š Spending Trends", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = uiState.selectedInsightsTab == 1,
                    onClick = { viewModel.selectInsightsTab(1) },
                    text = { Text("ðŸ—“ï¸ Heatmap", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = uiState.selectedInsightsTab == 2,
                    onClick = { viewModel.selectInsightsTab(2) },
                    text = { Text("ðŸ’° Net Worth & Debt", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = uiState.selectedInsightsTab == 3,
                    onClick = { viewModel.selectInsightsTab(3) },
                    text = { Text("ðŸ–ï¸ Retirement & FIRE", fontWeight = FontWeight.Bold) }
                )
            }

            when (uiState.selectedInsightsTab) {
                0 -> TrendsTabContent(uiState, viewModel)
                1 -> HeatmapTabContent(uiState)
                2 -> NetWorthAndDebtTabContent(uiState, viewModel)
                3 -> RetirementTabContent(uiState, viewModel)
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
                            selectedContainerColor = FinanceGreen.copy(alpha = 0.2f),
                            selectedLabelColor = FinanceGreenDark
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
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = FinanceBlue)
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
                                color = if (trend.netSavings >= 0) FinanceBlue else FinanceRed,
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
                        Text("${cat.count} transactions â€¢ ${String.format("%.1f", cat.percentageOfTotal)}% of total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(CurrencyFormatter.format(cat.totalAmount), fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        item {
            MerchantDrillSection(uiState)
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
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = FinanceGreen)
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
                                0 -> FinanceGreen.copy(alpha = 0.85f)      // $0 (Finance Green Success)
                                1 -> Color(0xFF66BB6A)                  // <$25 (Light Green)
                                2 -> FinanceAmber                            // <$75 (Amber)
                                3 -> Color(0xFFFF7043)                  // <$150 (Orange)
                                else -> FinanceRed                          // $150+ (Red Peak)
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
                                        Text("ðŸ›¡ï¸", style = MaterialTheme.typography.labelSmall)
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
                            Box(Modifier.size(12.dp).clip(CircleShape).background(FinanceGreen))
                            Text("\$0", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.width(4.dp))
                            Box(Modifier.size(12.dp).clip(CircleShape).background(FinanceAmber))
                            Text("<\$75", style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.width(4.dp))
                            Box(Modifier.size(12.dp).clip(CircleShape).background(FinanceRed))
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
                colors = CardDefaults.cardColors(containerColor = FinanceAmber.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Savings, contentDescription = null, tint = FinanceAmberDark, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Estimated Net Cash Flow", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = FinanceAmberDark)
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

        // Net Worth Trend (weekly real-balance snapshots)
        item {
            NetWorthTrendCard(history = uiState.netWorthHistory, current = uiState.liveNetWorth)
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
                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = FinanceBlue)
                        Spacer(Modifier.width(8.dp))
                        Text("Debt Payoff Accelerator", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    Text("Simulate debt-free timelines by adjusting your monthly debt acceleration payment.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // Strategy Selector (Snowball vs Avalanche)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FinanceButton(
                            onClick = { viewModel.setDebtStrategy("Snowball") },
                            backgroundColor = if (sim.strategy == "Snowball") FinanceGreen else FinanceCardDark,
                            shadowColor = if (sim.strategy == "Snowball") FinanceGreenDark else FinanceCardShadow,
                            cornerRadius = 8.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("â„ï¸ Snowball (Smallest First)", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }

                        FinanceButton(
                            onClick = { viewModel.setDebtStrategy("Avalanche") },
                            backgroundColor = if (sim.strategy == "Avalanche") FinanceBlue else FinanceCardDark,
                            shadowColor = if (sim.strategy == "Avalanche") FinanceBlueDark else FinanceCardShadow,
                            cornerRadius = 8.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("âš¡ Avalanche (Highest APR)", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
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
                                Text("${sim.estimatedMonthsToDebtFree} Months", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = FinanceBlueDark)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Est. Interest Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CurrencyFormatter.format(sim.totalInterestPaid), fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = FinanceRedDark)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetWorthTrendCard(history: List<NetWorthPoint>, current: Double?) {
    ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShowChart, contentDescription = null, tint = FinanceGreen)
                Spacer(Modifier.width(8.dp))
                Text("Net Worth Trend", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }

            if (history.isEmpty()) {
                Text(
                    "No net-worth history yet. Net worth snapshots are recorded each week after a successful bank sync.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val latest = history.last().netWorth
                val first = history.first().netWorth
                val delta = latest - first
                Text(
                    text = CurrencyFormatter.format(current ?: latest),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.headlineMedium,
                    color = FinanceGreen
                )
                Text(
                    text = "Snapshot ${history.size} week${if (history.size == 1) "" else "s"} Â· ${if (delta >= 0) "+" else ""}${CurrencyFormatter.format(delta)} since start",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (delta >= 0) FinanceGreen else FinanceRed
                )

                if (history.size >= 2) {
                    val values = history.map { it.netWorth }
                    val minVal = values.min()
                    val maxVal = values.max()
                    val range = (maxVal - minVal).takeIf { it > 0 } ?: 1.0
                    val lineColor = FinanceGreen

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        val n = values.size
                        val stepX = size.width / (n - 1).toFloat()
                        fun yFor(v: Double): Float = size.height - ((v - minVal) / range * size.height).toFloat()

                        val fillPath = Path().apply {
                            moveTo(0f, size.height)
                            values.forEachIndexed { i, v ->
                                lineTo(i * stepX, yFor(v))
                            }
                            lineTo(size.width, size.height)
                            close()
                        }
                        drawPath(fillPath, color = lineColor.copy(alpha = 0.18f))

                        val linePath = Path().apply {
                            moveTo(0f, yFor(values[0]))
                            values.drop(1).forEachIndexed { i, v ->
                                lineTo((i + 1) * stepX, yFor(v))
                            }
                        }
                        drawPath(linePath, color = lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f))

                        values.forEachIndexed { i, v ->
                            drawCircle(color = lineColor, radius = 8f, center = androidx.compose.ui.geometry.Offset(i * stepX, yFor(v)))
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${CurrencyFormatter.format(maxVal)} max", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${CurrencyFormatter.format(minVal)} min", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** Per-merchant drill-down: tap a top merchant to expand its real transactions. */
@Composable
private fun MerchantDrillSection(uiState: InsightsUiState) {
    if (uiState.topMerchants.isEmpty()) return

    fun merchantKey(tx: Transaction): String = tx.payee.ifBlank { tx.originalDesc }.take(25)

    var expanded by remember { mutableStateOf<String?>(null) }

    val merchants = uiState.topMerchants.take(10)

    Card(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Top merchants", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                "Where your money goes â€” tap a merchant to see its transactions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            merchants.forEach { m ->
                val isOpen = expanded == m.merchant
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = if (isOpen) null else m.merchant },
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOpen) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(m.merchant, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("${m.count} tx â€¢ ${String.format(Locale.US, "%.1f%%", m.percentageOfTotal)} of spend", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(CurrencyFormatter.format(m.totalAmount), fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyLarge)
                        }

                        if (isOpen) {
                            val txs = uiState.filteredTransactions
                                .filter { merchantKey(it) == m.merchant }
                                .sortedByDescending { it.postedEpochSeconds }
                            Spacer(Modifier.height(6.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(6.dp))
                            if (txs.isEmpty()) {
                                Text("No matching transactions in this range.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                txs.forEach { tx ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(DateUtils.formatDate(tx.postedEpochSeconds), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(tx.category.ifBlank { "Uncategorized" }, style = MaterialTheme.typography.bodySmall)
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
            }
        }
    }
}

// -------------------------------------------------------------
// 4. Retirement & FIRE Projection Calculator
// -------------------------------------------------------------

@Composable
private fun RetirementTabContent(
    uiState: InsightsUiState,
    viewModel: InsightsViewModel
) {
    val inputs = uiState.retirementInputs
    val result = uiState.retirementProjection

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Alert Card
        item {
            val isAhead = result.isOnTrackForRetirement
            val statusColor = if (isAhead) FinanceGreen else Color(0xFFFF9900)
            val statusBg = if (isAhead) FinanceGreen.copy(alpha = 0.12f) else Color(0xFFFF9900).copy(alpha = 0.12f)

            ExpressiveCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = statusBg)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isAhead) Icons.Default.CheckCircle else Icons.Default.Flag,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isAhead) "ðŸŽ‰ On Track for Financial Freedom!" else "âš ï¸ Retirement Shortfall Detected",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleSmall,
                            color = statusColor
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (isAhead) {
                                "You are projected to have a surplus of ${CurrencyFormatter.format(result.surplusOrShortfallReal)} by age ${inputs.retirementAge}."
                            } else {
                                "Increase monthly savings by ${CurrencyFormatter.format(result.monthlySavingsGap)}/mo to eliminate the gap by age ${inputs.retirementAge}."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Key Milestone KPI Cards (2x2 Grid)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExpressiveCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Target FIRE Number", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                CurrencyFormatter.format(result.targetFireNumber),
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                                color = FinanceGreenDark
                            )
                            Text("at ${inputs.safeWithdrawalRatePercent}% SWR", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    ExpressiveCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Projected at Age ${inputs.retirementAge}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                CurrencyFormatter.format(result.projectedNestEggAtRetirementReal),
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (result.isOnTrackForRetirement) FinanceGreenDark else MaterialTheme.colorScheme.error
                            )
                            Text("in today's purchasing power", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExpressiveCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Safe Monthly Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${CurrencyFormatter.format(result.safeMonthlyRetirementIncomeReal)}/mo",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                                color = FinanceBlueDark
                            )
                            Text("vs ${CurrencyFormatter.format(result.desiredMonthlyRetirementSpend)}/mo target", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    ExpressiveCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Coast FIRE Milestone", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                CurrencyFormatter.format(result.coastFireNumber),
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (result.isCoastFireAchieved) FinanceGreenDark else Color(0xFFFF9900)
                            )
                            Text(if (result.isCoastFireAchieved) "âœ… Target Achieved!" else "â³ In Accumulation", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Custom Canvas Projection Area Chart
        item {
            ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
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
                        Text("Growth Trajectory (Real \$)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(FinanceGreen))
                            Spacer(Modifier.width(4.dp))
                            Text("Age ${inputs.currentAge} âž” ${inputs.retirementAge}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    val points = result.yearlyProjections
                    if (points.isNotEmpty()) {
                        val maxVal = max(result.targetFireNumber, points.maxOf { it.portfolioReal }) * 1.15

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            val w = size.width
                            val h = size.height

                            val path = Path()
                            val fillPath = Path()

                            points.forEachIndexed { idx, pt ->
                                val x = (idx.toFloat() / (points.size - 1).coerceAtLeast(1)) * w
                                val y = h - ((pt.portfolioReal / maxVal).toFloat() * h).coerceIn(0f, h)

                                if (idx == 0) {
                                    path.moveTo(x, y)
                                    fillPath.moveTo(x, h)
                                    fillPath.lineTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                    fillPath.lineTo(x, y)
                                }
                            }
                            fillPath.lineTo(w, h)
                            fillPath.close()

                            // Fill gradient
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(FinanceGreen.copy(alpha = 0.35f), FinanceGreen.copy(alpha = 0.02f))
                                )
                            )

                            // Stroke line
                            drawPath(
                                path = path,
                                color = FinanceGreen,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Draw Target FIRE line
                            val fireY = h - ((result.targetFireNumber / maxVal).toFloat() * h).coerceIn(0f, h)
                            drawLine(
                                color = Color(0xFFFF9900).copy(alpha = 0.7f),
                                start = androidx.compose.ui.geometry.Offset(0f, fireY),
                                end = androidx.compose.ui.geometry.Offset(w, fireY),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Age ${inputs.currentAge} (Now)", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            Text("Target FIRE: ${CurrencyFormatter.format(result.targetFireNumber)}", fontSize = 10.sp, color = Color(0xFFFF9900), fontWeight = FontWeight.Bold)
                            Text("Age ${inputs.lifeExpectancyAge}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }

        // Interactive Sliders & Parameter Controls
        item {
            ExpressiveCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Interactive Projection Sliders", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    // Current Age & Retirement Age
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Current Age: ${inputs.currentAge}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Retire at: ${inputs.retirementAge}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FinanceGreenDark)
                    }
                    Slider(
                        value = inputs.retirementAge.toFloat(),
                        onValueChange = { viewModel.updateRetirementInputs { it.copy(retirementAge = it.retirementAge.coerceAtLeast(it.currentAge).let { _ -> it.retirementAge }.let { _ -> it.retirementAge }.let { _ -> it.retirementAge }) } ; viewModel.updateRetirementInputs { old -> old.copy(retirementAge = it.toInt()) } },
                        valueRange = inputs.currentAge.toFloat()..80f,
                        steps = (80 - inputs.currentAge).coerceAtLeast(1)
                    )

                    // Current Savings
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Current Savings / Portfolio", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(CurrencyFormatter.format(inputs.currentSavings), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FinanceGreenDark)
                    }
                    Slider(
                        value = inputs.currentSavings.toFloat().coerceIn(0f, 500000f),
                        onValueChange = { viewModel.updateRetirementInputs { old -> old.copy(currentSavings = it.toDouble()) } },
                        valueRange = 0f..500000f
                    )

                    // Monthly Contribution
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Monthly Contribution", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${CurrencyFormatter.format(inputs.monthlyContribution)}/mo", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FinanceBlueDark)
                    }
                    Slider(
                        value = inputs.monthlyContribution.toFloat().coerceIn(0f, 5000f),
                        onValueChange = { viewModel.updateRetirementInputs { old -> old.copy(monthlyContribution = it.toDouble()) } },
                        valueRange = 0f..5000f
                    )

                    // Expected Annual Return %
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Expected Annual Return (Nominal)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${String.format("%.1f", inputs.expectedAnnualReturnPercent)}%", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Slider(
                        value = inputs.expectedAnnualReturnPercent.toFloat(),
                        onValueChange = { viewModel.updateRetirementInputs { old -> old.copy(expectedAnnualReturnPercent = it.toDouble()) } },
                        valueRange = 3f..14f,
                        steps = 21
                    )

                    // Expected Annual Inflation %
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Expected Annual Inflation", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${String.format("%.1f", inputs.expectedAnnualInflationPercent)}%", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Slider(
                        value = inputs.expectedAnnualInflationPercent.toFloat(),
                        onValueChange = { viewModel.updateRetirementInputs { old -> old.copy(expectedAnnualInflationPercent = it.toDouble()) } },
                        valueRange = 1f..6f,
                        steps = 9
                    )

                    // Desired Annual Living Expenses in Retirement
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Desired Annual Living Spend", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${CurrencyFormatter.format(inputs.desiredAnnualRetirementSpend)}/yr", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FinanceGreenDark)
                    }
                    Slider(
                        value = inputs.desiredAnnualRetirementSpend.toFloat().coerceIn(15000f, 150000f),
                        onValueChange = { viewModel.updateRetirementInputs { old -> old.copy(desiredAnnualRetirementSpend = it.toDouble()) } },
                        valueRange = 15000f..150000f
                    )

                    // Safe Withdrawal Rate (SWR) %
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Safe Withdrawal Rate (SWR)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${String.format("%.1f", inputs.safeWithdrawalRatePercent)}%", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Slider(
                        value = inputs.safeWithdrawalRatePercent.toFloat(),
                        onValueChange = { viewModel.updateRetirementInputs { old -> old.copy(safeWithdrawalRatePercent = it.toDouble()) } },
                        valueRange = 3f..5f,
                        steps = 7
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
        }
    }
}

