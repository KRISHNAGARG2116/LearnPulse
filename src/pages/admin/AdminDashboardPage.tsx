import React from 'react';
import { Shield, Users, Database, Server } from 'lucide-react';

export const AdminDashboardPage: React.FC = () => {
  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-extrabold text-white tracking-tight">System Administration Console</h1>
        <p className="text-xs text-slate-400 mt-1">Platform management, system settings, and user directory control</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <div className="glass-card p-6 rounded-2xl border border-slate-800">
          <div className="w-10 h-10 rounded-xl bg-indigo-500/20 text-indigo-400 border border-indigo-500/30 flex items-center justify-center mb-3">
            <Users className="w-5 h-5" />
          </div>
          <p className="text-xs font-semibold text-slate-400 uppercase">System Accounts</p>
          <p className="text-2xl font-extrabold text-white mt-1">Active</p>
        </div>

        <div className="glass-card p-6 rounded-2xl border border-slate-800">
          <div className="w-10 h-10 rounded-xl bg-purple-500/20 text-purple-400 border border-purple-500/30 flex items-center justify-center mb-3">
            <Shield className="w-5 h-5" />
          </div>
          <p className="text-xs font-semibold text-slate-400 uppercase">Security Boundary</p>
          <p className="text-2xl font-extrabold text-purple-300 mt-1">RBAC Enforced</p>
        </div>

        <div className="glass-card p-6 rounded-2xl border border-slate-800">
          <div className="w-10 h-10 rounded-xl bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 flex items-center justify-center mb-3">
            <Database className="w-5 h-5" />
          </div>
          <p className="text-xs font-semibold text-slate-400 uppercase">Database Schema</p>
          <p className="text-2xl font-extrabold text-emerald-400 mt-1">PostgreSQL 16</p>
        </div>

        <div className="glass-card p-6 rounded-2xl border border-slate-800">
          <div className="w-10 h-10 rounded-xl bg-amber-500/20 text-amber-400 border border-amber-500/30 flex items-center justify-center mb-3">
            <Server className="w-5 h-5" />
          </div>
          <p className="text-xs font-semibold text-slate-400 uppercase">Backend API</p>
          <p className="text-2xl font-extrabold text-amber-300 mt-1">Spring Boot 3.2</p>
        </div>
      </div>
    </div>
  );
};
