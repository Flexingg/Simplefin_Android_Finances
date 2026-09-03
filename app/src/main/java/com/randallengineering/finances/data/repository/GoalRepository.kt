package com.randallengineering.finances.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.randallengineering.finances.core.auth.SyncScope
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.local.DomainRecordRow
import com.randallengineering.finances.data.local.GenericRecordDao
import com.randallengineering.finances.data.model.GoalEntity
import com.randallengineering.finances.domain.model.Goal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class GoalRepository(
    private val context: Context,
    private val dao: GenericRecordDao,
    private val firestore: FirebaseFirestore? = null
) {
    private val prefs = context.getSharedPreferences("randall_finances_goals", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val _goalsFlow = MutableStateFlow<List<Goal>>(emptyList())

    init {
        ioScope.launch {
            loadLocalGoals()
            attachFirestoreListenerIfAvailable()
        }
    }

    private suspend fun loadLocalGoals() = withContext(Dispatchers.IO) {
        try {
            if (dao.count(DomainRecordRow.KIND_GOAL) == 0) {
                val raw = prefs.getString("cached_goals", null)
                if (!raw.isNullOrBlank()) {
                    val legacy = json.decodeFromString<List<Goal>>(raw)
                    if (legacy.isNotEmpty()) { saveLocalGoals(legacy); return@withContext }
                }
            }
            val rows = dao.getAll(DomainRecordRow.KIND_GOAL)
            val list = rows.mapNotNull { r -> runCatching { json.decodeFromString<Goal>(r.json) }.getOrNull() }
            _goalsFlow.value = list.sortedBy { it.targetEpochSeconds }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun reloadGoals() = loadLocalGoals()

    suspend fun replaceLocalGoals(list: List<Goal>) = saveLocalGoals(list)

    private suspend fun saveLocalGoals(list: List<Goal>) = withContext(Dispatchers.IO) {
        try {
            val sorted = list.sortedBy { it.targetEpochSeconds }
            _goalsFlow.value = sorted
            dao.clear(DomainRecordRow.KIND_GOAL)
            dao.upsertAll(
                sorted.map { DomainRecordRow(DomainRecordRow.KIND_GOAL, it.id, json.encodeToString(it)) }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun attachFirestoreListenerIfAvailable() {
        try {
            firestore?.collection(SyncScope.path("goals"))
                ?.orderBy("targetEpochSeconds", Query.Direction.ASCENDING)
                ?.addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && !snapshot.isEmpty) {
                        ioScope.launch {
                            try {
                                val list = snapshot.documents.mapNotNull { doc ->
                                    doc.toObject(GoalEntity::class.java)?.copy(id = doc.id)?.toDomain()
                                }
                                if (list.isNotEmpty()) {
                                    saveLocalGoals(list)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            // Offline fallback
        }
    }

    fun getGoalsFlow(): Flow<Resource<List<Goal>>> {
        return _goalsFlow.asStateFlow().map { list ->
            Resource.Success(list)
        }
    }

    suspend fun saveGoal(goal: Goal): Resource<Unit> {
        val safeGoal = if (goal.id.isBlank()) goal.copy(id = UUID.randomUUID().toString()) else goal
        val current = _goalsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == safeGoal.id }
        if (index >= 0) {
            current[index] = safeGoal
        } else {
            current.add(safeGoal)
        }
        saveLocalGoals(current)

        try {
            firestore?.collection(SyncScope.path("goals"))?.document(safeGoal.id)
                ?.set(GoalEntity.fromDomain(safeGoal))
        } catch (e: Exception) {
            // Ignored if offline
        }
        return Resource.Success(Unit)
    }

    suspend fun deleteGoal(goalId: String): Resource<Unit> {
        val current = _goalsFlow.value.filter { it.id != goalId }
        saveLocalGoals(current)

        try {
            firestore?.collection(SyncScope.path("goals"))?.document(goalId)?.delete()
        } catch (e: Exception) {
            // Ignored if offline
        }
        return Resource.Success(Unit)
    }
}
