package com.randallengineering.finances.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.data.local.DomainRecordRow
import com.randallengineering.finances.data.local.GenericRecordDao
import com.randallengineering.finances.domain.model.CategoryHierarchy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CategoryRepository(
    private val context: Context,
    private val dao: GenericRecordDao,
    private val firestore: FirebaseFirestore? = null
) {
    private val prefs = context.getSharedPreferences("randall_categories", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val _categoriesFlow = MutableStateFlow<List<CategoryHierarchy>>(emptyList())
    private val _incomeCategoryFlow = MutableStateFlow(prefs.getString("income_category_name", "Income") ?: "Income")

    init {
        ioScope.launch {
            loadCategories()
        }
    }

    fun getIncomeCategory(): String = _incomeCategoryFlow.value

    fun getIncomeCategoryFlow(): Flow<String> = _incomeCategoryFlow.asStateFlow()

    fun setIncomeCategory(name: String) {
        val clean = name.trim().ifBlank { "Income" }
        _incomeCategoryFlow.value = clean
        ioScope.launch {
            prefs.edit().putString("income_category_name", clean).apply()
        }
    }

    private suspend fun loadCategories() = withContext(Dispatchers.IO) {
        try {
            if (dao.count(DomainRecordRow.KIND_CATEGORY) == 0) {
                val raw = prefs.getString("custom_categories", null)
                if (!raw.isNullOrBlank()) {
                    val legacy = json.decodeFromString<List<CategoryHierarchy>>(raw)
                    if (legacy.isNotEmpty()) { saveCategories(legacy); return@withContext }
                }
            }
            val rows = dao.getAll(DomainRecordRow.KIND_CATEGORY)
            _categoriesFlow.value = rows.mapNotNull { r -> runCatching { json.decodeFromString<CategoryHierarchy>(r.json) }.getOrNull() }
        } catch (e: Exception) {
            _categoriesFlow.value = emptyList()
        }
    }

    suspend fun reloadCategories() = loadCategories()

    suspend fun replaceLocalCategories(list: List<CategoryHierarchy>) = saveCategories(list)

    private suspend fun saveCategories(list: List<CategoryHierarchy>) = withContext(Dispatchers.IO) {
        _categoriesFlow.value = list
        try {
            dao.clear(DomainRecordRow.KIND_CATEGORY)
            dao.upsertAll(
                list.map { DomainRecordRow(DomainRecordRow.KIND_CATEGORY, it.mainCategory, json.encodeToString(it)) }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCategoriesFlow(): Flow<Resource<List<CategoryHierarchy>>> {
        return _categoriesFlow.asStateFlow()
            .map { Resource.Success(it) }
            .flowOn(Dispatchers.Default)
    }

    suspend fun addOrUpdateCategory(mainCategory: String, subCategory: String? = null) = withContext(Dispatchers.IO) {
        val cleanMain = mainCategory.trim().replaceFirstChar { it.uppercase() }
        if (cleanMain.isBlank()) return@withContext

        val current = _categoriesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.mainCategory.equals(cleanMain, ignoreCase = true) }

        if (index >= 0) {
            val existing = current[index]
            val cleanSub = subCategory?.trim()?.replaceFirstChar { it.uppercase() }
            val updatedSubs = if (!cleanSub.isNullOrBlank() && !existing.subCategories.any { it.equals(cleanSub, ignoreCase = true) }) {
                (existing.subCategories + cleanSub).sorted()
            } else {
                existing.subCategories
            }
            current[index] = existing.copy(subCategories = updatedSubs)
        } else {
            val cleanSub = subCategory?.trim()?.replaceFirstChar { it.uppercase() }
            val subs = if (!cleanSub.isNullOrBlank()) listOf(cleanSub) else emptyList()
            current.add(CategoryHierarchy(mainCategory = cleanMain, subCategories = subs))
        }

        saveCategories(current.sortedBy { it.mainCategory })
    }

    suspend fun renameCategory(oldName: String, newName: String) = withContext(Dispatchers.IO) {
        val cleanNew = newName.trim().replaceFirstChar { it.uppercase() }
        if (cleanNew.isBlank() || oldName.equals(cleanNew, ignoreCase = true)) return@withContext

        val current = _categoriesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.mainCategory.equals(oldName, ignoreCase = true) }
        if (index >= 0) {
            val existing = current[index]
            current[index] = existing.copy(mainCategory = cleanNew)
            saveCategories(current.sortedBy { it.mainCategory })
        }
    }

    suspend fun renameSubcategory(mainCategory: String, oldSub: String, newSub: String) = withContext(Dispatchers.IO) {
        val cleanNew = newSub.trim().replaceFirstChar { it.uppercase() }
        if (cleanNew.isBlank() || oldSub.equals(cleanNew, ignoreCase = true)) return@withContext

        val current = _categoriesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.mainCategory.equals(mainCategory, ignoreCase = true) }
        if (index >= 0) {
            val existing = current[index]
            val updatedSubs = existing.subCategories.map {
                if (it.equals(oldSub, ignoreCase = true)) cleanNew else it
            }.distinct().sorted()
            current[index] = existing.copy(subCategories = updatedSubs)
            saveCategories(current)
        }
    }

    suspend fun deleteCategory(mainCategory: String) = withContext(Dispatchers.IO) {
        val current = _categoriesFlow.value.filterNot { it.mainCategory.equals(mainCategory, ignoreCase = true) }
        saveCategories(current)
    }

    suspend fun deleteSubcategory(mainCategory: String, subCategory: String) = withContext(Dispatchers.IO) {
        val current = _categoriesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.mainCategory.equals(mainCategory, ignoreCase = true) }
        if (index >= 0) {
            val existing = current[index]
            val updatedSubs = existing.subCategories.filterNot { it.equals(subCategory, ignoreCase = true) }
            current[index] = existing.copy(subCategories = updatedSubs)
            saveCategories(current)
        }
    }
}
