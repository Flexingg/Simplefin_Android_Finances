import * as fs from 'fs';
import * as path from 'path';
import { Transaction, Budget, Rule, CategoryHierarchy, Goal, GamificationState, SimpleFinConfig } from './types.js';

export class FinanceStorage {
  private dataDir: string;
  private transactionsFile: string;
  private budgetsFile: string;
  private rulesFile: string;
  private categoriesFile: string;
  private goalsFile: string;
  private gamificationFile: string;
  private configFile: string;

  private transactions: Map<string, Transaction> = new Map();
  private budgets: Map<string, Budget> = new Map();
  private rules: Map<string, Rule> = new Map();
  private categories: Map<string, CategoryHierarchy> = new Map();
  private goals: Map<string, Goal> = new Map();
  private gamification: GamificationState;
  private config: SimpleFinConfig;
  private firestoreBridge: any = null;

  setFirestoreBridge(bridge: any) {
    this.firestoreBridge = bridge;
  }

  constructor(baseDir?: string) {
    this.dataDir = baseDir || path.join(process.cwd(), 'data');
    if (!fs.existsSync(this.dataDir)) {
      fs.mkdirSync(this.dataDir, { recursive: true });
    }

    this.transactionsFile = path.join(this.dataDir, 'transactions.json');
    this.budgetsFile = path.join(this.dataDir, 'budgets.json');
    this.rulesFile = path.join(this.dataDir, 'rules.json');
    this.categoriesFile = path.join(this.dataDir, 'categories.json');
    this.goalsFile = path.join(this.dataDir, 'goals.json');
    this.gamificationFile = path.join(this.dataDir, 'gamification.json');
    this.configFile = path.join(this.dataDir, 'config.json');

    this.gamification = {
      xp: 1477,
      level: 6,
      levelTitle: 'Compound Master',
      streakDays: 1,
      hearts: 5,
      maxHearts: 5,
      gems: 280,
      completedQuests: ['setup_simplefin', 'inbox_zero_day1']
    };

    this.config = {
      accessUrlConfigured: false
    };

    this.loadAll();
    this.seedDefaultCategoriesIfEmpty();
  }

  private loadAll(): void {
    this.loadMap<Transaction>(this.transactionsFile, this.transactions);
    this.loadMap<Budget>(this.budgetsFile, this.budgets);
    this.loadMap<Rule>(this.rulesFile, this.rules);
    this.loadMap<CategoryHierarchy>(this.categoriesFile, this.categories);
    this.loadMap<Goal>(this.goalsFile, this.goals);

    if (fs.existsSync(this.gamificationFile)) {
      try {
        this.gamification = JSON.parse(fs.readFileSync(this.gamificationFile, 'utf-8'));
      } catch (e) {}
    }

    if (fs.existsSync(this.configFile)) {
      try {
        this.config = JSON.parse(fs.readFileSync(this.configFile, 'utf-8'));
      } catch (e) {}
    }
  }

  private loadMap<T extends { id?: string; mainCategory?: string }>(filePath: string, map: Map<string, T>): void {
    if (fs.existsSync(filePath)) {
      try {
        const items: T[] = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
        items.forEach(item => {
          const key = item.id || item.mainCategory;
          if (key) map.set(key, item);
        });
      } catch (e) {}
    }
  }

  private saveFile(filePath: string, data: any): void {
    try {
      fs.writeFileSync(filePath, JSON.stringify(data, null, 2), 'utf-8');
    } catch (e) {
      console.error(`Error saving ${filePath}:`, e);
    }
    // Mirror the write to shared Firestore (if sync is configured).
    this.firestoreBridge?.pushFromFile(filePath, data).catch((e: any) => console.error('firestore push error', e));
  }

