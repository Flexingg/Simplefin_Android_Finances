package com.randallengineering.finances.ui.screens.quest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.core.util.CurrencyFormatter
import com.randallengineering.finances.data.repository.BudgetRepository
import com.randallengineering.finances.data.repository.GamificationRepository
import com.randallengineering.finances.data.repository.GoalRepository
import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.model.Budget
import com.randallengineering.finances.domain.model.GamificationState
import com.randallengineering.finances.domain.model.Goal
import com.randallengineering.finances.domain.model.QuestNode
import com.randallengineering.finances.domain.model.QuestNodeType
import com.randallengineering.finances.domain.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.max

data class QuestPathUiState(
    val gamificationState: GamificationState = GamificationState(),
    val nodes: List<QuestNode> = emptyList(),
    val activeNode: QuestNode? = null,
    val selectedNode: QuestNode? = null,
    val monthlySavingsTotal: Double = 0.0,
    val totalDiscretionarySaved: Double = 0.0
)

class QuestPathViewModel(
    private val gamificationRepository: GamificationRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _selectedNode = MutableStateFlow<QuestNode?>(null)

    val uiState: StateFlow<QuestPathUiState> = combine(
        gamificationRepository.stateFlow,
        budgetRepository.getBudgetsFlow(),
        goalRepository.getGoalsFlow(),
        transactionRepository.getTransactionsFlow(),
        _selectedNode
    ) { gState, budgetsRes, goalsRes, txsRes, selected ->
        val budgets = (budgetsRes as? Resource.Success)?.data.orEmpty()
        val goals = (goalsRes as? Resource.Success)?.data.orEmpty()
        val txs = (txsRes as? Resource.Success)?.data.orEmpty()

        val evaluatedNodes = evaluateAllQuests(gState, budgets, goals, txs)
        val active = evaluatedNodes.firstOrNull { !it.isCompleted && it.isUnlocked } ?: evaluatedNodes.lastOrNull()

        QuestPathUiState(
            gamificationState = gState,
            nodes = evaluatedNodes,
            activeNode = active,
            selectedNode = selected,
            monthlySavingsTotal = goals.sumOf { it.currentAmount },
            totalDiscretionarySaved = 142.50
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuestPathUiState()
    )

    fun selectNode(node: QuestNode) {
        _selectedNode.value = node
    }

    fun dismissNodeDialog() {
        _selectedNode.value = null
    }

    fun claimNodeReward(node: QuestNode) {
        if (!node.isCriteriaMet) return
        viewModelScope.launch {
            gamificationRepository.completeQuestNode(
                nodeId = node.id,
                xpReward = node.rewardXp,
                gemsReward = node.rewardGems
            )
            _selectedNode.value = null
        }
    }

    private fun evaluateAllQuests(
        gState: GamificationState,
        budgets: List<Budget>,
        goals: List<Goal>,
        txs: List<Transaction>
    ): List<QuestNode> {
        val completedIds = gState.completedNodeIds
        val today = LocalDate.now()

        // Calculate Today's Spending
        val todayTxs = txs.filter { tx ->
            val date = Instant.ofEpochSecond(tx.postedEpochSeconds).atZone(ZoneId.systemDefault()).toLocalDate()
            date.isEqual(today) && tx.amount < 0
        }
        val todayDiscretionarySpend = todayTxs.sumOf { abs(it.amount) }

        // Calculate Uncategorized Count
        val uncategorizedCount = txs.count { it.category.equals("Uncategorized", ignoreCase = true) || it.category.isBlank() }
        val verifiedCount = (txs.size - uncategorizedCount).coerceAtLeast(0)

        // Calculate Category Spends This Month
        val currentMonthTxs = txs.filter { tx ->
            val date = Instant.ofEpochSecond(tx.postedEpochSeconds).atZone(ZoneId.systemDefault()).toLocalDate()
            date.month == today.month && date.year == today.year && tx.amount < 0
        }
        val diningSpend = currentMonthTxs.filter { it.category.contains("Dining", ignoreCase = true) || it.category.contains("Food", ignoreCase = true) }.sumOf { abs(it.amount) }
        val grocerySpend = currentMonthTxs.filter { it.category.contains("Grocer", ignoreCase = true) }.sumOf { abs(it.amount) }
        val subSpend = currentMonthTxs.filter { it.category.contains("Subscription", ignoreCase = true) }.sumOf { abs(it.amount) }

        // Savings Goals
        val totalSavedInGoals = goals.sumOf { it.currentAmount }

        // Amazon Itemized Check
        val amazonMatchesCount = txs.count { it.splits.isNotEmpty() || (it.notes.contains("Amazon Items:", ignoreCase = true)) }

        val nodes = mutableListOf<QuestNode>()

        // 1. INBOX ZERO
        val q1Met = uncategorizedCount == 0 || verifiedCount >= 5
        nodes.add(
            QuestNode(
                id = "node_w1_inbox_zero",
                title = "Inbox Zero Reviewer",
                subtitle = "Categorize all incoming transactions (0 uncategorized remaining)",
                weekNumber = 1,
                nodeType = QuestNodeType.INBOX_ZERO,
                progressText = if (uncategorizedCount == 0) "✅ All transactions categorized!" else "$uncategorizedCount uncategorized remaining",
                progressPercent = if (txs.isNotEmpty()) ((txs.size - uncategorizedCount).toFloat() / txs.size.toFloat()).coerceIn(0f, 1f) else 1f,
                isCriteriaMet = q1Met,
                requirementDescription = "Confirm all transactions in your Action Queue to leave 0 items uncategorized.",
                rewardXp = 50,
                rewardGems = 10,
                isUnlocked = true,
                isCompleted = completedIds.contains("node_w1_inbox_zero")
            )
        )

        // 2. ZERO SPEND DAY
        val q2Unlocked = completedIds.contains("node_w1_inbox_zero")
        val q2Met = todayDiscretionarySpend == 0.0
        nodes.add(
            QuestNode(
                id = "node_w1_zero_spend",
                title = "Zero-Spend Day Hero",
                subtitle = "Complete today with \$0.00 discretionary spending",
                weekNumber = 1,
                nodeType = QuestNodeType.ZERO_SPEND,
                progressText = if (q2Met) "✅ \$0.00 spent today!" else "${CurrencyFormatter.format(todayDiscretionarySpend)} spent today",
                progressPercent = if (q2Met) 1f else 0.3f,
                isCriteriaMet = q2Met,
                requirementDescription = "Keep your discretionary purchases to \$0 for the entire day.",
                rewardXp = 60,
                rewardGems = 15,
                isUnlocked = q2Unlocked,
                isCompleted = completedIds.contains("node_w1_zero_spend")
            )
        )

        // 3. EMERGENCY FUND CHEST
        val q3Unlocked = completedIds.contains("node_w1_zero_spend")
        val q3Met = totalSavedInGoals >= 100.0
        nodes.add(
            QuestNode(
                id = "node_w1_chest",
                title = "Emergency Fund Chest",
                subtitle = "Accumulate at least \$100 in your savings goals",
                weekNumber = 1,
                nodeType = QuestNodeType.SAVINGS_CHEST,
                progressText = "${CurrencyFormatter.format(totalSavedInGoals)} / \$100.00 Saved",
                progressPercent = (totalSavedInGoals.toFloat() / 100f).coerceIn(0f, 1f),
                isCriteriaMet = q3Met,
                requirementDescription = "Fund your Savings Goals in Randall Finances to reach \$100.00.",
                rewardXp = 100,
                rewardGems = 25,
                rewardEquipmentId = "relic_vault",
                isUnlocked = q3Unlocked,
                isCompleted = completedIds.contains("node_w1_chest")
            )
        )

        // 4. STREAK CHAMPION
        val q4Unlocked = completedIds.contains("node_w1_chest")
        val q4Met = gState.streakDays >= 3
        nodes.add(
            QuestNode(
                id = "node_w2_streak",
                title = "Habit Master (3-Day Streak)",
                subtitle = "Maintain a 3-day consecutive budgeting streak",
                weekNumber = 2,
                nodeType = QuestNodeType.DAILY_ADHERENCE,
                progressText = "${gState.streakDays} / 3 Streak Days",
                progressPercent = (gState.streakDays.toFloat() / 3f).coerceIn(0f, 1f),
                isCriteriaMet = q4Met,
                requirementDescription = "Log in and review your finances for 3 consecutive days.",
                rewardXp = 75,
                rewardGems = 15,
                isUnlocked = q4Unlocked,
                isCompleted = completedIds.contains("node_w2_streak")
            )
        )

        // 5. DINING DRAGON BOSS BATTLE
        val q5Unlocked = completedIds.contains("node_w2_streak")
        val diningMaxHp = 350.0
        val remainingDiningHp = max(0.0, diningMaxHp - diningSpend)
        val q5Met = diningSpend < diningMaxHp && diningSpend > 0
        nodes.add(
            QuestNode(
                id = "node_boss_dining",
                title = "The Dining Dragon (Boss Battle)",
                subtitle = "Keep monthly dining out under \$350.00",
                weekNumber = 2,
                nodeType = QuestNodeType.BOSS_BATTLE,
                bossName = "Ignis the Takeout Wyrm",
                bossMaxHp = diningMaxHp,
                bossCurrentHp = remainingDiningHp,
                progressText = "${CurrencyFormatter.format(diningSpend)} / \$350.00 Max Limit (${remainingDiningHp.toInt()} HP remaining)",
                progressPercent = ((diningMaxHp - diningSpend) / diningMaxHp).toFloat().coerceIn(0f, 1f),
                isCriteriaMet = q5Met,
                requirementDescription = "Defeat the Dining Dragon by keeping your total monthly restaurant and takeout spend under \$350.00!",
                rewardXp = 250,
                rewardGems = 50,
                rewardEquipmentId = "pet_griffin",
                isUnlocked = q5Unlocked,
                isCompleted = completedIds.contains("node_boss_dining")
            )
        )

        // 6. GROCERY GUARDIAN
        val q6Unlocked = completedIds.contains("node_boss_dining")
        val q6Met = grocerySpend <= 300.0 && grocerySpend > 0
        nodes.add(
            QuestNode(
                id = "node_w3_grocery",
                title = "Grocery Guardian",
                subtitle = "Keep monthly grocery spending under \$300.00",
                weekNumber = 3,
                nodeType = QuestNodeType.WEEKLY_BUDGET,
                progressText = "${CurrencyFormatter.format(grocerySpend)} / \$300.00 Limit",
                progressPercent = (grocerySpend.toFloat() / 300f).coerceIn(0f, 1f),
                isCriteriaMet = q6Met,
                requirementDescription = "Plan your meals and keep total monthly groceries under \$300.00.",
                rewardXp = 80,
                rewardGems = 20,
                isUnlocked = q6Unlocked,
                isCompleted = completedIds.contains("node_w3_grocery")
            )
        )

        // 7. AMAZON ITEMIZER
        val q7Unlocked = completedIds.contains("node_w3_grocery")
        val q7Met = amazonMatchesCount >= 1
        nodes.add(
            QuestNode(
                id = "node_w3_amazon",
                title = "Amazon Itemizer",
                subtitle = "Itemize or split at least 1 Amazon purchase with AI Scan",
                weekNumber = 3,
                nodeType = QuestNodeType.AMAZON_MATCH,
                progressText = if (q7Met) "✅ Amazon itemized!" else "0 / 1 Amazon orders itemized",
                progressPercent = if (q7Met) 1f else 0f,
                isCriteriaMet = q7Met,
                requirementDescription = "Use the ⚡ Live AI Scan Amazon Orders tool to break down and categorize an Amazon transaction.",
                rewardXp = 90,
                rewardGems = 20,
                rewardEquipmentId = "chest_shield",
                isUnlocked = q7Unlocked,
                isCompleted = completedIds.contains("node_w3_amazon")
            )
        )

        // 8. SUBSCRIPTION AUDITOR CHEST
        val q8Unlocked = completedIds.contains("node_w3_amazon")
        val q8Met = subSpend <= 60.0
        nodes.add(
            QuestNode(
                id = "node_w4_sub_chest",
                title = "Subscription Vault Chest",
                subtitle = "Keep monthly recurring subscriptions under \$60.00",
                weekNumber = 4,
                nodeType = QuestNodeType.SAVINGS_CHEST,
                progressText = "${CurrencyFormatter.format(subSpend)} / \$60.00 Subscriptions",
                progressPercent = if (subSpend <= 60.0) 1f else 0.5f,
                isCriteriaMet = q8Met,
                requirementDescription = "Audit your monthly subscriptions to maintain recurring fees below \$60.00.",
                rewardXp = 150,
                rewardGems = 35,
                rewardEquipmentId = "relic_ring",
                isUnlocked = q8Unlocked,
                isCompleted = completedIds.contains("node_w4_sub_chest")
            )
        )

        // 9. THE COMPOUND COLOSSUS (FINAL BOSS)
        val q9Unlocked = completedIds.contains("node_w4_sub_chest")
        val q9Met = totalSavedInGoals >= 250.0
        nodes.add(
            QuestNode(
                id = "node_boss_colossus",
                title = "The Compound Colossus (Final Boss)",
                subtitle = "Build \$250.00+ total in your wealth vault",
                weekNumber = 4,
                nodeType = QuestNodeType.BOSS_BATTLE,
                bossName = "Chronos the Inflation Titan",
                bossMaxHp = 250.0,
                bossCurrentHp = max(0.0, 250.0 - totalSavedInGoals),
                progressText = "${CurrencyFormatter.format(totalSavedInGoals)} / \$250.00 Saved",
                progressPercent = (totalSavedInGoals.toFloat() / 250f).coerceIn(0f, 1f),
                isCriteriaMet = q9Met,
                requirementDescription = "Slay Chronos by locking away \$250.00 in your financial savings goals.",
                rewardXp = 500,
                rewardGems = 100,
                rewardEquipmentId = "head_crown",
                isUnlocked = q9Unlocked,
                isCompleted = completedIds.contains("node_boss_colossus")
            )
        )

        return nodes
    }
}
