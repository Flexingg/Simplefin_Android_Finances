package com.randallengineering.finances.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

enum class TimeRange(val label: String) {
    THIS_MONTH("This Month"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    LAST_90_DAYS("Last 90 Days"),
    THIS_YEAR("This Year"),
    LAST_365_DAYS("1 Year"),
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

data class InsightsUiState(
    val selectedTimeRange: TimeRange = TimeRange.THIS_MONTH,
    val allTransactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netCashflow: Double = 0.0,
    val savingsRate: Double = 0.0,
    val averageDailySpend: Double = 0.0,
    val categoryBreakdown: List<CategorySpendItem> = emptyList(),
    val dailySpendingTrend: List<DaySpendItem> = emptyList(),
    val topMerchants: List<MerchantSpendItem> = emptyList(),
    val isLoading: Boolean = false
)

class InsightsViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        observeTransactions()
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            transactionRepository.getTransactionsFlow().collect { resource ->
                val list = resource.getOrNull().orEmpty()
                _uiState.update { current ->
                    val filtered = filterTransactionsByRange(list, current.selectedTimeRange)
                    val metrics = calculateMetrics(filtered, current.selectedTimeRange)
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
                        topMerchants = metrics.topMerchants,
                        isLoading = resource.isLoading
                    )
                }
            }
        }
    }

    fun selectTimeRange(range: TimeRange) {
        _uiState.update { current ->
            val filtered = filterTransactionsByRange(current.allTransactions, range)
            val metrics = calculateMetrics(filtered, range)
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

    private fun filterTransactionsByRange(list: List<Transaction>, range: TimeRange): List<Transaction> {
        val now = LocalDate.now(ZoneId.systemDefault())
        val startEpochSeconds: Long = when (range) {
            TimeRange.THIS_MONTH -> {
                val startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth())
                startOfMonth.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
            }
            TimeRange.LAST_7_DAYS -> {
                now.minusDays(7).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
            }
            TimeRange.LAST_30_DAYS -> {
                now.minusDays(30).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
            }
            TimeRange.LAST_90_DAYS -> {
                now.minusDays(90).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
            }
            TimeRange.THIS_YEAR -> {
                val startOfYear = now.with(TemporalAdjusters.firstDayOfYear())
                startOfYear.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
            }
            TimeRange.LAST_365_DAYS -> {
                now.minusDays(365).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
            }
            TimeRange.ALL_TIME -> 0L
        }

        return list.filter { it.postedEpochSeconds >= startEpochSeconds }
    }

    private data class CalculatedMetrics(
        val totalIncome: Double,
        val totalExpenses: Double,
        val netCashflow: Double,
        val savingsRate: Double,
        val averageDailySpend: Double,
        val categoryBreakdown: List<CategorySpendItem>,
        val dailySpendingTrend: List<DaySpendItem>,
        val topMerchants: List<MerchantSpendItem>
    )

    private fun calculateMetrics(filtered: List<Transaction>, range: TimeRange): CalculatedMetrics {
        var income = 0.0
        var expenses = 0.0
        val categoryMap = mutableMapOf<String, Double>()
        val categoryCountMap = mutableMapOf<String, Int>()
        val merchantMap = mutableMapOf<String, Double>()
        val merchantCountMap = mutableMapOf<String, Int>()
        val dayMap = mutableMapOf<String, Double>()

        for (tx in filtered) {
            if (tx.isIncome) {
                income += tx.amount
            } else {
                val absAmount = abs(tx.amount)
                expenses += absAmount

                // Categories
                val cat = tx.category.ifBlank { "Uncategorized" }
                categoryMap[cat] = (categoryMap[cat] ?: 0.0) + absAmount
                categoryCountMap[cat] = (categoryCountMap[cat] ?: 0) + 1

                // Merchants
                val merch = tx.payee.ifBlank { tx.originalDesc.take(24) }.ifBlank { "Unknown Merchant" }
                merchantMap[merch] = (merchantMap[merch] ?: 0.0) + absAmount
                merchantCountMap[merch] = (merchantCountMap[merch] ?: 0) + 1

                // Day trend
                val dateStr = Instant.ofEpochSecond(tx.postedEpochSeconds)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toString()
                dayMap[dateStr] = (dayMap[dateStr] ?: 0.0) + absAmount
            }
        }

        val net = income - expenses
        val savingsRate = if (income > 0) ((net / income) * 100.0).coerceIn(-100.0, 100.0) else 0.0

        val daysCount = when (range) {
            TimeRange.THIS_MONTH -> LocalDate.now().dayOfMonth.coerceAtLeast(1)
            TimeRange.LAST_7_DAYS -> 7
            TimeRange.LAST_30_DAYS -> 30
            TimeRange.LAST_90_DAYS -> 90
            TimeRange.THIS_YEAR -> LocalDate.now().dayOfYear.coerceAtLeast(1)
            TimeRange.LAST_365_DAYS -> 365
            TimeRange.ALL_TIME -> (dayMap.size).coerceAtLeast(1)
        }
        val avgDaily = if (daysCount > 0) expenses / daysCount else 0.0

        // Category breakdown sorted descending
        val categoryList = categoryMap.map { (cat, amount) ->
            val pct = if (expenses > 0) (amount / expenses) * 100.0 else 0.0
            CategorySpendItem(
                category = cat,
                totalAmount = amount,
                percentageOfTotal = pct,
                count = categoryCountMap[cat] ?: 1
            )
        }.sortedByDescending { it.totalAmount }

        // Top merchants sorted descending
        val merchantList = merchantMap.map { (merch, amount) ->
            val pct = if (expenses > 0) (amount / expenses) * 100.0 else 0.0
            MerchantSpendItem(
                merchant = merch,
                totalAmount = amount,
                count = merchantCountMap[merch] ?: 1,
                percentageOfTotal = pct
            )
        }.sortedByDescending { it.totalAmount }.take(8)

        // Daily trend list sorted by date ascending
        val trendList = dayMap.entries.sortedBy { it.key }.takeLast(30).map { (dateStr, amount) ->
            DaySpendItem(
                label = dateStr.substring(5), // "MM-DD"
                amount = amount,
                epochDay = 0L
            )
        }

        return CalculatedMetrics(
            totalIncome = income,
            totalExpenses = expenses,
            netCashflow = net,
            savingsRate = savingsRate,
            averageDailySpend = avgDaily,
            categoryBreakdown = categoryList,
            dailySpendingTrend = trendList,
            topMerchants = merchantList
        )
    }
}
