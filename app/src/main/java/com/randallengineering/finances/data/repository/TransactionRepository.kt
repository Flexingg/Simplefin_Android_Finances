package com.randallengineering.finances.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.model.TransactionEntity
import com.randallengineering.finances.data.model.TransactionSplitEntity
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.domain.model.TransactionSplit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TransactionRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore? = null
) {
    private val prefs = context.getSharedPreferences("randall_finances_txs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val _transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())

    init {
        ioScope.launch {
            loadLocalTransactions()
            attachFirestoreListenerIfAvailable()
        }
    }

    private suspend fun loadLocalTransactions() = withContext(Dispatchers.IO) {
        try {
            val raw = prefs.getString("cached_txs", null)
            if (!raw.isNullOrBlank()) {
                val list = json.decodeFromString<List<Transaction>>(raw)
                _transactionsFlow.value = list.sortedByDescending { it.postedEpochSeconds }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun saveLocalTransactions(list: List<Transaction>) = withContext(Dispatchers.IO) {
        try {
            val sorted = list.sortedByDescending { it.postedEpochSeconds }
            _transactionsFlow.value = sorted
            prefs.edit().putString("cached_txs", json.encodeToString(sorted)).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun attachFirestoreListenerIfAvailable() {
        try {
            firestore?.collection("transactions")
                ?.orderBy("postedEpochSeconds", Query.Direction.DESCENDING)
                ?.addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && !snapshot.isEmpty) {
                        ioScope.launch {
                            val firestoreList = snapshot.documents.mapNotNull { doc ->
                                doc.toObject(TransactionEntity::class.java)?.copy(id = doc.id)?.toDomain()
                            }
                            if (firestoreList.isNotEmpty()) {
                                saveLocalTransactions(firestoreList)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            // Firestore not initialized or offline - local storage handles everything seamlessly
        }
    }

    fun getTransactionsFlow(): Flow<Resource<List<Transaction>>> {
        return _transactionsFlow.asStateFlow()
            .map { list -> Resource.Success(list) }
            .flowOn(Dispatchers.Default)
    }

    suspend fun getTransactionById(id: String): Resource<Transaction> = withContext(Dispatchers.Default) {
        val found = _transactionsFlow.value.find { it.id == id }
        if (found != null) {
            Resource.Success(found)
        } else {
            Resource.Error("Transaction not found")
        }
    }

    suspend fun saveTransaction(transaction: Transaction): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = _transactionsFlow.value.toMutableList()
            val index = current.indexOfFirst { it.id == transaction.id }
            if (index >= 0) {
                current[index] = transaction
            } else {
                current.add(0, transaction)
            }
            saveLocalTransactions(current)

            // Sync to Firestore if available
            firestore?.collection("transactions")
                ?.document(transaction.id)
                ?.set(TransactionEntity.fromDomain(transaction))
                ?.await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save transaction")
        }
    }

    suspend fun saveTransactions(transactions: List<Transaction>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentMap = _transactionsFlow.value.associateBy { it.id }.toMutableMap()
            transactions.forEach { currentMap[it.id] = it }
            val merged = currentMap.values.toList()
            saveLocalTransactions(merged)

            firestore?.let { db ->
                val batch = db.batch()
                transactions.forEach { tx ->
                    val docRef = db.collection("transactions").document(tx.id)
                    batch.set(docRef, TransactionEntity.fromDomain(tx))
                }
                batch.commit().await()
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save transactions")
        }
    }

    suspend fun attachReceiptUrl(transactionId: String, receiptUrl: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = _transactionsFlow.value.toMutableList()
            val index = current.indexOfFirst { it.id == transactionId }
            if (index >= 0) {
                val updated = current[index].copy(receiptUrls = current[index].receiptUrls + receiptUrl)
                current[index] = updated
                saveLocalTransactions(current)

                firestore?.collection("transactions")
                    ?.document(transactionId)
                    ?.update("receiptUrls", updated.receiptUrls)
                    ?.await()
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to attach receipt")
        }
    }

    suspend fun saveTransactionSplits(
        transactionId: String,
        splits: List<TransactionSplit>
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = _transactionsFlow.value.toMutableList()
            val index = current.indexOfFirst { it.id == transactionId }
            if (index >= 0) {
                val updated = current[index].copy(
                    splits = splits
                )
                current[index] = updated
                saveLocalTransactions(current)

                val splitEntities = splits.map { TransactionSplitEntity.fromDomain(it) }
                firestore?.collection("transactions")
                    ?.document(transactionId)
                    ?.update(
                        mapOf(
                            "isSplit" to (splits.isNotEmpty()),
                            "splits" to splitEntities
                        )
                    )
                    ?.await()
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save splits")
        }
    }

    suspend fun deleteTransaction(id: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val current = _transactionsFlow.value.filterNot { it.id == id }
            saveLocalTransactions(current)

            firestore?.collection("transactions")
                ?.document(id)
                ?.delete()
                ?.await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete transaction")
        }
    }
}
