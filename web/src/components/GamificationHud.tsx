import React, { useState } from 'react';
import { Flame, Zap, Diamond, Heart, Settings, Sparkles, Plus, X } from 'lucide-react';
import { GamificationState } from '../types';
import { DuolingoButton } from './DuolingoButton';
import { sound } from '../services/sound';

interface GamificationHudProps {
  state: GamificationState;
  onNavigateToGear: () => void;
  onNavigateToSettings: () => void;
  onRefillHearts: () => void;
  onOpenAi: () => void;
}

export const GamificationHud: React.FC<GamificationHudProps> = ({
  state,
  onNavigateToGear,
  onNavigateToSettings,
  onRefillHearts,
  onOpenAi
}) => {
  const [showHeartsModal, setShowHeartsModal] = useState(false);

  const levelXpTarget = 250;
  const currentLevelXp = state.xp % levelXpTarget;
  const levelProgress = (currentLevelXp / levelXpTarget) * 100;

  return (
    <>
      <header className="sticky top-0 z-40 bg-[#131F24]/95 backdrop-blur border-b-2 border-[#202F36] px-4 py-2.5">
        <div className="max-w-4xl mx-auto flex flex-col gap-2">
          {/* Top Status Row */}
          <div className="flex items-center justify-between gap-2">
            {/* Streak */}
            <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-[#FF9600]/15 border border-[#FF9600]/30">
              <Flame className="w-5 h-5 text-[#FF9600] animate-bounce" fill="#FF9600" />
              <span className="font-black text-[#FF9600] text-sm">{state.streakDays}</span>
            </div>

            {/* XP */}
            <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-[#1CB0F6]/15 border border-[#1CB0F6]/30">
              <Zap className="w-4 h-4 text-[#1CB0F6]" fill="#1CB0F6" />
              <span className="font-extrabold text-[#1CB0F6] text-sm">{state.xp} XP</span>
            </div>

            {/* Gems */}
            <button
              onClick={() => {
                sound.playButtonPress();
                onNavigateToGear();
              }}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-[#FFC800]/15 border border-[#FFC800]/30 hover:scale-105 transition active:scale-95"
            >
              <Diamond className="w-4 h-4 text-[#FFC800]" fill="#FFC800" />
              <span className="font-black text-[#FFC800] text-sm">{state.gems}</span>
            </button>

            {/* Hearts */}
            <button
              onClick={() => {
                sound.playButtonPress();
                setShowHeartsModal(true);
              }}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-[#FF4B4B]/15 border border-[#FF4B4B]/30 hover:scale-105 transition active:scale-95"
            >
              <Heart className="w-4 h-4 text-[#FF4B4B]" fill="#FF4B4B" />
              <span className="font-black text-[#FF4B4B] text-sm">{state.hearts}</span>
              <Plus className="w-3.5 h-3.5 text-[#FF4B4B] -ml-0.5" />
            </button>

            {/* AI Advisor & Settings */}
            <div className="flex items-center gap-1.5">
              <button
                onClick={() => {
                  sound.playButtonPress();
                  onOpenAi();
                }}
                title="Hermes / Gemini Financial Coach"
                className="p-2 rounded-xl bg-[#58CC02]/20 hover:bg-[#58CC02]/30 text-[#58CC02] border border-[#58CC02]/40 transition"
              >
                <Sparkles className="w-4 h-4" />
              </button>
              <button
                onClick={() => {
                  sound.playButtonPress();
                  onNavigateToSettings();
                }}
                title="Settings & Integrations"
                className="p-2 rounded-xl bg-[#202F36] hover:bg-[#2A3B44] text-gray-300 transition"
              >
                <Settings className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* Level Progress Bar */}
          <div className="flex items-center justify-between text-[11px] text-gray-400 font-bold px-0.5">
            <span>Lvl {state.level} • {state.levelTitle}</span>
            <span>{currentLevelXp}/{levelXpTarget} XP</span>
          </div>
          <div className="w-full h-2 rounded-full bg-[#202F36] overflow-hidden">
            <div
              className="h-full bg-[#58CC02] transition-all duration-300 rounded-full"
              style={{ width: `${Math.min(100, Math.max(5, levelProgress))}%` }}
            />
          </div>
        </div>
      </header>

      {/* Hearts Refill Dialog */}
      {showHeartsModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fade-in">
          <div className="bg-[#1B2A32] border-2 border-b-4 border-[#2E3C42] rounded-3xl p-6 max-w-sm w-full shadow-2xl relative">
            <button
              onClick={() => setShowHeartsModal(false)}
              className="absolute top-4 right-4 text-gray-400 hover:text-white"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="text-center flex flex-col items-center">
              <div className="w-16 h-16 rounded-3xl bg-[#FF4B4B]/20 flex items-center justify-center mb-3">
                <Heart className="w-10 h-10 text-[#FF4B4B]" fill="#FF4B4B" />
              </div>
              <h3 className="text-xl font-black text-white">Full Hearts</h3>
              <p className="text-sm text-gray-400 mt-1 mb-5">
                Keep your hearts full by reviewing transactions in the Action Queue and categorizing daily expenses!
              </p>

              <div className="w-full flex flex-col gap-2.5">
                <DuolingoButton
                  variant="green"
                  fullWidth
                  onClick={() => {
                    onRefillHearts();
                    sound.playLevelUpFanfare();
                    setShowHeartsModal(false);
                  }}
                >
                  ⚡ Refill with 20 Gems
                </DuolingoButton>
                <DuolingoButton
                  variant="outline"
                  fullWidth
                  onClick={() => setShowHeartsModal(false)}
                >
                  Close
                </DuolingoButton>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
};
