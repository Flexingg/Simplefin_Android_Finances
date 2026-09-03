package com.randallengineering.finances.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionRow::class, DomainRecordRow::class],
    version = 2,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun genericRecordDao(): GenericRecordDao

    companion object {
        /** v1 -> v2: add the generic JSON record store (rules/budgets/goals/categories). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `domain_records` (" +
                        "`kind` TEXT NOT NULL, " +
                        "`recordId` TEXT NOT NULL, " +
                        "`json` TEXT NOT NULL, " +
                        "PRIMARY KEY(`kind`, `recordId`))"
                )
            }
        }
    }
}
