package com.randallengineering.finances.data.repository

import android.content.Context
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.domain.model.SimpleFinAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Holds the live account list + balances returned by SimpleFIN. This is the
 * source of truth for accurate net worth (real bank balances, not a
 * transaction-derived estimate).
 */
class AccountRepository(context: Context) {

    private val prefs = context.getSharedPreferences("randall_finances_accounts", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    private val _accountsFlow = MutableStateFlow<List<SimpleFinAccount>>(emptyList())

    init {
        load()
    }

    private fun load() {
        try {
            val raw = prefs.getString("accounts", null)
            if (!raw.isNullOrBlank()) {
                _accountsFlow.value = json.decodeFromString<List<SimpleFinAccount>>(raw)
            }
        } catch (e: Exception) {
            _accountsFlow.value = emptyList()
        }
    }

    fun getAccountsFlow(): Flow<Resource<List<SimpleFinAccount>>> =
        _accountsFlow.asStateFlow().map { Resource.Success(it) }.flowOn(Dispatchers.Default)

    suspend fun saveAccounts(accounts: List<SimpleFinAccount>) = withContext(Dispatchers.IO) {
        val sorted = accounts.sortedBy { it.name.lowercase() }
        _accountsFlow.value = sorted
        try {
            prefs.edit().putString("accounts", json.encodeToString(sorted)).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Sum of live account balances (net worth). Null if no account data. */
    fun totalBalance(): Double? =
        if (_accountsFlow.value.isEmpty()) null
        else _accountsFlow.value.sumOf { it.balance }
}
