package com.randallengineering.finances.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A transaction row in the local Room DB. The domain object is stored as JSON in
 * [json] (id + postedEpochSeconds are indexed columns for ordering/lookup). This
 * replaces the old single-blob SharedPreferences JSON cache, which grew without
 * bound and re-decoded everything on every launch.
 */
@Entity(tableName = "transactions")
data class TransactionRow(
    @PrimaryKey val id: String,
    val postedEpochSeconds: Long,
    val json: String
)
