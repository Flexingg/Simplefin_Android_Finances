import { Transaction, Budget, Rule, CategoryHierarchy, Goal, GamificationState } from '../types';

type Listener = () => void;

class AppStateService {
  private transactions: Transaction[] = [];
  private budgets: Budget[] = [];
  private rules: Rule[] = [];
  private categories: CategoryHierarchy[] = [];
  private goals: Goal[] = [];
  private gamification: GamificationState = {
    xp: 1477,
    level: 6,
    levelTitle: 'Compound Master',
    streakDays: 1,
    hearts: 5,
    maxHearts: 5,
    gems: 280,
    completedQuests: ['inbox_zero_reviewer']
  };
  private listeners: Set<Listener> = new Set();
  private _dirty = false;

  constructor() {
    this.loadFromStorage();
    if (this.transactions.length === 0) {
      this.seedInitialData();
    }
  }

  /**
   * Hydrate state from a remote source (Firestore snapshot). Does NOT mark the
   * state dirty, so the sync bridge won't echo the change back to the server.
   */
  hydrate(partial: {
    transactions?: Transaction[];
    budgets?: Budget[];
    rules?: Rule[];
    categories?: CategoryHierarchy[];
    goals?: Goal[];
    gamification?: GamificationState;
  }) {
    if (partial.transactions) this.transactions = partial.transactions;
    if (partial.budgets) this.budgets = partial.budgets;
    if (partial.rules) this.rules = partial.rules;
    if (partial.categories) this.categories = partial.categories;
    if (partial.goals) this.goals = partial.goals;
    if (partial.gamification) this.gamification = partial.gamification;
    this._dirty = false;
    this.notify();
  }

  /** True if the last change was a local mutation (should be pushed to Firestore). */
  isDirty(): boolean {
    return this._dirty;
  }

  private loadFromStorage() {
    try {
      const txRaw = localStorage.getItem('rf_transactions');
      if (txRaw) this.transactions = JSON.parse(txRaw);

      const bRaw = localStorage.getItem('rf_budgets');
      if (bRaw) this.budgets = JSON.parse(bRaw);

      const rRaw = localStorage.getItem('rf_rules');
      if (rRaw) this.rules = JSON.parse(rRaw);

      const cRaw = localStorage.getItem('rf_categories');
      if (cRaw) this.categories = JSON.parse(cRaw);

      const gRaw = localStorage.getItem('rf_goals');
      if (gRaw) this.goals = JSON.parse(gRaw);

      const gamRaw = localStorage.getItem('rf_gamification');
      if (gamRaw) this.gamification = JSON.parse(gamRaw);
    } catch (e) {}
  }

  private saveToStorage() {
    try {
      localStorage.setItem('rf_transactions', JSON.stringify(this.transactions));
      localStorage.setItem('rf_budgets', JSON.stringify(this.budgets));
      localStorage.setItem('rf_rules', JSON.stringify(this.rules));
      localStorage.setItem('rf_categories', JSON.stringify(this.categories));
      localStorage.setItem('rf_goals', JSON.stringify(this.goals));
      localStorage.setItem('rf_gamification', JSON.stringify(this.gamification));
    } catch (e) {}
    this._dirty = true;
    this.notify();
  }

