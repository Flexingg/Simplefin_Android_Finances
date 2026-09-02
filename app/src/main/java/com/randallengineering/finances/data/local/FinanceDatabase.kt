package com.randallengineering.finances.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TransactionRow::class],
    version = 1,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}
