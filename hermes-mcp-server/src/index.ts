#!/usr/bin/env node

import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
  Tool
} from '@modelcontextprotocol/sdk/types.js';
import { FinanceStorage } from './storage.js';
import { HermesFinanceTools } from './tools.js';
import { FirestoreBridge } from './firestore.js';

// Initialize storage and tools
const storage = new FinanceStorage();
const tools = new HermesFinanceTools(storage);

// Shared cross-platform sync via Firestore (enabled with FIREBASE_PROJECT_ID + FIREBASE_UID)
const firestoreBridge = new FirestoreBridge();
storage.setFirestoreBridge(firestoreBridge);
firestoreBridge.connect(storage);

// Define tool specifications
const TOOL_DEFINITIONS: Tool[] = [
  {
    name: 'get_financial_summary',
    description: 'Fetches real-time financial health summary (MTD income, expenses, net savings, savings rate, target daily allowance, days remaining, active budget threshold warnings, gamification streak/hearts/gems/level).',
    inputSchema: {
      type: 'object',
      properties: {}
    }
  },
  {
    name: 'sync_simplefin_accounts',
    description: 'Triggers live bank synchronization via SimpleFIN bridge protocol, pulling all accounts, balances, and recent transactions across the connected institutions.',
    inputSchema: {
      type: 'object',
      properties: {
        daysBack: {
          type: 'number',
          description: 'Number of past days of transactions to pull (default 90 days).'
        }
      }
    }
  },
  {
    name: 'list_transactions',
    description: 'Searches and retrieves transactions with multi-criteria filtering (date range, query text, category, subcategory, uncategorized only, pending only).',
    inputSchema: {
      type: 'object',
      properties: {
        query: { type: 'string', description: 'Search term for merchant, payee, description, or notes.' },
        category: { type: 'string', description: 'Filter by main category.' },
        subCategory: { type: 'string', description: 'Filter by subcategory.' },
        limit: { type: 'number', description: 'Maximum transactions to return (default 50).' },
        daysBack: { type: 'number', description: 'Limit search to past N days.' },
        pendingOnly: { type: 'boolean', description: 'Return only pending transactions.' },
        uncategorizedOnly: { type: 'boolean', description: 'Return only uncategorized transactions.' }
      }
    }
  },
  {
    name: 'categorize_transaction',
    description: 'Assigns main category, subcategory, and notes to a specific transaction. Optionally generates an Auto-Rule to automatically classify matching future/past transactions and awards XP.',
    inputSchema: {
      type: 'object',
      properties: {
        transactionId: { type: 'string', description: 'Unique ID of the transaction.' },
        mainCategory: { type: 'string', description: 'Main category name (e.g. Housing, Food & Dining, Transportation).' },
        subCategory: { type: 'string', description: 'Optional subcategory (e.g. Groceries, Fuel, Utilities).' },
        notes: { type: 'string', description: 'Optional annotation or itemized memo.' },
        createAutoRule: { type: 'boolean', description: 'If true, automatically creates an auto-rule from the merchant name and runs it across all transactions.' }
      },
      required: ['transactionId', 'mainCategory']
    }
  },
  {
    name: 'batch_categorize_transactions',
    description: 'Categorizes multiple transactions in a single batch operation.',
    inputSchema: {
      type: 'object',
      properties: {
        proposals: {
          type: 'array',
          items: {
            type: 'object',
            properties: {
              transactionId: { type: 'string' },
              mainCategory: { type: 'string' },
              subCategory: { type: 'string' },
              notes: { type: 'string' }
            },
            required: ['transactionId', 'mainCategory']
          },
          description: 'List of transaction categorization proposals.'
        }
      },
      required: ['proposals']
    }
  },
  {
    name: 'split_transaction',
    description: 'Splits a transaction into itemized sub-allocations across multiple categories (e.g. splitting a $100 Walmart receipt into $60 Groceries and $40 Household Supplies).',
    inputSchema: {
      type: 'object',
      properties: {
        transactionId: { type: 'string', description: 'Unique transaction ID.' },
        splits: {
          type: 'array',
          items: {
            type: 'object',
            properties: {
              category: { type: 'string' },
              subCategory: { type: 'string' },
              amount: { type: 'number' },
              notes: { type: 'string' }
            },
            required: ['category', 'amount']
          },
          description: 'Itemized splits whose amounts sum up to the total transaction amount.'
        }
      },
      required: ['transactionId', 'splits']
    }
  },
  {
    name: 'list_budgets',
    description: 'Retrieves all active category and subcategory monthly budgets with live MTD spent, envelope rollover unspent balances, percent used, and over-budget alerts.',
    inputSchema: {
      type: 'object',
      properties: {}
    }
  },
  {
    name: 'create_or_update_budget',
    description: 'Creates or edits a monthly category/subcategory budget with target dollar limit or percentage of MTD income, and enables/disables envelope rollover.',
    inputSchema: {
      type: 'object',
      properties: {
        id: { type: 'string', description: 'Optional budget ID when updating.' },
        category: { type: 'string', description: 'Main category name.' },
        subCategory: { type: 'string', description: 'Optional subcategory name.' },
        categoryType: { type: 'string', enum: ['FIXED', 'PERCENT_INCOME', 'VARIABLE'], description: 'Budget calculation method.' },
        targetAmount: { type: 'number', description: 'Target dollar amount per month (e.g. 500).' },
        incomePercentage: { type: 'number', description: 'Percentage of total MTD income (used when categoryType is PERCENT_INCOME).' },
        rolloverEnabled: { type: 'boolean', description: 'Whether unspent positive balance rolls over to next month.' }
      },
      required: ['category', 'categoryType', 'targetAmount']
    }
  },
  {
    name: 'reset_rollover_balance',
    description: 'Resets the accumulated envelope rollover buffer back to $0 for a given budget for the specified month.',
    inputSchema: {
      type: 'object',
      properties: {
        budgetId: { type: 'string', description: 'ID of the budget to reset.' },
        monthKey: { type: 'string', description: 'Month in YYYY-MM format (defaults to current month).' }
      },
      required: ['budgetId']
    }
  },
  {
    name: 'list_rules',
    description: 'Lists all automated merchant pattern matching rules in priority order.',
    inputSchema: {
      type: 'object',
      properties: {}
    }
  },
  {
    name: 'create_auto_rule',
    description: 'Creates a new pattern-matching Auto-Rule and immediately evaluates it against all transactions in the ledger.',
    inputSchema: {
      type: 'object',
      properties: {
        pattern: { type: 'string', description: 'Regex or text pattern to match merchant name / description (e.g. "Kroger", "Shell", "Netflix").' },
        category: { type: 'string', description: 'Target main category.' },
        subCategory: { type: 'string', description: 'Target subcategory.' },
        minAmount: { type: 'number', description: 'Optional minimum transaction amount.' },
        maxAmount: { type: 'number', description: 'Optional maximum transaction amount.' }
      },
      required: ['pattern', 'category']
    }
  },
  {
    name: 'run_all_rules',
    description: 'Runs all active Auto-Rules sequentially across every transaction in the database and updates their categories.',
    inputSchema: {
      type: 'object',
      properties: {}
    }
  },
  {
    name: 'get_spending_trends',
    description: 'Fetches multi-month historical spending trends, month-over-month comparisons, savings rate history, and category breakdowns.',
    inputSchema: {
      type: 'object',
      properties: {
        monthsBack: { type: 'number', description: 'Number of past months to analyze (default 6).' }
      }
    }
  },
  {
    name: 'get_spending_heatmap',
    description: 'Retrieves 90-day daily spending intensity levels for the visual calendar heatmap.',
    inputSchema: {
      type: 'object',
      properties: {
        days: { type: 'number', description: 'Number of past days (default 90).' }
      }
    }
  },
  {
    name: 'simulate_debt_payoff',
    description: 'Simulates and compares Snowball (lowest balance first) vs Avalanche (highest interest rate first) debt payoff strategies.',
    inputSchema: {
      type: 'object',
      properties: {
        debts: {
          type: 'array',
          items: {
            type: 'object',
            properties: {
              name: { type: 'string' },
              balance: { type: 'number' },
              interestRate: { type: 'number' },
              minimumPayment: { type: 'number' }
            },
            required: ['name', 'balance', 'interestRate', 'minimumPayment']
          }
        },
        monthlyExtraBudget: { type: 'number', description: 'Extra monthly amount dedicated to paying off debt principal (default $200).' }
      },
      required: ['debts']
    }
  },
  {
    name: 'claim_simplefin_token',
    description: 'Connects bank accounts by claiming a base64 encoded setup token from bridge.simplefin.org and performing an initial 90-day sync.',
    inputSchema: {
      type: 'object',
      properties: {
        tokenBase64: { type: 'string', description: 'Base64 setup token from SimpleFIN bridge.' }
      },
      required: ['tokenBase64']
    }
  }
];

