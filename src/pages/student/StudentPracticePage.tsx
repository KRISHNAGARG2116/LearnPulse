import React from 'react';
import { BookOpen, ExternalLink } from 'lucide-react';

export const StudentPracticePage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight">Interactive Quiz Practice</h1>
          <p className="text-xs text-slate-400 mt-1">Single-page scrollable assessment experience (Google Forms style)</p>
        </div>
      </div>

      <div className="glass-panel rounded-2xl p-8 border border-slate-800 text-center space-y-4 max-w-2xl mx-auto">
        <div className="w-14 h-14 rounded-2xl bg-indigo-500/20 text-indigo-400 border border-indigo-500/30 flex items-center justify-center mx-auto">
          <BookOpen className="w-7 h-7" />
        </div>
        <h2 className="text-xl font-extrabold text-white">Single-Page Quiz Engine</h2>
        <p className="text-xs text-slate-400 leading-relaxed max-w-lg mx-auto">
          Experience our single scrollable quiz page featuring per-question option selection (invisible correctness), controlled answer reveals, and 100% server-side grading.
        </p>

        <div className="pt-2">
          <a
            href="/quiz.html"
            target="_blank"
            rel="noopener noreferrer"
            className="glow-button px-6 py-3 rounded-xl font-bold text-white text-sm inline-flex items-center gap-2 shadow-lg shadow-indigo-500/25"
          >
            <span>Launch Single Scrollable Quiz Experience</span>
            <ExternalLink className="w-4 h-4" />
          </a>
        </div>
      </div>
    </div>
  );
};
