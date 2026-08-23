package com.randallengineering.finances.ui.screens.gear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randallengineering.finances.core.gamification.DefaultEquipmentCatalog
import com.randallengineering.finances.data.repository.GamificationRepository
import com.randallengineering.finances.domain.model.EquipmentItem
import com.randallengineering.finances.domain.model.EquipmentSlot
import com.randallengineering.finances.domain.model.GamificationState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GearLoadoutUiState(
    val gamificationState: GamificationState = GamificationState(),
    val equippedHead: EquipmentItem? = null,
    val equippedChest: EquipmentItem? = null,
    val equippedRelic: EquipmentItem? = null,
    val equippedPet: EquipmentItem? = null,
    val inventoryItems: List<EquipmentItem> = emptyList()
)

class GearLoadoutViewModel(
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    val uiState: StateFlow<GearLoadoutUiState> = gamificationRepository.stateFlow.map { state ->
        val head = DefaultEquipmentCatalog.getItemById(state.equippedHeadId.orEmpty())
        val chest = DefaultEquipmentCatalog.getItemById(state.equippedChestId.orEmpty())
        val relic = DefaultEquipmentCatalog.getItemById(state.equippedRelicId.orEmpty())
        val pet = DefaultEquipmentCatalog.getItemById(state.equippedPetId.orEmpty())

        val inventory = state.inventoryGearIds.mapNotNull { DefaultEquipmentCatalog.getItemById(it) }

        GearLoadoutUiState(
            gamificationState = state,
            equippedHead = head,
            equippedChest = chest,
            equippedRelic = relic,
            equippedPet = pet,
            inventoryItems = inventory
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GearLoadoutUiState()
    )

    fun equipItem(item: EquipmentItem) {
        viewModelScope.launch {
            gamificationRepository.equipItem(item)
        }
    }
}
