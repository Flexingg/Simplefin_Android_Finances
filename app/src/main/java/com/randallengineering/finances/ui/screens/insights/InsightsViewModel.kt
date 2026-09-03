package com.randallengineering.finances.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randallengineering.finances.core.finance.TransferDetection
import com.randallengineering.finances.data.repository.NetWorthRepository
import com.randallengineering.finances.data.repository.NetWorthPoint
import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.math.max

enum class TimeRange(val label: String) {
    THIS_MONTH("This Month"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    LAST_90_DAYS("Last 90 Days"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time")
}

data class CategorySpendItem(
    val category: String,
    val totalAmount: Double,
    val percentageOfTotal: Double,
    val count: Int
)

data class DaySpendItem(
    val label: String,
    val amount: Double,
    val epochDay: Long
)

data class MerchantSpendItem(
    val merchant: String,
    val totalAmount: Double,
    val count: Int,
    val percentageOfTotal: Double
)

data class MonthlyTrendItem(
    val monthLabel: String,       // e.g. "Jun", "Jul", "Aug"
    val yearMonth: String,        // "2026-08"
    val totalIncome: Double,
    val totalExpenses: Double,
    val netSavings: Double
)

data class HeatmapDayItem(
    val dayOfMonth: Int,
    val dayOfWeekLabel: String,   // "M", "T", "W"
    val spendAmount: Double,
    val intensityLevel: Int       // 0 = $0, 1 = <$25, 2 = <$75, 3 = <$150, 4 = $150+
)

data class DebtPayoffSimulation(
    val strategy: String = "Snowball", // "Snowball" vs "Avalanche"
    val monthlyPaymentAmount: Double = 300.0,
    val totalDebtAmount: Double = 4500.0,
    val estimatedMonthsToDebtFree: Int = 16,
    val totalInterestPaid: Double = 320.0
)

data class InsightsUiState(
    val selectedTimeRange: TimeRange = TimeRange.THIS_MONTH,
    val selectedInsightsTab: Int = 0, // 0 = Trends & Heatmaps, 1 = Net Worth & Debt
    val allTransactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netCashflow: Double = 0.0,
    val savingsRate: Double = 0.0,
    val averageDailySpend: Double = 0.0,
    val categoryBreakdown: List<CategorySpendItem> = emptyList(),
    val dailySpendingTrend: List<DaySpendItem> = emptyList(),
    val monthlyTrends: List<MonthlyTrendItem> = emptyList(),
    val currentMonthHeatmap: List<HeatmapDayItem> = emptyList(),
    val topMerchants: List<MerchantSpendItem> = emptyList(),
    val estimatedNetWorth: Double = 0.0,
    val netWorthHistory: List<NetWorthPoint> = emptyList(),
    val liveNetWorth: Double? = null,
    val debtSimulation: DebtPayoffSimulation = DebtPayoffSimulation(),
    val isLoading: Boolean = false
)

class InsightsViewModel(
    private val transactionRepository: TransactionRepository,
    private val netWorthRepository: NetWorthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        observeTransactions()
        observeNetWorth()
    }

    private fun observeNetWorth() {
        viewModelScope.launch {
            netWorthRepository.snapshots.collect { history ->
                _uiState.update {
                    it.copy(
                        netWorthHistory = history,
                        liveNetWorth = history.lastOrNull()?.netWorth
                    )
                }
            }
        }
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            transactionRepository.getTransactionsFlow().collect { resource ->
                val list = resource.getOrNull().orEmpty()
                withContext(Dispatchers.Default) {
                    val filtered = filterTransactionsByRange(list, _uiState.value.selectedTimeRange)
                    val metrics = calculateMetrics(filtered, _uiState.value.selectedTimeRange)
                    val multiMonthTrends = calculateMultiMonthTrends(list)
                    val heatmap = calculateCurrentMonthHeatmap(list)
                    val netWorth = calculateEstimatedNetWorth(list)

                    _uiState.update { current ->
                        current.copy(
                            allTransactions = list,
                            filteredTransactions = filtered,
                            totalIncome = metrics.totalIncome,
                            totalExpenses = metrics.totalExpenses,
                            netCashflow = metrics.netCashflow,
                            savingsRate = metrics.savingsRate,
                            averageDailySpend = metrics.averageDailySpend,
                            categoryBreakdown = metrics.categoryBreakdown,
                            dailySpendingTrend = metrics.dailySpendingTrend,
                            monthlyTrends = multiMonthTrends,
                            currentMonthHeatmap = heatmap,
                            topMerchants = metrics.topMerchants,
                            estimatedNetWorth = netWorth,
                            isLoading = resource.isLoading
                        )
                    }
                }
            }
        }
    }

