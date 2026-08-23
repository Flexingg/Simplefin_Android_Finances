package com.randallengineering.finances.ui.screens.gear

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.randallengineering.finances.core.theme.Shapes
import com.randallengineering.finances.domain.model.EquipmentItem
import com.randallengineering.finances.domain.model.EquipmentRarity
import com.randallengineering.finances.domain.model.EquipmentSlot
import com.randallengineering.finances.ui.components.DuoBlue
import com.randallengineering.finances.ui.components.DuoBlueDark
import com.randallengineering.finances.ui.components.DuoGold
import com.randallengineering.finances.ui.components.DuoGoldDark
import com.randallengineering.finances.ui.components.DuoGreen
import com.randallengineering.finances.ui.components.DuoGreenDark
import com.randallengineering.finances.ui.components.DuolingoPressableButton
import com.randallengineering.finances.ui.components.GamificationHud
import org.koin.androidx.compose.koinViewModel

@Composable
fun GearLoadoutScreen(
    viewModel: GearLoadoutViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            GamificationHud()
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.large,
                    colors = CardDefaults.cardColors(containerColor = DuoBlue)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text("HERO LOADOUT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.8f))
                        Text("Financial Armor & Relics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text("Equip gear to unlock passive XP multipliers and heart shields against overspending!", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                    }
                }
            }

            // Equipped Slots 2x2 Grid
            item {
                Text("Equipped Loadout", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EquippedSlotCard(
                        title = "Headgear",
                        item = uiState.equippedHead,
                        modifier = Modifier.weight(1f)
                    )
                    EquippedSlotCard(
                        title = "Chest Armor",
                        item = uiState.equippedChest,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EquippedSlotCard(
                        title = "Relic / Ring",
                        item = uiState.equippedRelic,
                        modifier = Modifier.weight(1f)
                    )
                    EquippedSlotCard(
                        title = "Companion Pet",
                        item = uiState.equippedPet,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Inventory Section
            item {
                Text("Inventory Gear (${uiState.inventoryItems.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }

            items(uiState.inventoryItems) { item ->
                val isEquipped = item.id == uiState.equippedHead?.id ||
                        item.id == uiState.equippedChest?.id ||
                        item.id == uiState.equippedRelic?.id ||
                        item.id == uiState.equippedPet?.id

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(item.iconEmoji, style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = item.rarity.name,
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (item.rarity) {
                                        EquipmentRarity.LEGENDARY -> DuoGoldDark
                                        EquipmentRarity.EPIC -> Color(0xFF9333EA)
                                        EquipmentRarity.RARE -> DuoBlueDark
                                        else -> Color.Gray
                                    }
                                )
                            }
                        }

                        if (isEquipped) {
                            Card(
                                shape = Shapes.small,
                                colors = CardDefaults.cardColors(containerColor = DuoGreen.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = DuoGreenDark, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Equipped", fontWeight = FontWeight.Bold, color = DuoGreenDark, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        } else {
                            DuolingoPressableButton(
                                onClick = { viewModel.equipItem(item) },
                                backgroundColor = DuoGreen,
                                shadowColor = DuoGreenDark,
                                cornerRadius = 10.dp,
                                shadowHeight = 2.dp
                            ) {
                                Text("Equip", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EquippedSlotCard(
    title: String,
    item: EquipmentItem?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = Shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(item?.iconEmoji ?: "➕", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = item?.name ?: "Empty Slot",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}
