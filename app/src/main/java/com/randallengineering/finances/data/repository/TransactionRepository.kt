package com.randallengineering.finances.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.randallengineering.finances.core.auth.SyncScope
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.local.TransactionDao
import com.randallengineering.finances.data.local.TransactionRow
import com.randallengineering.finances.data.model.TransactionEntity
import com.randallengineering.finances.data.model.TransactionSplitEntity
import com.randallengineering.finances.domain.model.Transaction
import com.randallengineering.finances.domain.model.TransactionSplit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Transaction persistence backed by a local Room DB (replaces the fragile
 * single-JSON-blob SharedPreferences cache). The public API is unchanged so no
 * screen/view-model code needed touching.
 */
class TransactionRepository(
    context: Context,
    private val dao: TransactionDao,
    private val firestore: FirebaseFirestore? = null
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val ioScope = CoroutineScope(Dispatchers.IO)

    init {
        ioScope.launch {
            migrateFromSharedPreferencesIfNeeded(context)
            attachFirestoreListenerIfAvailable()
        }
    }

    /** One-time copy of any legacy SharedPreferences cache into Room. */
    private suspend fun migrateFromSharedPreferencesIfNeeded(context: Context) {
        try {
            if (dao.count() > 0) return
            val prefs = context.getSharedPreferences("randall_finances_txs", Context.MODE_PRIVATE)
            val raw = prefs.getString("cached_txs", null)
            if (!raw.isNullOrBlank()) {
                val legacy = json.decodeFromString<List<Transaction>>(raw)
                if (legacy.isNotEmpty()) dao.upsertAll(legacy.map { toRow(sanitize(it)) })
            }
        } catch (e: Exception) {
            // Corrupt/missing legacy cache — Room starts empty, fine.
        }
    }

    private fun toRow(tx: Transaction): TransactionRow = TransactionRow(
        id = tx.id,
        postedEpochSeconds = tx.postedEpochSeconds,
        json = json.encodeToString(tx)
    )

    private fun decodeRow(row: TransactionRow): Transaction? = try {
        json.decodeFromString<Transaction>(row.json)
    } catch (e: Exception) {
        null
    }

    private fun sanitize(tx: Transaction): Transaction = tx.copy(
        id = tx.id.ifBlank { UUID.randomUUID().toString() },
        amount = if (tx.amount.isFinite()) tx.amount else 0.0
    )

    fun getTransactionsFlow(): Flow<Resource<List<Transaction>>> {
        return dao.observeAll()
            .map { rows ->
                Resource.Success(rows.mapNotNull { decodeRow(it) })
            }
            .flowOn(Dispatchers.Default)
    }

    suspend fun getTransactionById(id: String): Resource<Transaction> = withContext(Dispatchers.Default) {
        val row = dao.getById(id)
        val tx = row?.let { decodeRow(it) }
        if (tx != null) Resource.Success(tx) else Resource.Error("Transaction not found")
    }

    suspend fun saveTransaction(transaction: Transaction): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val safe = sanitize(transaction)
            dao.upsert(toRow(safe))
            firestore?.collection(SyncScope.path("transactions"))
                ?.document(safe.id)
                ?.set(TransactionEntity.fromDomain(safe))
                ?.await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save transaction")
        }
    }

    suspend fun saveTransactions(transactions: List<Transaction>): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val rows = transactions.map { toRow(sanitize(it)) }
            dao.upsertAll(rows)
            firestore?.let { db ->
                val batch = db.batch()
                rows.forEach { row ->
                    val tx = decodeRow(row) ?: return@forEach
                    batch.set(db.collection(SyncScope.path("transactions")).document(tx.id), TransactionEntity.fromDomain(tx))
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
            val row = dao.getById(transactionId) ?: return@withContext Resource.Error("Transaction not found")
            val current = decodeRow(row) ?: return@withContext Resource.Error("Transaction not found")
            val updated = current.copy(receiptUrls = current.receiptUrls + receiptUrl)
            dao.upsert(toRow(updated))
            firestore?.collection(SyncScope.path("transactions"))
                ?.document(transactionId)
                ?.update("receiptUrls", updated.receiptUrls)
                ?.await()
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
            val row = dao.getById(transactionId) ?: return@withContext Resource.Error("Transaction not found")
            val current = decodeRow(row) ?: return@withContext Resource.Error("Transaction not found")
            val updated = current.copy(splits = splits)
            dao.upsert(toRow(updated))
            val splitEntities = splits.map { TransactionSplitEntity.fromDomain(it) }
            firestore?.collection(SyncScope.path("transactions"))
                ?.document(transactionId)
                ?.update(mapOf("isSplit" to (splits.isNotEmpty()), "splits" to splitEntities))
                ?.await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save splits")
        }
    }

    suspend fun deleteTransaction(id: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.delete(id)
            firestore?.collection(SyncScope.path("transactions"))?.document(id)?.delete()?.await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete transaction")
        }
    }

    private fun attachFirestoreListenerIfAvailable() {
        try {
            firestore?.collection(SyncScope.path("transactions"))
                ?.orderBy("postedEpochSeconds", Query.Direction.DESCENDING)
                ?.addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && !snapshot.isEmpty) {
                        ioScope.launch {
                            try {
                                val rows = snapshot.documents.mapNotNull { doc ->
                                    doc.toObject(TransactionEntity::class.java)?.copy(id = doc.id)?.toDomain()
                                }.map { toRow(sanitize(it)) }
                                if (rows.isNotEmpty()) dao.upsertAll(rows)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            // Firestore not initialized or offline - Room handles everything locally.
        }
    }
}