  private seedInitialData() {
    this.categories = [
      { mainCategory: 'Housing', subCategories: ['Rent / Mortgage', 'Utilities', 'Maintenance', 'Internet'] },
      { mainCategory: 'Food & Dining', subCategories: ['Groceries', 'Restaurants', 'Coffee Shops', 'Delivery'] },
      { mainCategory: 'Transportation', subCategories: ['Fuel', 'Car Payment', 'Public Transit', 'Auto Repairs'] },
      { mainCategory: 'Income', subCategories: ['Salary & Wages', 'Freelance', 'Investments', 'Refunds'] },
      { mainCategory: 'Personal & Lifestyle', subCategories: ['Entertainment', 'Shopping', 'Subscriptions', 'Fitness'] },
      { mainCategory: 'Financial & Goals', subCategories: ['Emergency Fund', 'Investments', 'Debt Paydown'] }
    ];

    this.budgets = [
      { id: 'b-1', category: 'Food & Dining', subCategory: 'Groceries', categoryType: 'FIXED', targetAmount: 450, rolloverEnabled: true },
      { id: 'b-2', category: 'Food & Dining', subCategory: 'Restaurants', categoryType: 'FIXED', targetAmount: 180, rolloverEnabled: false },
      { id: 'b-3', category: 'Transportation', subCategory: 'Fuel', categoryType: 'FIXED', targetAmount: 140, rolloverEnabled: true },
      { id: 'b-4', category: 'Housing', subCategory: 'Utilities', categoryType: 'PERCENT_INCOME', targetAmount: 220, incomePercentage: 5, rolloverEnabled: false },
      { id: 'b-5', category: 'Personal & Lifestyle', subCategory: 'Entertainment', categoryType: 'FIXED', targetAmount: 100, rolloverEnabled: true }
    ];

    this.rules = [
      { id: 'r-1', name: 'Kroger', priority: 1, pattern: 'Kroger', category: 'Food & Dining', subCategory: 'Groceries', isActive: true },
      { id: 'r-2', name: 'Shell / Chevron', priority: 2, pattern: 'Shell|Chevron|Exxon', category: 'Transportation', subCategory: 'Fuel', isActive: true },
      { id: 'r-3', name: 'Netflix / Spotify', priority: 3, pattern: 'Netflix|Spotify', category: 'Personal & Lifestyle', subCategory: 'Subscriptions', isActive: true },
      { id: 'r-4', name: 'Payroll Deposit', priority: 4, pattern: 'Payroll|Direct Dep', category: 'Income', subCategory: 'Salary & Wages', isActive: true }
    ];

    this.goals = [
      { id: 'g-1', title: 'Emergency Fund', targetAmount: 10000, currentAmount: 4200, isCompleted: false },
      { id: 'g-2', title: 'Vacation Trip', targetAmount: 2500, currentAmount: 1800, isCompleted: false }
    ];

    const now = Math.floor(Date.now() / 1000);
    this.transactions = [
      { id: 'tx-1', accountId: 'acc-1', postedEpochSeconds: now - 3600, amount: -74.50, originalDesc: 'KROGER #0482 ATLANTA GA', payee: 'Kroger', category: 'Food & Dining', subCategory: 'Groceries', notes: 'Weekly groceries', pending: false },
      { id: 'tx-2', accountId: 'acc-1', postedEpochSeconds: now - 86400, amount: -42.10, originalDesc: 'SHELL OIL 57544211', payee: 'Shell', category: 'Transportation', subCategory: 'Fuel', notes: '', pending: false },
      { id: 'tx-3', accountId: 'acc-1', postedEpochSeconds: now - 172800, amount: -15.99, originalDesc: 'NETFLIX.COM DIGITAL STREAM', payee: 'Netflix', category: 'Personal & Lifestyle', subCategory: 'Subscriptions', notes: 'Monthly sub', pending: false },
      { id: 'tx-4', accountId: 'acc-1', postedEpochSeconds: now - 250000, amount: 2850.00, originalDesc: 'EMPLOYER DIRECT DEP PAYROLL', payee: 'Direct Deposit', category: 'Income', subCategory: 'Salary & Wages', notes: 'Bi-weekly paycheck', pending: false },
      { id: 'tx-5', accountId: 'acc-1', postedEpochSeconds: now - 12000, amount: -38.45, originalDesc: 'CHIPOTLE 1042 ONLINE ORDER', payee: 'Chipotle', category: 'Uncategorized', subCategory: '', notes: '', pending: false },
      { id: 'tx-6', accountId: 'acc-1', postedEpochSeconds: now - 5000, amount: -19.99, originalDesc: 'TARGET STORE #1882', payee: 'Target', category: 'Uncategorized', subCategory: '', notes: '', pending: true }
    ];

    this.saveToStorage();
  }

  subscribe(listener: Listener): () => void {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  }

  private notify() {
    this.listeners.forEach(l => l());
  }

  // --- Getters ---
  getTransactions() { return [...this.transactions]; }
  getBudgets() { return [...this.budgets]; }
  getRules() { return [...this.rules]; }
  getCategories() { return [...this.categories]; }
  getGoals() { return [...this.goals]; }
  getGamification() { return { ...this.gamification }; }

  // --- Actions ---
  categorizeTransaction(txId: string, mainCategory: string, subCategory: string = '', notes: string = '', createRule: boolean = false) {
    const tx = this.transactions.find(t => t.id === txId);
    if (!tx) return;

    tx.category = mainCategory;
    tx.subCategory = subCategory;
    if (notes) tx.notes = notes;

    this.addCategory(mainCategory, subCategory);

    let xpGained = 15;
    if (notes) xpGained += 10;

    if (createRule) {
      const pattern = tx.payee || tx.originalDesc.split(' ')[0] || tx.originalDesc;
      const newRule: Rule = {
        id: `r-${Date.now()}`,
        name: pattern,
        priority: this.rules.length + 1,
        pattern,
        category: mainCategory,
        subCategory,
        isActive: true
      };
      this.rules.push(newRule);
      xpGained += 25;
      this.applyAllRules();
    }

    this.addXp(xpGained);
    this.saveToStorage();
  }