  private seedDefaultCategoriesIfEmpty(): void {
    if (this.categories.size === 0) {
      const defaults: CategoryHierarchy[] = [
        { mainCategory: 'Housing', subCategories: ['Rent / Mortgage', 'Utilities', 'Maintenance', 'Insurance'] },
        { mainCategory: 'Food & Dining', subCategories: ['Groceries', 'Restaurants', 'Coffee Shops', 'Delivery'] },
        { mainCategory: 'Transportation', subCategories: ['Fuel', 'Car Payment', 'Public Transit', 'Auto Repairs'] },
        { mainCategory: 'Income', subCategories: ['Salary & Wages', 'Freelance', 'Investments', 'Refunds'] },
        { mainCategory: 'Personal & Lifestyle', subCategories: ['Entertainment', 'Shopping', 'Subscriptions', 'Gym & Fitness'] },
        { mainCategory: 'Financial & Goals', subCategories: ['Emergency Fund', 'Investments', 'Debt Paydown', 'Savings'] }
      ];
      defaults.forEach(c => this.categories.set(c.mainCategory, c));
      this.saveCategories();
    }
  }

  // --- Transactions ---
  getTransactions(): Transaction[] {
    return Array.from(this.transactions.values()).sort((a, b) => b.postedEpochSeconds - a.postedEpochSeconds);
  }

  getTransaction(id: string): Transaction | undefined {
    return this.transactions.get(id);
  }

  saveTransaction(tx: Transaction): void {
    this.transactions.set(tx.id, tx);
    this.saveFile(this.transactionsFile, Array.from(this.transactions.values()));
  }

  saveTransactions(txs: Transaction[]): void {
    txs.forEach(t => this.transactions.set(t.id, t));
    this.saveFile(this.transactionsFile, Array.from(this.transactions.values()));
  }

  // --- Budgets ---
  getBudgets(): Budget[] {
    return Array.from(this.budgets.values());
  }

  saveBudget(budget: Budget): void {
    this.budgets.set(budget.id, budget);
    this.saveFile(this.budgetsFile, Array.from(this.budgets.values()));
  }

  deleteBudget(id: string): void {
    this.budgets.delete(id);
    this.saveFile(this.budgetsFile, Array.from(this.budgets.values()));
  }

  // --- Rules ---
  getRules(): Rule[] {
    return Array.from(this.rules.values()).sort((a, b) => a.priority - b.priority);
  }

  saveRule(rule: Rule): void {
    this.rules.set(rule.id, rule);
    this.saveFile(this.rulesFile, Array.from(this.rules.values()));
  }

  saveRules(rules: Rule[]): void {
    this.rules.clear();
    rules.forEach(r => this.rules.set(r.id, r));
    this.saveFile(this.rulesFile, rules);
  }

  deleteRule(id: string): void {
    this.rules.delete(id);
    this.saveFile(this.rulesFile, Array.from(this.rules.values()));
  }

  // --- Categories ---
  getCategories(): CategoryHierarchy[] {
    return Array.from(this.categories.values());
  }

  saveCategories(): void {
    this.saveFile(this.categoriesFile, Array.from(this.categories.values()));
  }

  addOrUpdateCategory(mainCategory: string, subCategory?: string): void {
    const cleanMain = mainCategory.trim();
    if (!cleanMain) return;
    const existing = this.categories.get(cleanMain) || { mainCategory: cleanMain, subCategories: [] };
    if (subCategory && subCategory.trim()) {
      const cleanSub = subCategory.trim();
      if (!existing.subCategories.includes(cleanSub)) {
        existing.subCategories.push(cleanSub);
      }
    }
    this.categories.set(cleanMain, existing);
    this.saveCategories();
  }

  // --- Goals ---
  getGoals(): Goal[] {
    return Array.from(this.goals.values());
  }

  saveGoal(goal: Goal): void {
    this.goals.set(goal.id, goal);
    this.saveFile(this.goalsFile, Array.from(this.goals.values()));
  }

  // --- Gamification ---
  getGamification(): GamificationState {
    return this.gamification;
  }

  updateGamification(updater: (current: GamificationState) => GamificationState): GamificationState {
    this.gamification = updater(this.gamification);
    this.saveFile(this.gamificationFile, this.gamification);
    return this.gamification;
  }

  // --- Config ---
  getConfig(): SimpleFinConfig {
    return this.config;
  }

  saveConfig(config: SimpleFinConfig): void {
    this.config = config;
    this.saveFile(this.configFile, this.config);
  }
}
