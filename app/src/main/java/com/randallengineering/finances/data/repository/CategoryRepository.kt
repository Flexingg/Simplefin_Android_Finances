package com.randallengineering.finances.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.domain.model.CategoryHierarchy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CategoryRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore? = null
) {
    private val prefs = context.getSharedPreferences("randall_categories", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val _categoriesFlow = MutableStateFlow<List<CategoryHierarchy>>(emptyList())
    private val _incomeCategoryFlow = MutableStateFlow(prefs.getString("income_category_name", "Income") ?: "Income")

    init {
        loadCategories()
    }

    fun getIncomeCategory(): String = _incomeCategoryFlow.value

    fun getIncomeCategoryFlow(): Flow<String> = _incomeCategoryFlow.asStateFlow()

    fun setIncomeCategory(name: String) {
        val clean = name.trim().ifBlank { "Income" }
        _incomeCategoryFlow.value = clean
        prefs.edit().putString("income_category_name", clean).apply()
    }

    private fun loadCategories() {
        try {
            val raw = prefs.getString("custom_categories", null)
            if (!raw.isNullOrBlank()) {
                val list = json.decodeFromString<List<CategoryHierarchy>>(raw)
                _categoriesFlow.value = list
            } else {
                _categoriesFlow.value = emptyList()
            }
        } catch (e: Exception) {
            _categoriesFlow.value = emptyList()
        }
    }

    private fun saveCategories(list: List<CategoryHierarchy>) {
        _categoriesFlow.value = list
        try {
            prefs.edit().putString("custom_categories", json.encodeToString(list)).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCategoriesFlow(): Flow<Resource<List<CategoryHierarchy>>> {
        return _categoriesFlow.asStateFlow().map { Resource.Success(it) }
    }

    fun addOrUpdateCategory(mainCategory: String, subCategory: String? = null) {
        val cleanMain = mainCategory.trim()
        if (cleanMain.isBlank()) return

        val current = _categoriesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.mainCategory.equals(cleanMain, ignoreCase = true) }

        if (index >= 0) {
            val existing = current[index]
            if (!subCategory.isNullOrBlank()) {
                val cleanSub = subCategory.trim()
                if (!existing.subCategories.any { it.equals(cleanSub, ignoreCase = true) }) {
                    current[index] = existing.copy(subCategories = existing.subCategories + cleanSub)
                }
            }
        } else {
            val subs = if (!subCategory.isNullOrBlank()) listOf(subCategory.trim()) else emptyList()
            current.add(CategoryHierarchy(mainCategory = cleanMain, subCategories = subs))
        }

        saveCategories(current)
    }

    fun renameMainCategory(oldMain: String, newMain: String) {
        val cleanOld = oldMain.trim()
        val cleanNew = newMain.trim()
        if (cleanOld.isBlank() || cleanNew.isBlank()) return

        val current = _categoriesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.mainCategory.equals(cleanOld, ignoreCase = true) }
        if (index >= 0) {
            val existing = current[index]
            current[index] = existing.copy(mainCategory = cleanNew)
            saveCategories(current)

            // If income category was this category, update it
            if (_incomeCategoryFlow.value.equals(cleanOld, ignoreCase = true)) {
                setIncomeCategory(cleanNew)
            }
        }
    }

    fun addSubCategory(mainCategory: String, subCategory: String) {
        val cleanMain = mainCategory.trim()
        val cleanSub = subCategory.trim()
        if (cleanMain.isBlank() || cleanSub.isBlank()) return

        val current = _categoriesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.mainCategory.equals(cleanMain, ignoreCase = true) }
        if (index >= 0) {
            val existing = current[index]
            if (!existing.subCategories.any { it.equals(cleanSub, ignoreCase = true) }) {
                current[index] = existing.copy(subCategories = existing.subCategories + cleanSub)
                saveCategories(current)
            }
        } else {
            current.add(CategoryHierarchy(mainCategory = cleanMain, subCategories = listOf(cleanSub)))
            saveCategories(current)
        }
    }

    fun renameSubCategory(mainCategory: String, oldSub: String, newSub: String) {
        val cleanMain = mainCategory.trim()
        val cleanOld = oldSub.trim()
        val cleanNew = newSub.trim()
        if (cleanMain.isBlank() || cleanOld.isBlank() || cleanNew.isBlank()) return

        val current = _categoriesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.mainCategory.equals(cleanMain, ignoreCase = true) }
        if (index >= 0) {
            val existing = current[index]
            val updatedSubs = existing.subCategories.map { if (it.equals(cleanOld, ignoreCase = true)) cleanNew else it }
            current[index] = existing.copy(subCategories = updatedSubs)
            saveCategories(current)
        }
    }

    fun deleteSubCategory(mainCategory: String, subCategory: String) {
        val current = _categoriesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.mainCategory.equals(mainCategory.trim(), ignoreCase = true) }
        if (index >= 0) {
            val existing = current[index]
            val updatedSubs = existing.subCategories.filterNot { it.equals(subCategory.trim(), ignoreCase = true) }
            current[index] = existing.copy(subCategories = updatedSubs)
            saveCategories(current)
        }
    }

    fun deleteMainCategory(mainCategory: String) {
        val current = _categoriesFlow.value.filterNot { it.mainCategory.equals(mainCategory.trim(), ignoreCase = true) }
        saveCategories(current)
    }
}