    fun selectTimeRange(range: TimeRange) {
        viewModelScope.launch(Dispatchers.Default) {
            val list = _uiState.value.allTransactions
            val filtered = filterTransactionsByRange(list, range)
            val metrics = calculateMetrics(filtered, range)
            _uiState.update { current ->
                current.copy(
                    selectedTimeRange = range,
                    filteredTransactions = filtered,
                    totalIncome = metrics.totalIncome,
                    totalExpenses = metrics.totalExpenses,
                    netCashflow = metrics.netCashflow,
                    savingsRate = metrics.savingsRate,
                    averageDailySpend = metrics.averageDailySpend,
                    categoryBreakdown = metrics.categoryBreakdown,
                    dailySpendingTrend = metrics.dailySpendingTrend,
                    topMerchants = metrics.topMerchants
                )
            }
        }
    }

    fun selectInsightsTab(tab: Int) {
        _uiState.update { it.copy(selectedInsightsTab = tab) }
    }

    fun updateDebtPayoffMonthlyPayment(payment: Double) {
        val cleanPayment = max(50.0, payment)
        val months = (4500.0 / cleanPayment).toInt() + 1
        val interest = months * (4500.0 * 0.015)
        _uiState.update {
            it.copy(
                debtSimulation = it.debtSimulation.copy(
                    monthlyPaymentAmount = cleanPayment,
                    estimatedMonthsToDebtFree = months,
                    totalInterestPaid = interest
                )
            )
        }
    }

    fun setDebtStrategy(strategy: String) {
        _uiState.update {
            it.copy(debtSimulation = it.debtSimulation.copy(strategy = strategy))
        }
    }

    private fun calculateMultiMonthTrends(transactions: List<Transaction>): List<MonthlyTrendItem> {
        val now = LocalDate.now()
        val result = mutableListOf<MonthlyTrendItem>()
        val transferIds = TransferDetection.detectTransferIds(transactions)

        for (i in 5 downTo 0) {
            val targetMonth = now.minusMonths(i.toLong())
            val startEpoch = targetMonth.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
            val endEpoch = targetMonth.withDayOfMonth(targetMonth.lengthOfMonth()).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond()

            val monthTxs = transactions.filter { it.postedEpochSeconds in startEpoch..endEpoch && it.id !in transferIds }
            val income = monthTxs.filter { it.amount > 0 || it.category.equals("Income", ignoreCase = true) }.sumOf { abs(it.amount) }
            val expense = monthTxs.filter { it.amount < 0 && !it.category.equals("Income", ignoreCase = true) }.sumOf { abs(it.amount) }

            result.add(
                MonthlyTrendItem(
                    monthLabel = targetMonth.format(DateTimeFormatter.ofPattern("MMM")),
                    yearMonth = targetMonth.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                    totalIncome = income,
                    totalExpenses = expense,
                    netSavings = income - expense
                )
            )
        }
        return result
    }

    private fun calculateCurrentMonthHeatmap(transactions: List<Transaction>): List<HeatmapDayItem> {
        val now = LocalDate.now()
        val daysInMonth = now.lengthOfMonth()
        val startEpoch = now.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
        val endEpoch = now.withDayOfMonth(daysInMonth).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond()

        val monthTxs = transactions.filter { it.postedEpochSeconds in startEpoch..endEpoch && it.amount < 0 }

        val spendPerDay = mutableMapOf<Int, Double>()
        for (tx in monthTxs) {
            val date = Instant.ofEpochSecond(tx.postedEpochSeconds).atZone(ZoneId.systemDefault()).toLocalDate()
            val day = date.dayOfMonth
            spendPerDay[day] = (spendPerDay[day] ?: 0.0) + abs(tx.amount)
        }

        val items = mutableListOf<HeatmapDayItem>()
        for (day in 1..daysInMonth) {
            val spend = spendPerDay[day] ?: 0.0
            val date = now.withDayOfMonth(day)
            val dayOfWeek = date.format(DateTimeFormatter.ofPattern("EEEEE")) // "M", "T", "W"

            val intensity = when {
                spend == 0.0 -> 0
                spend < 25.0 -> 1
                spend < 75.0 -> 2
                spend < 150.0 -> 3
                else -> 4
            }

            items.add(
                HeatmapDayItem(
                    dayOfMonth = day,
                    dayOfWeekLabel = dayOfWeek,
                    spendAmount = spend,
                    intensityLevel = intensity
                )
            )
        }
        return items
    }

    private fun calculateEstimatedNetWorth(transactions: List<Transaction>): Double {
        val totalIncome = transactions.filter { it.amount > 0 || it.category.equals("Income", ignoreCase = true) }.sumOf { abs(it.amount) }
        val totalExpense = transactions.filter { it.amount < 0 && !it.category.equals("Income", ignoreCase = true) }.sumOf { abs(it.amount) }
        return (totalIncome - totalExpense)
    }