  splitTransaction(txId: string, splits: { category: string; subCategory?: string; amount: number; notes?: string }[]) {
    const tx = this.transactions.find(t => t.id === txId);
    if (!tx) return;

    tx.isSplit = true;
    tx.splits = splits.map((s, idx) => ({
      id: `split-${idx}-${Date.now()}`,
      category: s.category,
      subCategory: s.subCategory || '',
      amount: -Math.abs(s.amount),
      notes: s.notes || ''
    }));
    tx.category = splits[0]?.category || tx.category;
    tx.subCategory = splits[0]?.subCategory || tx.subCategory;

    this.addXp(35);
    this.saveToStorage();
  }

  saveBudget(budget: Budget) {
    const idx = this.budgets.findIndex(b => b.id === budget.id);
    if (idx >= 0) {
      this.budgets[idx] = budget;
    } else {
      this.budgets.push({ ...budget, id: budget.id || `b-${Date.now()}` });
    }
    this.addCategory(budget.category, budget.subCategory);
    this.saveToStorage();
  }

  resetMonthRollover(budgetId: string) {
    const b = this.budgets.find(item => item.id === budgetId);
    if (!b) return;
    const now = new Date();
    const monthKey = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    const resets = b.rolloverResetMonths || [];
    if (!resets.includes(monthKey)) {
      resets.push(monthKey);
    }
    b.rolloverResetMonths = resets;
    this.saveToStorage();
  }

  saveRule(rule: Rule) {
    const idx = this.rules.findIndex(r => r.id === rule.id);
    if (idx >= 0) {
      this.rules[idx] = rule;
    } else {
      this.rules.push({ ...rule, id: rule.id || `r-${Date.now()}` });
    }
    this.applyAllRules();
    this.saveToStorage();
  }

  deleteRule(id: string) {
    this.rules = this.rules.filter(r => r.id !== id);
    this.saveToStorage();
  }

  applyAllRules(): number {
    let count = 0;
    const activeRules = this.rules.filter(r => r.isActive !== false).sort((a, b) => a.priority - b.priority);

    this.transactions.forEach(tx => {
      if (tx.isSplit) return;
      for (const rule of activeRules) {
        const regex = new RegExp(rule.pattern, 'i');
        if (regex.test(tx.originalDesc) || regex.test(tx.payee)) {
          if (tx.category !== rule.category || (rule.subCategory && tx.subCategory !== rule.subCategory)) {
            tx.category = rule.category;
            tx.subCategory = rule.subCategory || '';
            tx.matchedRuleId = rule.id;
            count++;
          }
          break;
        }
      }
    });

    this.saveToStorage();
    return count;
  }

  addCategory(main: string, sub?: string) {
    const cleanMain = main.trim();
    if (!cleanMain) return;
    let existing = this.categories.find(c => c.mainCategory.toLowerCase() === cleanMain.toLowerCase());
    if (!existing) {
      existing = { mainCategory: cleanMain, subCategories: [] };
      this.categories.push(existing);
    }
    if (sub && sub.trim()) {
      const cleanSub = sub.trim();
      if (!existing.subCategories.some(s => s.toLowerCase() === cleanSub.toLowerCase())) {
        existing.subCategories.push(cleanSub);
      }
    }
    this.saveToStorage();
  }

  saveGoal(goal: Goal) {
    const idx = this.goals.findIndex(g => g.id === goal.id);
    if (idx >= 0) {
      this.goals[idx] = goal;
    } else {
      this.goals.push({ ...goal, id: goal.id || `g-${Date.now()}` });
    }
    this.saveToStorage();
  }

  addXp(amount: number) {
    this.gamification.xp += amount;
    const currentMax = this.gamification.level * 250;
    if (this.gamification.xp >= currentMax) {
      this.gamification.level += 1;
      this.gamification.gems += 50;
    }
    this.saveToStorage();
  }

  refillHearts() {
    this.gamification.hearts = this.gamification.maxHearts;
    this.saveToStorage();
  }

  completeQuest(questId: string) {
    if (!this.gamification.completedQuests.includes(questId)) {
      this.gamification.completedQuests.push(questId);
      this.addXp(50);
      this.gamification.gems += 20;
      this.saveToStorage();
    }
  }
}

export const appState = new AppStateService();
