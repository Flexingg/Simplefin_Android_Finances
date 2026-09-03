package com.randallengineering.finances.data.repository

import android.content.Context
import android.util.Base64
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.randallengineering.finances.core.auth.SyncScope
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.core.network.SimpleFinAccountsResponse
import com.randallengineering.finances.data.model.SimpleFinConfigEntity
import com.randallengineering.finances.domain.model.SimpleFinAccount
import com.randallengineering.finances.domain.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs
import kotlin.math.min

class SimpleFinRepository(
    private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val netWorthRepository: NetWorthRepository,
    private val functions: FirebaseFunctions? = null,
    private val firestore: FirebaseFirestore? = null
) {
    private val prefs = context.getSharedPreferences("randall_simplefin_config", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val _configFlow = MutableStateFlow<SimpleFinConfigEntity?>(null)

    init {
        loadConfig()
    }

    fun loadConfig() {
        val accessUrl = prefs.getString("access_url", null)
        val lastSync = prefs.getLong("last_sync", 0L)
        val errorString = prefs.getString("error_list", null)
        val errList = if (!errorString.isNullOrBlank()) errorString.split(";;") else emptyList()

        val entity = SimpleFinConfigEntity(
            id = "simplefin",
            accessUrlConfigured = !accessUrl.isNullOrBlank(),
            lastSyncTimestamp = lastSync,
            errorList = errList
        )
        _configFlow.value = entity
    }

    fun getAccessUrl(): String? = prefs.getString("access_url", null)

    fun setAccessUrl(url: String, claimedAt: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putString("access_url", url)
            .putLong("claimed_at", claimedAt)
            .apply()
        loadConfig()
        SyncScope.uid?.let { uid ->
            firestore?.collection("users")?.document(uid)?.collection("config")?.document("settings")
                ?.set(
                    mapOf(
                        "simplefin_access_url" to url,
                        "simplefin_claimed_at" to claimedAt
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
        }
    }

    fun getConfigFlow(userId: String = "default_user"): Flow<Resource<SimpleFinConfigEntity?>> {
        return MutableStateFlow<Resource<SimpleFinConfigEntity?>>(Resource.Success(_configFlow.value)).asStateFlow()
    }

    /**
     * Claims the Base64 SimpleFIN Setup Token.
     */
    suspend fun claimSetupToken(setupToken: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val tokenClean = setupToken.trim()
            if (tokenClean.isBlank()) {
                return@withContext Resource.Error("Setup token cannot be empty.")
            }

            // 1. Decode Base64 Setup Token -> Claim URL
            val decodedBytes = try {
                Base64.decode(tokenClean, Base64.DEFAULT)
            } catch (e: Exception) {
                return@withContext Resource.Error("Invalid Base64 token encoding.")
            }
            val claimUrl = String(decodedBytes, Charsets.UTF_8).trim()

            if (!claimUrl.startsWith("http://") && !claimUrl.startsWith("https://")) {
                return@withContext Resource.Error("Decoded token does not contain a valid URL: $claimUrl")
            }

            // 2. Issue POST request to SimpleFIN claim URL
            val url = URL(claimUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                setFixedLengthStreamingMode(0)
                doOutput = true
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                return@withContext Resource.Error("SimpleFIN Bridge Claim Failed ($responseCode): $errorBody")
            }

            val accessUrl = connection.inputStream.bufferedReader().use { it.readText().trim() }
            if (!accessUrl.startsWith("http://") && !accessUrl.startsWith("https://")) {
                return@withContext Resource.Error("Received unexpected Access URL format: $accessUrl")
            }

            // 3. Save Access URL securely in SharedPreferences
            prefs.edit()
                .putString("access_url", accessUrl)
                .putLong("claimed_at", System.currentTimeMillis())
                .apply()

            loadConfig()

            SyncScope.uid?.let { uid ->
                firestore?.collection("users")?.document(uid)?.collection("config")?.document("settings")
                    ?.set(
                        mapOf(
                            "simplefin_access_url" to accessUrl,
                            "simplefin_claimed_at" to System.currentTimeMillis()
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
            }

            Resource.Success("SimpleFIN Bridge successfully connected!")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to claim SimpleFIN token", e)
        }
    }

    /**
     * Executes transaction sync in 89-day batch windows (up to 1000 days total).
     * SimpleFIN limits queries to max 90 days per request.
     */
    suspend fun triggerSync(daysBack: Int = 90): Resource<List<String>> = withContext(Dispatchers.IO) {
        try {
            val accessUrl = prefs.getString("access_url", null)
            if (accessUrl.isNullOrBlank()) {
                return@withContext Resource.Error("SimpleFIN Access URL is not configured. Please claim a setup token first.")
            }

            val parsedUrl = URL(accessUrl)
            val userInfo = parsedUrl.userInfo // "username:password"
            val basicAuth = if (!userInfo.isNullOrBlank()) {
                "Basic " + Base64.encodeToString(userInfo.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            } else null

            val baseUrl = if (!userInfo.isNullOrBlank()) {
                accessUrl.replaceFirst("${userInfo}@", "")
            } else {
                accessUrl
            }
            val cleanBaseUrl = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl

            val safeDaysBack = daysBack.coerceIn(1, 1000)
            val nowEpochSeconds = System.currentTimeMillis() / 1000L
            val dayInSeconds = 24L * 60L * 60L
            val batchWindowDays = 89L // 89-day batch window to stay strictly under SimpleFIN's 90-day limit

            val allTransactionsMap = mutableMapOf<String, Transaction>()
            val allAccountsMap = mutableMapOf<String, SimpleFinAccount>()
            val collectedErrors = mutableListOf<String>()

            var currentOffsetDays = 0L
            val totalBatches = ((safeDaysBack + batchWindowDays - 1) / batchWindowDays).coerceAtLeast(1)

            for (batchIndex in 0 until totalBatches) {
                val endDayOffset = currentOffsetDays
                val startDayOffset = min(currentOffsetDays + batchWindowDays, safeDaysBack.toLong())

                val endEpoch = nowEpochSeconds - (endDayOffset * dayInSeconds)
                val startEpoch = nowEpochSeconds - (startDayOffset * dayInSeconds)

                val requestUrl = "$cleanBaseUrl/accounts?version=2&start-date=$startEpoch&end-date=$endEpoch"

                try {
                    val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 25000
                        readTimeout = 30000
                        setRequestProperty("Accept", "application/json")
                        if (basicAuth != null) {
                            setRequestProperty("Authorization", basicAuth)
                        }
                    }

                    val responseCode = connection.responseCode
                    if (responseCode in 200..299) {
                        val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                        val data = json.decodeFromString<SimpleFinAccountsResponse>(responseBody)

                        collectedErrors.addAll(data.allErrors)

                        for (acc in data.accounts) {
                            // Capture live account + balance (source of truth for net worth).
                            allAccountsMap[acc.id] = SimpleFinAccount(
                                id = acc.id,
                                name = acc.name.ifBlank { acc.org?.name ?: acc.id },
                                orgName = acc.org?.name ?: "",
                                currency = acc.currency,
                                balance = acc.balance.toDoubleOrNull() ?: 0.0,
                                availableBalance = acc.availableBalance?.toDoubleOrNull(),
                                balanceDateEpochSeconds = acc.balanceDate
                            )
                            for (tx in acc.transactions) {
                                val amountDouble = tx.amount.toDoubleOrNull() ?: 0.0
                                allTransactionsMap[tx.id] = Transaction(
                                    id = tx.id,
                                    accountId = acc.id,
                                    postedEpochSeconds = tx.posted,
                                    amount = amountDouble,
                                    originalDesc = tx.description.ifBlank { tx.payee.orEmpty().ifBlank { "Transaction" } },
                                    payee = tx.payee.orEmpty(),
                                    notes = tx.memo.orEmpty(),
                                    pending = tx.pending,
                                    category = if (amountDouble > 0) "Income" else "Uncategorized"
                                )
                            }
                        }
                    } else {
                        val errBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                        collectedErrors.add("Batch ${batchIndex + 1} HTTP $responseCode: $errBody")
                    }
                } catch (e: Exception) {
                    collectedErrors.add("Batch ${batchIndex + 1} network error: ${e.message}")
                }

                currentOffsetDays = startDayOffset
                if (currentOffsetDays >= safeDaysBack) break
            }

            if (allTransactionsMap.isNotEmpty()) {
                transactionRepository.saveTransactions(allTransactionsMap.values.toList())
            }
            if (allAccountsMap.isNotEmpty()) {
                accountRepository.saveAccounts(allAccountsMap.values.toList())
                val netWorth = allAccountsMap.values.sumOf { it.balance }
                netWorthRepository.recordIfNewWeek(netWorth)
            }

            prefs.edit()
                .putLong("last_sync", System.currentTimeMillis())
                .putString("error_list", collectedErrors.distinct().joinToString(";;"))
                .apply()

            loadConfig()

            Resource.Success(collectedErrors.distinct())
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to sync transactions from SimpleFIN", e)
        }
    }
}
