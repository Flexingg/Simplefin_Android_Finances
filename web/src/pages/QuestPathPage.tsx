import React, { useState } from 'react';
import { Check, Lock, MessageSquare, ShieldAlert, Sparkles, ChevronRight, Award, Plus } from 'lucide-react';
import { QuestNode } from '../types';
import { DuolingoButton } from '../components/DuolingoButton';
import { ExpressiveCard } from '../components/ExpressiveCard';
import { appState } from '../services/storage';
import { sound } from '../services/sound';

interface QuestPathPageProps {
  onNavigateToQueue: () => void;
  onNavigateToBudgets: () => void;
}

export const QuestPathPage: React.FC<QuestPathPageProps> = ({
  onNavigateToQueue,
  onNavigateToBudgets
}) => {
  const [selectedChapter, setSelectedChapter] = useState(2);
  const [activeModalNode, setActiveModalNode] = useState<QuestNode | null>(null);

  const chapters = [
    { id: 1, title: '1. Foundations' },
    { id: 2, title: '2. Review Habits' },
    { id: 3, title: '3. Budget Bosses' },
    { id: 4, title: '4. Master Builder' },
  ];

  const nodes: QuestNode[] = [
    {
      id: 'node-c2-1',
      chapter: 2,
      orderInChapter: 1,
      title: 'Inbox Zero Reviewer',
      subtitle: 'Review and categorize all pending transactions',
      requirementDescription: 'Categorize all unassigned transactions in your Action Queue to maintain inbox zero.',
      nodeType: 'NORMAL',
      rewardXp: 50,
      rewardGems: 15,
      isUnlocked: true,
      isCompleted: true,
      isCriteriaMet: true,
      progressPercent: 1.0,
      progressText: '1/1 Done'
    },
    {
      id: 'node-c2-2',
      chapter: 2,
      orderInChapter: 2,
      title: 'Note Master',
      subtitle: 'Attach itemized notes to 3 transactions',
      requirementDescription: 'Add detailed merchant notes or descriptions to 3 transactions this week.',
      nodeType: 'NORMAL',
      rewardXp: 75,
      rewardGems: 20,
      isUnlocked: true,
      isCompleted: false,
      isCriteriaMet: true,
      progressPercent: 0.66,
      progressText: '2/3 Notes Added'
    },
    {
      id: 'node-c2-3',
      chapter: 2,
      orderInChapter: 3,
      title: 'The Grand Splitter',
      subtitle: 'Split a multi-item transaction',
      requirementDescription: 'Use Quick Split to divide a grocery or supermarket transaction into distinct subcategories.',
      nodeType: 'NORMAL',
      rewardXp: 100,
      rewardGems: 25,
      isUnlocked: false,
      isCompleted: false,
      isCriteriaMet: false,
      progressPercent: 0.0,
      progressText: '0/1 Split'
    },
    {
      id: 'node-c2-4',
      chapter: 2,
      orderInChapter: 4,
      title: 'Merchant Beast',
      subtitle: 'Defeat the Uncategorized Monster (Boss Battle)',
      requirementDescription: 'Apply Auto-Rules to at least 5 recurring merchants to defeat the beast!',
      nodeType: 'BOSS_BATTLE',
      rewardXp: 200,
      rewardGems: 50,
      isUnlocked: false,
      isCompleted: false,
      isCriteriaMet: false,
      progressPercent: 0.2,
      progressText: '1/5 Rules',
      bossCurrentHp: 80,
      bossMaxHp: 100
    }
  ];

  const handleNodeClick = (node: QuestNode) => {
    sound.playButtonPress();
    setActiveModalNode(node);
  };

  const handleClaimReward = (node: QuestNode) => {
    appState.completeQuest(node.id);
    sound.playLevelUpFanfare();
    setActiveModalNode(null);
  };

  return (
    <div className="pb-24 pt-4 px-4 max-w-xl mx-auto flex flex-col items-center">
      {/* Chapter Tabs */}
      <div className="w-full flex items-center justify-between border-b-2 border-[#202F36] mb-6 overflow-x-auto">
        {chapters.map((ch) => {
          const isSelected = selectedChapter === ch.id;
          return (
            <button
              key={ch.id}
              onClick={() => {
                sound.playButtonPress();
                setSelectedChapter(ch.id);
              }}
              className={`pb-3 px-3 font-black text-sm whitespace-nowrap transition border-b-2 -mb-[2px] ${
                isSelected
                  ? 'text-white border-[#A855F7]'
                  : 'text-gray-400 border-transparent hover:text-gray-200'
              }`}
            >
              {ch.title}
            </button>
          );
        })}
      </div>

      {/* Chapter Header Banner */}
      <div className="w-full bg-[#58CC02] text-black p-5 rounded-3xl mb-8 shadow-lg">
        <div className="flex items-center justify-between font-black text-xs uppercase tracking-wider mb-1 opacity-90">
          <span>Chapter 2: The Reviewer</span>
          <span>1 / 5 Done</span>
        </div>
        <p className="font-extrabold text-sm leading-snug">
          Categorize daily transactions, use Quick Split, attach notes, and maintain your streak!
        </p>
      </div>

      {/* Interactive Quest Path Nodes */}
      <div className="flex flex-col items-center gap-10 relative">
        {nodes.map((node, index) => {
          // Stagger left/center/right Duolingo snake alignment
          const offsets = ['translate-x-0', 'translate-x-6', '-translate-x-6', 'translate-x-0'];
          const offsetClass = offsets[index % offsets.length];

          return (
            <div key={node.id} className={`flex flex-col items-center ${offsetClass}`}>
              <button
                onClick={() => handleNodeClick(node)}
                className={`w-20 h-20 rounded-full border-b-8 flex items-center justify-center transition-all transform active:scale-95 shadow-xl relative ${
                  node.isCompleted
                    ? 'bg-[#58CC02] border-[#46A302] text-black'
                    : node.isUnlocked
                    ? node.nodeType === 'BOSS_BATTLE'
                      ? 'bg-[#FF4B4B] border-[#EA2B2B] text-white animate-pulse'
                      : 'bg-[#1CB0F6] border-[#1899D6] text-white hover:scale-105'
                    : 'bg-[#2E3C42] border-[#202F36] text-gray-500 cursor-not-allowed'
                }`}
              >
                {node.isCompleted ? (
                  <Check className="w-9 h-9 stroke-[3.5]" />
                ) : node.nodeType === 'BOSS_BATTLE' ? (
                  <ShieldAlert className="w-8 h-8" />
                ) : node.isUnlocked ? (
                  <MessageSquare className="w-8 h-8" />
                ) : (
                  <Lock className="w-7 h-7 text-gray-400" />
                )}

                {/* Star / Crown Indicator */}
                {node.isCompleted && (
                  <div className="absolute -top-1 -right-1 bg-[#FFC800] text-black rounded-full p-1 border border-black shadow">
                    <Sparkles className="w-3.5 h-3.5" />
                  </div>
                )}
              </button>

              <span className="font-extrabold text-xs text-gray-300 mt-2 text-center max-w-[120px]">
                {node.title}
              </span>

              {/* Dotted Connector Line */}
              {index < nodes.length - 1 && (
                <div className="h-8 border-l-4 border-dashed border-[#2E3C42] my-2" />
              )}
            </div>
          );
        })}
      </div>

      {/* Node Detail Dialog Modal */}
      {activeModalNode && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-fade-in">
          <div className="bg-[#1B2A32] border-2 border-b-4 border-[#2E3C42] rounded-3xl p-6 max-w-sm w-full shadow-2xl relative">
            <div className="text-center flex flex-col items-center">
              <div
                className={`w-16 h-16 rounded-3xl flex items-center justify-center mb-3 ${
                  activeModalNode.isCompleted
                    ? 'bg-[#58CC02]/20 text-[#58CC02]'
                    : activeModalNode.nodeType === 'BOSS_BATTLE'
                    ? 'bg-[#FF4B4B]/20 text-[#FF4B4B]'
                    : 'bg-[#1CB0F6]/20 text-[#1CB0F6]'
                }`}
              >
                {activeModalNode.isCompleted ? (
                  <Award className="w-9 h-9" />
                ) : activeModalNode.nodeType === 'BOSS_BATTLE' ? (
                  <ShieldAlert className="w-8 h-8" />
                ) : (
                  <Sparkles className="w-8 h-8" />
                )}
              </div>

              <h3 className="text-lg font-black text-white">{activeModalNode.title}</h3>
              <p className="text-xs font-bold text-gray-400 mt-1 mb-3">
                {activeModalNode.requirementDescription}
              </p>

              {/* Progress */}
              <div className="w-full bg-[#131F24] p-3 rounded-2xl mb-4 border border-[#202F36]">
                <div className="flex justify-between text-xs font-black text-gray-300 mb-1.5">
                  <span>Progress</span>
                  <span className="text-[#58CC02]">{activeModalNode.progressText}</span>
                </div>
                <div className="w-full h-2 rounded-full bg-[#202F36] overflow-hidden">
                  <div
                    className="h-full bg-[#58CC02] rounded-full"
                    style={{ width: `${activeModalNode.progressPercent * 100}%` }}
                  />
                </div>
              </div>

              {/* Rewards */}
              <div className="flex items-center justify-center gap-4 mb-5 text-sm font-black">
                <span className="text-[#1CB0F6]">+{activeModalNode.rewardXp} XP</span>
                <span className="text-[#FFC800]">+{activeModalNode.rewardGems} Gems</span>
              </div>

              {/* Actions */}
              <div className="w-full flex flex-col gap-2">
                {activeModalNode.isCompleted ? (
                  <DuolingoButton variant="green" fullWidth onClick={() => setActiveModalNode(null)}>
                    ✅ Completed
                  </DuolingoButton>
                ) : activeModalNode.isCriteriaMet ? (
                  <DuolingoButton
                    variant="gold"
                    fullWidth
                    onClick={() => handleClaimReward(activeModalNode)}
                  >
                    🎁 Claim Reward
                  </DuolingoButton>
                ) : (
                  <DuolingoButton
                    variant="green"
                    fullWidth
                    onClick={() => {
                      setActiveModalNode(null);
                      onNavigateToQueue();
                    }}
                  >
                    🚀 Jump to Queue
                  </DuolingoButton>
                )}
                <DuolingoButton variant="outline" fullWidth onClick={() => setActiveModalNode(null)}>
                  Close
                </DuolingoButton>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
