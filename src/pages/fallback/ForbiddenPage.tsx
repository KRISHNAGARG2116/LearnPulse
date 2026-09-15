import React from 'react';
import { Link } from 'react-router-dom';
import { ShieldAlert, Home } from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';

export const ForbiddenPage: React.FC = () => {
  const { user } = useAuth();

  const getRoleHome = () => {
    if (user?.role === 'STUDENT') return '/student/dashboard';
    if (user?.role === 'TEACHER') return '/teacher/dashboard';
    if (user?.role === 'ADMIN') return '/admin/dashboard';
    return '/login';
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-slate-950 text-slate-100">
      <div className="glass-panel p-8 rounded-2xl border border-rose-500/20 text-center max-w-md w-full space-y-4">
        <div className="w-14 h-14 rounded-2xl bg-rose-500/20 text-rose-400 border border-rose-500/30 flex items-center justify-center mx-auto">
          <ShieldAlert className="w-7 h-7" />
        </div>
        <h1 className="text-3xl font-extrabold text-white">403</h1>
        <h2 className="text-base font-bold text-rose-300">Access Restricted</h2>
        <p className="text-xs text-slate-400 leading-relaxed">
          You do not have the required role permissions to access this workspace.
        </p>
        <Link
          to={getRoleHome()}
          className="glow-button px-5 py-2.5 rounded-xl font-bold text-white text-xs inline-flex items-center gap-2 shadow-lg shadow-indigo-500/25"
        >
          <Home className="w-4 h-4" />
          <span>Return to Workspace Dashboard</span>
        </Link>
      </div>
    </div>
  );
};
