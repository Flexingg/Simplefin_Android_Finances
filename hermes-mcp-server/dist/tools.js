import { FinanceEngine } from './engine.js';
import { SimpleFinClient } from './simplefin.js';
import { randomUUID } from 'crypto';
export class HermesFinanceTools {
    storage;
    constructor(storage) {
        this.storage = storage;
    }
    // 1. Sync SimpleFIN Accounts
    async syncSimpleFin(args) {
        const config = this.storage.getConfig();
        if (!config.accessUrlConfigured || !config.accessUrl) {
            return {
                success: false,
                message: 'SimpleFIN access URL is not configured. Use claim_simplefin_token to connect your bank account.'
            };
        }
        const daysBack = args.daysBack || 90;
        const { accounts, transactions } = await SimpleFinClient.fetchAccountsAndTransactions(config.accessUrl, daysBack);
        // Save accounts & transactions
        const existing = this.storage.getTransactions();
        const existingMap = new Map(existing.map(t => [t.id, t]));
        let newCount = 0;
        const rules = this.storage.getRules();
        transactions.forEach(t => {
            if (!existingMap.has(t.id)) {
                newCount++;
                // Apply auto rules if applicable
                const matched = FinanceEngine.matchRule(t, rules);
                if (matched) {
                    t.category = matched.category;
                    t.subCategory = matched.subCategory || '';
                    t.matchedRuleId = matched.id;
                }
                existingMap.set(t.id, t);
            }
        });
        this.storage.saveTransactions(Array.from(existingMap.values()));
        this.storage.saveConfig({ ...config, lastSyncTimestamp: Date.now() });
        return {
            success: true,
            message: `Synced ${accounts.length} bank accounts. Discovered ${newCount} new transactions.`,
            accountsSummary: accounts.map(a => `${a.name}: $${a.balance.toFixed(2)} (${a.currency})`),
            totalTransactionsRecorded: existingMap.size
        };
    }
    // 2. Financial Summary
    getFinancialSummary() {
        const txs = this.storage.getTransactions();
        const budgets = this.storage.getBudgets();
        const summary = FinanceEngine.calculateSummary(txs, budgets);
        const gamification = this.storage.getGamification();
        return {
            success: true,
            summary: {
                mtdIncome: summary.totalMtdIncome,
                mtdExpenses: summary.totalMtdExpenses,
                netSavings: summary.netSavings,
                savingsRate: `${summary.savingsRatePercent.toFixed(1)}%`,
                targetDailyAllowance: summary.targetDailyAllowance,
                dailySpendActualAverage: summary.dailySpendActualAverage,
                daysRemainingInMonth: summary.daysRemainingInMonth,
                streakDays: gamification.streakDays,
                xp: gamification.xp,
                level: `Lvl ${gamification.level} (${gamification.levelTitle})`,
                hearts: `${gamification.hearts}/${gamification.maxHearts}`,
                gems: gamification.gems,
                budgetAlerts: summary.calculatedBudgets.filter(b => b.isOverBudget || b.percentUsed >= 90).map(b => ({
                    category: b.displayName,
                    spent: b.spentAmount,
                    limit: b.effectiveTargetAmount,
                    percentUsed: `${b.percentUsed.toFixed(1)}%`,
                    isOver: b.isOverBudget
                }))
            }
        };
    }
    // 3. List Transactions
    listTransactions(args) {
        let txs = this.storage.getTransactions();
        if (args.daysBack) {
            const minEpoch = Math.floor(Date.now() / 1000) - (args.daysBack * 86400);
            txs = txs.filter(t => t.postedEpochSeconds >= minEpoch);
        }
        if (args.pendingOnly) {
            txs = txs.filter(t => t.pending);
        }
        if (args.uncategorizedOnly) {
            txs = txs.filter(t => t.category.toLowerCase() === 'uncategorized' || !t.category);
        }
        if (args.category) {
            txs = txs.filter(t => t.category.toLowerCase() === args.category.toLowerCase());
        }
        if (args.subCategory) {
            txs = txs.filter(t => (t.subCategory || '').toLowerCase() === args.subCategory.toLowerCase());
        }
        if (args.query) {
            const q = args.query.toLowerCase();
            txs = txs.filter(t => t.originalDesc.toLowerCase().includes(q) ||
                t.payee.toLowerCase().includes(q) ||
                (t.notes || '').toLowerCase().includes(q));
        }
        const limit = args.limit || 50;
        const results = txs.slice(0, limit);
        return {
            success: true,
            count: results.length,
            totalMatching: txs.length,
            transactions: results.map(t => ({
                id: t.id,
                date: new Date(t.postedEpochSeconds * 1000).toISOString().split('T')[0],
                amount: t.amount,
                description: t.originalDesc,
                payee: t.payee,
                category: t.category,
                subCategory: t.subCategory || '',
                notes: t.notes || '',
                isSplit: t.isSplit || false,
                pending: t.pending
            }))
        };
    }
    // 4. Categorize Transaction
    categorizeTransaction(args) {
        const tx = this.storage.getTransaction(args.transactionId);
        if (!tx) {
            return { success: false, message: `Transaction ${args.transactionId} not found.` };
        }
        this.storage.addOrUpdateCategory(args.mainCategory, args.subCategory);
        tx.category = args.mainCategory;
        if (args.subCategory !== undefined)
            tx.subCategory = args.subCategory;
        if (args.notes !== undefined)
            tx.notes = args.notes;
        let ruleCreated = false;
        let ruleAppliedCount = 0;
        if (args.createAutoRule) {
            const pattern = tx.payee || tx.originalDesc.split(' ')[0] || tx.originalDesc;
            const newRule = {
                id: randomUUID(),
                name: pattern,
                priority: this.storage.getRules().length + 1,
                pattern: pattern,
                category: args.mainCategory,
                subCategory: args.subCategory || '',
                isActive: true
            };
            this.storage.saveRule(newRule);
            ruleCreated = true;
            // Apply across historical
            const allTxs = this.storage.getTransactions();
            const { updated, count } = FinanceEngine.applyRules(allTxs, this.storage.getRules());
            if (count > 0) {
                this.storage.saveTransactions(updated);
                ruleAppliedCount = count;
            }
        }
        else {
            this.storage.saveTransaction(tx);
        }
        // Award XP
        this.storage.updateGamification(curr => ({
            ...curr,
            xp: curr.xp + (ruleCreated ? 35 : 15)
        }));
        return {
            success: true,
            message: `Transaction categorized as ${args.mainCategory}${args.subCategory ? ` > ${args.subCategory}` : ''}.${ruleCreated ? ` Created auto-rule "${tx.payee || tx.originalDesc}" applied to ${ruleAppliedCount} transactions.` : ''}`,
            transaction: tx
        };
    }
    // 5. Batch Categorize Transactions
    batchCategorize(args) {
        const txs = this.storage.getTransactions();
        const map = new Map(txs.map(t => [t.id, t]));
        let updatedCount = 0;
        for (const p of args.proposals) {
            const target = map.get(p.transactionId);
            if (target) {
                this.storage.addOrUpdateCategory(p.mainCategory, p.subCategory);
                target.category = p.mainCategory;
                if (p.subCategory !== undefined)
                    target.subCategory = p.subCategory;
                if (p.notes !== undefined)
                    target.notes = p.notes;
                updatedCount++;
            }
        }
        if (updatedCount > 0) {
            this.storage.saveTransactions(Array.from(map.values()));
            this.storage.updateGamification(curr => ({ ...curr, xp: curr.xp + (updatedCount * 15) }));
        }
        return {
            success: true,
            message: `Batch categorized ${updatedCount} transactions.`
        };
    }
    // 6. Split Transaction
    splitTransaction(args) {
        const tx = this.storage.getTransaction(args.transactionId);
        if (!tx) {
            return { success: false, message: `Transaction ${args.transactionId} not found.` };
        }
        const totalSplit = args.splits.reduce((sum, s) => sum + s.amount, 0);
        if (Math.abs(totalSplit - Math.abs(tx.amount)) > 0.02) {
            return {
                success: false,
                message: `Sum of splits ($${totalSplit.toFixed(2)}) must match transaction amount ($${Math.abs(tx.amount).toFixed(2)})`
            };
        }
        args.splits.forEach(s => this.storage.addOrUpdateCategory(s.category, s.subCategory));
        tx.isSplit = true;
        tx.splits = args.splits.map(s => ({
            id: randomUUID(),
            category: s.category,
            subCategory: s.subCategory || '',
            amount: -Math.abs(s.amount),
            notes: s.notes || ''
        }));
        tx.category = args.splits[0]?.category || tx.category;
        tx.subCategory = args.splits[0]?.subCategory || tx.subCategory;
        this.storage.saveTransaction(tx);
        this.storage.updateGamification(curr => ({ ...curr, xp: curr.xp + 25 }));
        return {
            success: true,
            message: `Transaction split into ${args.splits.length} allocations. (+25 XP)`,
            transaction: tx
        };
    }
    // 7. Manage Budgets
    listBudgets() {
        const txs = this.storage.getTransactions();
        const budgets = this.storage.getBudgets();
        const summary = FinanceEngine.calculateSummary(txs, budgets);
        return {
            success: true,
            budgets: summary.calculatedBudgets.map(b => ({
                id: b.budget.id,
                category: b.budget.category,
                subCategory: b.budget.subCategory || '',
                categoryType: b.budget.categoryType,
                targetAmount: b.effectiveTargetAmount,
                baseTargetAmount: b.budget.targetAmount,
                incomePercentage: b.budget.incomePercentage,
                rolloverEnabled: b.budget.rolloverEnabled || false,
                rolloverAmount: b.rolloverAmount,
                spentAmount: b.spentAmount,
                remainingAmount: b.remainingAmount,
                percentUsed: `${b.percentUsed.toFixed(1)}%`,
                isOverBudget: b.isOverBudget
            }))
        };
    }
    createOrUpdateBudget(args) {
        const budget = {
            id: args.id || randomUUID(),
            category: args.category,
            subCategory: args.subCategory || '',
            categoryType: args.categoryType,
            targetAmount: args.targetAmount,
            incomePercentage: args.incomePercentage || null,
            rolloverEnabled: args.rolloverEnabled ?? false
        };
        this.storage.addOrUpdateCategory(args.category, args.subCategory);
        this.storage.saveBudget(budget);
        return {
            success: true,
            message: `Budget for ${budget.category}${budget.subCategory ? ` > ${budget.subCategory}` : ''} saved ($${budget.targetAmount}/mo).`,
            budget
        };
    }
    resetRollover(args) {
        const budgets = this.storage.getBudgets();
        const target = budgets.find(b => b.id === args.budgetId);
        if (!target) {
            return { success: false, message: `Budget ${args.budgetId} not found.` };
        }
        const now = new Date();
        const monthKey = args.monthKey || `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
        const resets = target.rolloverResetMonths || [];
        if (!resets.includes(monthKey)) {
            resets.push(monthKey);
        }
        target.rolloverResetMonths = resets;
        this.storage.saveBudget(target);
        return {
            success: true,
            message: `Rollover balance for ${target.category} reset to $0 for ${monthKey}.`
        };
    }
    // 8. Auto-Rules Engine
    listRules() {
        const rules = this.storage.getRules();
        return {
            success: true,
            rules: rules.map(r => ({
                id: r.id,
                name: r.name,
                pattern: r.pattern,
                category: r.category,
                subCategory: r.subCategory || '',
                priority: r.priority,
                isActive: r.isActive !== false
            }))
        };
    }
    createAutoRule(args) {
        const rules = this.storage.getRules();
        const newRule = {
            id: randomUUID(),
            name: args.pattern,
            priority: rules.length + 1,
            pattern: args.pattern,
            category: args.category,
            subCategory: args.subCategory || '',
            minAmount: args.minAmount || null,
            maxAmount: args.maxAmount || null,
            isActive: true
        };
        this.storage.saveRule(newRule);
        // Apply rule to all historical transactions
        const txs = this.storage.getTransactions();
        const { updated, count } = FinanceEngine.applyRules(txs, [...rules, newRule]);
        if (count > 0) {
            this.storage.saveTransactions(updated);
        }
        return {
            success: true,
            message: `Auto-rule "${args.pattern}" -> ${args.category} created and applied to ${count} transactions.`,
            rule: newRule
        };
    }
    runAllRules() {
        const txs = this.storage.getTransactions();
        const rules = this.storage.getRules();
        const { updated, count } = FinanceEngine.applyRules(txs, rules);
        if (count > 0) {
            this.storage.saveTransactions(updated);
        }
        return {
            success: true,
            message: `Applied ${rules.length} auto-rules across ${txs.length} transactions. Updated ${count} items.`
        };
    }
    // 9. Spending Trends & Analytics
    getSpendingTrends(args) {
        const txs = this.storage.getTransactions();
        const trends = FinanceEngine.getHistoricalTrends(txs, args.monthsBack || 6);
        return {
            success: true,
            trends
        };
    }
    getSpendingHeatmap(args) {
        const txs = this.storage.getTransactions();
        const heatmap = FinanceEngine.getDailyHeatmap(txs, args.days || 90);
        return {
            success: true,
            heatmap
        };
    }
    simulateDebtPayoff(args) {
        const result = FinanceEngine.simulateDebtPayoff(args.debts, args.monthlyExtraBudget || 200);
        return {
            success: true,
            simulation: {
                snowball: {
                    monthsToDebtFree: result.snowballMonths,
                    yearsToDebtFree: (result.snowballMonths / 12).toFixed(1),
                    totalInterestPaid: `$${result.snowballInterest}`
                },
                avalanche: {
                    monthsToDebtFree: result.avalancheMonths,
                    yearsToDebtFree: (result.avalancheMonths / 12).toFixed(1),
                    totalInterestPaid: `$${result.avalancheInterest}`
                },
                recommendation: result.avalancheInterest < result.snowballInterest
                    ? `Avalanche saves $${result.snowballInterest - result.avalancheInterest} in total interest.`
                    : `Both strategies are comparable; Snowball gives faster psychological wins.`
            }
        };
    }
    // 10. Claim SimpleFIN Token
    async claimSimpleFinToken(args) {
        const accessUrl = await SimpleFinClient.claimSetupToken(args.tokenBase64);
        this.storage.saveConfig({
            accessUrlConfigured: true,
            accessUrl: accessUrl,
            lastSyncTimestamp: Date.now()
        });
        // Run initial sync
        const syncRes = await this.syncSimpleFin({ daysBack: 90 });
        return {
            success: true,
            message: 'SimpleFIN bridge successfully connected and synced!',
            initialSync: syncRes
        };
    }
}
