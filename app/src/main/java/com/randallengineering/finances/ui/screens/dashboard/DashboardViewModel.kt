package com.randallengineering.finances.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.core.prefs.DashboardLayoutRepository
import com.randallengineering.finances.core.finance.TransferDetection
import com.randallengineering.finances.data.repository.AccountRepository
import com.randallengineering.finances.data.repository.BudgetRepository
import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.model.DashboardCardType
import com.randallengineering.finances.domain.model.SimpleFinAccount
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.domain.usecase.BudgetCalculatorUseCase
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
    val savingsRate: Double = 0.0,
    val incomeDeltaPct: Double? = null,
    val expenseDeltaPct: Double? = null,
    val dailyAllowance: Double = 0.0,
    val budgetAlertCount: Int = 0,
    val topCategories: List<TopCategoryItem> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val uncategorizedCount: Int = 0,
    val hasTransactions: Boolean = false,
    val accounts: List<SimpleFinAccount> = emptyList(),
    val accountTxCounts: Map<String, Int> = emptyMap(),
    val netWorth: Double = 0.0,
    val hasLiveBalances: Boolean = false,
    val enabledCards: List<DashboardCardType> = DashboardCardType.entries.toList(),
    val fullLayout: List<Pair<DashboardCardType, Boolean>> = DashboardCardType.entries.map { it to true },
    val isLoading: Boolean = false
)

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val budgetCalculatorUseCase: BudgetCalculatorUseCase,
    private val layoutRepository: DashboardLayoutRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    @Volatile
    private var latestTransactions: List<Transaction> = emptyList()

    init {
        loadLayout()
        observeTransactions()
        observeBudgets()
        observeAccounts()
    }

    private fun observeAccounts() {
        viewModelScope.launch {
            accountRepository.getAccountsFlow().collect { resource ->
                val accounts = resource.getOrNull().orEmpty()
                val netWorth = if (accounts.isNotEmpty()) accounts.sumOf { it.balance } else 0.0
                _uiState.update {
                    it.copy(
                        accounts = accounts,
                        netWorth = netWorth,
                        hasLiveBalances = accounts.isNotEmpty()
                    )
                }
            }
        }
    }

    private fun loadLayout() {
        var layout = layoutRepository.getLayout()
        // Auto-add any newly-introduced card types (e.g. ACCOUNTS) so they show
        // even if the user has an older saved/customized layout.
        val present = layout.map { it.first }.toSet()
        val missing = DashboardCardType.entries.filter { it !in present }.map { it to true }
        if (missing.isNotEmpty()) {
            layout = layout + missing
            layoutRepository.saveLayout(layout)
        }
        _uiState.update {
            it.copy(
                fullLayout = layout,
                enabledCards = layout.filter { snd -> snd.second }.map { fst -> fst.first }
            )
        }
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            transactionRepository.getTransactionsFlow().collect { resource ->
                val list = resource.getOrNull().orEmpty()
                latestTransactions = list
                withContext(Dispatchers.Default) {
                    val metrics = calculate(list)
                    _uiState.update {
                        it.copy(
                            totalBalance = metrics.totalBalance,
                            monthIncome = metrics.monthIncome,
                            monthExpenses = metrics.monthExpenses,
                            monthNet = metrics.monthNet,
                            savingsRate = metrics.savingsRate,
                            incomeDeltaPct = pctDelta(metrics.monthIncome, metrics.lastMonthIncome),
                            expenseDeltaPct = pctDelta(metrics.monthExpenses, metrics.lastMonthExpenses),
                            topCategories = metrics.topCategories,
                            recentTransactions = metrics.recent,
                            uncategorizedCount = metrics.uncategorized,
                            accountTxCounts = list.groupingBy { it.accountId }.eachCount(),
                            hasTransactions = list.isNotEmpty(),
                            isLoading = resource.isLoading
                        )
                    }
                }
            }
        }
    }

    private fun observeBudgets() {
        viewModelScope.launch {
            budgetRepository.getBudgetsFlow().collect { resource ->
                val budgets = resource.getOrNull().orEmpty()
                withContext(Dispatchers.Default) {
                    val result = budgetCalculatorUseCase.calculate(budgets, latestTransactions)
                    val alerts = result.calculatedBudgets.count { it.isOverBudget || it.pacingPercent >= 90.0 }
                    _uiState.update {
                        it.copy(
                            dailyAllowance = result.targetDailyAllowance,
                            budgetAlertCount = alerts
                        )
                    }
                }
            }
        }
    }

    fun toggleCard(type: DashboardCardType) {
        val layout = _uiState.value.fullLayout.map { (t, enabled) -> if (t == type) t to !enabled else t to enabled }
        layoutRepository.saveLayout(layout)
        _uiState.update {
            it.copy(
                fullLayout = layout,
                enabledCards = layout.filter { snd -> snd.second }.map { fst -> fst.first }
            )
        }
    }

    fun moveCard(type: DashboardCardType, moveUp: Boolean) {
        val layout = _uiState.value.fullLayout.toMutableList()
        val idx = layout.indexOfFirst { it.first == type }
        if (idx < 0) return
        val target = if (moveUp) idx - 1 else idx + 1
        if (target < 0 || target >= layout.size) return
        val item = layout.removeAt(idx)
        layout.add(target, item)
        layoutRepository.saveLayout(layout)
        _uiState.update { it.copy(fullLayout = layout) }
    }

    fun resetLayout() {
        val layout = DashboardCardType.entries.map { it to true }
        layoutRepository.saveLayout(layout)
        _uiState.update {
            it.copy(
                fullLayout = layout,
                enabledCards = DashboardCardType.entries.toList()
            )
        }
    }

    private fun calculate(transactions: List<Transaction>): DashboardMetrics {
        val now = LocalDate.now()
        val monthStart = now.with(TemporalAdjusters.firstDayOfMonth())
            .atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
        val prevMonthStart = now.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth())
            .atStartOfDay(ZoneId.systemDefault()).toEpochSecond()

        val monthTxs = transactions.filter { it.postedEpochSeconds >= monthStart }
        val prevMonthTxs = transactions.filter { it.postedEpochSeconds >= prevMonthStart && it.postedEpochSeconds < monthStart }

        // Internal transfers between own accounts are not income or spending.
        val transferIds = TransferDetection.detectTransferIds(transactions)
        fun isIncomeTx(tx: Transaction): Boolean =
            tx.amount > 0 && tx.id !in transferIds
        fun isExpenseTx(tx: Transaction): Boolean =
            tx.amount < 0 && !tx.category.equals("Income", ignoreCase = true) && tx.id !in transferIds

        val totalBalance = transactions.sumOf { it.amount }
        val monthIncome = monthTxs
            .filter { isIncomeTx(it) }
            .sumOf { abs(it.amount) }
        val monthExpenses = monthTxs
            .filter { isExpenseTx(it) }
            .sumOf { abs(it.amount) }
        val lastMonthIncome = prevMonthTxs
            .filter { isIncomeTx(it) }
            .sumOf { abs(it.amount) }
        val lastMonthExpenses = prevMonthTxs
            .filter { isExpenseTx(it) }
            .sumOf { abs(it.amount) }
        val monthNet = monthIncome - monthExpenses

        val catMap = mutableMapOf<String, Double>()
        for (tx in monthTxs.filter { isExpenseTx(it) }) {
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
            lastMonthIncome = lastMonthIncome,
            lastMonthExpenses = lastMonthExpenses,
            monthNet = monthNet,
            savingsRate = if (monthIncome > 0) (monthNet / monthIncome * 100.0) else 0.0,
            topCategories = topCategories,
            recent = recent,
            uncategorized = uncategorized
        )
    }

    private data class DashboardMetrics(
        val totalBalance: Double,
        val monthIncome: Double,
        val monthExpenses: Double,
        val lastMonthIncome: Double,
        val lastMonthExpenses: Double,
        val monthNet: Double,
        val savingsRate: Double,
        val topCategories: List<TopCategoryItem>,
        val recent: List<Transaction>,
        val uncategorized: Int
    )
}

/** Percent change vs last month; null when there's no prior-month data to compare. */
private fun pctDelta(current: Double, previous: Double): Double? =
    if (previous > 0) (current - previous) / previous * 100.0 else null
