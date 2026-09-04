package com.randallengineering.finances.core.sync

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.randallengineering.finances.core.auth.SyncScope
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.local.DomainRecordRow
import com.randallengineering.finances.data.local.GenericRecordDao
import com.randallengineering.finances.data.local.TransactionDao
import com.randallengineering.finances.data.local.TransactionRow
import com.randallengineering.finances.data.model.BudgetEntity
import com.randallengineering.finances.data.model.GoalEntity
import com.randallengineering.finances.data.model.RuleEntity
import com.randallengineering.finances.data.model.TransactionEntity
import com.randallengineering.finances.data.repository.AiConfigRepository
import com.randallengineering.finances.data.repository.AiProviderMode
import com.randallengineering.finances.data.repository.BudgetRepository
import com.randallengineering.finances.data.repository.CategoryRepository
import com.randallengineering.finances.data.repository.DiscretionaryRepository
import com.randallengineering.finances.data.repository.GoalRepository
import com.randallengineering.finances.data.repository.NotificationPrefsRepository
import com.randallengineering.finances.data.repository.RuleRepository
import com.randallengineering.finances.data.repository.SimpleFinRepository
import com.randallengineering.finances.domain.model.Budget
import com.randallengineering.finances.domain.model.CategoryHierarchy
import com.randallengineering.finances.domain.model.Goal
import com.randallengineering.finances.domain.model.Rule
import com.randallengineering.finances.domain.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class CloudSyncState(
    val isSyncing: Boolean = false,
    val lastSyncEpochSeconds: Long = 0L,
    val message: String? = null
)

/**
 * Coordinates bidirectional synchronization between the local Room/SharedPreferences
 * storage and the authenticated user's Google Cloud / Firebase Firestore account.
 *
 * Scoped under `users/{uid}/...`, ensuring SimpleFIN tokens, transactions, categories,
 * budgets, rules, goals, and settings persist across device uninstalls and multi-device logins.
 */
