package com.randallengineering.finances.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CategoryHierarchy(
    val mainCategory: String,
    val subCategories: List<String> = emptyList(),
    val iconName: String = "Folder"
)

object DefaultCategories {
    // App ships clean with 0 default categories so the user creates and curates their own
    val list: List<CategoryHierarchy> = emptyList()
}
