import { Transaction, Budget, Rule, CategoryHierarchy, SimpleFinAccount } from './types.js';

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
  monthKey: string; // "2026-08"
  income: number;
  expenses: number;
  savings: number;
  categoryBreakdown: Record<string, number>;
}

export interface DebtAccount {
  name: string;
  balance: number;
  interestRate: number; // e.g. 18.99 for 18.99%
  minimumPayment: number;
}

export class FinanceEngine {
  /**
   * Evaluates rules in ascending priority order against a transaction
   */
  static matchRule(tx: Transaction, rules: Rule[]): Rule | null {
    if (tx.isSplit) return null;
    const activeRules = rules.filter(r => r.isActive !== false).sort((a, b) => a.priority - b.priority);

    for (const rule of activeRules) {
      if (!rule.pattern || !rule.pattern.trim()) continue;
      const regex = new RegExp(rule.pattern.trim(), 'i');
      if (regex.test(tx.originalDesc) || regex.test(tx.payee)) {
        if (rule.minAmount != null && Math.abs(tx.amount) < rule.minAmount) continue;
        if (rule.maxAmount != null && Math.abs(tx.amount) > rule.maxAmount) continue;
        return rule;
      }
    }
    return null;
  }

  /**
   * Applies rules in batch across an array of transactions
   */
  static applyRules(transactions: Transaction[], rules: Rule[]): { updated: Transaction[]; count: number } {
    let count = 0;
    const updated = transactions.map(tx => {
      const matched = this.matchRule(tx, rules);
      if (matched && (tx.category !== matched.category || (matched.subCategory && tx.subCategory !== matched.subCategory))) {
        count++;
        return {
          ...tx,
          category: matched.category,
          subCategory: matched.subCategory || tx.subCategory || '',
          matchedRuleId: matched.id
        };
      }
      return tx;
    });
    return { updated, count };
  }

  /**
   * Calculates MTD financial summary with envelope rollover logic
   */
  static calculateSummary(
    transactions: Transaction[],
    budgets: Budget[],
    incomeCategoryName: string = 'Income'
  ): FinancialSummary {
    const now = new Date();
    const currentYear = now.getFullYear();
    const currentMonth = now.getMonth(); // 0-indexed
    const startOfMonthEpoch = Math.floor(new Date(currentYear, currentMonth, 1).getTime() / 1000);
    const daysInMonth = new Date(currentYear, currentMonth + 1, 0).getDate();
    const currentDay = now.getDate();
    const daysRemaining = Math.max(1, daysInMonth - currentDay + 1);

    // Current month transactions
    const mtdTxs = transactions.filter(t => t.postedEpochSeconds >= startOfMonthEpoch);

    let totalMtdIncome = 0;
    let totalMtdExpenses = 0;

    mtdTxs.forEach(t => {
      if (t.category.toLowerCase() === incomeCategoryName.toLowerCase() || t.amount > 0) {
        totalMtdIncome += Math.abs(t.amount);
      } else {
        totalMtdExpenses += Math.abs(t.amount);
      }
    });

    // Calculate each budget's spent amount & rollover
    const currentMonthKey = `${currentYear}-${String(currentMonth + 1).padStart(2, '0')}`;

    const calculatedBudgets: CalculatedBudget[] = budgets.map(budget => {
      const displayName = budget.subCategory ? `${budget.category} > ${budget.subCategory}` : budget.category;

      // Base target amount
      let baseTarget = budget.targetAmount;
      if (budget.categoryType === 'PERCENT_INCOME' && budget.incomePercentage) {
        baseTarget = (budget.incomePercentage / 100.0) * totalMtdIncome;
      }

      // Rollover unspent calculations from last month if enabled and not reset
      let rollover = 0;
      const isResetThisMonth = budget.rolloverResetMonths?.includes(currentMonthKey);
      if (budget.rolloverEnabled && !isResetThisMonth) {
        // Calculate prior month balance
        const priorMonthStart = Math.floor(new Date(currentYear, currentMonth - 1, 1).getTime() / 1000);
        const priorMonthTxs = transactions.filter(t => t.postedEpochSeconds >= priorMonthStart && t.postedEpochSeconds < startOfMonthEpoch);
        let priorSpent = 0;
        priorMonthTxs.forEach(t => {
          if (this.matchesCategory(t, budget.category, budget.subCategory)) {
            priorSpent += Math.abs(t.amount);
          }
        });
        const unspent = Math.max(0, baseTarget - priorSpent);
        rollover = unspent;
      }

      const effectiveTarget = baseTarget + rollover;

      // Current MTD spent
      let spent = 0;
      mtdTxs.forEach(t => {
        if (this.matchesCategory(t, budget.category, budget.subCategory)) {
          if (t.isSplit && t.splits && t.splits.length > 0) {
            t.splits.forEach(s => {
              if (this.matchesCategory({ category: s.category, subCategory: s.subCategory }, budget.category, budget.subCategory)) {
                spent += Math.abs(s.amount);
              }
            });
          } else {
            spent += Math.abs(t.amount);
          }
        }
      });

      const remaining = effectiveTarget - spent;
      const percentUsed = effectiveTarget > 0 ? (spent / effectiveTarget) * 100 : 0;

      return {
        budget,
        displayName,
        spentAmount: spent,
        effectiveTargetAmount: effectiveTarget,
        rolloverAmount: rollover,
        remainingAmount: remaining,
        percentUsed,
        isOverBudget: spent > effectiveTarget
      };
    });

    const netSavings = totalMtdIncome - totalMtdExpenses;
    const savingsRatePercent = totalMtdIncome > 0 ? (netSavings / totalMtdIncome) * 100 : 0;
    const totalBudgetCap = calculatedBudgets.reduce((sum, b) => sum + b.effectiveTargetAmount, 0);
    const remainingBudgetPool = Math.max(0, totalBudgetCap - totalMtdExpenses);
    const targetDailyAllowance = remainingBudgetPool / daysRemaining;
    const dailySpendActualAverage = currentDay > 0 ? totalMtdExpenses / currentDay : 0;

    return {
      totalMtdIncome,
      totalMtdExpenses,
      netSavings,
      savingsRatePercent,
      targetDailyAllowance,
      dailySpendActualAverage,
      daysRemainingInMonth: daysRemaining,
      calculatedBudgets
    };
  }

