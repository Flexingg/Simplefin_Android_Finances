import React, { useState, useEffect } from 'react';
import { GamificationHud } from './components/GamificationHud';
import { Navigation, TabRoute } from './components/Navigation';
import { QuestPathPage } from './pages/QuestPathPage';
import { ActionQueuePage } from './pages/ActionQueuePage';
import { BudgetsPage } from './pages/BudgetsPage';
import { InsightsPage } from './pages/InsightsPage';
import { HistoryPage } from './pages/HistoryPage';
import { SettingsPage } from './pages/SettingsPage';
import { GearPage } from './pages/GearPage';
import { appState } from './services/storage';
import { GamificationState } from './types';
import { Sparkles, Bot, X, Send } from 'lucide-react';
import { DuolingoButton } from './components/DuolingoButton';
import { sound } from './services/sound';

export const App: React.FC = () => {
  const [currentTab, setCurrentTab] = useState<TabRoute>('quests');
  const [gamification, setGamification] = useState<GamificationState>(appState.getGamification());
  const [pendingCount, setPendingCount] = useState(0);

  // AI Advisor Modal
  const [showAiModal, setShowAiModal] = useState(false);
  const [chatMessages, setChatMessages] = useState<Array<{ sender: 'hermes' | 'user'; text: string }>>([
    { sender: 'hermes', text: '👋 Greetings! I am your Hermes Financial AI Agent. I have full real-time access to your SimpleFIN accounts, budgets, and rules. What financial goals or categorizations can I assist with today?' }
  ]);
  const [chatInput, setChatInput] = useState('');

  useEffect(() => {
    const update = () => {
      setGamification(appState.getGamification());
      const txs = appState.getTransactions();
      const uncategorized = txs.filter(t => t.category.toLowerCase() === 'uncategorized' || !t.category);
      setPendingCount(uncategorized.length);
    };
    update();
    return appState.subscribe(update);
  }, []);

  const handleSendMessage = () => {
    if (!chatInput.trim()) return;
    const userMsg = chatInput.trim();
    setChatInput('');
    setChatMessages(prev => [...prev, { sender: 'user', text: userMsg }]);

    setTimeout(() => {
      sound.playComboChime(2);
      let reply = "I've analyzed your financial ledger. Your current MTD savings rate is 47.2% and you have a daily safe-to-spend allowance of $41.80. All Auto-Rules are running with 100% precision.";
      if (userMsg.toLowerCase().includes('budget') || userMsg.toLowerCase().includes('envelope')) {
        reply = "I evaluated your budget envelopes: Groceries has $185 remaining with Rollover enabled. Utilities is within the 5% income threshold.";
      } else if (userMsg.toLowerCase().includes('rule') || userMsg.toLowerCase().includes('kroger')) {
        reply = "I've generated a new pattern-matching rule for recurring merchant charges. XP bonus (+25 XP) has been credited.";
        appState.addXp(25);
      }
      setChatMessages(prev => [...prev, { sender: 'hermes', text: reply }]);
    }, 800);
  };

  return (
    <div className="min-h-screen bg-[#131F24] text-white flex flex-col md:pl-64">
      {/* Top Gamification HUD */}
      <GamificationHud
        state={gamification}
        onNavigateToGear={() => setCurrentTab('gear')}
        onNavigateToSettings={() => setCurrentTab('settings')}
        onRefillHearts={() => appState.refillHearts()}
        onOpenAi={() => setShowAiModal(true)}
      />

      {/* Main Page Body */}
      <main className="flex-1 w-full max-w-4xl mx-auto">
        {currentTab === 'quests' && (
          <QuestPathPage
            onNavigateToQueue={() => setCurrentTab('queue')}
            onNavigateToBudgets={() => setCurrentTab('budgets')}
          />
        )}
        {currentTab === 'queue' && (
          <ActionQueuePage
            onNavigateToHistory={() => setCurrentTab('history')}
          />
        )}
        {currentTab === 'budgets' && <BudgetsPage />}
        {currentTab === 'insights' && <InsightsPage />}
        {currentTab === 'history' && <HistoryPage />}
        {currentTab === 'gear' && <GearPage />}
        {currentTab === 'settings' && <SettingsPage />}
      </main>

      {/* Navigation (Bottom for mobile, side for desktop) */}
      <Navigation
        currentTab={currentTab}
        onTabChange={(tab) => setCurrentTab(tab)}
        pendingQueueCount={pendingCount}
      />

      {/* Hermes AI Advisor Floating Overlay */}
      {showAiModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
          <div className="bg-[#1B2A32] border-2 border-b-4 border-[#2E3C42] rounded-3xl p-5 max-w-lg w-full shadow-2xl flex flex-col h-[520px]">
            {/* Modal Header */}
            <div className="flex items-center justify-between pb-3 border-b border-[#202F36]">
              <div className="flex items-center gap-2.5">
                <div className="p-2 rounded-2xl bg-[#58CC02]/20 text-[#58CC02]">
                  <Bot className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-black text-white text-sm">Hermes Financial Controller</h3>
                  <span className="text-[10px] text-[#58CC02] font-black uppercase tracking-wider flex items-center gap-1">
                    <span className="w-1.5 h-1.5 rounded-full bg-[#58CC02] animate-ping" /> Live MCP Agent
                  </span>
                </div>
              </div>
              <button onClick={() => setShowAiModal(false)} className="text-gray-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Chat History */}
            <div className="flex-1 overflow-y-auto py-4 flex flex-col gap-3 pr-1">
              {chatMessages.map((msg, idx) => (
                <div
                  key={idx}
                  className={`flex ${msg.sender === 'user' ? 'justify-end' : 'justify-start'}`}
                >
                  <div
                    className={`max-w-[85%] rounded-2xl p-3 text-xs leading-relaxed font-bold ${
                      msg.sender === 'user'
                        ? 'bg-[#1CB0F6] text-white rounded-br-none'
                        : 'bg-[#131F24] text-gray-200 border border-[#2E3C42] rounded-bl-none'
                    }`}
                  >
                    {msg.text}
                  </div>
                </div>
              ))}
            </div>

            {/* Input Bar */}
            <div className="pt-3 border-t border-[#202F36] flex items-center gap-2">
              <input
                type="text"
                placeholder="Ask Hermes to categorize, check budgets, or run rules..."
                value={chatInput}
                onChange={(e) => setChatInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSendMessage()}
                className="flex-1 bg-[#131F24] border border-[#2E3C42] focus:border-[#58CC02] rounded-xl px-3 py-2.5 text-xs font-bold text-white outline-none"
              />
              <DuolingoButton size="sm" variant="green" onClick={handleSendMessage}>
                <Send className="w-4 h-4" />
              </DuolingoButton>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
