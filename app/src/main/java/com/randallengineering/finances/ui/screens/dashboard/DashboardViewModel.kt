package com.randallengineering.finances.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

data class TopCategoryItem(
    val category: String,
    val total: Double,
    val percentage: Double
)

data class DashboardUiState(
    val totalBalance: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthExpenses: Double = 0.0,
    val monthNet: Double = 0.0,
    val topCategories: List<TopCategoryItem> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val uncategorizedCount: Int = 0,
    val isLoading: Boolean = false
)

class DashboardViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeTransactions()
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            transactionRepository.getTransactionsFlow().collect { resource ->
                val list = resource.getOrNull().orEmpty()
                withContext(Dispatchers.Default) {
                    val metrics = calculate(list)
                    _uiState.update {
                        it.copy(
                            totalBalance = metrics.totalBalance,
                            monthIncome = metrics.monthIncome,
                            monthExpenses = metrics.monthExpenses,
                            monthNet = metrics.monthNet,
                            topCategories = metrics.topCategories,
                            recentTransactions = metrics.recent,
                            uncategorizedCount = metrics.uncategorized,
                            isLoading = resource.isLoading
                        )
                    }
                }
            }
        }
    }

    private fun calculate(transactions: List<Transaction>): DashboardMetrics {
        val now = LocalDate.now()
        val monthStart = now.with(TemporalAdjusters.firstDayOfMonth())
            .atStartOfDay(ZoneId.systemDefault()).toEpochSecond()

        val monthTxs = transactions.filter { it.postedEpochSeconds >= monthStart }

        val totalBalance = transactions.sumOf { it.amount }
        val monthIncome = monthTxs
            .filter { it.amount > 0 || it.category.equals("Income", ignoreCase = true) }
            .sumOf { abs(it.amount) }
        val monthExpenses = monthTxs
            .filter { it.amount < 0 && !it.category.equals("Income", ignoreCase = true) }
            .sumOf { abs(it.amount) }

        val catMap = mutableMapOf<String, Double>()
        for (tx in monthTxs.filter { it.amount < 0 && !it.category.equals("Income", ignoreCase = true) }) {
            val cat = tx.category.ifBlank { "Uncategorized" }
            catMap[cat] = (catMap[cat] ?: 0.0) + abs(tx.amount)
        }
        val topCategories = catMap.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { (c, v) ->
                TopCategoryItem(
                    category = c,
                    total = v,
                    percentage = if (monthExpenses > 0) (v / monthExpenses * 100.0) else 0.0
                )
            }

        val recent = transactions.sortedByDescending { it.postedEpochSeconds }.take(8)
        val uncategorized = transactions.count {
            it.category.equals("Uncategorized", ignoreCase = true) || it.category.isBlank()
        }

        return DashboardMetrics(
            totalBalance = totalBalance,
            monthIncome = monthIncome,
            monthExpenses = monthExpenses,
            monthNet = monthIncome - monthExpenses,
            topCategories = topCategories,
            recent = recent,
            uncategorized = uncategorized
        )
    }

    private data class DashboardMetrics(
        val totalBalance: Double,
        val monthIncome: Double,
        val monthExpenses: Double,
        val monthNet: Double,
        val topCategories: List<TopCategoryItem>,
        val recent: List<Transaction>,
        val uncategorized: Int
    )
}
