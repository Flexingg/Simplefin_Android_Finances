package com.randallengineering.finances.ui.screens.transactions

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.repository.AmazonRepository
import com.randallengineering.finances.data.repository.CategoryRepository
import com.randallengineering.finances.data.repository.RuleRepository
import com.randallengineering.finances.data.repository.StorageRepository
import com.randallengineering.finances.data.repository.TransactionRepository
import com.randallengineering.finances.domain.model.CategoryHierarchy
import com.randallengineering.finances.domain.model.MatchedAmazonOrder
import com.randallengineering.finances.domain.model.Rule
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.domain.model.TransactionSplit
import com.randallengineering.finances.domain.usecase.RuleMatcherUseCase
import com.randallengineering.finances.domain.usecase.TransactionSplitUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val rules: List<Rule> = emptyList(),
    val categories: List<CategoryHierarchy> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryFilter: String? = null,
    val isLoading: Boolean = false,
    val selectedTransactionForSplit: Transaction? = null,
    val selectedTransactionForCategoryPicker: Transaction? = null,
    val selectedTransactionForRuleGen: Transaction? = null,
    val isUploadingReceipt: Boolean = false,
    val selectedAmazonTransaction: Transaction? = null,
    val matchedAmazonOrder: MatchedAmazonOrder? = null,
    val isFetchingAmazonOrder: Boolean = false,
    val amazonErrorMessage: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class TransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val ruleRepository: RuleRepository,
    private val categoryRepository: CategoryRepository,
    private val amazonRepository: AmazonRepository,
    private val ruleMatcherUseCase: RuleMatcherUseCase,
    private val transactionSplitUseCase: TransactionSplitUseCase,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                transactionRepository.getTransactionsFlow(),
                ruleRepository.getRulesFlow(),
                categoryRepository.getCategoriesFlow()
            ) { txResource, rulesResource, catResource ->
                Triple(txResource, rulesResource, catResource)
            }.collect { (txResource, rulesResource, catResource) ->
                val isLoading = txResource.isLoading || rulesResource.isLoading || catResource.isLoading
                val txList = txResource.getOrNull().orEmpty()
                val rulesList = rulesResource.getOrNull().orEmpty()
                val catList = catResource.getOrNull().orEmpty()

                val categorized = if (rulesList.isNotEmpty()) {
                    ruleMatcherUseCase.categorizeBatch(txList, rulesList)
                } else {
                    txList
                }

                _uiState.update { current ->
                    current.copy(
                        transactions = categorized,
                        rules = rulesList,
                        categories = catList,
                        isLoading = isLoading,
                        filteredTransactions = applyFilter(categorized, current.searchQuery, current.selectedCategoryFilter)
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                filteredTransactions = applyFilter(current.transactions, query, current.selectedCategoryFilter)
            )
        }
    }

    fun onCategoryFilterSelect(category: String?) {
        _uiState.update { current ->
            current.copy(
                selectedCategoryFilter = category,
                filteredTransactions = applyFilter(current.transactions, current.searchQuery, category)
            )
        }
    }

    private fun applyFilter(
        list: List<Transaction>,
        query: String,
        categoryFilter: String?
    ): List<Transaction> {
        return list.filter { tx ->
            val matchesQuery = query.isBlank() ||
                    tx.originalDesc.contains(query, ignoreCase = true) ||
                    tx.payee.contains(query, ignoreCase = true) ||
                    tx.category.contains(query, ignoreCase = true) ||
                    tx.subCategory.contains(query, ignoreCase = true)

            val matchesCategory = categoryFilter == null || tx.category.equals(categoryFilter, ignoreCase = true)

            matchesQuery && matchesCategory
        }
    }

    fun openCategoryPicker(transaction: Transaction) {
        _uiState.update { it.copy(selectedTransactionForCategoryPicker = transaction) }
    }

    fun closeCategoryPicker() {
        _uiState.update { it.copy(selectedTransactionForCategoryPicker = null) }
    }

    fun updateTransactionCategory(transaction: Transaction, mainCategory: String, subCategory: String) {
        val updated = transaction.copy(
            category = mainCategory,
            subCategory = subCategory,
            matchedRuleId = null
        )
        viewModelScope.launch {
            transactionRepository.saveTransaction(updated)
            _uiState.update {
                it.copy(
                    selectedTransactionForCategoryPicker = null,
                    successMessage = "Categorized as '$mainCategory${if (subCategory.isNotBlank()) " > $subCategory" else ""}'"
                )
            }
        }
    }

    fun addCustomCategory(mainCategory: String, subCategory: String?) {
        categoryRepository.addOrUpdateCategory(mainCategory, subCategory)
    }

    fun openRuleGenDialog(transaction: Transaction) {
        _uiState.update { it.copy(selectedTransactionForRuleGen = transaction) }
    }

    fun closeRuleGenDialog() {
        _uiState.update { it.copy(selectedTransactionForRuleGen = null) }
    }

    fun saveGeneratedRule(rule: Rule) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = ruleRepository.saveRule(rule)) {
                is Resource.Success -> {
                    categoryRepository.addOrUpdateCategory(rule.category, rule.subCategory.ifBlank { null })
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedTransactionForRuleGen = null,
                            successMessage = "Auto-rule '${rule.name}' saved and applied!"
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun openSplitDialog(transaction: Transaction) {
        _uiState.update { it.copy(selectedTransactionForSplit = transaction) }
    }

    fun closeSplitDialog() {
        _uiState.update { it.copy(selectedTransactionForSplit = null) }
    }

    fun saveTransactionSplits(transaction: Transaction, splits: List<TransactionSplit>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = transactionSplitUseCase.applySplits(transaction, splits)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedTransactionForSplit = null,
                            successMessage = "Transaction successfully split into ${splits.size} allocations"
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun uploadReceipt(
        transactionId: String,
        uri: Uri,
        fileName: String = "receipt_${System.currentTimeMillis()}.jpg",
        mimeType: String = "image/jpeg",
        userId: String = "default_user"
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingReceipt = true, errorMessage = null) }
            when (val uploadResult = storageRepository.uploadReceipt(userId, transactionId, fileName, uri, mimeType)) {
                is Resource.Success -> {
                    val downloadUrl = uploadResult.data
                    transactionRepository.attachReceiptUrl(transactionId, downloadUrl)
                    _uiState.update {
                        it.copy(
                            isUploadingReceipt = false,
                            successMessage = "Receipt uploaded and attached successfully!"
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isUploadingReceipt = false,
                            errorMessage = uploadResult.message
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    // Amazon Order Details
    fun openAmazonOrderDetails(transaction: Transaction) {
        _uiState.update {
            it.copy(
                selectedAmazonTransaction = transaction,
                isFetchingAmazonOrder = true,
                matchedAmazonOrder = null,
                amazonErrorMessage = null
            )
        }
        viewModelScope.launch {
            when (val result = amazonRepository.fetchOrderDetailsForTransaction(transaction.postedEpochSeconds, transaction.amount)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isFetchingAmazonOrder = false,
                            matchedAmazonOrder = result.data,
                            amazonErrorMessage = null
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isFetchingAmazonOrder = false,
                            amazonErrorMessage = result.message
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun closeAmazonOrderDetails() {
        _uiState.update {
            it.copy(
                selectedAmazonTransaction = null,
                matchedAmazonOrder = null,
                isFetchingAmazonOrder = false,
                amazonErrorMessage = null
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