class UserDataSyncManager(
    private val context: Context,
    private val firestore: FirebaseFirestore?,
    private val transactionDao: TransactionDao,
    private val genericRecordDao: GenericRecordDao,
    private val simpleFinRepository: SimpleFinRepository,
    private val budgetRepository: BudgetRepository,
    private val ruleRepository: RuleRepository,
    private val goalRepository: GoalRepository,
    private val categoryRepository: CategoryRepository,
    private val aiConfigRepository: AiConfigRepository,
    private val discretionaryRepository: DiscretionaryRepository,
    private val notificationPrefsRepository: NotificationPrefsRepository
) {
    private val TAG = "UserDataSyncManager"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val prefs = context.getSharedPreferences("randall_cloud_sync_prefs", Context.MODE_PRIVATE)

    private val _syncState = MutableStateFlow(
        CloudSyncState(
            lastSyncEpochSeconds = prefs.getLong("last_sync_epoch", 0L)
        )
    )
    val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    /**
     * Performs a full bidirectional sync for the given user ID.
     */
    suspend fun syncAll(uid: String? = SyncScope.uid): Resource<String> = withContext(Dispatchers.IO) {
        val targetUid = uid ?: SyncScope.uid
        if (targetUid.isNullOrBlank()) {
            return@withContext Resource.Error("Cannot sync: not signed in to a Google account.")
        }
        val db = firestore ?: return@withContext Resource.Error("Firestore is not available.")

        _syncState.value = _syncState.value.copy(isSyncing = true, message = "Syncing with Google account...")

        try {
            // 1. Sync Settings & SimpleFIN Link
            syncSettingsAndSimpleFin(db, targetUid)

            // 2. Sync Custom Categories
            syncCategories(db, targetUid)

            // 3. Sync Budgets
            syncBudgets(db, targetUid)

            // 4. Sync Rules
            syncRules(db, targetUid)

            // 5. Sync Goals
            syncGoals(db, targetUid)

            // 6. Sync Transactions
            syncTransactions(db, targetUid)

            val now = System.currentTimeMillis() / 1000
            prefs.edit().putLong("last_sync_epoch", now).apply()
            _syncState.value = CloudSyncState(
                isSyncing = false,
                lastSyncEpochSeconds = now,
                message = "Synced successfully with Google account."
            )
            Log.i(TAG, "Full Google Cloud sync complete for user $targetUid")
            Resource.Success("Cloud sync complete! All settings, SimpleFIN connection, and financial data restored.")
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}", e)
            _syncState.value = _syncState.value.copy(
                isSyncing = false,
                message = "Sync failed: ${e.localizedMessage}"
            )
            Resource.Error(e.localizedMessage ?: "Cloud sync failed")
        }
    }

    private suspend fun syncSettingsAndSimpleFin(db: FirebaseFirestore, uid: String) {
        val settingsDocRef = db.collection("users").document(uid).collection("config").document("settings")
        val snapshot = try { settingsDocRef.get().await() } catch (e: Exception) { null }

        val localSimpleFin = simpleFinRepository.getAccessUrl()
        val localApiKey = aiConfigRepository.getApiKey()
        val localMode = aiConfigRepository.getProviderMode()
        val localIncomeCat = categoryRepository.getIncomeCategory()
        val localSetpoint = discretionaryRepository.config.value.setpoint
        val localModel = aiConfigRepository.getSelectedModel()

        val cloudSimpleFin = snapshot?.getString("simplefin_access_url")
        val cloudClaimedAt = snapshot?.getLong("simplefin_claimed_at") ?: System.currentTimeMillis()
        val cloudApiKey = snapshot?.getString("gemini_api_key")
        val cloudMode = snapshot?.getString("ai_provider_mode")
        val cloudModel = snapshot?.getString("selected_model")
        val cloudIncomeCat = snapshot?.getString("income_category")
        val cloudSetpoint = snapshot?.getDouble("discretionary_setpoint")

        // SimpleFIN link restore / push
        if (localSimpleFin.isNullOrBlank() && !cloudSimpleFin.isNullOrBlank()) {
            simpleFinRepository.setAccessUrl(cloudSimpleFin, cloudClaimedAt)
            Log.i(TAG, "Restored SimpleFIN link from cloud")
        } else if (!localSimpleFin.isNullOrBlank() && cloudSimpleFin != localSimpleFin) {
            settingsDocRef.set(
                mapOf(
                    "simplefin_access_url" to localSimpleFin,
                    "simplefin_claimed_at" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            ).await()
        }

        // Gemini AI settings restore / push
        if (localApiKey.isBlank() && !cloudApiKey.isNullOrBlank()) {
            val mode = if (cloudMode == AiProviderMode.BUILTIN_VERTEX.name) AiProviderMode.BUILTIN_VERTEX else AiProviderMode.CUSTOM_KEY
            val model = cloudModel ?: AiConfigRepository.DEFAULT_MODEL
            aiConfigRepository.saveConfig(cloudApiKey, mode, model)
            Log.i(TAG, "Restored Gemini AI config from cloud")
        } else if (localApiKey.isNotBlank() && (cloudApiKey != localApiKey || cloudModel != localModel)) {
            settingsDocRef.set(
                mapOf(
                    "gemini_api_key" to localApiKey,
                    "ai_provider_mode" to localMode.name,
                    "selected_model" to localModel
                ),
                SetOptions.merge()
            ).await()
        } else if (!cloudModel.isNullOrBlank() && cloudModel != localModel) {
            aiConfigRepository.setSelectedModel(cloudModel)
        }

        // Income category restore / push
        if (!cloudIncomeCat.isNullOrBlank() && localIncomeCat == "Income" && cloudIncomeCat != "Income") {
            categoryRepository.setIncomeCategory(cloudIncomeCat)
        } else if (localIncomeCat != "Income") {
            settingsDocRef.set(mapOf("income_category" to localIncomeCat), SetOptions.merge()).await()
        }

        // Discretionary setpoint restore / push
        if (localSetpoint <= 0.0 && (cloudSetpoint ?: 0.0) > 0.0) {
            discretionaryRepository.setSetpoint(cloudSetpoint!!)
        } else if (localSetpoint > 0.0) {
            settingsDocRef.set(mapOf("discretionary_setpoint" to localSetpoint), SetOptions.merge()).await()
        }
    }

    private suspend fun syncCategories(db: FirebaseFirestore, uid: String) {
        val catDocRef = db.collection("users").document(uid).collection("config").document("categories")
        val snapshot = try { catDocRef.get().await() } catch (e: Exception) { null }
        val cloudJson = snapshot?.getString("categories_json")

        val localRows = genericRecordDao.getAll(DomainRecordRow.KIND_CATEGORY)
        if (localRows.isEmpty() && !cloudJson.isNullOrBlank()) {
            try {
                val list = json.decodeFromString<List<CategoryHierarchy>>(cloudJson)
                if (list.isNotEmpty()) {
                    categoryRepository.replaceLocalCategories(list)
                    Log.i(TAG, "Restored ${list.size} categories from cloud")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to decode cloud categories: ${e.message}")
            }
        } else if (localRows.isNotEmpty() && cloudJson.isNullOrBlank()) {
            val list = localRows.mapNotNull { r -> runCatching { json.decodeFromString<CategoryHierarchy>(r.json) }.getOrNull() }
            if (list.isNotEmpty()) {
                catDocRef.set(mapOf("categories_json" to json.encodeToString(list)), SetOptions.merge()).await()
            }
        }
    }

    private suspend fun syncBudgets(db: FirebaseFirestore, uid: String) {
        val budgetsColl = db.collection("users").document(uid).collection("budgets")
        val cloudDocs = try { budgetsColl.get().await() } catch (e: Exception) { null }

        val localCount = genericRecordDao.count(DomainRecordRow.KIND_BUDGET)
        if (cloudDocs != null && !cloudDocs.isEmpty) {
            val list = cloudDocs.documents.mapNotNull { doc ->
                runCatching {
                    (doc.toObject(BudgetEntity::class.java) ?: BudgetEntity()).apply {
                        if (id.isBlank()) id = doc.id
                    }.toDomain()
                }.getOrNull()
            }
            if (list.isNotEmpty()) {
                budgetRepository.replaceLocalBudgets(list)
                Log.i(TAG, "Restored ${list.size} budgets from cloud")
            }
        } else if (localCount > 0) {
            val rows = genericRecordDao.getAll(DomainRecordRow.KIND_BUDGET)
            val list = rows.mapNotNull { r -> runCatching { json.decodeFromString<Budget>(r.json) }.getOrNull() }
            val batch = db.batch()
            list.forEach { b ->
                batch.set(budgetsColl.document(b.id), BudgetEntity.fromDomain(b))
            }
            batch.commit().await()
            Log.i(TAG, "Uploaded ${list.size} local budgets to cloud")
        }
    }

    private suspend fun syncRules(db: FirebaseFirestore, uid: String) {
        val rulesColl = db.collection("users").document(uid).collection("rules")
        val cloudDocs = try { rulesColl.get().await() } catch (e: Exception) { null }

        val localCount = genericRecordDao.count(DomainRecordRow.KIND_RULE)
        if (cloudDocs != null && !cloudDocs.isEmpty) {
            val list = cloudDocs.documents.mapNotNull { doc ->
                runCatching {
                    (doc.toObject(RuleEntity::class.java) ?: RuleEntity()).apply {
                        if (id.isBlank()) id = doc.id
                    }.toDomain()
                }.getOrNull()
            }
            if (list.isNotEmpty()) {
                ruleRepository.replaceLocalRules(list)
                Log.i(TAG, "Restored ${list.size} rules from cloud")
            }
        } else if (localCount > 0) {
            val rows = genericRecordDao.getAll(DomainRecordRow.KIND_RULE)
            val list = rows.mapNotNull { r -> runCatching { json.decodeFromString<Rule>(r.json) }.getOrNull() }
            val batch = db.batch()
            list.forEach { r ->
                batch.set(rulesColl.document(r.id), RuleEntity.fromDomain(r))
            }
            batch.commit().await()
            Log.i(TAG, "Uploaded ${list.size} local rules to cloud")
        }
    }

    private suspend fun syncGoals(db: FirebaseFirestore, uid: String) {
        val goalsColl = db.collection("users").document(uid).collection("goals")
        val cloudDocs = try { goalsColl.get().await() } catch (e: Exception) { null }

        val localCount = genericRecordDao.count(DomainRecordRow.KIND_GOAL)
        if (cloudDocs != null && !cloudDocs.isEmpty) {
            val list = cloudDocs.documents.mapNotNull { doc ->
                runCatching {
                    (doc.toObject(GoalEntity::class.java) ?: GoalEntity()).apply {
                        if (id.isBlank()) id = doc.id
                    }.toDomain()
                }.getOrNull()
            }
            if (list.isNotEmpty()) {
                goalRepository.replaceLocalGoals(list)
                Log.i(TAG, "Restored ${list.size} goals from cloud")
            }
        } else if (localCount > 0) {
            val rows = genericRecordDao.getAll(DomainRecordRow.KIND_GOAL)
            val list = rows.mapNotNull { r -> runCatching { json.decodeFromString<Goal>(r.json) }.getOrNull() }
            val batch = db.batch()
            list.forEach { g ->
                batch.set(goalsColl.document(g.id), GoalEntity.fromDomain(g))
            }
            batch.commit().await()
            Log.i(TAG, "Uploaded ${list.size} local goals to cloud")
        }
    }

    private suspend fun syncTransactions(db: FirebaseFirestore, uid: String) {
        val txColl = db.collection("users").document(uid).collection("transactions")
        val cloudDocs = try { txColl.get().await() } catch (e: Exception) { null }

        val localRows = transactionDao.getAll()

        if (cloudDocs != null && !cloudDocs.isEmpty) {
            val newRows = mutableListOf<TransactionRow>()
            cloudDocs.documents.forEach { doc ->
                val domain = runCatching {
                    (doc.toObject(TransactionEntity::class.java) ?: TransactionEntity()).apply {
                        if (id.isBlank()) id = doc.id
                    }.toDomain()
                }.getOrNull()
                if (domain != null) {
                    newRows.add(
                        TransactionRow(
                            id = domain.id,
                            postedEpochSeconds = domain.postedEpochSeconds,
                            json = json.encodeToString(domain)
                        )
                    )
                }
            }
            if (newRows.isNotEmpty()) {
                transactionDao.upsertAll(newRows)
                Log.i(TAG, "Restored/merged ${newRows.size} transactions from cloud")
            }
        }

        // Upload any local transactions not yet in cloud
        val cloudIds = cloudDocs?.documents?.map { it.id }?.toSet().orEmpty()
        val toUpload = localRows.filter { it.id !in cloudIds }
        if (toUpload.isNotEmpty()) {
            toUpload.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { row ->
                    val tx = runCatching { json.decodeFromString<Transaction>(row.json) }.getOrNull()
                    if (tx != null) {
                        batch.set(txColl.document(tx.id), TransactionEntity.fromDomain(tx))
                    }
                }
                batch.commit().await()
            }
            Log.i(TAG, "Uploaded ${toUpload.size} local transactions to cloud")
        }
    }
}
