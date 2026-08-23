package com.randallengineering.finances.ui.screens.quest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.core.util.CurrencyFormatter
import com.randallengineering.finances.data.model.SimpleFinConfigEntity
import com.randallengineering.finances.data.repository.BudgetRepository
import com.randallengineering.finances.data.repository.CategoryRepository
import com.randallengineering.finances.data.repository.GamificationRepository
import com.randallengineering.finances.data.repository.GoalRepository
import com.randallengineering.finances.data.repository.SimpleFinRepository
import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.model.Budget
import com.randallengineering.finances.domain.model.CategoryHierarchy
import com.randallengineering.finances.domain.model.GamificationState
import com.randallengineering.finances.domain.model.Goal
import com.randallengineering.finances.domain.model.QuestNode
import com.randallengineering.finances.domain.model.QuestNodeType
import com.randallengineering.finances.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
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
    val selectedChapter: Int = 1
)

@Suppress("UNCHECKED_CAST")
class QuestPathViewModel(
    private val gamificationRepository: GamificationRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val simpleFinRepository: SimpleFinRepository
) : ViewModel() {

    private val _selectedNode = MutableStateFlow<QuestNode?>(null)
    private val _selectedChapter = MutableStateFlow(1)

    val uiState: StateFlow<QuestPathUiState> = combine(
        listOf(
            gamificationRepository.stateFlow,
            budgetRepository.getBudgetsFlow(),
            goalRepository.getGoalsFlow(),
            transactionRepository.getTransactionsFlow(),
            categoryRepository.getCategoriesFlow(),
            simpleFinRepository.getConfigFlow(),
            _selectedNode,
            _selectedChapter
        )
    ) { array ->
        val gState = array[0] as GamificationState
        val budgetsRes = array[1] as? Resource<List<Budget>>
        val goalsRes = array[2] as? Resource<List<Goal>>
        val txsRes = array[3] as? Resource<List<Transaction>>
        val catsRes = array[4] as? Resource<List<CategoryHierarchy>>
        val simpleFinRes = array[5] as? Resource<SimpleFinConfigEntity?>
        val selected = array[6] as? QuestNode
        val currentChap = (array[7] as? Int) ?: 1

        val budgets = (budgetsRes as? Resource.Success<List<Budget>>)?.data.orEmpty()
        val goals = (goalsRes as? Resource.Success<List<Goal>>)?.data.orEmpty()
        val txs = (txsRes as? Resource.Success<List<Transaction>>)?.data.orEmpty()
        val cats = (catsRes as? Resource.Success<List<CategoryHierarchy>>)?.data.orEmpty()
        val simpleFinConfig = (simpleFinRes as? Resource.Success<SimpleFinConfigEntity?>)?.data

        val evaluatedNodes = evaluateAllQuests(gState, budgets, goals, txs, cats, simpleFinConfig)
        val active = evaluatedNodes.firstOrNull { !it.isCompleted && it.isUnlocked } ?: evaluatedNodes.lastOrNull()

        // Auto-select active chapter if needed
        val chapToDisplay = active?.chapter ?: currentChap

        QuestPathUiState(
            gamificationState = gState,
            nodes = evaluatedNodes,
            activeNode = active,
            selectedNode = selected,
            selectedChapter = chapToDisplay
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

    fun selectChapter(chapter: Int) {
        _selectedChapter.value = chapter
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
        txs: List<Transaction>,
        categories: List<CategoryHierarchy>,
        simpleFinConfig: SimpleFinConfigEntity?
    ): List<QuestNode> {
        val completedIds = gState.completedNodeIds
        val today = LocalDate.now()

        val nodes = mutableListOf<QuestNode>()

        // ==========================================
        // CHAPTER 1: APP SETUP & FINANCIAL FOUNDATIONS
        // ==========================================

        // 1.1 SimpleFIN Bridge Link
        val hasSimpleFin = simpleFinConfig?.accessUrlConfigured == true
        val c1_1 = QuestNode(
            id = "node_c1_simplefin",
            title = "Connect Bank Bridge",
            subtitle = "Link your bank accounts via SimpleFIN Bridge",
            chapter = 1,
            chapterTitle = "Chapter 1: Financial Foundations",
            weekNumber = 1,
            nodeType = QuestNodeType.SETUP_SIMPLEFIN,
            progressText = if (hasSimpleFin) "✅ Bank Bridge Connected!" else "Not configured yet",
            progressPercent = if (hasSimpleFin) 1f else 0f,
            isCriteriaMet = hasSimpleFin,
            requirementDescription = "Open Settings and configure your SimpleFIN Access URL to stream bank transactions.",
            rewardXp = 100,
            rewardGems = 20,
            isUnlocked = true,
            isCompleted = completedIds.contains("node_c1_simplefin")
        )
        nodes.add(c1_1)

        // 1.2 Setup Main Categories (At least 6)
        val mainCatCount = categories.size
        val c1_2_unlocked = completedIds.contains("node_c1_simplefin")
        val c1_2_met = mainCatCount >= 6
        val c1_2 = QuestNode(
            id = "node_c1_categories",
            title = "Category Architect",
            subtitle = "Create at least 6 primary spending categories",
            chapter = 1,
            chapterTitle = "Chapter 1: Financial Foundations",
            weekNumber = 1,
            nodeType = QuestNodeType.SETUP_CATEGORIES,
            progressText = "$mainCatCount / 6 Main Categories Created",
            progressPercent = (mainCatCount.toFloat() / 6f).coerceIn(0f, 1f),
            isCriteriaMet = c1_2_met,
            requirementDescription = "Define at least 6 main categories (e.g. Dining, Groceries, Auto, Utilities, Housing, Fun).",
            rewardXp = 80,
            rewardGems = 15,
            isUnlocked = c1_2_unlocked,
            isCompleted = completedIds.contains("node_c1_categories")
        )
        nodes.add(c1_2)

        // 1.3 Setup Subcategories (At least 25 total)
        val totalSubCategories = categories.sumOf { it.subCategories.size }
        val c1_3_unlocked = completedIds.contains("node_c1_categories")
        val c1_3_met = totalSubCategories >= 25
        val c1_3 = QuestNode(
            id = "node_c1_subcategories",
            title = "Subcategory Specialist",
            subtitle = "Build at least 25 total subcategories across your budget",
            chapter = 1,
            chapterTitle = "Chapter 1: Financial Foundations",
            weekNumber = 1,
            nodeType = QuestNodeType.SETUP_SUBCATEGORIES,
            progressText = "$totalSubCategories / 25 Subcategories Created",
            progressPercent = (totalSubCategories.toFloat() / 25f).coerceIn(0f, 1f),
            isCriteriaMet = c1_3_met,
            requirementDescription = "Add specific subcategories to your categories in Budgets & Categories (reach 25+ total).",
            rewardXp = 120,
            rewardGems = 25,
            rewardEquipmentId = "head_visor",
            isUnlocked = c1_3_unlocked,
            isCompleted = completedIds.contains("node_c1_subcategories")
        )
        nodes.add(c1_3)

        // 1.4 Setup Category Budgets (At least 3)
        val budgetCount = budgets.size
        val c1_4_unlocked = completedIds.contains("node_c1_subcategories")
        val c1_4_met = budgetCount >= 3
        val c1_4 = QuestNode(
            id = "node_c1_budgets",
            title = "Budget Blueprint Creator",
            subtitle = "Set monthly spending limits for at least 3 categories",
            chapter = 1,
            chapterTitle = "Chapter 1: Financial Foundations",
            weekNumber = 1,
            nodeType = QuestNodeType.SETUP_BUDGETS,
            progressText = "$budgetCount / 3 Budgets Configured",
            progressPercent = (budgetCount.toFloat() / 3f).coerceIn(0f, 1f),
            isCriteriaMet = c1_4_met,
            requirementDescription = "Set monthly budget limits for at least 3 spending categories.",
            rewardXp = 100,
            rewardGems = 20,
            isUnlocked = c1_4_unlocked,
            isCompleted = completedIds.contains("node_c1_budgets")
        )
        nodes.add(c1_4)

        // 1.5 Setup Savings Goal
        val goalCount = goals.size
        val c1_5_unlocked = completedIds.contains("node_c1_budgets")
        val c1_5_met = goalCount >= 1
        val c1_5 = QuestNode(
            id = "node_c1_goal_chest",
            title = "Goal Setter Vault",
            subtitle = "Create your first financial target in Savings & Goals",
            chapter = 1,
            chapterTitle = "Chapter 1: Financial Foundations",
            weekNumber = 1,
            nodeType = QuestNodeType.SAVINGS_CHEST,
            progressText = "$goalCount / 1 Savings Goals Created",
            progressPercent = (goalCount.toFloat() / 1f).coerceIn(0f, 1f),
            isCriteriaMet = c1_5_met,
            requirementDescription = "Establish at least one savings or investment goal in Savings & Goals.",
            rewardXp = 150,
            rewardGems = 30,
            rewardEquipmentId = "chest_shield",
            isUnlocked = c1_5_unlocked,
            isCompleted = completedIds.contains("node_c1_goal_chest")
        )
        nodes.add(c1_5)

        // ==========================================
        // CHAPTER 2: THE REVIEWER'S JOURNEY (CATEGORIZATION & HABITS)
        // ==========================================

        val uncategorizedCount = txs.count { it.category.equals("Uncategorized", ignoreCase = true) || it.category.isBlank() }
        val verifiedCount = (txs.size - uncategorizedCount).coerceAtLeast(0)

        // 2.1 First Inbox Zero Clear
        val c2_1_unlocked = completedIds.contains("node_c1_goal_chest")
        val c2_1_met = uncategorizedCount == 0 || verifiedCount >= 5
        val c2_1 = QuestNode(
            id = "node_c2_inbox_zero",
            title = "Inbox Zero Reviewer",
            subtitle = "Categorize all incoming transactions (0 uncategorized remaining)",
            chapter = 2,
            chapterTitle = "Chapter 2: The Reviewer's Journey",
            weekNumber = 2,
            nodeType = QuestNodeType.INBOX_ZERO,
            progressText = if (uncategorizedCount == 0) "✅ All transactions categorized!" else "$uncategorizedCount uncategorized remaining",
            progressPercent = if (txs.isNotEmpty()) ((txs.size - uncategorizedCount).toFloat() / txs.size.toFloat()).coerceIn(0f, 1f) else 1f,
            isCriteriaMet = c2_1_met,
            requirementDescription = "Confirm all transactions in your Action Queue to leave 0 items uncategorized.",
            rewardXp = 80,
            rewardGems = 15,
            isUnlocked = c2_1_unlocked,
            isCompleted = completedIds.contains("node_c2_inbox_zero")
        )
        nodes.add(c2_1)

        // 2.2 Note Master (Annotate Transactions)
        val txsWithNotes = txs.count { it.notes.isNotBlank() }
        val c2_2_unlocked = completedIds.contains("node_c2_inbox_zero")
        val c2_2_met = txsWithNotes >= 3
        val c2_2 = QuestNode(
            id = "node_c2_notes",
            title = "Note Master",
            subtitle = "Attach context notes to at least 3 transactions",
            chapter = 2,
            chapterTitle = "Chapter 2: The Reviewer's Journey",
            weekNumber = 2,
            nodeType = QuestNodeType.NOTE_BONUS,
            progressText = "$txsWithNotes / 3 Transactions with Notes",
            progressPercent = (txsWithNotes.toFloat() / 3f).coerceIn(0f, 1f),
            isCriteriaMet = c2_2_met,
            requirementDescription = "Add quick context notes (e.g. 'Team lunch' or 'Office supplies') when reviewing transactions.",
            rewardXp = 90,
            rewardGems = 20,
            isUnlocked = c2_2_unlocked,
            isCompleted = completedIds.contains("node_c2_notes")
        )
        nodes.add(c2_2)

        // 2.3 The Grand Splitter
        val splitTxsCount = txs.count { it.splits.isNotEmpty() }
        val c2_3_unlocked = completedIds.contains("node_c2_notes")
        val c2_3_met = splitTxsCount >= 1
        val c2_3 = QuestNode(
            id = "node_c2_split",
            title = "The Grand Splitter",
            subtitle = "Split at least 1 transaction into multiple categories",
            chapter = 2,
            chapterTitle = "Chapter 2: The Reviewer's Journey",
            weekNumber = 2,
            nodeType = QuestNodeType.SPLIT_TRANSACTION,
            progressText = if (c2_3_met) "✅ Transaction Split Completed!" else "0 / 1 Split Transactions",
            progressPercent = if (c2_3_met) 1f else 0f,
            isCriteriaMet = c2_3_met,
            requirementDescription = "Use the ✂️ Quick Split tool in the Action Queue or Transaction Details to split a purchase.",
            rewardXp = 100,
            rewardGems = 20,
            isUnlocked = c2_3_unlocked,
            isCompleted = completedIds.contains("node_c2_split")
        )
        nodes.add(c2_3)

        // 2.4 3-Day Daily Review Streak
        val c2_4_unlocked = completedIds.contains("node_c2_split")
        val c2_4_met = gState.streakDays >= 3
        val c2_4 = QuestNode(
            id = "node_c2_streak",
            title = "Habit Master (3-Day Streak)",
            subtitle = "Maintain a 3-day consecutive budgeting streak",
            chapter = 2,
            chapterTitle = "Chapter 2: The Reviewer's Journey",
            weekNumber = 2,
            nodeType = QuestNodeType.DAILY_ADHERENCE,
            progressText = "${gState.streakDays} / 3 Streak Days",
            progressPercent = (gState.streakDays.toFloat() / 3f).coerceIn(0f, 1f),
            isCriteriaMet = c2_4_met,
            requirementDescription = "Log in and review your finances for 3 consecutive days.",
            rewardXp = 120,
            rewardGems = 25,
            isUnlocked = c2_4_unlocked,
            isCompleted = completedIds.contains("node_c2_streak")
        )
        nodes.add(c2_4)

        // 2.5 Amazon AI Scanner Chest
        val amazonMatchesCount = txs.count { it.notes.contains("Amazon Items:", ignoreCase = true) || it.splits.isNotEmpty() }
        val c2_5_unlocked = completedIds.contains("node_c2_streak")
        val c2_5_met = amazonMatchesCount >= 1
        val c2_5 = QuestNode(
            id = "node_c2_amazon_chest",
            title = "Amazon AI Scanner Chest",
            subtitle = "Itemize an Amazon purchase using Live AI Scan",
            chapter = 2,
            chapterTitle = "Chapter 2: The Reviewer's Journey",
            weekNumber = 2,
            nodeType = QuestNodeType.SAVINGS_CHEST,
            progressText = if (c2_5_met) "✅ Amazon itemized!" else "0 / 1 Amazon orders itemized",
            progressPercent = if (c2_5_met) 1f else 0f,
            isCriteriaMet = c2_5_met,
            requirementDescription = "Use the ⚡ Live AI Scan tool to capture and itemize Amazon orders.",
            rewardXp = 150,
            rewardGems = 35,
            rewardEquipmentId = "relic_ring",
            isUnlocked = c2_5_unlocked,
            isCompleted = completedIds.contains("node_c2_amazon_chest")
        )
        nodes.add(c2_5)

        // ==========================================
        // CHAPTER 3: BUDGET MASTERY & BOSS BATTLES
        // ==========================================

        // Calculate Today's Spending
        val todayTxs = txs.filter { tx ->
            val date = Instant.ofEpochSecond(tx.postedEpochSeconds).atZone(ZoneId.systemDefault()).toLocalDate()
            date.isEqual(today) && tx.amount < 0
        }
        val todayDiscretionarySpend = todayTxs.sumOf { abs(it.amount) }

        // Calculate Month Spending
        val currentMonthTxs = txs.filter { tx ->
            val date = Instant.ofEpochSecond(tx.postedEpochSeconds).atZone(ZoneId.systemDefault()).toLocalDate()
            date.month == today.month && date.year == today.year && tx.amount < 0
        }
        val diningSpend = currentMonthTxs.filter { it.category.contains("Dining", ignoreCase = true) || it.category.contains("Food", ignoreCase = true) }.sumOf { abs(it.amount) }
        val grocerySpend = currentMonthTxs.filter { it.category.contains("Grocer", ignoreCase = true) }.sumOf { abs(it.amount) }
        val subSpend = currentMonthTxs.filter { it.category.contains("Subscription", ignoreCase = true) }.sumOf { abs(it.amount) }
        val totalSavedInGoals = goals.sumOf { it.currentAmount }

        // 3.1 Zero-Spend Day
        val c3_1_unlocked = completedIds.contains("node_c2_amazon_chest")
        val c3_1_met = todayDiscretionarySpend == 0.0
        val c3_1 = QuestNode(
            id = "node_c3_zero_spend",
            title = "Zero-Spend Day Sentinel",
            subtitle = "Complete today with \$0.00 discretionary spending",
            chapter = 3,
            chapterTitle = "Chapter 3: Budget Mastery & Boss Battles",
            weekNumber = 3,
            nodeType = QuestNodeType.ZERO_SPEND,
            progressText = if (c3_1_met) "✅ \$0.00 spent today!" else "${CurrencyFormatter.format(todayDiscretionarySpend)} spent today",
            progressPercent = if (c3_1_met) 1f else 0.3f,
            isCriteriaMet = c3_1_met,
            requirementDescription = "Keep your discretionary purchases to \$0 for the entire day.",
            rewardXp = 100,
            rewardGems = 20,
            isUnlocked = c3_1_unlocked,
            isCompleted = completedIds.contains("node_c3_zero_spend")
        )
        nodes.add(c3_1)

        // 3.2 Weekly Grocery Sentinel
        val c3_2_unlocked = completedIds.contains("node_c3_zero_spend")
        val c3_2_met = grocerySpend <= 300.0 && grocerySpend > 0
        val c3_2 = QuestNode(
            id = "node_c3_grocery",
            title = "Grocery Sentinel",
            subtitle = "Keep monthly grocery spending under \$300.00",
            chapter = 3,
            chapterTitle = "Chapter 3: Budget Mastery & Boss Battles",
            weekNumber = 3,
            nodeType = QuestNodeType.WEEKLY_BUDGET,
            progressText = "${CurrencyFormatter.format(grocerySpend)} / \$300.00 Limit",
            progressPercent = (grocerySpend.toFloat() / 300f).coerceIn(0f, 1f),
            isCriteriaMet = c3_2_met,
            requirementDescription = "Plan your meals and keep total groceries under \$300.00.",
            rewardXp = 120,
            rewardGems = 25,
            isUnlocked = c3_2_unlocked,
            isCompleted = completedIds.contains("node_c3_grocery")
        )
        nodes.add(c3_2)

        // 3.3 Subscription Audit
        val c3_3_unlocked = completedIds.contains("node_c3_grocery")
        val c3_3_met = subSpend <= 60.0
        val c3_3 = QuestNode(
            id = "node_c3_subscriptions",
            title = "Subscription Auditor",
            subtitle = "Keep monthly recurring subscriptions under \$60.00",
            chapter = 3,
            chapterTitle = "Chapter 3: Budget Mastery & Boss Battles",
            weekNumber = 3,
            nodeType = QuestNodeType.WEEKLY_BUDGET,
            progressText = "${CurrencyFormatter.format(subSpend)} / \$60.00 Subscriptions",
            progressPercent = if (subSpend <= 60.0) 1f else 0.5f,
            isCriteriaMet = c3_3_met,
            requirementDescription = "Audit your recurring monthly subscriptions to keep them below \$60.00.",
            rewardXp = 140,
            rewardGems = 30,
            isUnlocked = c3_3_unlocked,
            isCompleted = completedIds.contains("node_c3_subscriptions")
        )
        nodes.add(c3_3)

        // 3.4 The Dining Dragon (Boss Battle)
        val c3_4_unlocked = completedIds.contains("node_c3_subscriptions")
        val diningMaxHp = 350.0
        val remainingDiningHp = max(0.0, diningMaxHp - diningSpend)
        val c3_4_met = diningSpend < diningMaxHp && diningSpend > 0
        val c3_4 = QuestNode(
            id = "node_c3_boss_dining",
            title = "The Dining Dragon (Boss Battle)",
            subtitle = "Keep monthly dining out under \$350.00",
            chapter = 3,
            chapterTitle = "Chapter 3: Budget Mastery & Boss Battles",
            weekNumber = 3,
            nodeType = QuestNodeType.BOSS_BATTLE,
            bossName = "Ignis the Takeout Wyrm",
            bossMaxHp = diningMaxHp,
            bossCurrentHp = remainingDiningHp,
            progressText = "${CurrencyFormatter.format(diningSpend)} / \$350.00 Max Limit (${remainingDiningHp.toInt()} HP remaining)",
            progressPercent = ((diningMaxHp - diningSpend) / diningMaxHp).toFloat().coerceIn(0f, 1f),
            isCriteriaMet = c3_4_met,
            requirementDescription = "Defeat the Dining Dragon by keeping your total restaurant and takeout spend under \$350.00!",
            rewardXp = 300,
            rewardGems = 50,
            rewardEquipmentId = "pet_piggy",
            isUnlocked = c3_4_unlocked,
            isCompleted = completedIds.contains("node_c3_boss_dining")
        )
        nodes.add(c3_4)

        // 3.5 The Compound Colossus (Final Boss)
        val c3_5_unlocked = completedIds.contains("node_c3_boss_dining")
        val c3_5_met = totalSavedInGoals >= 250.0
        val c3_5 = QuestNode(
            id = "node_c3_boss_colossus",
            title = "The Compound Colossus (Final Boss)",
            subtitle = "Build \$250.00+ total in your wealth vault",
            chapter = 3,
            chapterTitle = "Chapter 3: Budget Mastery & Boss Battles",
            weekNumber = 3,
            nodeType = QuestNodeType.BOSS_BATTLE,
            bossName = "Chronos the Inflation Titan",
            bossMaxHp = 250.0,
            bossCurrentHp = max(0.0, 250.0 - totalSavedInGoals),
            progressText = "${CurrencyFormatter.format(totalSavedInGoals)} / \$250.00 Saved",
            progressPercent = (totalSavedInGoals.toFloat() / 250f).coerceIn(0f, 1f),
            isCriteriaMet = c3_5_met,
            requirementDescription = "Slay Chronos by locking away \$250.00 in your financial savings goals.",
            rewardXp = 500,
            rewardGems = 100,
            rewardEquipmentId = "head_crown",
            isUnlocked = c3_5_unlocked,
            isCompleted = completedIds.contains("node_c3_boss_colossus")
        )
        nodes.add(c3_5)

        return nodes
    }
}
