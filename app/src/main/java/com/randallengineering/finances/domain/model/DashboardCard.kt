package com.randallengineering.finances.domain.model

/**
 * The set of dashboard cards the user can show, hide, and reorder. Each card is
 * backed by real finance data (no demo values). Default order is the enum order.
 */
enum class DashboardCardType(val title: String, val subtitle: String) {
    TOTAL_BALANCE("Total Balance", "Across all transactions"),
    MONTH_INCOME("Income This Month", "Real income this month"),
    MONTH_EXPENSE("Spending This Month", "Real spending this month"),
    MONTH_NET("Net This Month", "Income minus spending"),
    SAVINGS_RATE("Savings Rate", "Share of income kept"),
    DAILY_ALLOWANCE("Daily Allowance", "Remaining variable budget per day"),
    BUDGET_ALERTS("Budget Alerts", "Over or near limit"),
    TOP_CATEGORIES("Top Categories", "Where the money went"),
    RECENT_TRANSACTIONS("Recent Activity", "Latest transactions"),
    NEEDS_REVIEW("Needs Review", "Uncategorized transactions"),
    QUICK_ACTIONS("Quick Actions", "Shortcuts"),
    ACCOUNTS("Accounts", "Per-account live balances")
}
