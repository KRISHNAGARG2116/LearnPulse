import React from 'react';
import { useAuth } from '../../hooks/useAuth';
import { User, Mail, Shield } from 'lucide-react';

export const StudentProfilePage: React.FC = () => {
  const { user } = useAuth();

  return (
    <div className="space-y-6 max-w-2xl">
      <div>
        <h1 className="text-2xl font-extrabold text-white tracking-tight">Student Profile</h1>
        <p className="text-xs text-slate-400 mt-1">Personal account details and credentials</p>
      </div>

      <div className="glass-panel p-6 rounded-2xl border border-slate-800 space-y-4">
        <div className="flex items-center gap-4 pb-6 border-b border-slate-800">
          <div className="w-16 h-16 rounded-2xl bg-indigo-500/20 text-indigo-400 border border-indigo-500/30 flex items-center justify-center font-bold text-xl">
            {user?.firstName?.[0] || user?.email[0].toUpperCase()}
          </div>
          <div>
            <h2 className="text-lg font-bold text-white">
              {user?.firstName ? `${user.firstName} ${user.lastName}` : user?.email}
            </h2>
            <p className="text-xs text-indigo-400 font-medium">{user?.role} Account</p>
          </div>
        </div>

        <div className="space-y-3 pt-2">
          <div className="flex items-center gap-3 text-sm">
            <Mail className="w-4 h-4 text-slate-400 shrink-0" />
            <span className="text-slate-400 w-24">Email:</span>
            <span className="text-slate-100 font-medium">{user?.email}</span>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <Shield className="w-4 h-4 text-slate-400 shrink-0" />
            <span className="text-slate-400 w-24">Role:</span>
            <span className="text-slate-100 font-medium">{user?.role}</span>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <User className="w-4 h-4 text-slate-400 shrink-0" />
            <span className="text-slate-400 w-24">User ID:</span>
            <span className="text-slate-100 font-mono text-xs">{user?.userId}</span>
          </div>
        </div>
      </div>
    </div>
  );
};
