import React, { useState, useEffect } from 'react';
import { BarChart3, Calendar, DollarSign, ArrowUpRight, ArrowDownRight, TrendingUp, ShieldCheck } from 'lucide-react';
import { ExpressiveCard } from '../components/ExpressiveCard';
import { DuolingoButton } from '../components/DuolingoButton';
import { appState } from '../services/storage';
import { sound } from '../services/sound';

export const InsightsPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'trends' | 'heatmap' | 'networth'>('trends');
  const [extraDebtBudget, setExtraDebtBudget] = useState(250);

  const transactions = appState.getTransactions();

  // Calculate MTD numbers
  const totalIncome = transactions
    .filter(t => t.category.toLowerCase() === 'income' || t.amount > 0)
    .reduce((sum, t) => sum + Math.abs(t.amount), 0);

  const totalExpenses = transactions
    .filter(t => t.category.toLowerCase() !== 'income' && t.amount < 0)
    .reduce((sum, t) => sum + Math.abs(t.amount), 0);

  const netSavings = totalIncome - totalExpenses;
  const savingsRate = totalIncome > 0 ? ((netSavings / totalIncome) * 100).toFixed(1) : '0.0';

  // 6-Month Mock/Calculated Trends
  const trends = [
    { month: 'Mar', income: 4800, expenses: 3100, savings: 1700 },
    { month: 'Apr', income: 5100, expenses: 3400, savings: 1700 },
    { month: 'May', income: 4900, expenses: 2900, savings: 2000 },
    { month: 'Jun', income: 5200, expenses: 3300, savings: 1900 },
    { month: 'Jul', income: 5300, expenses: 3150, savings: 2150 },
    { month: 'Aug', income: totalIncome || 5400, expenses: totalExpenses || 2850, savings: netSavings || 2550 },
  ];

  // 90-Day Heatmap generation
  const heatmapDays = Array.from({ length: 84 }).map((_, idx) => {
    const dayAgo = 83 - idx;
    const date = new Date();
    date.setDate(date.getDate() - dayAgo);
    // Pseudo intensity for visual display
    const seed = (idx * 37) % 5;
    const spent = seed === 0 ? 0 : seed * 28 + (idx % 7) * 8;
    return {
      date: date.toISOString().split('T')[0],
      dayName: date.toLocaleDateString(undefined, { weekday: 'short' }),
      level: seed,
      spent
    };
  });

  return (
    <div className="pb-24 pt-4 px-4 max-w-2xl mx-auto">
      {/* Top 3-Tab Bar */}
      <div className="bg-[#1B2A32] p-1 rounded-2xl flex items-center justify-between gap-1 mb-6 border border-[#2E3C42]">
        <button
          onClick={() => { sound.playButtonPress(); setActiveTab('trends'); }}
          className={`flex-1 py-2 rounded-xl text-xs font-black transition flex items-center justify-center gap-1.5 ${
            activeTab === 'trends' ? 'bg-[#58CC02] text-black shadow' : 'text-gray-400 hover:text-white'
          }`}
        >
          <BarChart3 className="w-3.5 h-3.5" />
          Spending Trends
        </button>
        <button
          onClick={() => { sound.playButtonPress(); setActiveTab('heatmap'); }}
          className={`flex-1 py-2 rounded-xl text-xs font-black transition flex items-center justify-center gap-1.5 ${
            activeTab === 'heatmap' ? 'bg-[#58CC02] text-black shadow' : 'text-gray-400 hover:text-white'
          }`}
        >
          <Calendar className="w-3.5 h-3.5" />
          Heatmap (90D)
        </button>
        <button
          onClick={() => { sound.playButtonPress(); setActiveTab('networth'); }}
          className={`flex-1 py-2 rounded-xl text-xs font-black transition flex items-center justify-center gap-1.5 ${
            activeTab === 'networth' ? 'bg-[#58CC02] text-black shadow' : 'text-gray-400 hover:text-white'
          }`}
        >
          <DollarSign className="w-3.5 h-3.5" />
          Net Worth & Debt
        </button>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-2 gap-3 mb-6">
        <ExpressiveCard className="p-4">
          <div className="flex items-center gap-2 text-xs font-bold text-gray-400 mb-1">
            <ArrowDownRight className="w-4 h-4 text-[#58CC02]" /> Total Income
          </div>
          <span className="text-xl font-black text-[#58CC02]">${totalIncome.toFixed(2)}</span>
        </ExpressiveCard>

        <ExpressiveCard className="p-4">
          <div className="flex items-center gap-2 text-xs font-bold text-gray-400 mb-1">
            <ArrowUpRight className="w-4 h-4 text-[#FF4B4B]" /> Total Spent
          </div>
          <span className="text-xl font-black text-[#FF4B4B]">${totalExpenses.toFixed(2)}</span>
        </ExpressiveCard>
      </div>

      {/* TAB 1: SPENDING TRENDS */}
      {activeTab === 'trends' && (
        <div className="flex flex-col gap-4">
          <ExpressiveCard className="p-5">
            <h3 className="text-sm font-black text-white mb-1">6-Month Cash Flow & Savings</h3>
            <p className="text-xs font-bold text-gray-400 mb-6">Monthly Income vs Outflow</p>

            <div className="flex items-end justify-between h-48 pt-6 border-b border-[#202F36] pb-2">
              {trends.map((t) => {
                const max = 6000;
                const incomeH = (t.income / max) * 100;
                const expH = (t.expenses / max) * 100;

                return (
                  <div key={t.month} className="flex flex-col items-center gap-2 flex-1">
                    <div className="flex items-end gap-1.5 h-36">
                      <div
                        className="w-3.5 bg-[#58CC02] rounded-t-md transition-all duration-500"
                        style={{ height: `${incomeH}%` }}
                        title={`Income: $${t.income}`}
                      />
                      <div
                        className="w-3.5 bg-[#FF4B4B] rounded-t-md transition-all duration-500"
                        style={{ height: `${expH}%` }}
                        title={`Expenses: $${t.expenses}`}
                      />
                    </div>
                    <span className="text-[11px] font-bold text-gray-400">{t.month}</span>
                  </div>
                );
              })}
            </div>

            <div className="flex items-center justify-center gap-6 mt-4 text-xs font-bold">
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded bg-[#58CC02]" />
                <span className="text-gray-300">Income</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 rounded bg-[#FF4B4B]" />
                <span className="text-gray-300">Expenses</span>
              </div>
            </div>
          </ExpressiveCard>
        </div>
      )}

      {/* TAB 2: HEATMAP */}
      {activeTab === 'heatmap' && (
        <ExpressiveCard className="p-5">
          <h3 className="text-sm font-black text-white mb-1">Daily Spending Intensity (90 Days)</h3>
          <p className="text-xs font-bold text-gray-400 mb-4">Darker tiles represent higher spending days</p>

          <div className="grid grid-cols-12 gap-2 mb-4">
            {heatmapDays.map((d, idx) => {
              const colors = ['bg-[#131F24]', 'bg-[#58CC02]/30', 'bg-[#58CC02]/60', 'bg-[#FFC800]', 'bg-[#FF4B4B]'];
              return (
                <div
                  key={idx}
                  title={`${d.date}: $${d.spent.toFixed(2)}`}
                  className={`w-full aspect-square rounded-md border border-[#202F36] ${colors[d.level]} hover:scale-125 transition cursor-pointer`}
                />
              );
            })}
          </div>

          <div className="flex items-center justify-between text-xs font-bold text-gray-400 pt-3 border-t border-[#202F36]">
            <span>Less Spent</span>
            <div className="flex items-center gap-1.5">
              <div className="w-3 h-3 rounded bg-[#131F24] border border-[#202F36]" />
              <div className="w-3 h-3 rounded bg-[#58CC02]/30" />
              <div className="w-3 h-3 rounded bg-[#58CC02]/60" />
              <div className="w-3 h-3 rounded bg-[#FFC800]" />
              <div className="w-3 h-3 rounded bg-[#FF4B4B]" />
            </div>
            <span>High Spend</span>
          </div>
        </ExpressiveCard>
      )}

      {/* TAB 3: NET WORTH & DEBT */}
      {activeTab === 'networth' && (
        <div className="flex flex-col gap-4">
          <ExpressiveCard className="p-5">
            <h3 className="text-sm font-black text-white mb-1">Liquid Net Worth Vault</h3>
            <p className="text-xs font-bold text-gray-400 mb-4">Assets vs Liabilities</p>

            <div className="flex items-center justify-between bg-[#131F24] p-4 rounded-2xl border border-[#2E3C42] mb-4">
              <div>
                <span className="text-xs font-bold text-gray-400">Total Liquid Net Worth</span>
                <h2 className="text-2xl font-black text-[#58CC02]">$24,850.00</h2>
              </div>
              <ShieldCheck className="w-8 h-8 text-[#58CC02]" />
            </div>
          </ExpressiveCard>

          <ExpressiveCard className="p-5">
            <h3 className="text-sm font-black text-white mb-1">Debt Payoff Simulator</h3>
            <p className="text-xs font-bold text-gray-400 mb-4">Snowball vs. Avalanche Comparison</p>

            <div className="mb-4">
              <label className="text-xs font-bold text-gray-400 block mb-1">
                Extra Monthly Payoff Allocation: ${extraDebtBudget}/mo
              </label>
              <input
                type="range"
                min="50"
                max="1000"
                step="25"
                value={extraDebtBudget}
                onChange={(e) => setExtraDebtBudget(parseInt(e.target.value))}
                className="w-full accent-[#58CC02]"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="bg-[#131F24] p-3 rounded-2xl border border-[#2E3C42]">
                <span className="text-xs font-black text-[#1CB0F6]">⚡ Avalanche</span>
                <span className="text-lg font-black text-white block mt-1">11 Months</span>
                <span className="text-[11px] text-gray-400 font-bold">$340 Interest Paid</span>
              </div>
              <div className="bg-[#131F24] p-3 rounded-2xl border border-[#2E3C42]">
                <span className="text-xs font-black text-[#FFC800]">❄️ Snowball</span>
                <span className="text-lg font-black text-white block mt-1">13 Months</span>
                <span className="text-[11px] text-gray-400 font-bold">$410 Interest Paid</span>
              </div>
            </div>
          </ExpressiveCard>
        </div>
      )}
    </div>
  );
};
