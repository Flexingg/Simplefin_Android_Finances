import { Transaction, Budget, Rule } from './types.js';
export interface CalculatedBudget {
    budget: Budget;
    displayName: string;
    spentAmount: number;
    effectiveTargetAmount: number;
    rolloverAmount: number;
    remainingAmount: number;
    percentUsed: number;
    isOverBudget: boolean;
}
export interface FinancialSummary {
    totalMtdIncome: number;
    totalMtdExpenses: number;
    netSavings: number;
    savingsRatePercent: number;
    targetDailyAllowance: number;
    dailySpendActualAverage: number;
    daysRemainingInMonth: number;
    calculatedBudgets: CalculatedBudget[];
}
export interface MonthTrend {
    monthKey: string;
    income: number;
    expenses: number;
    savings: number;
    categoryBreakdown: Record<string, number>;
}
export interface DebtAccount {
    name: string;
    balance: number;
    interestRate: number;
    minimumPayment: number;
}
export declare class FinanceEngine {
    /**
     * Evaluates rules in ascending priority order against a transaction
     */
    static matchRule(tx: Transaction, rules: Rule[]): Rule | null;
    /**
     * Applies rules in batch across an array of transactions
     */
    static applyRules(transactions: Transaction[], rules: Rule[]): {
        updated: Transaction[];
        count: number;
    };
    /**
     * Calculates MTD financial summary with envelope rollover logic
     */
    static calculateSummary(transactions: Transaction[], budgets: Budget[], incomeCategoryName?: string): FinancialSummary;
    private static matchesCategory;
    /**
     * Generates month-by-month spending trends for the last N months
     */
    static getHistoricalTrends(transactions: Transaction[], monthsBack?: number): MonthTrend[];
    /**
     * Generates daily spending intensity matrix for calendar heatmap (last 90 days)
     */
    static getDailyHeatmap(transactions: Transaction[], days?: number): Record<string, {
        totalSpent: number;
        count: number;
        level: number;
    }>;
    /**
     * Debt Payoff Projections: Snowball vs Avalanche
     */
    static simulateDebtPayoff(debts: DebtAccount[], monthlyExtraBudget?: number): {
        snowballMonths: number;
        avalancheMonths: number;
        snowballInterest: number;
        avalancheInterest: number;
    };
}
