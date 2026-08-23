package com.randallengineering.finances.core.util

data class SuggestedCategory(
    val category: String,
    val subCategory: String
)

object AmazonCategorySuggester {

    fun suggestCategory(title: String): SuggestedCategory {
        val lower = title.lowercase()

        return when {
            // Kids & Toys
            lower.contains("toy") || lower.contains("die cast") || lower.contains("matchbox") || lower.contains("hot wheels") || lower.contains("lego") || lower.contains("doll") || lower.contains("game") -> {
                SuggestedCategory("Kids & Family", "Toys & Games")
            }
            // Baby & Clothing
            lower.contains("baby") || lower.contains("girls") || lower.contains("boys") || lower.contains("toddler") || lower.contains("infant") || lower.contains("pajama") || lower.contains("children's place") || lower.contains("onesie") || lower.contains("diaper") -> {
                SuggestedCategory("Kids & Family", "Baby & Clothing")
            }
            // Office & School Supplies
            lower.contains("whiteboard") || lower.contains("eraser") || lower.contains("marker") || lower.contains("chalk") || lower.contains("pen") || lower.contains("pencil") || lower.contains("notebook") || lower.contains("paper") || lower.contains("stapler") -> {
                SuggestedCategory("Office", "Supplies & Stationery")
            }
            // Health & Medical / Personal Care
            lower.contains("ph balance") || lower.contains("test strip") || lower.contains("supplement") || lower.contains("vitamin") || lower.contains("medicine") || lower.contains("first aid") || lower.contains("bandage") -> {
                SuggestedCategory("Health & Medical", "Personal Care & Wellness")
            }
            // Beauty & Haircare
            lower.contains("curls") || lower.contains("styling cream") || lower.contains("shampoo") || lower.contains("conditioner") || lower.contains("sun bum") || lower.contains("lotion") || lower.contains("sunscreen") || lower.contains("skincare") || lower.contains("soap") -> {
                SuggestedCategory("Personal Care", "Beauty & Haircare")
            }
            // Automotive & Maintenance
            lower.contains("gas can") || lower.contains("car ") || lower.contains("auto") || lower.contains("tire") || lower.contains("oil") || lower.contains("wiper") || lower.contains("scepter") || lower.contains("fuel") -> {
                SuggestedCategory("Automotive", "Supplies & Maintenance")
            }
            // Electronics & Tech
            lower.contains("cable") || lower.contains("usb") || lower.contains("charger") || lower.contains("battery") || lower.contains("power bank") || lower.contains("adapter") || lower.contains("headphone") || lower.contains("phone") -> {
                SuggestedCategory("Electronics", "Accessories")
            }
            // Home & Kitchen
            lower.contains("kitchen") || lower.contains("foodi") || lower.contains("air fryer") || lower.contains("cookware") || lower.contains("pan") || lower.contains("pot") || lower.contains("towel") || lower.contains("blanket") || lower.contains("pillow") -> {
                SuggestedCategory("Home & Living", "Kitchen & Housewares")
            }
            // Groceries & Food
            lower.contains("organic") || lower.contains("olive oil") || lower.contains("coffee") || lower.contains("tea") || lower.contains("snack") || lower.contains("protein") || lower.contains("food") -> {
                SuggestedCategory("Groceries", "Pantry")
            }
            else -> {
                SuggestedCategory("Shopping", "Amazon Retail")
            }
        }
    }
}
