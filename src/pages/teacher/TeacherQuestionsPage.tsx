import React from 'react';
import { Sparkles, Upload, Edit3 } from 'lucide-react';

export const TeacherQuestionsPage: React.FC = () => {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight">Quiz & Question Authoring</h1>
          <p className="text-xs text-slate-400 mt-1">Configure chapter quizzes, final course assessments, and teacher priority inputs</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="glass-card p-6 rounded-2xl border border-slate-800 space-y-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-500/20 text-indigo-400 border border-indigo-500/30 flex items-center justify-center">
            <Edit3 className="w-5 h-5" />
          </div>
          <h3 className="text-base font-bold text-white">Manual Question Authoring</h3>
          <p className="text-xs text-slate-400 leading-relaxed">
            Create questions manually with custom option text, marks allocation, and answer keys.
          </p>
        </div>

        <div className="glass-card p-6 rounded-2xl border border-slate-800 space-y-3">
          <div className="w-10 h-10 rounded-xl bg-purple-500/20 text-purple-400 border border-purple-500/30 flex items-center justify-center">
            <Upload className="w-5 h-5" />
          </div>
          <h3 className="text-base font-bold text-white">Document Ingestion Questions</h3>
          <p className="text-xs text-slate-400 leading-relaxed">
            Reference uploaded chapter syllabus PDFs, DOCs, and DOCX files for question authoring.
          </p>
        </div>

        <div className="glass-card p-6 rounded-2xl border border-slate-800 space-y-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 flex items-center justify-center">
            <Sparkles className="w-5 h-5" />
          </div>
          <h3 className="text-base font-bold text-white">Teacher Priority Inputs</h3>
          <p className="text-xs text-slate-400 leading-relaxed">
            Provide teacher priorities and custom concept instructions as extension points for future AI generation.
          </p>
        </div>
      </div>
    </div>
  );
};
