import React from 'react';
import { FileQuestion, FolderKanban, Sparkles, Plus, CheckCircle2 } from 'lucide-react';
import { Link } from 'react-router-dom';

export const TeacherDashboardPage: React.FC = () => {

  return (
    <div className="space-y-8">
      {/* Teacher Welcome Header */}
      <div className="glass-panel rounded-2xl p-6 md:p-8 border border-purple-500/20 relative overflow-hidden">
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-purple-500/10 text-purple-300 border border-purple-500/20 text-xs font-semibold mb-3">
              <Sparkles className="w-3.5 h-3.5" />
              <span>Teacher Faculty Workspace</span>
            </div>
            <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight">
              Faculty Command Console
            </h1>
            <p className="text-slate-400 text-sm mt-1 max-w-xl">
              Author structured quizzes, manage chapter material, and configure teacher priorities for future AI quiz generation.
            </p>
          </div>
          <Link
            to="/teacher/questions"
            className="glow-button px-5 py-3 rounded-xl font-bold text-white text-sm inline-flex items-center gap-2 shadow-lg shadow-purple-500/25 shrink-0"
          >
            <Plus className="w-4 h-4" />
            <span>Create New Quiz</span>
          </Link>
        </div>
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="glass-card rounded-2xl p-6 border border-slate-800">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-purple-500/20 border border-purple-500/30 flex items-center justify-center text-purple-400">
              <FileQuestion className="w-6 h-6" />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-400 uppercase">Assigned Quizzes</p>
              <p className="text-2xl font-extrabold text-white mt-0.5">Chapter & Final</p>
            </div>
          </div>
        </div>

        <div className="glass-card rounded-2xl p-6 border border-slate-800">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-indigo-500/20 border border-indigo-500/30 flex items-center justify-center text-indigo-400">
              <FolderKanban className="w-6 h-6" />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-400 uppercase">Uploaded Documents</p>
              <p className="text-2xl font-extrabold text-indigo-300 mt-0.5">PDF / DOC / DOCX</p>
            </div>
          </div>
        </div>

        <div className="glass-card rounded-2xl p-6 border border-slate-800">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
              <CheckCircle2 className="w-6 h-6" />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-400 uppercase">Quiz Publishing</p>
              <p className="text-2xl font-extrabold text-emerald-400 mt-0.5">Draft / Published</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
