export interface TransactionSplit {
    id: string;
    category: string;
    subCategory?: string;
    amount: number;
    notes?: string;
}
export interface Transaction {
    id: string;
    accountId: string;
    postedEpochSeconds: number;
    amount: number;
    originalDesc: string;
    payee: string;
    category: string;
    subCategory?: string;
    notes?: string;
    pending: boolean;
    isSplit?: boolean;
    splits?: TransactionSplit[];
    receiptUrls?: string[];
    matchedRuleId?: string | null;
}
export interface CategoryHierarchy {
    mainCategory: string;
    subCategories: string[];
}
export type BudgetCategoryType = 'FIXED' | 'PERCENT_INCOME' | 'VARIABLE';
export interface Budget {
    id: string;
    category: string;
    subCategory?: string;
    categoryType: BudgetCategoryType;
    targetAmount: number;
    incomePercentage?: number | null;
    rolloverEnabled?: boolean;
    rolloverResetMonths?: string[];
    notes?: string;
}
export interface Rule {
    id: string;
    name: string;
    priority: number;
    pattern: string;
    category: string;
    subCategory?: string;
    minAmount?: number | null;
    maxAmount?: number | null;
    isActive?: boolean;
    matchCount?: number;
}
export interface Goal {
    id: string;
    title: string;
    targetAmount: number;
    currentAmount: number;
    targetDateEpochSeconds?: number | null;
    notes?: string;
    isCompleted?: boolean;
}
export interface SimpleFinAccount {
    id: string;
    name: string;
    currency: string;
    balance: number;
    availableBalance?: number;
    type?: string;
}
export interface SimpleFinConfig {
    accessUrlConfigured: boolean;
    accessUrl?: string;
    lastSyncTimestamp?: number;
}
