import * as fs from 'fs';
import * as path from 'path';
export class FinanceStorage {
    dataDir;
    transactionsFile;
    budgetsFile;
    rulesFile;
    categoriesFile;
    goalsFile;
    gamificationFile;
    configFile;
    transactions = new Map();
    budgets = new Map();
    rules = new Map();
    categories = new Map();
    goals = new Map();
    gamification;
    config;
    firestoreBridge = null;
    silentPush = false;
    setFirestoreBridge(bridge) {
        this.firestoreBridge = bridge;
    }
    /**
     * When true, saveFile() writes to disk but does NOT push to Firestore.
     * Used by FirestoreBridge when applying REMOTE changes (realtime listeners /
     * bootstrap) so it doesn't echo its own writes back — breaking the sync loop.
     */
    setSilentPush(v) {
        this.silentPush = v;
    }
    constructor(baseDir) {
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
    loadAll() {
        this.loadMap(this.transactionsFile, this.transactions);
        this.loadMap(this.budgetsFile, this.budgets);
        this.loadMap(this.rulesFile, this.rules);
        this.loadMap(this.categoriesFile, this.categories);
        this.loadMap(this.goalsFile, this.goals);
        if (fs.existsSync(this.gamificationFile)) {
            try {
                this.gamification = JSON.parse(fs.readFileSync(this.gamificationFile, 'utf-8'));
            }
            catch (e) { }
        }
        if (fs.existsSync(this.configFile)) {
            try {
                this.config = JSON.parse(fs.readFileSync(this.configFile, 'utf-8'));
            }
            catch (e) { }
        }
    }
    loadMap(filePath, map) {
        if (fs.existsSync(filePath)) {
            try {
                const items = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
                items.forEach(item => {
                    const key = item.id || item.mainCategory;
                    if (key)
                        map.set(key, item);
                });
            }
            catch (e) { }
        }
    }
    saveFile(filePath, data) {
        try {
            fs.writeFileSync(filePath, JSON.stringify(data, null, 2), 'utf-8');
        }
        catch (e) {
            console.error(`Error saving ${filePath}:`, e);
        }
        // Mirror the write to shared Firestore (if sync is configured).
        // Skip when applying remote changes (silentPush) to avoid an echo loop.
        if (!this.silentPush) {
            this.firestoreBridge?.pushFromFile(filePath, data).catch((e) => console.error('firestore push error', e));
        }
    }
    seedDefaultCategoriesIfEmpty() {
        if (this.categories.size === 0) {
            const defaults = [
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
    getTransactions() {
        return Array.from(this.transactions.values()).sort((a, b) => b.postedEpochSeconds - a.postedEpochSeconds);
    }
    getTransaction(id) {
        return this.transactions.get(id);
    }
    saveTransaction(tx) {
        this.transactions.set(tx.id, tx);
        this.saveFile(this.transactionsFile, Array.from(this.transactions.values()));
    }
    saveTransactions(txs) {
        txs.forEach(t => this.transactions.set(t.id, t));
        this.saveFile(this.transactionsFile, Array.from(this.transactions.values()));
    }
    // --- Budgets ---
    getBudgets() {
        return Array.from(this.budgets.values());
    }
    saveBudget(budget) {
        this.budgets.set(budget.id, budget);
        this.saveFile(this.budgetsFile, Array.from(this.budgets.values()));
    }
    deleteBudget(id) {
        this.budgets.delete(id);
        this.saveFile(this.budgetsFile, Array.from(this.budgets.values()));
    }
    // --- Rules ---
    getRules() {
        return Array.from(this.rules.values()).sort((a, b) => a.priority - b.priority);
    }
    saveRule(rule) {
        this.rules.set(rule.id, rule);
        this.saveFile(this.rulesFile, Array.from(this.rules.values()));
    }
    saveRules(rules) {
        this.rules.clear();
        rules.forEach(r => this.rules.set(r.id, r));
        this.saveFile(this.rulesFile, rules);
    }
    deleteRule(id) {
        this.rules.delete(id);
        this.saveFile(this.rulesFile, Array.from(this.rules.values()));
    }
    // --- Categories ---
    getCategories() {
        return Array.from(this.categories.values());
    }
    saveCategories() {
        this.saveFile(this.categoriesFile, Array.from(this.categories.values()));
    }
    addOrUpdateCategory(mainCategory, subCategory) {
        const cleanMain = mainCategory.trim();
        if (!cleanMain)
            return;
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
    getGoals() {
        return Array.from(this.goals.values());
    }
    saveGoal(goal) {
        this.goals.set(goal.id, goal);
        this.saveFile(this.goalsFile, Array.from(this.goals.values()));
    }
    // --- Gamification ---
    getGamification() {
        return this.gamification;
    }
    updateGamification(updater) {
        this.gamification = updater(this.gamification);
        this.saveFile(this.gamificationFile, this.gamification);
        return this.gamification;
    }
    // --- Config ---
    getConfig() {
        return this.config;
    }
    saveConfig(config) {
        this.config = config;
        this.saveFile(this.configFile, this.config);
    }
}
