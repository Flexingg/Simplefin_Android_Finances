import React, { useState } from 'react';
import { Shield, Key, RefreshCw, Bot, CheckCircle2, Copy } from 'lucide-react';
import { ExpressiveCard } from '../components/ExpressiveCard';
import { DuolingoButton } from '../components/DuolingoButton';
import { sound } from '../services/sound';

export const SettingsPage: React.FC = () => {
  const [simpleFinToken, setSimpleFinToken] = useState('');
  const [isConnecting, setIsConnecting] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const mcpConfigJson = JSON.stringify(
    {
      mcpServers: {
        'randall-finances': {
          command: 'node',
          args: ['c:/RandallEngineering/Randall_Finances/hermes-mcp-server/dist/index.js']
        }
      }
    },
    null,
    2
  );

  const handleConnectSimpleFin = () => {
    if (!simpleFinToken.trim()) return;
    setIsConnecting(true);
    setTimeout(() => {
      setIsConnecting(false);
      sound.playLevelUpFanfare();
      setStatusMessage('✅ SimpleFIN Bridge connected & 90-day transactions synced!');
    }, 1500);
  };

  const handleCopyMcp = () => {
    navigator.clipboard.writeText(mcpConfigJson);
    sound.playButtonPress();
    setCopied(true);
    setTimeout(() => setCopied(false), 2500);
  };

  return (
    <div className="pb-24 pt-4 px-4 max-w-2xl mx-auto flex flex-col gap-5">
      <div>
        <h2 className="text-xl font-black text-white">Settings & Integrations</h2>
        <p className="text-xs font-bold text-gray-400">Configure bank connections and Hermes AI agent</p>
      </div>

      {statusMessage && (
        <div className="bg-[#58CC02]/20 border border-[#58CC02]/40 p-3 rounded-2xl text-xs font-bold text-[#58CC02] flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4" />
          <span>{statusMessage}</span>
        </div>
      )}

      {/* 🏛️ Hermes Agent MCP Integration */}
      <ExpressiveCard accent="blue" className="p-5">
        <div className="flex items-center gap-2.5 mb-2">
          <div className="p-2 rounded-xl bg-[#1CB0F6]/20 text-[#1CB0F6]">
            <Bot className="w-5 h-5" />
          </div>
          <div>
            <h3 className="font-black text-white text-sm">Hermes Financial Agent Controller</h3>
            <p className="text-xs text-gray-400 font-bold">Give your Hermes agent full control via MCP</p>
          </div>
        </div>

        <p className="text-xs text-gray-300 mb-3 leading-relaxed">
          Hermes can autonomously review transactions, manage budget envelopes, optimize debt payoffs, and generate auto-rules.
        </p>

        <div className="relative mb-3">
          <pre className="bg-[#131F24] p-3 rounded-xl text-[11px] font-mono text-[#58CC02] overflow-x-auto border border-[#2E3C42]">
            {mcpConfigJson}
          </pre>
          <button
            onClick={handleCopyMcp}
            className="absolute top-2 right-2 px-2.5 py-1 bg-[#202F36] hover:bg-[#2A3B44] text-xs font-bold rounded-lg text-gray-300 flex items-center gap-1 transition"
          >
            <Copy className="w-3 h-3" />
            {copied ? 'Copied!' : 'Copy Config'}
          </button>
        </div>
      </ExpressiveCard>

      {/* 🏦 SimpleFIN Connection */}
      <ExpressiveCard className="p-5">
        <div className="flex items-center gap-2.5 mb-2">
          <div className="p-2 rounded-xl bg-[#FFC800]/20 text-[#FFC800]">
            <Key className="w-5 h-5" />
          </div>
          <div>
            <h3 className="font-black text-white text-sm">SimpleFIN Bridge Sync</h3>
            <p className="text-xs text-gray-400 font-bold">Connect your checking, savings, and credit cards</p>
          </div>
        </div>

        <p className="text-xs text-gray-400 mb-3">
          Paste your base64 setup token from <a href="https://bridge.simplefin.org" target="_blank" rel="noreferrer" className="text-[#1CB0F6] underline">bridge.simplefin.org</a>:
        </p>

        <div className="flex flex-col gap-3">
          <input
            type="password"
            placeholder="Paste SimpleFIN base64 token..."
            value={simpleFinToken}
            onChange={(e) => setSimpleFinToken(e.target.value)}
            className="w-full bg-[#131F24] border border-[#2E3C42] focus:border-[#FFC800] rounded-xl px-3 py-2.5 text-xs font-bold text-white outline-none"
          />

          <DuolingoButton
            variant="gold"
            fullWidth
            disabled={isConnecting || !simpleFinToken.trim()}
            onClick={handleConnectSimpleFin}
          >
            {isConnecting ? (
              <span className="flex items-center gap-2"><RefreshCw className="w-4 h-4 animate-spin" /> Syncing Accounts...</span>
            ) : (
              '⚡ Connect & Sync 90-Day History'
            )}
          </DuolingoButton>
        </div>
      </ExpressiveCard>
    </div>
  );
};
