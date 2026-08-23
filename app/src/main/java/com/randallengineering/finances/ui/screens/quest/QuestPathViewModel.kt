package com.randallengineering.finances.ui.screens.quest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.repository.BudgetRepository
import com.randallengineering.finances.data.repository.GamificationRepository
import com.randallengineering.finances.data.repository.GoalRepository
import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.model.GamificationState
import com.randallengineering.finances.domain.model.QuestNode
import com.randallengineering.finances.domain.model.QuestNodeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
        _selectedNode
    ) { gState, budgetsRes, goalsRes, selected ->
        val completedIds = gState.completedNodeIds

        val defaultNodes = generateQuestNodes(completedIds)
        val active = defaultNodes.firstOrNull { !it.isCompleted } ?: defaultNodes.lastOrNull()

        QuestPathUiState(
            gamificationState = gState,
            nodes = defaultNodes,
            activeNode = active,
            selectedNode = selected,
            monthlySavingsTotal = 350.0,
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
        viewModelScope.launch {
            gamificationRepository.completeQuestNode(
                nodeId = node.id,
                xpReward = node.rewardXp,
                gemsReward = node.rewardGems
            )
            _selectedNode.value = null
        }
    }

    private fun generateQuestNodes(completedIds: List<String>): List<QuestNode> {
        return listOf(
            QuestNode(
                id = "node_w1_d1",
                title = "Frugal Dawn",
                subtitle = "Log your first day under daily allowance",
                weekNumber = 1,
                nodeType = QuestNodeType.DAILY_ADHERENCE,
                rewardXp = 40,
                rewardGems = 5,
                isUnlocked = true,
                isCompleted = completedIds.contains("node_w1_d1")
            ),
            QuestNode(
                id = "node_w1_d2",
                title = "Queue Master",
                subtitle = "Review and confirm 5 SimpleFIN transactions",
                weekNumber = 1,
                nodeType = QuestNodeType.DAILY_ADHERENCE,
                rewardXp = 50,
                rewardGems = 10,
                isUnlocked = true,
                isCompleted = completedIds.contains("node_w1_d2")
            ),
            QuestNode(
                id = "node_w1_chest",
                title = "Emergency Fund Chest",
                subtitle = "Save \$50 into your High Yield Savings goal",
                weekNumber = 1,
                nodeType = QuestNodeType.SAVINGS_CHEST,
                rewardXp = 100,
                rewardGems = 25,
                rewardEquipmentId = "relic_vault",
                isUnlocked = true,
                isCompleted = completedIds.contains("node_w1_chest")
            ),
            QuestNode(
                id = "node_w2_d1",
                title = "Zero-Spend Hero",
                subtitle = "Complete a day with \$0 discretionary spending",
                weekNumber = 2,
                nodeType = QuestNodeType.DAILY_ADHERENCE,
                rewardXp = 60,
                rewardGems = 15,
                isUnlocked = completedIds.contains("node_w1_chest"),
                isCompleted = completedIds.contains("node_w2_d1")
            ),
            QuestNode(
                id = "node_w2_d2",
                title = "Grocery Guardian",
                subtitle = "Keep weekly grocery bill under \$120",
                weekNumber = 2,
                nodeType = QuestNodeType.WEEKLY_BUDGET,
                rewardXp = 75,
                rewardGems = 15,
                isUnlocked = completedIds.contains("node_w2_d1"),
                isCompleted = completedIds.contains("node_w2_d2")
            ),
            QuestNode(
                id = "node_boss_dining",
                title = "The Dining Dragon (Boss Battle)",
                subtitle = "Keep monthly dining out spend under \$350",
                weekNumber = 2,
                nodeType = QuestNodeType.BOSS_BATTLE,
                bossName = "Ignis the Takeout Wyrm",
                bossMaxHp = 350.0,
                bossCurrentHp = 210.0,
                rewardXp = 250,
                rewardGems = 50,
                rewardEquipmentId = "pet_griffin",
                isUnlocked = completedIds.contains("node_w2_d2"),
                isCompleted = completedIds.contains("node_boss_dining")
            )
        )
    }
}
