package com.randallengineering.finances.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Transactions : Screen("transactions")
    data object TransactionDetail : Screen("transaction_detail/{transactionId}") {
        fun createRoute(transactionId: String) = "transaction_detail/$transactionId"
    }
    data object Rules : Screen("rules")
    data object Budgets : Screen("budgets")
    data object Insights : Screen("insights")
    data object Goals : Screen("goals")
    data object AiAdvisor : Screen("ai_advisor")
    data object Settings : Screen("settings")
}
