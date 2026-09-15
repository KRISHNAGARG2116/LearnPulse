import React from 'react';
import { Link } from 'react-router-dom';
import { AlertCircle, Home } from 'lucide-react';

export const NotFoundPage: React.FC = () => {
  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-slate-950 text-slate-100">
      <div className="glass-panel p-8 rounded-2xl border border-slate-800 text-center max-w-md w-full space-y-4">
        <div className="w-14 h-14 rounded-2xl bg-amber-500/20 text-amber-400 border border-amber-500/30 flex items-center justify-center mx-auto">
          <AlertCircle className="w-7 h-7" />
        </div>
        <h1 className="text-3xl font-extrabold text-white">404</h1>
        <h2 className="text-base font-bold text-slate-200">Page Not Found</h2>
        <p className="text-xs text-slate-400 leading-relaxed">
          The requested route does not exist or has been moved.
        </p>
        <Link
          to="/login"
          className="glow-button px-5 py-2.5 rounded-xl font-bold text-white text-xs inline-flex items-center gap-2 shadow-lg shadow-indigo-500/25"
        >
          <Home className="w-4 h-4" />
          <span>Return to Portal</span>
        </Link>
      </div>
    </div>
  );
};
