package com.randallengineering.finances.core.util

import com.randallengineering.finances.domain.model.AiSnapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AiSnapshotFormatter {

    private val jsonConfig = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun toJson(snapshot: AiSnapshot): String {
        return jsonConfig.encodeToString(snapshot)
    }

    fun toMarkdown(snapshot: AiSnapshot): String {
        val sb = StringBuilder()
        sb.appendLine("# 📊 Financial Health & Context Snapshot")
        sb.appendLine("*Generated at:* ${snapshot.generatedTimestamp}\n")

        sb.appendLine("## 💰 Active Balances & Net Worth")
        sb.appendLine("- **Total Cash Balance:** ${CurrencyFormatter.format(snapshot.totalCashBalance)}")
        if (snapshot.accounts.isNotEmpty()) {
            sb.appendLine("| Account | Institution | Balance |")
            sb.appendLine("| :--- | :--- | :--- |")
            for (acc in snapshot.accounts) {
                sb.appendLine("| ${acc.name} | ${acc.orgName.ifEmpty { "Bank" }} | ${CurrencyFormatter.format(acc.balance)} |")
            }
        }
        sb.appendLine()

        sb.appendLine("## 📈 Month-To-Date (MTD) Cash Flow")
        sb.appendLine("- **Total MTD Income:** ${CurrencyFormatter.format(snapshot.mtdTotalIncome)}")
        sb.appendLine("- **Total MTD Spend:** ${CurrencyFormatter.format(snapshot.mtdTotalExpense)}")
        sb.appendLine("- **MTD Net Savings:** ${CurrencyFormatter.format(snapshot.mtdNetSavings)}")
        sb.appendLine("- **Target Daily Allowance (Remaining Days):** ${CurrencyFormatter.format(snapshot.targetDailyAllowance)}/day\n")

        sb.appendLine("## 🏷️ Category Spending & Pacing")
        sb.appendLine("| Category | Spent | Budget | Pacing % | Status |")
        sb.appendLine("| :--- | :--- | :--- | :--- | :--- |")
        for (cat in snapshot.categorySpends) {
            val status = if (cat.isAnomaly) "⚠️ >120% OVERPACING" else if (cat.spentAmount > cat.budgetAmount && cat.budgetAmount > 0) "🔴 Over Budget" else "🟢 On Track"
            sb.appendLine("| ${cat.category} | ${CurrencyFormatter.format(cat.spentAmount)} | ${CurrencyFormatter.format(cat.budgetAmount)} | ${CurrencyFormatter.formatPacing(cat.pacingPercent)} | $status |")
        }
        sb.appendLine()

        sb.appendLine("## 🎯 Active Goals & Wants")
        if (snapshot.activeGoals.isEmpty()) {
            sb.appendLine("No active goals registered.")
        } else {
            sb.appendLine("| Goal | Current | Target | Progress | Target Date | Req. Daily | Req. Monthly |")
            sb.appendLine("| :--- | :--- | :--- | :--- | :--- | :--- | :--- |")
            for (goal in snapshot.activeGoals) {
                sb.appendLine("| ${goal.title} | ${CurrencyFormatter.format(goal.currentAmount)} | ${CurrencyFormatter.format(goal.targetAmount)} | ${String.format("%.1f%%", goal.progressPercent)} | ${goal.targetDate} | ${CurrencyFormatter.format(goal.requiredDailySaving)}/day | ${CurrencyFormatter.format(goal.requiredMonthlySaving)}/mo |")
            }
        }
        sb.appendLine()

        sb.appendLine("## 🧾 Recent Transactions (Last 30 Days)")
        if (snapshot.recentTransactions.isEmpty()) {
            sb.appendLine("No recent transactions.")
        } else {
            sb.appendLine("| Date | Description | Amount | Category |")
            sb.appendLine("| :--- | :--- | :--- | :--- |")
            for (tx in snapshot.recentTransactions.take(30)) {
                sb.appendLine("| ${tx.date} | ${tx.description} | ${CurrencyFormatter.formatWithSign(tx.amount)} | ${tx.category} |")
            }
        }

        return sb.toString()
    }
}
