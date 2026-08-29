import React, { useState, useEffect } from 'react';
import { Check, Flame, Sparkles, Split, ArrowRight, Tag, Search, Plus } from 'lucide-react';
import { Transaction, CategoryHierarchy } from '../types';
import { DuolingoButton } from '../components/DuolingoButton';
import { ExpressiveCard } from '../components/ExpressiveCard';
import { appState } from '../services/storage';
import { sound } from '../services/sound';

interface ActionQueuePageProps {
  onNavigateToHistory: () => void;
}

export const ActionQueuePage: React.FC<ActionQueuePageProps> = ({
  onNavigateToHistory
}) => {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [categories, setCategories] = useState<CategoryHierarchy[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);

  const [selectedMain, setSelectedMain] = useState('');
  const [selectedSub, setSelectedSub] = useState('');
  const [notes, setNotes] = useState('');
  const [createAutoRule, setCreateAutoRule] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [comboCount, setComboCount] = useState(1);

  // Split Dialog state
  const [showSplitModal, setShowSplitModal] = useState(false);
  const [splitItems, setSplitItems] = useState<{ category: string; subCategory: string; amount: string }[]>([
    { category: 'Food & Dining', subCategory: 'Groceries', amount: '' },
    { category: 'Personal & Lifestyle', subCategory: 'Shopping', amount: '' }
  ]);

  useEffect(() => {
    const update = () => {
      const all = appState.getTransactions();
      const uncategorized = all.filter(t => t.category.toLowerCase() === 'uncategorized' || !t.category);
      setTransactions(uncategorized);
      setCategories(appState.getCategories());
    };
    update();
    return appState.subscribe(update);
  }, []);

  const currentTx = transactions[currentIndex];

  useEffect(() => {
    if (currentTx) {
      // Auto-suggest category if payee matches known pattern
      setSearchQuery('');
      setNotes(currentTx.notes || '');
      setCreateAutoRule(false);

      if (currentTx.payee.toLowerCase().includes('kroger') || currentTx.originalDesc.toLowerCase().includes('walmart')) {
        setSelectedMain('Food & Dining');
        setSelectedSub('Groceries');
      } else if (currentTx.payee.toLowerCase().includes('shell') || currentTx.originalDesc.toLowerCase().includes('fuel')) {
        setSelectedMain('Transportation');
        setSelectedSub('Fuel');
      } else {
        setSelectedMain(categories[0]?.mainCategory || 'Food & Dining');
        setSelectedSub(categories[0]?.subCategories[0] || '');
      }
    }
  }, [currentTx, categories]);

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;
      if (e.code === 'Space') {
        e.preventDefault();
        handleConfirm();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [currentTx, selectedMain, selectedSub, notes, createAutoRule, comboCount]);

  const handleConfirm = () => {
    if (!currentTx || !selectedMain) return;

    appState.categorizeTransaction(currentTx.id, selectedMain, selectedSub, notes, createAutoRule);
    sound.playComboChime(comboCount);
    setComboCount(prev => Math.min(10, prev + 1));
  };

  const handleConfirmSplit = () => {
    if (!currentTx) return;
    const parsedSplits = splitItems.map(s => ({
      category: s.category,
      subCategory: s.subCategory,
      amount: parseFloat(s.amount) || 0
    }));
    appState.splitTransaction(currentTx.id, parsedSplits);
    sound.playLevelUpFanfare();
    setShowSplitModal(false);
  };

  // Quick Filter categories based on search input
  const filteredCategories = categories.filter(c => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return c.mainCategory.toLowerCase().includes(q) || c.subCategories.some(s => s.toLowerCase().includes(q));
  });

  if (!currentTx || transactions.length === 0) {
    return (
      <div className="min-h-[75vh] flex flex-col items-center justify-center p-6 text-center max-w-md mx-auto">
        <div className="w-24 h-24 rounded-full bg-[#58CC02]/20 flex items-center justify-center text-5xl mb-4 border-2 border-[#58CC02]/40 shadow-xl">
          🎉
        </div>
        <h2 className="text-2xl font-black text-white">Inbox Zero!</h2>
        <p className="text-sm font-bold text-gray-400 mt-2 mb-6">
          All imported transactions have been reviewed and categorized. Your financial streak is safe!
        </p>
        <DuolingoButton variant="green" onClick={onNavigateToHistory}>
          📜 View Transaction History
        </DuolingoButton>
      </div>
    );
  }

  return (
    <div className="pb-24 pt-4 px-4 max-w-xl mx-auto">
      {/* Top Header & Combo Meter */}
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="text-xl font-black text-white flex items-center gap-2">
            <span>Action Queue</span>
            <span className="text-xs px-2.5 py-0.5 rounded-full bg-[#1CB0F6]/20 text-[#1CB0F6] font-bold">
              {transactions.length} Remaining
            </span>
          </h2>
          <p className="text-xs font-bold text-gray-400">Press Spacebar or tap Confirm to review</p>
        </div>

        {comboCount > 1 && (
          <div className="flex items-center gap-1.5 px-3 py-1 rounded-2xl bg-[#FF9600]/20 border border-[#FF9600]/40 animate-pulse">
            <Flame className="w-4 h-4 text-[#FF9600]" fill="#FF9600" />
            <span className="font-black text-sm text-[#FF9600]">{comboCount}x Combo!</span>
          </div>
        )}
      </div>

      {/* Main Review Card */}
      <ExpressiveCard accent="green" className="p-5 mb-5 shadow-2xl">
        {/* Merchant & Amount */}
        <div className="flex items-start justify-between mb-4">
          <div>
            <span className="text-xs font-black text-gray-400 uppercase tracking-wider">
              {new Date(currentTx.postedEpochSeconds * 1000).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
            </span>
            <h3 className="text-lg font-black text-white mt-0.5 leading-tight">
              {currentTx.payee || currentTx.originalDesc}
            </h3>
            <p className="text-xs text-gray-400 font-bold truncate max-w-[280px]">
              {currentTx.originalDesc}
            </p>
          </div>

          <div className="text-right">
            <span className="text-2xl font-black text-white">
              ${Math.abs(currentTx.amount).toFixed(2)}
            </span>
            {currentTx.pending && (
              <span className="block text-[10px] font-black text-[#FFC800] uppercase">
                Pending
              </span>
            )}
          </div>
        </div>

        {/* Quick Search */}
        <div className="relative mb-3">
          <Search className="w-4 h-4 absolute left-3 top-3 text-gray-400" />
          <input
            type="text"
            placeholder="Type 'fu' for Fuel, 'groc' for Groceries..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-[#131F24] border border-[#2E3C42] focus:border-[#58CC02] rounded-xl pl-9 pr-3 py-2 text-xs font-bold text-white placeholder-gray-500 outline-none"
          />
        </div>

        {/* Category Pickers */}
        <div className="flex flex-col gap-2 mb-4">
          <label className="text-xs font-black text-gray-300">Select Category:</label>
          <div className="flex flex-wrap gap-2 max-h-36 overflow-y-auto pr-1">
            {filteredCategories.map((c) => {
              const isSelected = selectedMain === c.mainCategory;
              return (
                <button
                  key={c.mainCategory}
                  onClick={() => {
                    sound.playButtonPress();
                    setSelectedMain(c.mainCategory);
                    setSelectedSub(c.subCategories[0] || '');
                  }}
                  className={`px-3 py-1.5 rounded-xl text-xs font-extrabold transition border ${
                    isSelected
                      ? 'bg-[#58CC02] text-black border-[#46A302] shadow-sm'
                      : 'bg-[#131F24] text-gray-300 border-[#2E3C42] hover:border-[#3E4F57]'
                  }`}
                >
                  {c.mainCategory}
                </button>
              );
            })}
          </div>

          {/* Subcategories (if selected main has any) */}
          {selectedMain && (
            <div className="mt-2">
              <span className="text-[11px] font-black text-gray-400 block mb-1">Subcategory:</span>
              <div className="flex flex-wrap gap-1.5">
                {categories.find(c => c.mainCategory === selectedMain)?.subCategories.map(s => {
                  const isSubSelected = selectedSub === s;
                  return (
                    <button
                      key={s}
                      onClick={() => {
                        sound.playButtonPress();
                        setSelectedSub(s);
                      }}
                      className={`px-2.5 py-1 rounded-lg text-xs font-bold transition border ${
                        isSubSelected
                          ? 'bg-[#1CB0F6] text-white border-[#1899D6]'
                          : 'bg-[#131F24] text-gray-400 border-[#202F36]'
                      }`}
                    >
                      {s}
                    </button>
                  );
                })}
              </div>
            </div>
          )}
        </div>

        {/* Optional Notes Input */}
        <div className="mb-4">
          <input
            type="text"
            placeholder="Add itemized note (+10 XP bonus)..."
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            className="w-full bg-[#131F24] border border-[#2E3C42] rounded-xl px-3 py-2 text-xs font-bold text-white placeholder-gray-500 outline-none focus:border-[#1CB0F6]"
          />
        </div>

        {/* Auto Rule Generator Toggle */}
        <label className="flex items-center gap-2.5 cursor-pointer bg-[#131F24] p-3 rounded-xl border border-[#2E3C42] mb-5">
          <input
            type="checkbox"
            checked={createAutoRule}
            onChange={(e) => setCreateAutoRule(e.target.checked)}
            className="w-4 h-4 rounded accent-[#58CC02] cursor-pointer"
          />
          <div className="text-xs">
            <span className="font-extrabold text-white block">
              ⚡ Create Auto-Rule for "{currentTx.payee || currentTx.originalDesc.split(' ')[0]}"
            </span>
            <span className="text-gray-400 font-medium">Automatically categorize all matching past & future charges (+25 XP)</span>
          </div>
        </label>

        {/* Bottom Actions */}
        <div className="flex items-center gap-3">
          <DuolingoButton variant="green" fullWidth size="lg" onClick={handleConfirm}>
            <Check className="w-5 h-5 mr-2 stroke-[3]" />
            Confirm (+15 XP)
          </DuolingoButton>

          <DuolingoButton
            variant="outline"
            onClick={() => {
              setSplitItems([
                { category: selectedMain || 'Food & Dining', subCategory: selectedSub || '', amount: (Math.abs(currentTx.amount) / 2).toFixed(2) },
                { category: 'Personal & Lifestyle', subCategory: 'Shopping', amount: (Math.abs(currentTx.amount) / 2).toFixed(2) }
              ]);
              setShowSplitModal(true);
            }}
            title="Split Transaction"
          >
            <Split className="w-5 h-5" />
          </DuolingoButton>
        </div>
      </ExpressiveCard>

      {/* Split Transaction Modal */}
      {showSplitModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-fade-in">
          <div className="bg-[#1B2A32] border-2 border-b-4 border-[#2E3C42] rounded-3xl p-6 max-w-md w-full shadow-2xl">
            <h3 className="text-lg font-black text-white mb-1 flex items-center gap-2">
              <Split className="w-5 h-5 text-[#1CB0F6]" />
              Quick Split Transaction
            </h3>
            <p className="text-xs text-gray-400 font-bold mb-4">
              Total to allocate: ${Math.abs(currentTx.amount).toFixed(2)}
            </p>

            <div className="flex flex-col gap-3 mb-5 max-h-64 overflow-y-auto">
              {splitItems.map((item, idx) => (
                <div key={idx} className="bg-[#131F24] p-3 rounded-2xl border border-[#2E3C42] flex items-center gap-2">
                  <div className="flex-1">
                    <select
                      value={item.category}
                      onChange={(e) => {
                        const next = [...splitItems];
                        next[idx].category = e.target.value;
                        setSplitItems(next);
                      }}
                      className="w-full bg-[#1B2A32] text-xs font-bold text-white rounded-lg p-1.5 outline-none mb-1 border border-[#2E3C42]"
                    >
                      {categories.map(c => <option key={c.mainCategory} value={c.mainCategory}>{c.mainCategory}</option>)}
                    </select>
                    <input
                      type="text"
                      placeholder="Subcategory (optional)"
                      value={item.subCategory}
                      onChange={(e) => {
                        const next = [...splitItems];
                        next[idx].subCategory = e.target.value;
                        setSplitItems(next);
                      }}
                      className="w-full bg-transparent text-xs text-gray-300 px-1 outline-none"
                    />
                  </div>
                  <div className="w-24">
                    <input
                      type="number"
                      placeholder="0.00"
                      value={item.amount}
                      onChange={(e) => {
                        const next = [...splitItems];
                        next[idx].amount = e.target.value;
                        setSplitItems(next);
                      }}
                      className="w-full bg-[#1B2A32] border border-[#2E3C42] text-xs font-black text-white p-2 rounded-xl text-right outline-none focus:border-[#1CB0F6]"
                    />
                  </div>
                </div>
              ))}
            </div>

            <div className="flex flex-col gap-2">
              <DuolingoButton variant="blue" fullWidth onClick={handleConfirmSplit}>
                💾 Apply Split Allocations
              </DuolingoButton>
              <DuolingoButton variant="outline" fullWidth onClick={() => setShowSplitModal(false)}>
                Cancel
              </DuolingoButton>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
