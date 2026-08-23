package com.randallengineering.finances.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class EquipmentSlot {
    HEAD,
    CHEST,
    RELIC,
    PET
}

@Serializable
enum class EquipmentRarity {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY
}

@Serializable
enum class PerkType {
    XP_BOOST_GENERAL,
    XP_BOOST_DINING,
    XP_BOOST_GROCERIES,
    HEART_PROTECTION_COFFEE,
    SAVINGS_GEM_FINDER,
    STREAK_SHIELD
}

@Serializable
data class EquipmentItem(
    val id: String,
    val name: String,
    val slot: EquipmentSlot,
    val rarity: EquipmentRarity = EquipmentRarity.COMMON,
    val description: String,
    val perkType: PerkType,
    val multiplier: Double = 1.0,
    val iconEmoji: String = "🛡️"
)

@Serializable
enum class QuestNodeType {
    DAILY_ADHERENCE,
    WEEKLY_BUDGET,
    SAVINGS_CHEST,
    BOSS_BATTLE,
    FINANCIAL_QUIZ,
    ZERO_SPEND,
    AMAZON_MATCH,
    INBOX_ZERO
}

@Serializable
data class QuestNode(
    val id: String,
    val title: String,
    val subtitle: String,
    val weekNumber: Int,
    val nodeType: QuestNodeType,
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val progressText: String = "",
    val progressPercent: Float = 0f,
    val isCriteriaMet: Boolean = false,
    val requirementDescription: String = "",
    val rewardXp: Int = 50,
    val rewardGems: Int = 10,
    val rewardEquipmentId: String? = null,
    val isUnlocked: Boolean = false,
    val isCompleted: Boolean = false,
    val bossName: String? = null,
    val bossMaxHp: Double = 500.0,
    val bossCurrentHp: Double = 500.0
)

@Serializable
data class GamificationState(
    val streakDays: Int = 1,
    val lastActiveEpochSeconds: Long = System.currentTimeMillis() / 1000,
    val xp: Int = 120,
    val level: Int = 1,
    val gems: Int = 50,
    val hearts: Int = 5,
    val maxHearts: Int = 5,
    val lastHeartRegenEpochSeconds: Long = System.currentTimeMillis() / 1000,
    val streakFreezesAvailable: Int = 1,
    val equippedHeadId: String? = "head_visor",
    val equippedChestId: String? = "chest_shield",
    val equippedRelicId: String? = "relic_ring",
    val equippedPetId: String? = "pet_piggy",
    val inventoryGearIds: List<String> = listOf("head_visor", "chest_shield", "relic_ring", "pet_piggy", "head_crown", "chest_cloak"),
    val completedNodeIds: List<String> = listOf("node_w1_d1")
) {
    val levelTitle: String
        get() = when (level) {
            1 -> "Novice Saver"
            2 -> "Budget Scout"
            3 -> "Frugal Knight"
            4 -> "Treasury Keeper"
            5 -> "Wealth Paladin"
            6 -> "Compound Master"
            else -> "Financial Grandmaster"
        }

    val xpForNextLevel: Int
        get() = level * 250

    val levelProgressPercent: Float
        get() = ((xp % 250).toFloat() / 250f).coerceIn(0f, 1f)
}
