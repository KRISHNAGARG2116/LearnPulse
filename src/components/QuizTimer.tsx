import React, { useEffect, useState } from 'react';
import { Clock } from 'lucide-react';

interface QuizTimerProps {
  durationMinutes?: number;
  onExpire: () => void;
}

export const QuizTimer: React.FC<QuizTimerProps> = ({ durationMinutes = 15, onExpire }) => {
  const [secondsLeft, setSecondsLeft] = useState<number>(durationMinutes * 60);

  useEffect(() => {
    if (secondsLeft <= 0) {
      onExpire();
      return;
    }

    const interval = setInterval(() => {
      setSecondsLeft((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          onExpire();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [secondsLeft, onExpire]);

  const minutes = Math.floor(secondsLeft / 60);
  const seconds = secondsLeft % 60;
  const isTimeLow = secondsLeft <= 120; // 2 minutes or less

  return (
    <div
      className={`inline-flex items-center gap-2 px-3.5 py-1.5 rounded-xl border text-xs font-mono font-bold transition-all ${
        isTimeLow
          ? 'bg-rose-500/20 text-rose-300 border-rose-500/40 animate-pulse'
          : 'bg-indigo-500/10 text-indigo-300 border-indigo-500/20'
      }`}
    >
      <Clock className="w-4 h-4" />
      <span>
        Time Remaining: {String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')}
      </span>
    </div>
  );
};
