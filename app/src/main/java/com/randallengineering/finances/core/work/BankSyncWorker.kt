package com.randallengineering.finances.core.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.core.notifications.NotificationHelper
import com.randallengineering.finances.data.repository.BudgetRepository
import com.randallengineering.finances.data.repository.CategoryRepository
import com.randallengineering.finances.data.repository.RuleRepository
import com.randallengineering.finances.data.repository.SimpleFinRepository
import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.domain.usecase.BudgetCalculatorUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.getKoin
import kotlin.math.abs

class BankSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val simpleFinRepository: SimpleFinRepository = getKoin().get()
            val transactionRepository: TransactionRepository = getKoin().get()
            val ruleRepository: RuleRepository = getKoin().get()
            val budgetRepository: BudgetRepository = getKoin().get()
            val categoryRepository: CategoryRepository = getKoin().get()
            val budgetCalculatorUseCase: BudgetCalculatorUseCase = getKoin().get()

            // 1. Check if SimpleFIN is configured
            val configRes = simpleFinRepository.getConfigFlow().firstOrNull()
            val config = (configRes as? Resource.Success)?.data
            if (config?.accessUrlConfigured != true) {
                return@withContext Result.success()
            }

            // 2. Capture baseline count
            val beforeTxs = transactionRepository.getTransactionsFlow().firstOrNull()?.getOrNull().orEmpty()
            val beforeIds = beforeTxs.map { it.id }.toSet()

            // 3. Trigger SimpleFIN background sync (90 days)
            val syncResult = simpleFinRepository.triggerSync(daysBack = 90)
            if (syncResult is Resource.Error) {
                return@withContext Result.retry()
            }

            // 4. Evaluate new incoming transactions & auto-rules
            val afterTxs = transactionRepository.getTransactionsFlow().firstOrNull()?.getOrNull().orEmpty()
            val newTxs = afterTxs.filter { it.id !in beforeIds }

            val rules = ruleRepository.getRulesFlow().firstOrNull()?.getOrNull().orEmpty()
            val autoRunEnabled = ruleRepository.getAutoRunEnabledFlow().firstOrNull() ?: true

            var autoCategorizedCount = 0
            val updatedTxs = mutableListOf<Transaction>()

            if (autoRunEnabled && rules.isNotEmpty()) {
                for (tx in afterTxs) {
                    val matchingRule = rules.firstOrNull { it.matches(tx.originalDesc, tx.amount) }
                    if (matchingRule != null) {
                        val hasChanged = !tx.category.equals(matchingRule.category, ignoreCase = true) ||
                                !tx.subCategory.equals(matchingRule.subCategory, ignoreCase = true)
                        if (hasChanged) {
                            val updated = tx.copy(
                                category = matchingRule.category,
                                subCategory = matchingRule.subCategory,
                                matchedRuleId = matchingRule.id
                            )
                            updatedTxs.add(updated)
                            autoCategorizedCount++
                        }
                    }
                }
                if (updatedTxs.isNotEmpty()) {
                    transactionRepository.saveTransactions(updatedTxs)
                }
            }

            // 5. Send smart sync notification if new items arrived
            if (newTxs.isNotEmpty()) {
                NotificationHelper.sendSyncSummaryNotification(
                    context = appContext,
                    newTxCount = newTxs.size,
                    autoCategorizedCount = autoCategorizedCount
                )

                // Check for large individual transactions (> $150)
                newTxs.filter { it.amount < -150.0 }.forEach { largeTx ->
                    NotificationHelper.sendHighSpendAlert(
                        context = appContext,
                        merchant = largeTx.payee.ifBlank { largeTx.originalDesc },
                        amount = abs(largeTx.amount)
                    )
                }
            }

            // 6. Check budget limits and send warnings (Functional #9)
            val budgets = budgetRepository.getBudgetsFlow().firstOrNull()?.getOrNull().orEmpty()
            val incomeCategory = categoryRepository.getIncomeCategory()
            val calculation = budgetCalculatorUseCase.calculate(budgets, afterTxs, incomeCategory)

            calculation.calculatedBudgets.forEach { budget ->
                if (budget.effectiveTargetAmount > 0) {
                    val percent = budget.spentAmount / budget.effectiveTargetAmount
                    if (percent >= 0.90) {
                        NotificationHelper.sendBudgetWarningNotification(
                            context = appContext,
                            category = budget.displayName,
                            percent = percent,
                            spent = budget.spentAmount,
                            limit = budget.effectiveTargetAmount
                        )
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
