import React from 'react';
import { sound } from '../services/sound';

interface DuolingoButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'green' | 'blue' | 'gold' | 'red' | 'dark' | 'outline';
  fullWidth?: boolean;
  size?: 'sm' | 'md' | 'lg';
}

export const DuolingoButton: React.FC<DuolingoButtonProps> = ({
  children,
  variant = 'green',
  fullWidth = false,
  size = 'md',
  onClick,
  className = '',
  disabled,
  ...props
}) => {
  const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
    if (!disabled) {
      sound.playButtonPress();
      onClick?.(e);
    }
  };

  const getVariantStyles = () => {
    if (disabled) {
      return 'bg-[#2E3C42] border-b-4 border-[#202F36] text-gray-500 cursor-not-allowed';
    }
    switch (variant) {
      case 'green':
        return 'bg-[#58CC02] border-b-4 border-[#46A302] text-white hover:brightness-105 active:border-b-0 active:translate-y-1';
      case 'blue':
        return 'bg-[#1CB0F6] border-b-4 border-[#1899D6] text-white hover:brightness-105 active:border-b-0 active:translate-y-1';
      case 'gold':
        return 'bg-[#FFC800] border-b-4 border-[#E5A500] text-black font-extrabold hover:brightness-105 active:border-b-0 active:translate-y-1';
      case 'red':
        return 'bg-[#FF4B4B] border-b-4 border-[#EA2B2B] text-white hover:brightness-105 active:border-b-0 active:translate-y-1';
      case 'dark':
        return 'bg-[#202F36] border-b-4 border-[#142026] text-white hover:brightness-110 active:border-b-0 active:translate-y-1';
      case 'outline':
        return 'bg-transparent border-2 border-b-4 border-[#37464F] text-gray-300 hover:bg-[#202F36] active:border-b-2 active:translate-y-0.5';
    }
  };

  const getSizeStyles = () => {
    switch (size) {
      case 'sm':
        return 'px-3 py-1.5 text-xs rounded-xl font-bold tracking-wide';
      case 'md':
        return 'px-4 py-2.5 text-sm rounded-2xl font-black uppercase tracking-wider';
      case 'lg':
        return 'px-6 py-3.5 text-base rounded-2xl font-black uppercase tracking-wider';
    }
  };

  return (
    <button
      {...props}
      disabled={disabled}
      onClick={handleClick}
      className={`relative inline-flex items-center justify-center select-none transition-all duration-75 text-center ${
        fullWidth ? 'w-full' : ''
      } ${getSizeStyles()} ${getVariantStyles()} ${className}`}
    >
      {children}
    </button>
  );
};
