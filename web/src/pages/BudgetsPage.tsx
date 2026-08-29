import React, { useState, useEffect } from 'react';
import { PieChart, Target, FolderTree, Zap, Plus, RefreshCw, RotateCcw, Edit2, Trash2, CheckCircle2 } from 'lucide-react';
import { Budget, Goal, Rule, CategoryHierarchy, Transaction } from '../types';
import { DuolingoButton } from '../components/DuolingoButton';
import { ExpressiveCard } from '../components/ExpressiveCard';
import { appState } from '../services/storage';
import { sound } from '../services/sound';

export const BudgetsPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'budgets' | 'goals' | 'categories' | 'rules'>('budgets');

  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [goals, setGoals] = useState<Goal[]>([]);
  const [categories, setCategories] = useState<CategoryHierarchy[]>([]);
  const [rules, setRules] = useState<Rule[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);

  // Modals
  const [showBudgetModal, setShowBudgetModal] = useState(false);
  const [editingBudget, setEditingBudget] = useState<Partial<Budget>>({
    category: 'Food & Dining',
    subCategory: 'Groceries',
    categoryType: 'FIXED',
    targetAmount: 400,
    rolloverEnabled: true
  });

  const [showGoalModal, setShowGoalModal] = useState(false);
  const [editingGoal, setEditingGoal] = useState<Partial<Goal>>({
    title: '',
    targetAmount: 1000,
    currentAmount: 0
  });

  const [showCategoryModal, setShowCategoryModal] = useState(false);
  const [newMainCategory, setNewMainCategory] = useState('');
  const [newSubCategory, setNewSubCategory] = useState('');

  const [showRuleModal, setShowRuleModal] = useState(false);
  const [newRulePattern, setNewRulePattern] = useState('');
  const [newRuleCategory, setNewRuleCategory] = useState('Food & Dining');
  const [newRuleSubCategory, setNewRuleSubCategory] = useState('');

  const [ruleBannerMessage, setRuleBannerMessage] = useState<string | null>(null);

  useEffect(() => {
    const update = () => {
      setBudgets(appState.getBudgets());
      setGoals(appState.getGoals());
      setCategories(appState.getCategories());
      setRules(appState.getRules());
      setTransactions(appState.getTransactions());
    };
    update();
    return appState.subscribe(update);
  }, []);

  // Calculate MTD income
  const totalMtdIncome = transactions
    .filter(t => t.category.toLowerCase() === 'income' || t.amount > 0)
    .reduce((sum, t) => sum + Math.abs(t.amount), 0);

  const handleSaveBudget = () => {
    if (!editingBudget.category || !editingBudget.targetAmount) return;
    appState.saveBudget(editingBudget as Budget);
    sound.playLevelUpFanfare();
    setShowBudgetModal(false);
  };

  const handleResetRollover = (budgetId: string) => {
    appState.resetMonthRollover(budgetId);
    sound.playButtonPress();
  };

  const handleRunAllRules = () => {
    const count = appState.applyAllRules();
    sound.playLevelUpFanfare();
    setRuleBannerMessage(`⚡ Successfully evaluated rules! Updated ${count} transactions.`);
    setTimeout(() => setRuleBannerMessage(null), 4000);
  };

  return (
    <div className="pb-24 pt-4 px-4 max-w-2xl mx-auto">
      {/* 4-Tab Bar */}
      <div className="bg-[#1B2A32] p-1 rounded-2xl flex items-center justify-between gap-1 mb-6 border border-[#2E3C42]">
        <button
          onClick={() => { sound.playButtonPress(); setActiveTab('budgets'); }}
          className={`flex-1 py-2 rounded-xl text-xs font-black transition flex items-center justify-center gap-1.5 ${
            activeTab === 'budgets' ? 'bg-[#58CC02] text-black shadow' : 'text-gray-400 hover:text-white'
          }`}
        >
          <PieChart className="w-3.5 h-3.5" />
          Budgets
        </button>
        <button
          onClick={() => { sound.playButtonPress(); setActiveTab('goals'); }}
          className={`flex-1 py-2 rounded-xl text-xs font-black transition flex items-center justify-center gap-1.5 ${
            activeTab === 'goals' ? 'bg-[#58CC02] text-black shadow' : 'text-gray-400 hover:text-white'
          }`}
        >
          <Target className="w-3.5 h-3.5" />
          Goals ({goals.length})
        </button>
        <button
          onClick={() => { sound.playButtonPress(); setActiveTab('categories'); }}
          className={`flex-1 py-2 rounded-xl text-xs font-black transition flex items-center justify-center gap-1.5 ${
            activeTab === 'categories' ? 'bg-[#58CC02] text-black shadow' : 'text-gray-400 hover:text-white'
          }`}
        >
          <FolderTree className="w-3.5 h-3.5" />
          Categories
        </button>
        <button
          onClick={() => { sound.playButtonPress(); setActiveTab('rules'); }}
          className={`flex-1 py-2 rounded-xl text-xs font-black transition flex items-center justify-center gap-1.5 ${
            activeTab === 'rules' ? 'bg-[#58CC02] text-black shadow' : 'text-gray-400 hover:text-white'
          }`}
        >
          <Zap className="w-3.5 h-3.5" />
          Rules ({rules.length})
        </button>
      </div>

      {/* Rule Execution Banner */}
      {ruleBannerMessage && (
        <div className="bg-[#1CB0F6]/20 border border-[#1CB0F6]/40 p-3 rounded-2xl text-xs font-bold text-[#1CB0F6] mb-4 flex items-center justify-between">
          <span>{ruleBannerMessage}</span>
          <button onClick={() => setRuleBannerMessage(null)} className="text-[#1CB0F6] hover:text-white">✕</button>
        </div>
      )}

      {/* TAB 1: BUDGETS */}
      {activeTab === 'budgets' && (
        <div className="flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-black text-lg text-white">Monthly Envelopes</h3>
              <p className="text-xs font-bold text-gray-400">Rollovers automatically protect unspent savings</p>
            </div>
            <DuolingoButton size="sm" variant="green" onClick={() => {
              setEditingBudget({ category: 'Food & Dining', subCategory: '', categoryType: 'FIXED', targetAmount: 250, rolloverEnabled: true });
              setShowBudgetModal(true);
            }}>
              <Plus className="w-4 h-4 mr-1" /> Add Budget
            </DuolingoButton>
          </div>

          <div className="flex flex-col gap-3">
            {budgets.map((b) => {
              // Calculate spent
              const spent = transactions
                .filter(t => t.category.toLowerCase() === b.category.toLowerCase() && (!b.subCategory || (t.subCategory || '').toLowerCase() === b.subCategory.toLowerCase()))
                .reduce((sum, t) => sum + Math.abs(t.amount), 0);

              const percent = b.targetAmount > 0 ? (spent / b.targetAmount) * 100 : 0;
              const isOver = spent > b.targetAmount;

              return (
                <ExpressiveCard key={b.id} accent={isOver ? 'red' : 'none'} className="p-4">
                  <div className="flex items-start justify-between mb-2">
                    <div>
                      <h4 className="font-black text-white text-sm">
                        {b.category} {b.subCategory && <span className="text-gray-400">› {b.subCategory}</span>}
                      </h4>
                      <div className="flex items-center gap-2 mt-1">
                        <span className="text-[11px] font-bold px-2 py-0.5 rounded-md bg-[#202F36] text-gray-300">
                          {b.categoryType === 'PERCENT_INCOME' ? `${b.incomePercentage}% of Income` : 'Fixed Budget'}
                        </span>
                        {b.rolloverEnabled && (
                          <span className="text-[11px] font-bold px-2 py-0.5 rounded-md bg-[#58CC02]/20 text-[#58CC02]">
                            🔄 Rollover On
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="text-right">
                      <span className={`text-base font-black ${isOver ? 'text-[#FF4B4B]' : 'text-white'}`}>
                        ${spent.toFixed(2)}
                      </span>
                      <span className="text-xs text-gray-400 font-bold block">
                        of ${b.targetAmount.toFixed(2)}
                      </span>
                    </div>
                  </div>

                  {/* Progress Bar */}
                  <div className="w-full h-2.5 rounded-full bg-[#131F24] overflow-hidden mb-3">
                    <div
                      className={`h-full rounded-full transition-all duration-300 ${
                        isOver ? 'bg-[#FF4B4B]' : percent >= 80 ? 'bg-[#FFC800]' : 'bg-[#58CC02]'
                      }`}
                      style={{ width: `${Math.min(100, Math.max(2, percent))}%` }}
                    />
                  </div>

                  <div className="flex items-center justify-between text-xs font-bold pt-1 border-t border-[#202F36]">
                    <span className={isOver ? 'text-[#FF4B4B]' : 'text-gray-400'}>
                      {isOver ? `$${(spent - b.targetAmount).toFixed(2)} Over Budget` : `$${(b.targetAmount - spent).toFixed(2)} remaining`}
                    </span>

                    <div className="flex items-center gap-2">
                      {b.rolloverEnabled && (
                        <button
                          onClick={() => handleResetRollover(b.id)}
                          title="Reset rollover buffer to $0 for this month"
                          className="text-[11px] text-gray-400 hover:text-white flex items-center gap-1 font-bold"
                        >
                          <RotateCcw className="w-3 h-3" /> Reset
                        </button>
                      )}
                      <button
                        onClick={() => {
                          setEditingBudget(b);
                          setShowBudgetModal(true);
                        }}
                        className="text-[11px] text-[#1CB0F6] hover:underline font-bold"
                      >
                        Edit
                      </button>
                    </div>
                  </div>
                </ExpressiveCard>
              );
            })}
          </div>
        </div>
      )}

      {/* TAB 2: GOALS */}
      {activeTab === 'goals' && (
        <div className="flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-black text-lg text-white">Savings Targets</h3>
              <p className="text-xs font-bold text-gray-400">Gamified milestone vaults</p>
            </div>
            <DuolingoButton size="sm" variant="gold" onClick={() => {
              setEditingGoal({ title: '', targetAmount: 2000, currentAmount: 500 });
              setShowGoalModal(true);
            }}>
              <Plus className="w-4 h-4 mr-1" /> New Goal
            </DuolingoButton>
          </div>

          <div className="flex flex-col gap-3">
            {goals.map((g) => {
              const progress = g.targetAmount > 0 ? (g.currentAmount / g.targetAmount) * 100 : 0;
              return (
                <ExpressiveCard key={g.id} className="p-4">
                  <div className="flex items-start justify-between mb-2">
                    <div>
                      <h4 className="font-black text-white text-base">{g.title}</h4>
                      <span className="text-xs font-bold text-gray-400">
                        ${g.currentAmount.toFixed(2)} saved
                      </span>
                    </div>
                    <span className="text-sm font-black text-[#FFC800]">
                      ${g.targetAmount.toFixed(2)}
                    </span>
                  </div>

                  <div className="w-full h-3 rounded-full bg-[#131F24] overflow-hidden mb-2">
                    <div
                      className="h-full bg-[#FFC800] rounded-full transition-all duration-300"
                      style={{ width: `${Math.min(100, progress)}%` }}
                    />
                  </div>

                  <div className="flex justify-between text-xs font-black text-gray-400">
                    <span>{progress.toFixed(0)}% Complete</span>
                    <span className="text-[#58CC02]">${(g.targetAmount - g.currentAmount).toFixed(2)} to go</span>
                  </div>
                </ExpressiveCard>
              );
            })}
          </div>
        </div>
      )}

      {/* TAB 3: CATEGORIES */}
      {activeTab === 'categories' && (
        <div className="flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-black text-lg text-white">Categories & Subcategories</h3>
              <p className="text-xs font-bold text-gray-400">Organize your financial tax & budget buckets</p>
            </div>
            <DuolingoButton size="sm" variant="green" onClick={() => setShowCategoryModal(true)}>
              <Plus className="w-4 h-4 mr-1" /> New Category
            </DuolingoButton>
          </div>

          <div className="flex flex-col gap-3">
            {categories.map((c) => (
              <ExpressiveCard key={c.mainCategory} className="p-4">
                <h4 className="font-black text-white text-sm mb-2">{c.mainCategory}</h4>
                <div className="flex flex-wrap gap-1.5">
                  {c.subCategories.map((s) => (
                    <span key={s} className="px-2.5 py-1 rounded-xl bg-[#131F24] text-gray-300 text-xs font-bold border border-[#2E3C42]">
                      {s}
                    </span>
                  ))}
                </div>
              </ExpressiveCard>
            ))}
          </div>
        </div>
      )}

      {/* TAB 4: RULES */}
      {activeTab === 'rules' && (
        <div className="flex flex-col gap-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-black text-lg text-white">Automated Rules</h3>
              <p className="text-xs font-bold text-gray-400">Evaluates patterns on import and on-demand</p>
            </div>
            <div className="flex items-center gap-2">
              <DuolingoButton size="sm" variant="blue" onClick={handleRunAllRules}>
                <RefreshCw className="w-3.5 h-3.5 mr-1" /> Run All
              </DuolingoButton>
              <DuolingoButton size="sm" variant="green" onClick={() => setShowRuleModal(true)}>
                <Plus className="w-3.5 h-3.5 mr-1" /> Add Rule
              </DuolingoButton>
            </div>
          </div>

          <div className="flex flex-col gap-3">
            {rules.map((r) => (
              <ExpressiveCard key={r.id} className="p-4 flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-black text-[#1CB0F6]">#{r.priority}</span>
                    <h4 className="font-black text-white text-sm">"{r.pattern}"</h4>
                  </div>
                  <span className="text-xs font-bold text-gray-400 block mt-0.5">
                    → {r.category} {r.subCategory && `› ${r.subCategory}`}
                  </span>
                </div>

                <button
                  onClick={() => {
                    appState.deleteRule(r.id);
                    sound.playButtonPress();
                  }}
                  className="p-2 text-gray-500 hover:text-[#FF4B4B] transition"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </ExpressiveCard>
            ))}
          </div>
        </div>
      )}

      {/* Create / Edit Budget Modal */}
      {showBudgetModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-fade-in">
          <div className="bg-[#1B2A32] border-2 border-b-4 border-[#2E3C42] rounded-3xl p-6 max-w-md w-full shadow-2xl">
            <h3 className="text-lg font-black text-white mb-4">Set Budget Envelope</h3>

            <div className="flex flex-col gap-3 mb-5">
              <div>
                <label className="text-xs font-bold text-gray-400 block mb-1">Category</label>
                <select
                  value={editingBudget.category}
                  onChange={(e) => setEditingBudget({ ...editingBudget, category: e.target.value })}
                  className="w-full bg-[#131F24] border border-[#2E3C42] text-xs font-bold text-white p-2.5 rounded-xl outline-none"
                >
                  {categories.map(c => <option key={c.mainCategory} value={c.mainCategory}>{c.mainCategory}</option>)}
                </select>
              </div>

              <div>
                <label className="text-xs font-bold text-gray-400 block mb-1">Subcategory (optional)</label>
                <input
                  type="text"
                  placeholder="e.g. Groceries"
                  value={editingBudget.subCategory || ''}
                  onChange={(e) => setEditingBudget({ ...editingBudget, subCategory: e.target.value })}
                  className="w-full bg-[#131F24] border border-[#2E3C42] text-xs font-bold text-white p-2.5 rounded-xl outline-none"
                />
              </div>

              <div>
                <label className="text-xs font-bold text-gray-400 block mb-1">Target Monthly Limit ($)</label>
                <input
                  type="number"
                  placeholder="400"
                  value={editingBudget.targetAmount || ''}
                  onChange={(e) => setEditingBudget({ ...editingBudget, targetAmount: parseFloat(e.target.value) || 0 })}
                  className="w-full bg-[#131F24] border border-[#2E3C42] text-xs font-bold text-white p-2.5 rounded-xl outline-none"
                />
              </div>

              <label className="flex items-center gap-2 cursor-pointer mt-1">
                <input
                  type="checkbox"
                  checked={editingBudget.rolloverEnabled}
                  onChange={(e) => setEditingBudget({ ...editingBudget, rolloverEnabled: e.target.checked })}
                  className="w-4 h-4 rounded accent-[#58CC02]"
                />
                <span className="text-xs font-bold text-gray-300">Enable Envelope Rollover (carry over unspent funds)</span>
              </label>
            </div>

            <div className="flex flex-col gap-2">
              <DuolingoButton variant="green" fullWidth onClick={handleSaveBudget}>
                💾 Save Budget
              </DuolingoButton>
              <DuolingoButton variant="outline" fullWidth onClick={() => setShowBudgetModal(false)}>
                Cancel
              </DuolingoButton>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