  private static matchesCategory(item: { category: string; subCategory?: string }, targetCat: string, targetSub?: string): boolean {
    const mainMatch = item.category.toLowerCase() === targetCat.toLowerCase();
    if (!mainMatch) return false;
    if (!targetSub || !targetSub.trim()) return true;
    return (item.subCategory || '').toLowerCase() === targetSub.toLowerCase();
  }

  /**
   * Generates month-by-month spending trends for the last N months
   */
  static getHistoricalTrends(transactions: Transaction[], monthsBack: number = 6): MonthTrend[] {
    const trends: MonthTrend[] = [];
    const now = new Date();

    for (let i = monthsBack - 1; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const startEpoch = Math.floor(d.getTime() / 1000);
      const endEpoch = Math.floor(new Date(d.getFullYear(), d.getMonth() + 1, 1).getTime() / 1000);
      const monthKey = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;

      const monthTxs = transactions.filter(t => t.postedEpochSeconds >= startEpoch && t.postedEpochSeconds < endEpoch);
      let income = 0;
      let expenses = 0;
      const categoryBreakdown: Record<string, number> = {};

      monthTxs.forEach(t => {
        if (t.amount > 0 || t.category.toLowerCase() === 'income') {
          income += Math.abs(t.amount);
        } else {
          const amt = Math.abs(t.amount);
          expenses += amt;
          categoryBreakdown[t.category] = (categoryBreakdown[t.category] || 0) + amt;
        }
      });

      trends.push({
        monthKey,
        income,
        expenses,
        savings: income - expenses,
        categoryBreakdown
      });
    }

    return trends;
  }

  /**
   * Generates daily spending intensity matrix for calendar heatmap (last 90 days)
   */
  static getDailyHeatmap(transactions: Transaction[], days: number = 90): Record<string, { totalSpent: number; count: number; level: number }> {
    const heatmap: Record<string, { totalSpent: number; count: number; level: number }> = {};
    const nowEpoch = Math.floor(Date.now() / 1000);
    const startEpoch = nowEpoch - (days * 86400);

    const relevant = transactions.filter(t => t.postedEpochSeconds >= startEpoch && t.amount < 0);
    relevant.forEach(t => {
      const dateStr = new Date(t.postedEpochSeconds * 1000).toISOString().split('T')[0];
      if (!heatmap[dateStr]) {
        heatmap[dateStr] = { totalSpent: 0, count: 0, level: 0 };
      }
      heatmap[dateStr].totalSpent += Math.abs(t.amount);
      heatmap[dateStr].count += 1;
    });

    // Assign intensity levels 0-4
    Object.values(heatmap).forEach(entry => {
      if (entry.totalSpent === 0) entry.level = 0;
      else if (entry.totalSpent < 35) entry.level = 1;
      else if (entry.totalSpent < 100) entry.level = 2;
      else if (entry.totalSpent < 250) entry.level = 3;
      else entry.level = 4;
    });

    return heatmap;
  }

  /**
   * Debt Payoff Projections: Snowball vs Avalanche
   */
  static simulateDebtPayoff(
    debts: DebtAccount[],
    monthlyExtraBudget: number = 200
  ): { snowballMonths: number; avalancheMonths: number; snowballInterest: number; avalancheInterest: number } {
    if (debts.length === 0) {
      return { snowballMonths: 0, avalancheMonths: 0, snowballInterest: 0, avalancheInterest: 0 };
    }

    const runSimulation = (sortStrategy: 'balance' | 'interest') => {
      let activeDebts = debts.map(d => ({ ...d }));
      let totalMonths = 0;
      let totalInterestPaid = 0;

      while (activeDebts.some(d => d.balance > 0) && totalMonths < 360) {
        totalMonths++;
        let extraPool = monthlyExtraBudget;

        // Apply monthly interest
        activeDebts.forEach(d => {
          if (d.balance > 0) {
            const monthlyInterest = (d.balance * (d.interestRate / 100)) / 12;
            d.balance += monthlyInterest;
            totalInterestPaid += monthlyInterest;
          }
        });

        // Pay minimums
        activeDebts.forEach(d => {
          if (d.balance > 0) {
            const pay = Math.min(d.balance, d.minimumPayment);
            d.balance -= pay;
          }
        });

        // Apply extra pool to target debt
        const sorted = activeDebts
          .filter(d => d.balance > 0)
          .sort((a, b) => sortStrategy === 'balance' ? a.balance - b.balance : b.interestRate - a.interestRate);

        for (const target of sorted) {
          if (extraPool <= 0) break;
          const pay = Math.min(target.balance, extraPool);
          target.balance -= pay;
          extraPool -= pay;
        }
      }

      return { months: totalMonths, interest: Math.round(totalInterestPaid) };
    };

    const snowball = runSimulation('balance');
    const avalanche = runSimulation('interest');

    return {
      snowballMonths: snowball.months,
      avalancheMonths: avalanche.months,
      snowballInterest: snowball.interest,
      avalancheInterest: avalanche.interest
    };
  }
}
