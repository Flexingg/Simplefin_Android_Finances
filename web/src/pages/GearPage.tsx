import React from 'react';
import { ExpressiveCard } from '../components/ExpressiveCard';
import { DuolingoButton } from '../components/DuolingoButton';
import { appState } from '../services/storage';
import { sound } from '../services/sound';

export const GearPage: React.FC = () => {
  const gamification = appState.getGamification();

  const items = [
    {
      id: 'streak_freeze',
      title: 'Streak Freeze',
      description: 'Equip an ice shield that protects your daily review streak if you miss a day.',
      cost: 50,
      icon: '❄️',
      action: () => {
        if (gamification.gems >= 50) {
          appState.completeQuest('gear_streak_freeze');
          sound.playLevelUpFanfare();
        }
      }
    },
    {
      id: 'heart_refill',
      title: 'Full Heart Refill',
      description: 'Instantly restore all 5 review hearts to continue power-clearing your Action Queue.',
      cost: 20,
      icon: '❤️',
      action: () => {
        if (gamification.gems >= 20) {
          appState.refillHearts();
          sound.playLevelUpFanfare();
        }
      }
    },
    {
      id: 'double_xp',
      title: '2x XP Super Potion',
      description: 'Earn double XP on all transaction reviews and auto-rule creations for 24 hours.',
      cost: 100,
      icon: '⚡',
      action: () => {
        if (gamification.gems >= 100) {
          appState.addXp(100);
          sound.playLevelUpFanfare();
        }
      }
    }
  ];

  return (
    <div className="pb-24 pt-4 px-4 max-w-2xl mx-auto flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-black text-white">Power-Up Shop</h2>
          <p className="text-xs font-bold text-gray-400">Spend earned gems to boost your financial progress</p>
        </div>
        <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-2xl bg-[#FFC800]/20 border border-[#FFC800]/40">
          <span className="text-base">💎</span>
          <span className="font-black text-[#FFC800] text-sm">{gamification.gems} Gems</span>
        </div>
      </div>

      <div className="flex flex-col gap-3">
        {items.map((item) => (
          <ExpressiveCard key={item.id} className="p-4 flex items-center justify-between gap-4">
            <div className="flex items-start gap-3">
              <div className="w-12 h-12 rounded-2xl bg-[#131F24] flex items-center justify-center text-2xl border border-[#2E3C42]">
                {item.icon}
              </div>
              <div>
                <h4 className="font-black text-white text-sm">{item.title}</h4>
                <p className="text-xs text-gray-400 font-bold max-w-sm mt-0.5 leading-snug">
                  {item.description}
                </p>
              </div>
            </div>

            <DuolingoButton
              size="sm"
              variant="gold"
              disabled={gamification.gems < item.cost}
              onClick={item.action}
            >
              💎 {item.cost}
            </DuolingoButton>
          </ExpressiveCard>
        ))}
      </div>
    </div>
  );
};
