# 🏛️ Hermes Agent Financial Controller MCP Server

This MCP (Model Context Protocol) Server equips your **Hermes Agent** (or any autonomous LLM agent) with programmatic control over **Randall Finances**.

---

## ⚡ Features Controlled by Hermes
- **🏦 SimpleFIN Bank Sync**: Trigger on-demand syncs, pull transactions, and inspect account balances.
- **🏷️ Intelligent Transaction Categorization**: Classify expenses, update subcategories, and split multi-item receipts.
- **⚡ Automated Rule Engine**: Create regex-based merchant rules and run the rule pipeline across all transactions.
- **📊 Envelope / Rollover Budgeting**: Adjust category budgets, set income percentage targets, and reset monthly rollovers.
- **📈 Projections & Insights**: Calculate net worth, analyze 6-month trends, generate daily heatmap matrices, and simulate Snowball vs Avalanche debt payoffs.
- **🎮 Gamification Progression**: Check streaks, refill hearts, and monitor XP/Gems.

---

## 🚀 Quick Start

### 1. Install Dependencies & Build
```bash
cd hermes-mcp-server
npm install
npm run build
```

### 2. Configure in Hermes Agent Configuration
In your Hermes agent configuration (e.g. `hermes.json`, Claude desktop config, or agent tool manifest):

```json
{
  "mcpServers": {
    "randall-finances": {
      "command": "node",
      "args": ["c:/RandallEngineering/Randall_Finances/hermes-mcp-server/dist/index.js"]
    }
  }
}
```

---

## 🛠️ Available MCP Tools

| Tool Name | Description |
|---|---|
| `get_financial_summary` | Returns MTD income, expenses, savings rate, target daily allowance, budget alert thresholds, and gamification level. |
| `sync_simplefin_accounts` | Pulls live bank transactions and balances across all connected bank accounts. |
| `list_transactions` | Search transactions by keyword, category, subcategory, date, or pending state. |
| `categorize_transaction` | Assigns category and notes to a transaction, with optional auto-rule generation. |
| `batch_categorize_transactions` | Bulk categorizes multiple transactions simultaneously. |
| `split_transaction` | Splits a single transaction into multiple categorized sub-allocations. |
| `list_budgets` | Lists all monthly budgets, live spent amount, envelope rollover buffers, and percent used. |
| `create_or_update_budget` | Sets fixed dollar or % income limits with rollover support. |
| `reset_rollover_balance` | Resets the unspent rollover balance to $0 for a given month. |
| `list_rules` | Returns all auto-rules in priority order. |
| `create_auto_rule` | Creates a new merchant matching rule and runs it across historical transactions. |
| `run_all_rules` | Re-evaluates all active rules across the entire transaction ledger. |
| `get_spending_trends` | Multi-month historical spending and category breakdowns. |
| `get_spending_heatmap` | 90-day daily spending intensity levels for calendar heatmap visualization. |
| `simulate_debt_payoff` | Simulates and compares Snowball vs. Avalanche debt payoff timelines and interest savings. |
| `claim_simplefin_token` | Claims a setup token from bridge.simplefin.org to connect bank accounts. |
