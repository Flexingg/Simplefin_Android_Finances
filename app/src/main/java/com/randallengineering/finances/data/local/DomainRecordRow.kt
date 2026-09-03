package com.randallengineering.finances.data.local

import androidx.room.Entity

/**
 * A generic JSON-backed record row used by the non-transaction repositories
 * (rules, budgets, goals, categories). [kind] partitions the single table so a
 * shared DAO serves every repository without a DAO per type.
 */
@Entity(tableName = "domain_records", primaryKeys = ["kind", "recordId"])
data class DomainRecordRow(
    val kind: String,
    val recordId: String,
    val json: String
) {
    companion object {
        const val KIND_RULE = "rule"
        const val KIND_BUDGET = "budget"
        const val KIND_GOAL = "goal"
        const val KIND_CATEGORY = "category"
    }
}
