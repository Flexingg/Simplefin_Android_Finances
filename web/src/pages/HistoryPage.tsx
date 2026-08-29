import React, { useState, useEffect } from 'react';
import { Search, Filter, Split, ShoppingBag, ExternalLink, Calendar, ArrowDownRight, ArrowUpRight } from 'lucide-react';
import { Transaction, CategoryHierarchy } from '../types';
import { ExpressiveCard } from '../components/ExpressiveCard';
import { DuolingoButton } from '../components/DuolingoButton';
import { appState } from '../services/storage';
import { sound } from '../services/sound';

export const HistoryPage: React.FC = () => {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [categories, setCategories] = useState<CategoryHierarchy[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategoryFilter, setSelectedCategoryFilter] = useState('ALL');

  useEffect(() => {
    const update = () => {
      setTransactions(appState.getTransactions());
      setCategories(appState.getCategories());
    };
    update();
    return appState.subscribe(update);
  }, []);

  const filtered = transactions.filter(t => {
    if (selectedCategoryFilter !== 'ALL' && t.category.toLowerCase() !== selectedCategoryFilter.toLowerCase()) {
      return false;
    }
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      return (
        t.originalDesc.toLowerCase().includes(q) ||
        t.payee.toLowerCase().includes(q) ||
        (t.notes || '').toLowerCase().includes(q) ||
        t.category.toLowerCase().includes(q)
      );
    }
    return true;
  });

  return (
    <div className="pb-24 pt-4 px-4 max-w-2xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="text-xl font-black text-white">Transaction History</h2>
          <p className="text-xs font-bold text-gray-400">{filtered.length} transactions found</p>
        </div>
      </div>

      {/* Search Input */}
      <div className="relative mb-3">
        <Search className="w-4 h-4 absolute left-3 top-3 text-gray-400" />
        <input
          type="text"
          placeholder="Search by merchant, note, or amount..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full bg-[#1B2A32] border border-[#2E3C42] focus:border-[#58CC02] rounded-2xl pl-9 pr-4 py-2.5 text-xs font-bold text-white placeholder-gray-500 outline-none shadow-sm"
        />
      </div>

      {/* Category Filter Pills */}
      <div className="flex items-center gap-1.5 overflow-x-auto pb-2 mb-4 scrollbar-none">
        <button
          onClick={() => { sound.playButtonPress(); setSelectedCategoryFilter('ALL'); }}
          className={`px-3 py-1.5 rounded-xl text-xs font-black whitespace-nowrap transition border ${
            selectedCategoryFilter === 'ALL'
              ? 'bg-[#58CC02] text-black border-[#46A302]'
              : 'bg-[#1B2A32] text-gray-400 border-[#2E3C42] hover:text-white'
          }`}
        >
          All Categories
        </button>
        {categories.map(c => (
          <button
            key={c.mainCategory}
            onClick={() => { sound.playButtonPress(); setSelectedCategoryFilter(c.mainCategory); }}
            className={`px-3 py-1.5 rounded-xl text-xs font-black whitespace-nowrap transition border ${
              selectedCategoryFilter === c.mainCategory
                ? 'bg-[#58CC02] text-black border-[#46A302]'
                : 'bg-[#1B2A32] text-gray-400 border-[#2E3C42] hover:text-white'
            }`}
          >
            {c.mainCategory}
          </button>
        ))}
      </div>

      {/* Transactions List */}
      <div className="flex flex-col gap-2.5">
        {filtered.map((t) => {
          const isIncome = t.amount > 0 || t.category.toLowerCase() === 'income';
          const isAmazon = t.originalDesc.toLowerCase().includes('amzn') || t.originalDesc.toLowerCase().includes('amazon');

          return (
            <ExpressiveCard key={t.id} className="p-4">
              <div className="flex items-start justify-between">
                <div className="flex items-start gap-3">
                  <div
                    className={`w-10 h-10 rounded-2xl flex items-center justify-center font-black ${
                      isIncome ? 'bg-[#58CC02]/20 text-[#58CC02]' : 'bg-[#131F24] text-gray-300'
                    }`}
                  >
                    {isIncome ? <ArrowDownRight className="w-5 h-5" /> : <ArrowUpRight className="w-5 h-5 text-[#FF4B4B]" />}
                  </div>

                  <div>
                    <h4 className="font-black text-white text-sm leading-snug">
                      {t.payee || t.originalDesc}
                    </h4>
                    <span className="text-[11px] font-bold text-gray-400 block mt-0.5">
                      {new Date(t.postedEpochSeconds * 1000).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}
                    </span>

                    <div className="flex items-center gap-1.5 mt-1.5">
                      <span className="text-[10px] font-extrabold px-2 py-0.5 rounded-md bg-[#131F24] text-gray-300 border border-[#2E3C42]">
                        {t.category} {t.subCategory && `› ${t.subCategory}`}
                      </span>

                      {t.isSplit && (
                        <span className="text-[10px] font-black px-1.5 py-0.5 rounded-md bg-[#1CB0F6]/20 text-[#1CB0F6] flex items-center gap-1">
                          <Split className="w-3 h-3" /> Split
                        </span>
                      )}

                      {isAmazon && (
                        <a
                          href="https://www.amazon.com/gp/your-account/order-history"
                          target="_blank"
                          rel="noreferrer"
                          className="text-[10px] font-black px-2 py-0.5 rounded-md bg-[#FF9600]/20 text-[#FF9600] flex items-center gap-1 hover:underline"
                        >
                          <ShoppingBag className="w-3 h-3" /> Amazon Order
                        </a>
                      )}
                    </div>

                    {t.notes && (
                      <p className="text-[11px] text-gray-400 italic mt-1">
                        📝 {t.notes}
                      </p>
                    )}
                  </div>
                </div>

                <div className="text-right">
                  <span className={`text-base font-black ${isIncome ? 'text-[#58CC02]' : 'text-white'}`}>
                    {isIncome ? '+' : '-'}${Math.abs(t.amount).toFixed(2)}
                  </span>
                  {t.pending && (
                    <span className="block text-[10px] font-black text-[#FFC800] uppercase">
                      Pending
                    </span>
                  )}
                </div>
              </div>
            </ExpressiveCard>
          );
        })}
      </div>
    </div>
  );
};
