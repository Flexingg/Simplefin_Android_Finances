package com.randallengineering.finances.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.randallengineering.finances.core.network.Resource
import com.randallengineering.finances.core.util.AmazonCsvParser
import com.randallengineering.finances.domain.model.AmazonOrderItem
import com.randallengineering.finances.domain.model.MatchedAmazonOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

@Serializable
data class AmazonTokenResponse(
    val access_token: String? = null,
    val refresh_token: String? = null,
    val token_type: String? = null,
    val expires_in: Int? = null,
    val error: String? = null,
    val error_description: String? = null
)

@Serializable
data class AmazonUserProfile(
    val user_id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val postal_code: String? = null
)

@Serializable
data class AmazonRawDebugInfo(
    val connected: Boolean,
    val clientId: String,
    val userId: String?,
    val userName: String?,
    val userEmail: String?,
    val tokenType: String?,
    val connectedAt: String?,
    val hasAccessToken: Boolean,
    val hasRefreshToken: Boolean,
    val cachedCsvOrdersCount: Int
)

class AmazonRepository(
    private val context: Context,
    private val functions: FirebaseFunctions? = null,
    private val firestore: FirebaseFirestore? = null
) {
    private val prefs = context.getSharedPreferences("randall_amazon_config", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; prettyPrint = true }

    private val _isConnectedFlow = MutableStateFlow(prefs.getBoolean("is_connected", false))
    private val _clientIdFlow = MutableStateFlow(prefs.getString("client_id", "") ?: "")
    private val _clientSecretFlow = MutableStateFlow(prefs.getString("client_secret", "") ?: "")
    private val _userEmailFlow = MutableStateFlow(prefs.getString("user_email", "") ?: "")
    private val _userNameFlow = MutableStateFlow(prefs.getString("user_name", "") ?: "")
    private val _redirectUriFlow = MutableStateFlow(prefs.getString("redirect_uri", "https://randall-finances.web.app/amazonOAuthCallback") ?: "https://randall-finances.web.app/amazonOAuthCallback")
    private val _importedOrdersCountFlow = MutableStateFlow(0)
    private val cachedImportedOrders = mutableListOf<MatchedAmazonOrder>()

    init {
        loadStatus()
        loadImportedOrders()
    }

    private fun loadStatus() {
        val isConn = prefs.getBoolean("is_connected", false)
        val cId = prefs.getString("client_id", "") ?: ""
        val cSec = prefs.getString("client_secret", "") ?: ""
        val email = prefs.getString("user_email", "") ?: ""
        val name = prefs.getString("user_name", "") ?: ""
        val rUri = prefs.getString("redirect_uri", "https://randall-finances.web.app/amazonOAuthCallback") ?: "https://randall-finances.web.app/amazonOAuthCallback"
        _isConnectedFlow.value = isConn
        _clientIdFlow.value = cId
        _clientSecretFlow.value = cSec
        _userEmailFlow.value = email
        _userNameFlow.value = name
        _redirectUriFlow.value = rUri
    }

    private fun loadImportedOrders() {
        val rawJson = prefs.getString("imported_orders_json", null)
        if (!rawJson.isNullOrBlank()) {
            try {
                val list = json.decodeFromString<List<MatchedAmazonOrder>>(rawJson)
                cachedImportedOrders.clear()
                cachedImportedOrders.addAll(list)
                _importedOrdersCountFlow.value = list.size
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun isConnectedFlow(): Flow<Boolean> = _isConnectedFlow.asStateFlow()
    fun isConnected(): Boolean = _isConnectedFlow.value
    fun getClientIdFlow(): Flow<String> = _clientIdFlow.asStateFlow()
    fun getClientSecretFlow(): Flow<String> = _clientSecretFlow.asStateFlow()
    fun getUserEmailFlow(): Flow<String> = _userEmailFlow.asStateFlow()
    fun getUserNameFlow(): Flow<String> = _userNameFlow.asStateFlow()
    fun getRedirectUriFlow(): Flow<String> = _redirectUriFlow.asStateFlow()
    fun getImportedOrdersCountFlow(): Flow<Int> = _importedOrdersCountFlow.asStateFlow()

    fun getClientId(): String = prefs.getString("client_id", "") ?: ""
    fun getClientSecret(): String = prefs.getString("client_secret", "") ?: ""
    fun getUserEmail(): String = prefs.getString("user_email", "") ?: ""
    fun getUserName(): String = prefs.getString("user_name", "") ?: ""
    fun getRedirectUri(): String = prefs.getString("redirect_uri", "https://randall-finances.web.app/amazonOAuthCallback") ?: "https://randall-finances.web.app/amazonOAuthCallback"

    fun saveCredentials(clientId: String, clientSecret: String = "", redirectUri: String = "") {
        val editor = prefs.edit()
            .putString("client_id", clientId.trim())
            .putString("client_secret", clientSecret.trim())
        if (redirectUri.isNotBlank()) {
            editor.putString("redirect_uri", redirectUri.trim())
            _redirectUriFlow.value = redirectUri.trim()
        }
        editor.apply()
        _clientIdFlow.value = clientId.trim()
        _clientSecretFlow.value = clientSecret.trim()
    }

    fun setConnected(connected: Boolean) {
        val editor = prefs.edit().putBoolean("is_connected", connected)
        if (!connected) {
            editor.remove("refresh_token")
                .remove("access_token")
                .remove("user_email")
                .remove("user_name")
                .remove("user_id")
            _userEmailFlow.value = ""
            _userNameFlow.value = ""
        }
        editor.apply()
        _isConnectedFlow.value = connected
    }

    /**
     * Returns formatted Raw JSON of what Amazon LWA returned.
     */
    fun getRawApiData(): String {
        val connectedAtEpoch = prefs.getLong("connected_at", 0L)
        val connectedAtStr = if (connectedAtEpoch > 0) {
            Instant.ofEpochMilli(connectedAtEpoch).atZone(ZoneId.systemDefault()).toString()
        } else "Never"

        val rawInfo = AmazonRawDebugInfo(
            connected = _isConnectedFlow.value,
            clientId = getClientId(),
            userId = prefs.getString("user_id", null),
            userName = prefs.getString("user_name", null),
            userEmail = prefs.getString("user_email", null),
            tokenType = prefs.getString("token_type", "bearer"),
            connectedAt = connectedAtStr,
            hasAccessToken = prefs.getString("access_token", null).isNullOrBlank().not(),
            hasRefreshToken = prefs.getString("refresh_token", null).isNullOrBlank().not(),
            cachedCsvOrdersCount = cachedImportedOrders.size
        )
        return json.encodeToString(rawInfo)
    }

    /**
     * Directly exchanges the Amazon OAuth authorization code for an Amazon Refresh Token and fetches the User Profile.
     */
    suspend fun exchangeOAuthCode(authCode: String): Resource<String> = withContext(Dispatchers.IO) {
        val clientId = getClientId()
        val clientSecret = getClientSecret()
        val redirectUri = getRedirectUri()

        if (clientId.isBlank() || clientSecret.isBlank()) {
            return@withContext Resource.Error("Missing Amazon Client ID or Client Secret in Settings.")
        }

        try {
            val url = URL("https://api.amazon.com/auth/o2/token")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20000
                readTimeout = 25000
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }

            val params = "grant_type=authorization_code" +
                    "&code=" + URLEncoder.encode(authCode.trim(), "UTF-8") +
                    "&client_id=" + URLEncoder.encode(clientId.trim(), "UTF-8") +
                    "&client_secret=" + URLEncoder.encode(clientSecret.trim(), "UTF-8") +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri.trim(), "UTF-8")

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(params)
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                val tokenData = json.decodeFromString<AmazonTokenResponse>(responseBody)

                val refreshToken = tokenData.refresh_token
                val accessToken = tokenData.access_token

                var userEmail = ""
                var userName = ""
                var userId = ""

                // Fetch real User Profile from Amazon
                if (!accessToken.isNullOrBlank()) {
                    try {
                        val profileUrl = URL("https://api.amazon.com/user/profile")
                        val profileConn = (profileUrl.openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 10000
                            readTimeout = 10000
                            setRequestProperty("Authorization", "Bearer $accessToken")
                            setRequestProperty("Accept", "application/json")
                        }
                        if (profileConn.responseCode in 200..299) {
                            val profileJson = profileConn.inputStream.bufferedReader().use { it.readText() }
                            val profile = json.decodeFromString<AmazonUserProfile>(profileJson)
                            userEmail = profile.email ?: ""
                            userName = profile.name ?: ""
                            userId = profile.user_id ?: ""
                        }
                    } catch (e: Exception) {
                        // Ignore profile fetch failure
                    }
                }

                val editor = prefs.edit()
                    .putBoolean("is_connected", true)
                    .putLong("connected_at", System.currentTimeMillis())
                if (!refreshToken.isNullOrBlank()) {
                    editor.putString("refresh_token", refreshToken)
                }
                if (!accessToken.isNullOrBlank()) {
                    editor.putString("access_token", accessToken)
                }
                if (userEmail.isNotBlank()) {
                    editor.putString("user_email", userEmail)
                    _userEmailFlow.value = userEmail
                }
                if (userName.isNotBlank()) {
                    editor.putString("user_name", userName)
                    _userNameFlow.value = userName
                }
                if (userId.isNotBlank()) {
                    editor.putString("user_id", userId)
                }
                editor.apply()

                _isConnectedFlow.value = true
                val msg = if (userEmail.isNotBlank()) "Linked Amazon: $userEmail" else "Amazon Account Linked Successfully!"
                Resource.Success(msg)
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Resource.Error("Amazon Token Exchange Failed ($responseCode): $errorBody")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to exchange token with Amazon", e)
        }
    }

    suspend fun importOrdersFromCsv(uri: Uri): Resource<Int> = withContext(Dispatchers.IO) {
        try {
            val parsedOrders = AmazonCsvParser.parseAmazonOrdersCsv(context, uri)
            if (parsedOrders.isEmpty()) {
                return@withContext Resource.Error("No valid Amazon orders found in the selected CSV file.")
            }

            cachedImportedOrders.addAll(parsedOrders)
            val distinctOrders = cachedImportedOrders.distinctBy { it.orderId }

            prefs.edit()
                .putString("imported_orders_json", json.encodeToString(distinctOrders))
                .apply()

            cachedImportedOrders.clear()
            cachedImportedOrders.addAll(distinctOrders)
            _importedOrdersCountFlow.value = distinctOrders.size

            Resource.Success(parsedOrders.size)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to import Amazon CSV", e)
        }
    }

    suspend fun importExtractedOrders(newOrders: List<MatchedAmazonOrder>): Resource<Int> = withContext(Dispatchers.IO) {
        if (newOrders.isEmpty()) {
            return@withContext Resource.Error("No Amazon orders found on this page.")
        }
        try {
            cachedImportedOrders.addAll(newOrders)
            val distinctOrders = cachedImportedOrders.distinctBy { it.orderId }

            prefs.edit()
                .putString("imported_orders_json", json.encodeToString(distinctOrders))
                .apply()

            cachedImportedOrders.clear()
            cachedImportedOrders.addAll(distinctOrders)
            _importedOrdersCountFlow.value = distinctOrders.size

            Resource.Success(newOrders.size)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save extracted orders", e)
        }
    }

    suspend fun parseScreenshotWithAi(bitmap: android.graphics.Bitmap): Resource<Int> = withContext(Dispatchers.IO) {
        try {
            val outputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
            val base64Image = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)

            if (functions != null) {
                val payload = mapOf("imageBase64" to base64Image)
                val result = functions.getHttpsCallable("parseAmazonOrderScreenshot").call(payload).await()
                val data = result.data as? Map<*, *>
                val rawOrders = data?.get("orders") as? List<Map<*, *>> ?: emptyList()

                if (rawOrders.isNotEmpty()) {
                    val parsed = rawOrders.map { orderMap ->
                        val orderId = orderMap["orderId"] as? String ?: "114-UNKNOWN"
                        val purchaseDate = orderMap["purchaseDate"] as? String ?: ""
                        val orderTotal = (orderMap["orderTotal"] as? Number)?.toDouble() ?: 0.0
                        val orderStatus = orderMap["orderStatus"] as? String ?: "Delivered"
                        val rawItems = orderMap["items"] as? List<Map<*, *>> ?: emptyList()
                        val items = rawItems.map { item ->
                            AmazonOrderItem(
                                title = item["title"] as? String ?: "Amazon Item",
                                asin = item["asin"] as? String ?: "",
                                quantityOrdered = (item["quantityOrdered"] as? Number)?.toInt() ?: 1,
                                itemPrice = (item["itemPrice"] as? Number)?.toDouble() ?: 0.0,
                                itemTax = (item["itemTax"] as? Number)?.toDouble() ?: 0.0,
                                totalPrice = (item["totalPrice"] as? Number)?.toDouble() ?: 0.0
                            )
                        }
                        MatchedAmazonOrder(orderId, purchaseDate, orderTotal, orderStatus, items)
                    }
                    return@withContext importExtractedOrders(parsed)
                }
            }
            Resource.Error("AI could not detect any Amazon orders in this image.")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "AI Vision parsing failed", e)
        }
    }

    fun clearImportedOrders() {
        prefs.edit().remove("imported_orders_json").apply()
        cachedImportedOrders.clear()
        _importedOrdersCountFlow.value = 0
    }

    suspend fun getAmazonOAuthUrl(): Resource<String> = withContext(Dispatchers.IO) {
        val customClientId = getClientId()
        val redirectUri = getRedirectUri()

        if (customClientId.isBlank()) {
            return@withContext Resource.Error("Please enter your Amazon LWA Client ID first.")
        }

        val encodedClientId = URLEncoder.encode(customClientId, "UTF-8")
        val encodedRedirectUri = URLEncoder.encode(redirectUri, "UTF-8")
        // prompt=consent ensures Amazon always displays the sign-in / authorization dialog
        val authUrl = "https://www.amazon.com/ap/oa?client_id=$encodedClientId&scope=profile&response_type=code&redirect_uri=$encodedRedirectUri&state=default_user&prompt=consent"
        Resource.Success(authUrl)
    }

    /**
     * Look up real order details for a bank transaction.
     * Note: NO fake/simulated items are generated. If no real match is found, returns Error so the UI shows the search link.
     */
    suspend fun fetchOrderDetailsForTransaction(
        transactionDateEpoch: Long,
        amount: Double
    ): Resource<MatchedAmazonOrder> = withContext(Dispatchers.IO) {
        val targetAmount = abs(amount)

        // 1. Check imported real Amazon orders first (from CSV Order Report)
        if (cachedImportedOrders.isNotEmpty()) {
            val matched = cachedImportedOrders.find { order ->
                abs(order.orderTotal - targetAmount) < 0.20
            }
            if (matched != null) {
                return@withContext Resource.Success(matched)
            }
        }

        // 2. Try Firebase Cloud Function if SP-API credentials are configured
        try {
            if (functions != null && getClientId().isNotBlank()) {
                val payload = mapOf(
                    "transactionDateEpoch" to transactionDateEpoch,
                    "amount" to targetAmount
                )
                val result = functions.getHttpsCallable("getAmazonOrderDetailsForTransaction").call(payload).await()
                val data = result.data as? Map<*, *>
                val isMatched = data?.get("matched") as? Boolean ?: false

                if (isMatched) {
                    val orderMap = data?.get("order") as? Map<*, *>
                    if (orderMap != null) {
                        val orderId = orderMap["orderId"] as? String ?: "114-UNKNOWN"
                        val purchaseDate = orderMap["purchaseDate"] as? String ?: ""
                        val orderTotal = (orderMap["orderTotal"] as? Number)?.toDouble() ?: targetAmount
                        val orderStatus = orderMap["orderStatus"] as? String ?: "Delivered"
                        val rawItems = orderMap["items"] as? List<Map<*, *>> ?: emptyList()

                        val items = rawItems.map { item ->
                            val title = item["title"] as? String ?: "Amazon Item"
                            val asin = item["asin"] as? String ?: ""
                            val qty = (item["quantityOrdered"] as? Number)?.toInt() ?: 1
                            val price = (item["itemPrice"] as? Number)?.toDouble() ?: 0.0
                            val tax = (item["itemTax"] as? Number)?.toDouble() ?: 0.0
                            val total = (item["totalPrice"] as? Number)?.toDouble() ?: (price + tax)
                            AmazonOrderItem(
                                title = title,
                                asin = asin,
                                quantityOrdered = qty,
                                itemPrice = price,
                                itemTax = tax,
                                totalPrice = total
                            )
                        }

                        return@withContext Resource.Success(
                            MatchedAmazonOrder(
                                orderId = orderId,
                                purchaseDate = purchaseDate,
                                orderTotal = orderTotal,
                                orderStatus = orderStatus,
                                items = items
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore API failure
        }

        // 3. No match found - Return clean Error so the UI displays the direct search link
        Resource.Error("No matching Amazon order found for $$targetAmount in your order history.")
    }
}