// Create MCP Server instance
const server = new Server(
  {
    name: 'hermes-finance-mcp-server',
    version: '1.0.0'
  },
  {
    capabilities: {
      tools: {}
    }
  }
);

// Register tool listing handler
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: TOOL_DEFINITIONS
  };
});

// Register tool execution handler
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;
  const safeArgs: any = args || {};

  try {
    let result: any;
    switch (name) {
      case 'get_financial_summary':
        result = tools.getFinancialSummary();
        break;
      case 'sync_simplefin_accounts':
        result = await tools.syncSimpleFin(safeArgs);
        break;
      case 'list_transactions':
        result = tools.listTransactions(safeArgs);
        break;
      case 'categorize_transaction':
        result = tools.categorizeTransaction(safeArgs);
        break;
      case 'batch_categorize_transactions':
        result = tools.batchCategorize(safeArgs);
        break;
      case 'split_transaction':
        result = tools.splitTransaction(safeArgs);
        break;
      case 'list_budgets':
        result = tools.listBudgets();
        break;
      case 'create_or_update_budget':
        result = tools.createOrUpdateBudget(safeArgs);
        break;
      case 'reset_rollover_balance':
        result = tools.resetRollover(safeArgs);
        break;
      case 'list_rules':
        result = tools.listRules();
        break;
      case 'create_auto_rule':
        result = tools.createAutoRule(safeArgs);
        break;
      case 'run_all_rules':
        result = tools.runAllRules();
        break;
      case 'get_spending_trends':
        result = tools.getSpendingTrends(safeArgs);
        break;
      case 'get_spending_heatmap':
        result = tools.getSpendingHeatmap(safeArgs);
        break;
      case 'simulate_debt_payoff':
        result = tools.simulateDebtPayoff(safeArgs);
        break;
      case 'claim_simplefin_token':
        result = await tools.claimSimpleFinToken(safeArgs);
        break;
      default:
        throw new Error(`Unknown tool: ${name}`);
    }

    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify(result, null, 2)
        }
      ]
    };
  } catch (error: any) {
    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify({ success: false, error: error.message }, null, 2)
        }
      ],
      isError: true
    };
  }
});

// Start server
async function main() {
  // Await Firestore bootstrap so shared data is loaded before serving tools.
  await firestoreBridge.connect(storage);
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error('Hermes Financial Controller MCP Server running on stdio');
}

main().catch((err) => {
  console.error('Fatal error starting Hermes MCP Server:', err);
  process.exit(1);
});
