package com.randallengineering.finances.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.randallengineering.finances.domain.model.Rule

@IgnoreExtraProperties
data class RuleEntity(
    @DocumentId
    var id: String = "",
    var name: String = "",
    var priority: Int = 0,
    var pattern: String = "",
    var minAmount: Double? = null,
    var maxAmount: Double? = null,
    var category: String = "",
    var subCategory: String = "",
    var matchCount: Long = 0L,
    var isActive: Boolean = true
) {
    fun toDomain(): Rule = Rule(
        id = id,
        name = name,
        priority = priority,
        pattern = pattern,
        minAmount = minAmount,
        maxAmount = maxAmount,
        category = category,
        subCategory = subCategory,
        matchCount = matchCount,
        isActive = isActive
    )

    companion object {
        fun fromDomain(domain: Rule): RuleEntity = RuleEntity(
            id = domain.id,
            name = domain.name,
            priority = domain.priority,
            pattern = domain.pattern,
            minAmount = domain.minAmount,
            maxAmount = domain.maxAmount,
            category = domain.category,
            subCategory = domain.subCategory,
            matchCount = domain.matchCount,
            isActive = domain.isActive
        )
    }
}
