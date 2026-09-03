package com.randallengineering.finances.data.repository

import com.randallengineering.finances.core.finance.DiscretionaryCalculator
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.local.DomainRecordRow
import com.randallengineering.finances.data.local.GenericRecordDao
import com.randallengineering.finances.domain.model.DiscretionaryConfig
import com.randallengineering.finances.domain.model.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Holds the discretionary-spending setpoint + the "Necessary" categories, and
 * exposes a single shared [state] combining the config with the transaction set
 * so Dashboard, Settings and (via Firestore) Home Assistant all read the same
 * numbers. Config persists as a single Room row; state is recomputed whenever
 * either the config or the transactions change.
 */
class DiscretionaryRepository(
    private val dao: GenericRecordDao,
    private val transactionRepository: TransactionRepository
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _config = MutableStateFlow(DiscretionaryConfig())
    val config: StateFlow<DiscretionaryConfig> = _config.asStateFlow()

    data class DiscretionaryState(
        val config: DiscretionaryConfig = DiscretionaryConfig(),
        val monthlySpend: Double = 0.0,
        val remaining: Double = 0.0,
        val spentByCategory: List<DiscretionaryCalculator.CategoryRow> = emptyList(),
        /** Every spend category the user has ever used, for the Necessary/Discretionary toggle list. */
        val toggleCategories: List<String> = emptyList()
    )

    private val transactionList = transactionRepository.getTransactionsFlow()
        .map { res -> res.getOrNull().orEmpty() }

    private val _state = MutableStateFlow(DiscretionaryState())
    val state: StateFlow<DiscretionaryState> = _state.asStateFlow()

    init {
        scope.launch { loadConfig() }
        scope.launch { collectState() }
    }

    private suspend fun collectState() {
        combine(config, transactionList) { cfg, txs ->
            val summary = DiscretionaryCalculator.summary(
                transactions = txs,
                necessaryCategories = cfg.necessaryCategories.toSet(),
                setpoint = cfg.setpoint
            )
            DiscretionaryState(
                config = cfg,
                monthlySpend = summary.monthlySpend,
                remaining = summary.remaining,
                spentByCategory = summary.categories,
                toggleCategories = toggleCategoriesFrom(txs, cfg)
            )
        }.collect { _state.value = it }
    }

    private fun toggleCategoriesFrom(txs: List<Transaction>, cfg: DiscretionaryConfig): List<String> {
        val transferIds = com.randallengineering.finances.core.finance.TransferDetection.detectTransferIds(txs)
        val used = txs
            .filter { it.amount < 0 && it.id !in transferIds }
            .map { it.category.ifBlank { "Uncategorized" } }
            .filter { it.isNotBlank() }
            .toMutableSet()
        used.addAll(cfg.necessaryCategories)
        return used.sorted()
    }

    private suspend fun loadConfig() = withContext(Dispatchers.IO) {
        try {
            val row = dao.getAll(DomainRecordRow.KIND_DISCRETIONARY)
                .firstOrNull { it.recordId == CONFIG_ID }
            if (row != null) _config.value = json.decodeFromString<DiscretionaryConfig>(row.json)
        } catch (_: Exception) {
            _config.value = DiscretionaryConfig()
        }
    }

    fun setSetpoint(setpoint: Double) {
        persist(_config.value.copy(setpoint = if (setpoint >= 0) setpoint else 0.0))
    }

    /** Marks [category] as Necessary (true) or Discretionary (false). */
    fun setCategoryNecessary(category: String, necessary: Boolean) {
        val current = _config.value.necessaryCategories.toMutableSet()
        if (necessary) current.add(category) else current.remove(category)
        persist(_config.value.copy(necessaryCategories = current.sorted().toList()))
    }

    private fun persist(next: DiscretionaryConfig) {
        _config.value = next
        scope.launch {
            runCatching {
                dao.clear(DomainRecordRow.KIND_DISCRETIONARY)
                dao.upsertAll(
                    listOf(DomainRecordRow(DomainRecordRow.KIND_DISCRETIONARY, CONFIG_ID, json.encodeToString(next)))
                )
            }
        }
    }

    companion object {
        private const val CONFIG_ID = "config"
    }
}
