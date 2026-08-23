package com.randallengineering.finances.ui.screens.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.core.util.CurrencyFormatter
import com.randallengineering.finances.data.repository.CategoryRepository
import com.randallengineering.finances.data.repository.GamificationRepository
import com.randallengineering.finances.data.repository.RuleRepository
import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.model.CategoryHierarchy
import com.randallengineering.finances.domain.model.Rule
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.domain.model.TransactionSplit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class CategorySearchResult(
    val mainCategory: String,
    val subCategory: String = "",
    val fullDisplayName: String = if (subCategory.isNotBlank()) "$mainCategory > $subCategory" else mainCategory
)

data class ActionQueueUiState(
    val pendingTransactions: List<Transaction> = emptyList(),
    val allTransactions: List<Transaction> = emptyList(),
    val currentCardIndex: Int = 0,
    val comboMultiplier: Int = 1,
    val totalXpEarnedInSession: Int = 0,
    val isSessionComplete: Boolean = false,
    val availableCategories: List<CategoryHierarchy> = emptyList(),
    val rules: List<Rule> = emptyList(),
    val lastRuleCreatedMessage: String? = null
) {
    val currentTransaction: Transaction?
        get() = pendingTransactions.getOrNull(currentCardIndex)
}

class ActionQueueViewModel(
    private val transactionRepository: TransactionRepository,
    private val gamificationRepository: GamificationRepository,
    private val categoryRepository: CategoryRepository,
    private val ruleRepository: RuleRepository
) : ViewModel() {

    private val _currentIndex = MutableStateFlow(0)
    private val _combo = MutableStateFlow(1)
    private val _sessionXp = MutableStateFlow(0)
    private val _ruleMessage = MutableStateFlow<String?>(null)

    private var cachedAllTransactions: List<Transaction> = emptyList()

    val uiState: StateFlow<ActionQueueUiState> = combine(
        listOf(
            transactionRepository.getTransactionsFlow(),
            categoryRepository.getCategoriesFlow(),
            ruleRepository.getRulesFlow(),
            _currentIndex,
            _combo,
            _sessionXp,
            _ruleMessage
        )
    ) { args ->
        val txResource = args[0] as Resource<List<Transaction>>
        val catResource = args[1] as Resource<List<CategoryHierarchy>>
        val ruleResource = args[2] as Resource<List<Rule>>
        val index = args[3] as Int
        val combo = args[4] as Int
        val xpEarned = args[5] as Int
        val ruleMsg = args[6] as? String

        val allTxs = (txResource as? Resource.Success)?.data.orEmpty()
        val customCats = (catResource as? Resource.Success)?.data.orEmpty()
        val existingRules = (ruleResource as? Resource.Success)?.data.orEmpty()

        cachedAllTransactions = allTxs

        // Extract all distinct categories from transactions in database
        val txCategories = allTxs
            .map { it.category.trim() }
            .filter { it.isNotBlank() && !it.equals("Uncategorized", ignoreCase = true) }
            .distinct()

        // Build combined category list
        val customMainNames = customCats.map { it.mainCategory.lowercase() }.toSet()
        val extraFromTxs = txCategories.filter { it.lowercase() !in customMainNames }.map { CategoryHierarchy(mainCategory = it) }
        
        val defaultFallback = if (customCats.isEmpty() && extraFromTxs.isEmpty()) {
            listOf(
                CategoryHierarchy("Dining", listOf("Restaurants", "Fast Food", "Coffee")),
                CategoryHierarchy("Groceries", listOf("Supermarket", "Pantry")),
                CategoryHierarchy("Automotive", listOf("Gas", "Fuel", "Maintenance")),
                CategoryHierarchy("Utilities", listOf("Electric", "Internet", "Water", "Fuel")),
                CategoryHierarchy("Shopping", listOf("Clothing", "Electronics")),
                CategoryHierarchy("Entertainment", listOf("Movies", "Games")),
                CategoryHierarchy("Health & Medical", listOf("Pharmacy", "Doctor")),
                CategoryHierarchy("Income", listOf("Salary", "Deposit"))
            )
        } else {
            emptyList()
        }

        val allCategories = (customCats + extraFromTxs + defaultFallback).distinctBy { it.mainCategory.lowercase() }

        // Prioritize uncategorized or transactions needing review first
        val uncategorized = allTxs.filter { it.category.equals("Uncategorized", ignoreCase = true) || it.category.isBlank() }
        val otherPending = allTxs.filter { !it.category.equals("Uncategorized", ignoreCase = true) && it.category.isNotBlank() }
        val pending = if (uncategorized.isNotEmpty()) {
            (uncategorized + otherPending).take(25)
        } else if (allTxs.isNotEmpty()) {
            allTxs.take(20)
        } else {
            generateSampleQueue()
        }

        val isComplete = index >= pending.size

        ActionQueueUiState(
            pendingTransactions = pending,
            allTransactions = allTxs,
            currentCardIndex = index,
            comboMultiplier = combo,
            totalXpEarnedInSession = xpEarned,
            isSessionComplete = isComplete,
            availableCategories = allCategories,
            rules = existingRules,
            lastRuleCreatedMessage = ruleMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ActionQueueUiState()
    )

    fun searchCategoriesAndSubCategories(query: String, categories: List<CategoryHierarchy>): List<CategorySearchResult> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return emptyList()

        val results = mutableListOf<CategorySearchResult>()

        for (cat in categories) {
            val mainMatches = cat.mainCategory.lowercase().contains(q)
            if (mainMatches) {
                results.add(CategorySearchResult(mainCategory = cat.mainCategory, subCategory = ""))
            }

            for (sub in cat.subCategories) {
                if (sub.lowercase().contains(q) || (mainMatches && q.length > 2)) {
                    results.add(CategorySearchResult(mainCategory = cat.mainCategory, subCategory = sub))
                }
            }
        }

        return results.distinctBy { it.fullDisplayName }.take(8)
    }

    fun extractCleanMerchantPattern(rawDesc: String): String {
        var clean = rawDesc.uppercase().trim()
        
        // Remove common banking prefixes
        listOf("POS DEBIT", "PURCHASE", "CHECKCARD", "DEBIT CARD", "VISA DDA", "RECURRING", "AUTOPAY", "PAYMENT", "TRANSFER", "TST*", "SQ *", "SP *", "FSP*", "PAYPAL *").forEach { prefix ->
            clean = clean.removePrefix(prefix).trim()
        }

        // Clean out transaction reference numbers (#1234, *1234, numbers at end)
        clean = clean.replace(Regex("#\\s*\\d+"), "")
        clean = clean.replace(Regex("\\*\\s*\\d+"), "")
        clean = clean.replace(Regex("\\b\\d{4,}\\b"), "") // Remove standalone 4+ digit numbers

        // Remove extra spaces or trailing punctuation
        clean = clean.replace(Regex("[^A-Z0-9&\\s-]"), " ").trim()
        clean = clean.replace(Regex("\\s+"), " ").trim()

        // Extract first 1-3 significant words
        val words = clean.split(" ").filter { it.length > 1 && !it.all { ch -> ch.isDigit() } }
        return if (words.isNotEmpty()) words.take(2).joinToString(" ") else rawDesc.take(15).trim()
    }

    fun calculateMatchesForPattern(pattern: String): Int {
        if (pattern.isBlank()) return 0
        val testRule = Rule(
            id = "test",
            name = "test",
            priority = 1,
            pattern = pattern,
            category = "test"
        )
        return cachedAllTransactions.count { testRule.matches(it.originalDesc, it.amount) }
    }

    fun confirmCategory(tx: Transaction, note: String = "") {
        viewModelScope.launch {
            val combo = _combo.value
            val hasNoteBonus = note.isNotBlank()
            val baseReward = (15 * combo) + (if (hasNoteBonus) 10 else 0)
            
            if (hasNoteBonus && note != tx.notes) {
                transactionRepository.saveTransaction(tx.copy(notes = note.trim()))
            }
            
            val gainedXp = gamificationRepository.addXp(baseReward, tx.category)
            _sessionXp.value += gainedXp
            _combo.value = (combo + 1).coerceAtMost(5)
            _currentIndex.value += 1
        }
    }

    fun editCategory(tx: Transaction, newCategory: String, newSubCategory: String, note: String = "") {
        viewModelScope.launch {
            val hasNoteBonus = note.isNotBlank()
            val baseReward = 10 + (if (hasNoteBonus) 10 else 0)
            
            val updated = tx.copy(
                category = newCategory,
                subCategory = newSubCategory,
                notes = if (hasNoteBonus) note.trim() else tx.notes
            )
            transactionRepository.saveTransaction(updated)
            
            val gainedXp = gamificationRepository.addXp(baseReward, newCategory)
            _sessionXp.value += gainedXp
            _currentIndex.value += 1
        }
    }

    /**
     * Creates an auto-rule from the current queue card and applies it to all matching transactions in database.
     */
    fun createAutoRuleAndCategorize(
        tx: Transaction,
        pattern: String,
        newCategory: String,
        newSubCategory: String,
        note: String = ""
    ) {
        viewModelScope.launch {
            val safePattern = pattern.trim().ifBlank { extractCleanMerchantPattern(tx.originalDesc) }
            val nextPriority = uiState.value.rules.size + 1
            val newRule = Rule(
                id = UUID.randomUUID().toString(),
                name = safePattern,
                priority = nextPriority,
                pattern = safePattern,
                category = newCategory,
                subCategory = newSubCategory
            )

            // Save the rule
            ruleRepository.saveRule(newRule)

            // Apply to existing transactions
            val (updatedTxs, count) = ruleRepository.applySingleRule(newRule, cachedAllTransactions)
            if (updatedTxs.isNotEmpty()) {
                transactionRepository.saveTransactionsBatch(updatedTxs)
            }

            // Update current transaction
            val hasNoteBonus = note.isNotBlank()
            val updatedCurrent = tx.copy(
                category = newCategory,
                subCategory = newSubCategory,
                matchedRuleId = newRule.id,
                notes = if (hasNoteBonus) note.trim() else tx.notes
            )
            transactionRepository.saveTransaction(updatedCurrent)

            // Award extra XP (+25 bonus XP for creating an Auto-Rule!)
            val combo = _combo.value
            val gainedXp = gamificationRepository.addXp(35 + (if (hasNoteBonus) 10 else 0), newCategory)
            _sessionXp.value += gainedXp
            _combo.value = (combo + 1).coerceAtMost(5)

            _ruleMessage.value = "⚡ Auto-Rule created for \"$safePattern\" & applied to $count transactions (+35 XP!)"
            _currentIndex.value += 1
        }
    }

    fun splitTransaction(tx: Transaction, splits: List<TransactionSplit>) {
        viewModelScope.launch {
            val primaryCat = splits.firstOrNull()?.category ?: tx.category
            val primarySub = splits.firstOrNull()?.subCategory ?: tx.subCategory
            val notesSummary = splits.joinToString(", ") { 
                "${it.category}: ${CurrencyFormatter.format(it.amount)}${if (it.notes.isNotBlank()) " (${it.notes})" else ""}" 
            }

            val noteBonusCount = splits.count { it.notes.isNotBlank() }
            val noteBonusXp = noteBonusCount * 10

            val updated = tx.copy(
                category = primaryCat,
                subCategory = primarySub,
                splits = splits,
                notes = if (tx.notes.isNotBlank()) "${tx.notes} | Split: $notesSummary" else "Split: $notesSummary"
            )
            transactionRepository.saveTransaction(updated)

            val combo = _combo.value
            val gainedXp = gamificationRepository.addXp(35 + noteBonusXp, primaryCat)
            _sessionXp.value += gainedXp
            _combo.value = (combo + 1).coerceAtMost(5)
            _currentIndex.value += 1
        }
    }

    fun addCustomCategory(mainCategory: String, subCategory: String?) {
        viewModelScope.launch {
            categoryRepository.addOrUpdateCategory(mainCategory, subCategory)
        }
    }

    fun resetSession() {
        _currentIndex.value = 0
        _combo.value = 1
        _sessionXp.value = 0
        _ruleMessage.value = null
    }

    fun clearRuleMessage() {
        _ruleMessage.value = null
    }

    private fun generateSampleQueue(): List<Transaction> {
        val now = System.currentTimeMillis() / 1000
        return listOf(
            Transaction(
                id = "sample-1",
                originalDesc = "SHELL OIL 12345 HOUSTON TX",
                amount = -45.20,
                postedEpochSeconds = now - 3600,
                category = "Automotive",
                subCategory = "Gas",
                payee = "Shell Gas Station"
            ),
            Transaction(
                id = "sample-2",
                originalDesc = "STARBUCKS STORE #1042 SEATTLE",
                amount = -6.75,
                postedEpochSeconds = now - 7200,
                category = "Dining",
                subCategory = "Coffee",
                payee = "Starbucks"
            ),
            Transaction(
                id = "sample-3",
                originalDesc = "KROGER #544 GROCERY",
                amount = -112.40,
                postedEpochSeconds = now - 86400,
                category = "Groceries",
                subCategory = "Supermarket",
                payee = "Kroger"
            )
        )
    }
}
