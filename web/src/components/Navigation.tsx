import React from 'react';
import { Map, Layers, PieChart, BarChart3, Receipt } from 'lucide-react';
import { sound } from '../services/sound';

export type TabRoute = 'quests' | 'queue' | 'budgets' | 'insights' | 'history' | 'gear' | 'settings';

interface NavigationProps {
  currentTab: TabRoute;
  onTabChange: (tab: TabRoute) => void;
  pendingQueueCount: number;
}

export const Navigation: React.FC<NavigationProps> = ({
  currentTab,
  onTabChange,
  pendingQueueCount
}) => {
  const tabs = [
    { id: 'quests' as TabRoute, label: 'Quests', icon: Map },
    { id: 'queue' as TabRoute, label: 'Queue', icon: Layers, badge: pendingQueueCount > 0 ? pendingQueueCount : undefined },
    { id: 'budgets' as TabRoute, label: 'Budgets', icon: PieChart },
    { id: 'insights' as TabRoute, label: 'Insights', icon: BarChart3 },
    { id: 'history' as TabRoute, label: 'History', icon: Receipt },
  ];

  return (
    <>
      {/* Mobile / Tablet Bottom Bar */}
      <nav className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-[#131F24]/95 backdrop-blur border-t-2 border-[#202F36] px-2 py-1.5 flex items-center justify-around">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = currentTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => {
                sound.playButtonPress();
                onTabChange(tab.id);
              }}
              className={`flex flex-col items-center justify-center py-1 px-3 rounded-2xl transition relative ${
                isActive ? 'text-[#58CC02] font-black' : 'text-gray-400 font-bold hover:text-gray-200'
              }`}
            >
              <div className="relative">
                <Icon className={`w-6 h-6 transition-transform ${isActive ? 'scale-110' : ''}`} />
                {tab.badge && (
                  <span className="absolute -top-1.5 -right-2 bg-[#FF4B4B] text-white text-[10px] font-black px-1.5 py-0.2 rounded-full border border-[#131F24]">
                    {tab.badge}
                  </span>
                )}
              </div>
              <span className="text-[11px] mt-0.5 tracking-tight">{tab.label}</span>
              {isActive && (
                <div className="w-1 h-1 rounded-full bg-[#58CC02] mt-0.5" />
              )}
            </button>
          );
        })}
      </nav>

      {/* Desktop Sidebar Rail */}
      <aside className="hidden md:flex flex-col fixed left-0 top-0 bottom-0 w-64 bg-[#131F24] border-r-2 border-[#202F36] p-4 z-40">
        <div className="flex items-center gap-3 px-3 py-4 mb-6">
          <div className="w-10 h-10 rounded-2xl bg-[#58CC02] flex items-center justify-center font-black text-black text-xl shadow-lg">
            🦉
          </div>
          <div>
            <h1 className="font-black text-white text-lg tracking-tight">Randall Finances</h1>
            <p className="text-xs text-gray-400 font-bold">Hermes Agent Ready</p>
          </div>
        </div>

        <div className="flex flex-col gap-1.5 flex-1">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            const isActive = currentTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => {
                  sound.playButtonPress();
                  onTabChange(tab.id);
                }}
                className={`flex items-center justify-between px-4 py-3 rounded-2xl font-black transition-all ${
                  isActive
                    ? 'bg-[#58CC02]/15 text-[#58CC02] border-2 border-[#58CC02]/40 shadow-sm'
                    : 'text-gray-400 hover:text-white hover:bg-[#1B2A32]'
                }`}
              >
                <div className="flex items-center gap-3">
                  <Icon className="w-5 h-5" />
                  <span className="text-sm uppercase tracking-wider">{tab.label}</span>
                </div>
                {tab.badge && (
                  <span className="bg-[#FF4B4B] text-white text-xs font-black px-2 py-0.5 rounded-full">
                    {tab.badge}
                  </span>
                )}
              </button>
            );
          })}
        </div>

        <div className="pt-4 border-t-2 border-[#202F36] text-[11px] text-gray-500 font-bold text-center">
          Desktop v2.0 • Web & Cloud Synced
        </div>
      </aside>
    </>
  );
};
