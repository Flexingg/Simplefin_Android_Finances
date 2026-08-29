package com.randallengineering.finances.core.auth

/**
 * Lightweight holder for the current signed-in Firebase uid, so repositories can
 * scope their Firestore paths to `users/{uid}/{collection}` without constructor
 * churn. Falls back to the flat collection when signed out (placeholder config).
 */
object SyncScope {
    @Volatile
    var uid: String? = null

    fun path(collection: String): String =
        uid?.let { "users/$it/$collection" } ?: collection
}
