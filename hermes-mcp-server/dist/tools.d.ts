import { FinanceStorage } from './storage.js';
import { Transaction, Budget, Rule } from './types.js';
export declare class HermesFinanceTools {
    private storage;
    constructor(storage: FinanceStorage);
    syncSimpleFin(args: {
        daysBack?: number;
    }): Promise<{
        success: boolean;
        message: string;
        accountsSummary?: undefined;
        totalTransactionsRecorded?: undefined;
    } | {
        success: boolean;
        message: string;
        accountsSummary: string[];
        totalTransactionsRecorded: number;
    }>;
    getFinancialSummary(): {
        success: boolean;
        summary: {
            mtdIncome: number;
            mtdExpenses: number;
            netSavings: number;
            savingsRate: string;
            targetDailyAllowance: number;
            dailySpendActualAverage: number;
            daysRemainingInMonth: number;
            streakDays: number;
            xp: number;
            level: string;
            hearts: string;
            gems: number;
            budgetAlerts: {
                category: string;
                spent: number;
                limit: number;
                percentUsed: string;
                isOver: boolean;
            }[];
        };
    };
    listTransactions(args: {
        query?: string;
        category?: string;
        subCategory?: string;
        limit?: number;
        daysBack?: number;
        pendingOnly?: boolean;
        uncategorizedOnly?: boolean;
    }): {
        success: boolean;
        count: number;
        totalMatching: number;
        transactions: {
            id: string;
            date: string;
            amount: number;
            description: string;
            payee: string;
            category: string;
            subCategory: string;
            notes: string;
            isSplit: boolean;
            pending: boolean;
        }[];
    };
    categorizeTransaction(args: {
        transactionId: string;
        mainCategory: string;
        subCategory?: string;
        notes?: string;
        createAutoRule?: boolean;
    }): {
        success: boolean;
        message: string;
        transaction?: undefined;
    } | {
        success: boolean;
        message: string;
        transaction: Transaction;
    };
    batchCategorize(args: {
        proposals: Array<{
            transactionId: string;
            mainCategory: string;
            subCategory?: string;
            notes?: string;
        }>;
    }): {
        success: boolean;
        message: string;
    };
    splitTransaction(args: {
        transactionId: string;
        splits: Array<{
            category: string;
            subCategory?: string;
            amount: number;
            notes?: string;
        }>;
    }): {
        success: boolean;
        message: string;
        transaction?: undefined;
    } | {
        success: boolean;
        message: string;
        transaction: Transaction;
    };
    listBudgets(): {
        success: boolean;
        budgets: {
            id: string;
            category: string;
            subCategory: string;
            categoryType: import("./types.js").BudgetCategoryType;
            targetAmount: number;
            baseTargetAmount: number;
            incomePercentage: number | null | undefined;
            rolloverEnabled: boolean;
            rolloverAmount: number;
            spentAmount: number;
            remainingAmount: number;
            percentUsed: string;
            isOverBudget: boolean;
        }[];
    };
    createOrUpdateBudget(args: {
        id?: string;
        category: string;
        subCategory?: string;
        categoryType: 'FIXED' | 'PERCENT_INCOME' | 'VARIABLE';
        targetAmount: number;
        incomePercentage?: number;
        rolloverEnabled?: boolean;
    }): {
        success: boolean;
        message: string;
        budget: Budget;
    };
    resetRollover(args: {
        budgetId: string;
        monthKey?: string;
    }): {
        success: boolean;
        message: string;
    };
    listRules(): {
        success: boolean;
        rules: {
            id: string;
            name: string;
            pattern: string;
            category: string;
            subCategory: string;
            priority: number;
            isActive: boolean;
        }[];
    };
    createAutoRule(args: {
        pattern: string;
        category: string;
        subCategory?: string;
        minAmount?: number;
        maxAmount?: number;
    }): {
        success: boolean;
        message: string;
        rule: Rule;
    };
    runAllRules(): {
        success: boolean;
        message: string;
    };
    getSpendingTrends(args: {
        monthsBack?: number;
    }): {
        success: boolean;
        trends: import("./engine.js").MonthTrend[];
    };
    getSpendingHeatmap(args: {
        days?: number;
    }): {
        success: boolean;
        heatmap: Record<string, {
            totalSpent: number;
            count: number;
            level: number;
        }>;
    };
    simulateDebtPayoff(args: {
        debts: Array<{
            name: string;
            balance: number;
            interestRate: number;
            minimumPayment: number;
        }>;
        monthlyExtraBudget?: number;
    }): {
        success: boolean;
        simulation: {
            snowball: {
                monthsToDebtFree: number;
                yearsToDebtFree: string;
                totalInterestPaid: string;
            };
            avalanche: {
                monthsToDebtFree: number;
                yearsToDebtFree: string;
                totalInterestPaid: string;
            };
            recommendation: string;
        };
    };
    claimSimpleFinToken(args: {
        tokenBase64: string;
    }): Promise<{
        success: boolean;
        message: string;
        initialSync: {
            success: boolean;
            message: string;
            accountsSummary?: undefined;
            totalTransactionsRecorded?: undefined;
        } | {
            success: boolean;
            message: string;
            accountsSummary: string[];
            totalTransactionsRecorded: number;
        };
    }>;
}
