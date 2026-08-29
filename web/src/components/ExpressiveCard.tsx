import React from 'react';

interface ExpressiveCardProps {
  children: React.ReactNode;
  className?: string;
  onClick?: () => void;
  accent?: 'green' | 'blue' | 'gold' | 'red' | 'none';
}

export const ExpressiveCard: React.FC<ExpressiveCardProps> = ({
  children,
  className = '',
  onClick,
  accent = 'none'
}) => {
  const getAccentBorder = () => {
    switch (accent) {
      case 'green': return 'border-[#58CC02]/40 hover:border-[#58CC02]';
      case 'blue': return 'border-[#1CB0F6]/40 hover:border-[#1CB0F6]';
      case 'gold': return 'border-[#FFC800]/40 hover:border-[#FFC800]';
      case 'red': return 'border-[#FF4B4B]/40 hover:border-[#FF4B4B]';
      case 'none': return 'border-[#2E3C42] hover:border-[#3E4F57]';
    }
  };

  return (
    <div
      onClick={onClick}
      className={`bg-[#1B2A32] border-2 border-b-4 rounded-2xl transition-all shadow-md ${
        onClick ? 'cursor-pointer active:scale-[0.99]' : ''
      } ${getAccentBorder()} ${className}`}
    >
      {children}
    </div>
  );
};
