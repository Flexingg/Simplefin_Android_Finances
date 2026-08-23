package com.randallengineering.finances.core.gamification

import com.randallengineering.finances.domain.model.EquipmentItem
import com.randallengineering.finances.domain.model.EquipmentRarity
import com.randallengineering.finances.domain.model.EquipmentSlot
import com.randallengineering.finances.domain.model.PerkType

object DefaultEquipmentCatalog {

    val ALL_ITEMS = listOf(
        EquipmentItem(
            id = "head_visor",
            name = "Visor of Diligence",
            slot = EquipmentSlot.HEAD,
            rarity = EquipmentRarity.RARE,
            description = "+15% XP for confirming SimpleFIN transactions within 24h",
            perkType = PerkType.XP_BOOST_GENERAL,
            multiplier = 1.15,
            iconEmoji = "🥽"
        ),
        EquipmentItem(
            id = "head_crown",
            name = "Crown of Thrift",
            slot = EquipmentSlot.HEAD,
            rarity = EquipmentRarity.EPIC,
            description = "+25% XP bonus for zero-spend days",
            perkType = PerkType.XP_BOOST_GENERAL,
            multiplier = 1.25,
            iconEmoji = "👑"
        ),
        EquipmentItem(
            id = "chest_shield",
            name = "Aegis of Frugality",
            slot = EquipmentSlot.CHEST,
            rarity = EquipmentRarity.RARE,
            description = "+1.5x XP when Dining Out spending remains under weekly target",
            perkType = PerkType.XP_BOOST_DINING,
            multiplier = 1.5,
            iconEmoji = "🛡️"
        ),
        EquipmentItem(
            id = "chest_cloak",
            name = "Cloak of Discretion",
            slot = EquipmentSlot.CHEST,
            rarity = EquipmentRarity.COMMON,
            description = "+10% XP across all verified categorized transactions",
            perkType = PerkType.XP_BOOST_GENERAL,
            multiplier = 1.10,
            iconEmoji = "🧥"
        ),
        EquipmentItem(
            id = "relic_ring",
            name = "Coffee Ring of Restraint",
            slot = EquipmentSlot.RELIC,
            rarity = EquipmentRarity.EPIC,
            description = "Absorbs 1 Heart loss per week for coffee shop splurges",
            perkType = PerkType.HEART_PROTECTION_COFFEE,
            multiplier = 1.0,
            iconEmoji = "💍"
        ),
        EquipmentItem(
            id = "relic_vault",
            name = "Vault Charm of Compounding",
            slot = EquipmentSlot.RELIC,
            rarity = EquipmentRarity.LEGENDARY,
            description = "Earns 5 Gems for every $50 transferred to Savings Goals",
            perkType = PerkType.SAVINGS_GEM_FINDER,
            multiplier = 2.0,
            iconEmoji = "💎"
        ),
        EquipmentItem(
            id = "pet_piggy",
            name = "Penny the Piggy",
            slot = EquipmentSlot.PET,
            rarity = EquipmentRarity.RARE,
            description = "Finds +2 Gems on daily login streaks",
            perkType = PerkType.SAVINGS_GEM_FINDER,
            multiplier = 1.0,
            iconEmoji = "🐷"
        ),
        EquipmentItem(
            id = "pet_griffin",
            name = "Gilded Griffin",
            slot = EquipmentSlot.PET,
            rarity = EquipmentRarity.LEGENDARY,
            description = "+2.0x XP multiplier across all quest milestone completions",
            perkType = PerkType.XP_BOOST_GENERAL,
            multiplier = 2.0,
            iconEmoji = "🦅"
        )
    )

    fun getItemById(id: String): EquipmentItem? = ALL_ITEMS.find { it.id == id }
}
