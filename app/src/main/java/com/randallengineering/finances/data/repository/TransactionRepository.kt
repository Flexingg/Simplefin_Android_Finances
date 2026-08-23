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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TransactionRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore? = null
) {
    private val prefs = context.getSharedPreferences("randall_finances_txs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    private val _transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())

    init {
        loadLocalTransactions()
        attachFirestoreListenerIfAvailable()
    }

    private fun loadLocalTransactions() {
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

    private fun saveLocalTransactions(list: List<Transaction>) {
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
                        val firestoreList = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(TransactionEntity::class.java)?.copy(id = doc.id)?.toDomain()
                        }
                        if (firestoreList.isNotEmpty()) {
                            saveLocalTransactions(firestoreList)
                        }
                    }
                }
        } catch (e: Exception) {
            // Firestore not initialized or offline - local storage handles everything seamlessly
        }
    }

    fun getTransactionsFlow(): Flow<Resource<List<Transaction>>> {
        return _transactionsFlow.asStateFlow().map { list ->
            Resource.Success(list)
        }
    }

    suspend fun getTransactionById(id: String): Resource<Transaction> {
        val found = _transactionsFlow.value.find { it.id == id }
        return if (found != null) {
            Resource.Success(found)
        } else {
            Resource.Error("Transaction not found")
        }
    }

    suspend fun saveTransaction(transaction: Transaction): Resource<Unit> {
        val current = _transactionsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == transaction.id }
        if (index >= 0) {
            current[index] = transaction
        } else {
            current.add(0, transaction)
        }
        saveLocalTransactions(current)

        // Also push to Firestore in background if available
        try {
            firestore?.collection("transactions")?.document(transaction.id)
                ?.set(TransactionEntity.fromDomain(transaction))
        } catch (e: Exception) {
            // Ignored if offline
        }
        return Resource.Success(Unit)
    }

    suspend fun saveTransactionsBatch(updatedList: List<Transaction>): Resource<Unit> {
        val current = _transactionsFlow.value.toMutableList()
        val updateMap = updatedList.associateBy { it.id }
        val merged = current.map { existing -> updateMap[existing.id] ?: existing }
        saveLocalTransactions(merged)

        try {
            val batch = firestore?.batch()
            if (batch != null) {
                for (tx in updatedList) {
                    val docRef = firestore.collection("transactions").document(tx.id)
                    batch.set(docRef, TransactionEntity.fromDomain(tx))
                }
                batch.commit()
            }
        } catch (e: Exception) {
            // Offline fallback
        }
        return Resource.Success(Unit)
    }

    suspend fun saveTransactionSplits(
        transactionId: String,
        splits: List<TransactionSplit>
    ): Resource<Unit> {
        val current = _transactionsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == transactionId }
        if (index >= 0) {
            val updated = current[index].copy(splits = splits)
            current[index] = updated
            saveLocalTransactions(current)

            try {
                val splitEntities = splits.map { TransactionSplitEntity.fromDomain(it) }
                firestore?.collection("transactions")?.document(transactionId)
                    ?.update("splits", splitEntities)
            } catch (e: Exception) {
                // Ignored if offline
            }
            return Resource.Success(Unit)
        }
        return Resource.Error("Transaction not found")
    }

    suspend fun attachReceiptUrl(
        transactionId: String,
        downloadUrl: String
    ): Resource<Unit> {
        val current = _transactionsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == transactionId }
        if (index >= 0) {
            val existing = current[index].receiptUrls
            val updated = current[index].copy(receiptUrls = (existing + downloadUrl).distinct())
            current[index] = updated
            saveLocalTransactions(current)

            try {
                firestore?.collection("transactions")?.document(transactionId)
                    ?.update("receiptUrls", updated.receiptUrls)
            } catch (e: Exception) {
                // Ignored if offline
            }
            return Resource.Success(Unit)
        }
        return Resource.Error("Transaction not found")
    }

    suspend fun batchInsertOrUpdate(transactions: List<Transaction>): Resource<Unit> {
        val map = _transactionsFlow.value.associateBy { it.id }.toMutableMap()
        transactions.forEach { tx ->
            map[tx.id] = tx
        }
        val mergedList = map.values.toList()
        saveLocalTransactions(mergedList)

        // Try pushing batch to Firestore in background
        try {
            if (firestore != null) {
                val batch = firestore.batch()
                val coll = firestore.collection("transactions")
                transactions.take(500).forEach { tx ->
                    val docRef = coll.document(tx.id)
                    batch.set(docRef, TransactionEntity.fromDomain(tx))
                }
                batch.commit()
            }
        } catch (e: Exception) {
            // Ignored if offline
        }

        return Resource.Success(Unit)
    }
}