    private fun filterTransactionsByRange(transactions: List<Transaction>, range: TimeRange): List<Transaction> {
        val now = LocalDate.now()
        val zone = ZoneId.systemDefault()

        return when (range) {
            TimeRange.THIS_MONTH -> {
                val start = now.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(zone).toEpochSecond()
                transactions.filter { it.postedEpochSeconds >= start }
            }
            TimeRange.LAST_7_DAYS -> {
                val start = now.minusDays(7).atStartOfDay(zone).toEpochSecond()
                transactions.filter { it.postedEpochSeconds >= start }
            }
            TimeRange.LAST_30_DAYS -> {
                val start = now.minusDays(30).atStartOfDay(zone).toEpochSecond()
                transactions.filter { it.postedEpochSeconds >= start }
            }
            TimeRange.LAST_90_DAYS -> {
                val start = now.minusDays(90).atStartOfDay(zone).toEpochSecond()
                transactions.filter { it.postedEpochSeconds >= start }
            }
            TimeRange.THIS_YEAR -> {
                val start = now.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay(zone).toEpochSecond()
                transactions.filter { it.postedEpochSeconds >= start }
            }
            TimeRange.ALL_TIME -> transactions
        }
    }

    private fun calculateMetrics(transactions: List<Transaction>, range: TimeRange): MetricsResult {
        var income = 0.0
        var expenses = 0.0

        val transferIds = TransferDetection.detectTransferIds(transactions)
        val categoryMap = mutableMapOf<String, Double>()
        val categoryCountMap = mutableMapOf<String, Int>()
        val merchantMap = mutableMapOf<String, Double>()
        val merchantCountMap = mutableMapOf<String, Int>()
        val daySpendMap = mutableMapOf<Long, Double>()

        for (tx in transactions) {
            if (tx.id in transferIds) continue // internal transfers are not income/spending
            val isIncome = tx.amount > 0 || tx.category.equals("Income", ignoreCase = true)
            if (isIncome) {
                income += abs(tx.amount)
            } else {
                val amount = abs(tx.amount)
                expenses += amount

                // Category allocation
                if (tx.isSplit && tx.splits.isNotEmpty()) {
                    for (split in tx.splits) {
                        val splitCat = split.category.ifBlank { "Uncategorized" }
                        val splitAmt = abs(split.amount)
                        categoryMap[splitCat] = (categoryMap[splitCat] ?: 0.0) + splitAmt
                        categoryCountMap[splitCat] = (categoryCountMap[splitCat] ?: 0) + 1
                    }
                } else {
                    val cat = tx.category.ifBlank { "Uncategorized" }
                    categoryMap[cat] = (categoryMap[cat] ?: 0.0) + amount
                    categoryCountMap[cat] = (categoryCountMap[cat] ?: 0) + 1
                }

                // Merchant spend
                val merchant = tx.payee.ifBlank { tx.originalDesc }.take(25)
                merchantMap[merchant] = (merchantMap[merchant] ?: 0.0) + amount
                merchantCountMap[merchant] = (merchantCountMap[merchant] ?: 0) + 1

                // Daily grouping
                val epochDay = tx.postedEpochSeconds / 86400
                daySpendMap[epochDay] = (daySpendMap[epochDay] ?: 0.0) + amount
            }
        }

        val net = income - expenses
        val savingsRate = if (income > 0) ((income - expenses) / income * 100.0).coerceIn(-100.0, 100.0) else 0.0

        val daysCount = when (range) {
            TimeRange.LAST_7_DAYS -> 7
            TimeRange.LAST_30_DAYS -> 30
            TimeRange.LAST_90_DAYS -> 90
            TimeRange.THIS_MONTH -> LocalDate.now().dayOfMonth
            TimeRange.THIS_YEAR -> LocalDate.now().dayOfYear
            TimeRange.ALL_TIME -> max(1, daySpendMap.size)
        }
        val avgDaily = expenses / max(1, daysCount)

        val catList = categoryMap.map { (cat, total) ->
            CategorySpendItem(
                category = cat,
                totalAmount = total,
                percentageOfTotal = if (expenses > 0) (total / expenses * 100.0) else 0.0,
                count = categoryCountMap[cat] ?: 0
            )
        }.sortedByDescending { it.totalAmount }

        val merchantList = merchantMap.map { (merchant, total) ->
            MerchantSpendItem(
                merchant = merchant,
                totalAmount = total,
                count = merchantCountMap[merchant] ?: 0,
                percentageOfTotal = if (expenses > 0) (total / expenses * 100.0) else 0.0
            )
        }.sortedByDescending { it.totalAmount }.take(10)

        val trendList = daySpendMap.entries.sortedBy { it.key }.takeLast(14).map { (epochDay, amount) ->
            val date = LocalDate.ofEpochDay(epochDay)
            DaySpendItem(
                label = "${date.monthValue}/${date.dayOfMonth}",
                amount = amount,
                epochDay = epochDay
            )
        }

        return MetricsResult(
            totalIncome = income,
            totalExpenses = expenses,
            netCashflow = net,
            savingsRate = savingsRate,
            averageDailySpend = avgDaily,
            categoryBreakdown = catList,
            dailySpendingTrend = trendList,
            topMerchants = merchantList
        )
    }

    private data class MetricsResult(
        val totalIncome: Double,
        val totalExpenses: Double,
        val netCashflow: Double,
        val savingsRate: Double,
        val averageDailySpend: Double,
        val categoryBreakdown: List<CategorySpendItem>,
        val dailySpendingTrend: List<DaySpendItem>,
        val topMerchants: List<MerchantSpendItem>
    )
}
