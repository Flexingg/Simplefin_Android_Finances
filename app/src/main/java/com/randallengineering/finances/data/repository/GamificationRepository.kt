package com.randallengineering.finances.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.randallengineering.finances.core.gamification.DefaultEquipmentCatalog
import com.randallengineering.finances.domain.model.EquipmentItem
import com.randallengineering.finances.domain.model.EquipmentSlot
import com.randallengineering.finances.domain.model.GamificationState
import com.randallengineering.finances.domain.model.PerkType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class GamificationRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore? = null
) {
    private val prefs = context.getSharedPreferences("randall_finances_gamification", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    private val _stateFlow = MutableStateFlow(loadState())
    val stateFlow: StateFlow<GamificationState> = _stateFlow.asStateFlow()

    private fun loadState(): GamificationState {
        val raw = prefs.getString("gamification_state", null)
        return if (!raw.isNullOrBlank()) {
            try {
                json.decodeFromString<GamificationState>(raw)
            } catch (e: Exception) {
                GamificationState()
            }
        } else {
            GamificationState()
        }
    }

    private fun saveState(state: GamificationState) {
        _stateFlow.value = state
        prefs.edit().putString("gamification_state", json.encodeToString(state)).apply()

        try {
            firestore?.collection("gamification")?.document("user_state")
                ?.set(state)
        } catch (e: Exception) {
            // Offline fallback
        }
    }

    fun addXp(baseXp: Int, category: String? = null): Int {
        val current = _stateFlow.value
        val equippedItems = getEquippedItems(current)
        
        var multiplier = 1.0
        equippedItems.forEach { item ->
            when (item.perkType) {
                PerkType.XP_BOOST_GENERAL -> multiplier *= item.multiplier
                PerkType.XP_BOOST_DINING -> if (category?.lowercase()?.contains("dining") == true || category?.lowercase()?.contains("food") == true) multiplier *= item.multiplier
                PerkType.XP_BOOST_GROCERIES -> if (category?.lowercase()?.contains("grocer") == true) multiplier *= item.multiplier
                else -> Unit
            }
        }

        val finalXpGained = (baseXp * multiplier).toInt().coerceAtLeast(1)
        val newTotalXp = current.xp + finalXpGained
        val newLevel = (newTotalXp / 250) + 1

        saveState(current.copy(xp = newTotalXp, level = newLevel))
        return finalXpGained
    }

    fun loseHeart(category: String? = null): Boolean {
        val current = _stateFlow.value
        
        // Check if Coffee Ring of Restraint absorbs this loss
        if (category?.lowercase()?.contains("coffee") == true) {
            val hasCoffeeRing = current.equippedRelicId == "relic_ring"
            if (hasCoffeeRing) {
                // Heart loss absorbed!
                return false
            }
        }

        if (current.hearts > 0) {
            val newHearts = current.hearts - 1
            saveState(current.copy(hearts = newHearts))
            return true
        }
        return false
    }

    fun refillHearts() {
        val current = _stateFlow.value
        saveState(current.copy(hearts = current.maxHearts))
    }

    fun updateStreak(): Boolean {
        val current = _stateFlow.value
        val lastDate = Instant.ofEpochSecond(current.lastActiveEpochSeconds).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()

        return when {
            lastDate.isEqual(today) -> false // Already counted today
            lastDate.plusDays(1).isEqual(today) -> {
                // Consecutive streak day
                val newStreak = current.streakDays + 1
                val bonusXp = 20 * newStreak.coerceAtMost(5)
                saveState(current.copy(streakDays = newStreak, lastActiveEpochSeconds = System.currentTimeMillis() / 1000, xp = current.xp + bonusXp))
                true
            }
            else -> {
                // Streak broken unless freeze used
                val newStreak = if (current.streakFreezesAvailable > 0) {
                    current.streakDays
                } else {
                    1
                }
                saveState(current.copy(streakDays = newStreak, lastActiveEpochSeconds = System.currentTimeMillis() / 1000))
                true
            }
        }
    }

    fun equipItem(item: EquipmentItem) {
        val current = _stateFlow.value
        val updated = when (item.slot) {
            EquipmentSlot.HEAD -> current.copy(equippedHeadId = item.id)
            EquipmentSlot.CHEST -> current.copy(equippedChestId = item.id)
            EquipmentSlot.RELIC -> current.copy(equippedRelicId = item.id)
            EquipmentSlot.PET -> current.copy(equippedPetId = item.id)
        }
        saveState(updated)
    }

    fun completeQuestNode(nodeId: String, xpReward: Int, gemsReward: Int) {
        val current = _stateFlow.value
        if (!current.completedNodeIds.contains(nodeId)) {
            val newCompleted = current.completedNodeIds + nodeId
            saveState(
                current.copy(
                    completedNodeIds = newCompleted,
                    xp = current.xp + xpReward,
                    gems = current.gems + gemsReward
                )
            )
        }
    }

    fun getEquippedItems(state: GamificationState = _stateFlow.value): List<EquipmentItem> {
        val ids = listOfNotNull(state.equippedHeadId, state.equippedChestId, state.equippedRelicId, state.equippedPetId)
        return ids.mapNotNull { DefaultEquipmentCatalog.getItemById(it) }
    }
}
