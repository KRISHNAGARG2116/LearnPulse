import React, { useEffect, useState } from 'react';
import { CourseProgressionDTO } from '../../types/auth';
import apiClient from '../../api/axios';
import { BookOpen, Award, CheckCircle2, Lock, ArrowRight, Sparkles } from 'lucide-react';
import { Link } from 'react-router-dom';

export const StudentDashboardPage: React.FC = () => {
  const [progression, setProgression] = useState<CourseProgressionDTO | null>(null);

  useEffect(() => {
    const fetchProgression = async () => {
      try {
        const res = await apiClient.get('/student/courses/progression');
        if (res.data?.data) {
          setProgression(res.data.data);
        }
      } catch {
        // Fallback default
      }
    };
    fetchProgression();
  }, []);

  return (
    <div className="space-y-8">
      {/* Welcome Banner */}
      <div className="glass-panel rounded-2xl p-6 md:p-8 border border-indigo-500/20 relative overflow-hidden">
        <div className="absolute -right-10 -bottom-10 w-64 h-64 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 text-xs font-semibold mb-3">
              <Sparkles className="w-3.5 h-3.5" />
              <span>Student Workspace</span>
            </div>
            <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight">
              Learning Dashboard
            </h1>
            <p className="text-slate-400 text-sm mt-1 max-w-xl">
              Track your course progression, attempt chapter quizzes, and unlock your final course assessment.
            </p>
          </div>
          <Link
            to="/student/practice"
            className="glow-button px-5 py-3 rounded-xl font-bold text-white text-sm inline-flex items-center gap-2 shadow-lg shadow-indigo-500/25 shrink-0"
          >
            <span>Open Quiz Practice</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      </div>

      {/* Progression Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="glass-card rounded-2xl p-6 border border-slate-800 flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-indigo-500/20 border border-indigo-500/30 flex items-center justify-center text-indigo-400">
            <BookOpen className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Chapters Unlocked</p>
            <p className="text-2xl font-extrabold text-white mt-0.5">
              {progression?.completedChapters || 1} / {progression?.totalChapters || 10}
            </p>
          </div>
        </div>

        <div className="glass-card rounded-2xl p-6 border border-slate-800 flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
            <CheckCircle2 className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Chapter Quiz Pass Rule</p>
            <p className="text-2xl font-extrabold text-emerald-400 mt-0.5">80% Threshold</p>
          </div>
        </div>

        <div className="glass-card rounded-2xl p-6 border border-slate-800 flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-purple-500/20 border border-purple-500/30 flex items-center justify-center text-purple-400">
            <Award className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Final Course Assessment</p>
            <p className="text-2xl font-extrabold text-purple-300 mt-0.5">
              {progression?.isFinalQuizUnlocked ? 'Unlocked (75%)' : 'Locked'}
            </p>
          </div>
        </div>
      </div>

      {/* Chapter Learning Progression Section */}
      <div className="glass-panel rounded-2xl p-6 border border-slate-800">
        <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
          <BookOpen className="w-5 h-5 text-indigo-400" />
          <span>Course Chapter Progression</span>
        </h2>

        <div className="space-y-3">
          {(progression?.chapters || [
            { chapterNumber: 1, title: 'Chapter 1: Foundation & Concepts', isUnlocked: true, isCompleted: true, highestScorePercentage: 90 },
            { chapterNumber: 2, title: 'Chapter 2: Core Data Structures', isUnlocked: true, isCompleted: false, highestScorePercentage: 0 },
            { chapterNumber: 3, title: 'Chapter 3: Advanced Algorithms', isUnlocked: false, isCompleted: false, highestScorePercentage: 0 },
          ]).map((ch) => (
            <div
              key={ch.chapterNumber}
              className={`p-4 rounded-xl border flex items-center justify-between gap-4 transition-all ${
                ch.isUnlocked
                  ? 'bg-slate-900/60 border-slate-800 hover:border-indigo-500/40'
                  : 'bg-slate-900/30 border-slate-900 opacity-60'
              }`}
            >
              <div className="flex items-center gap-3">
                <div
                  className={`w-8 h-8 rounded-lg flex items-center justify-center text-xs font-bold ${
                    ch.isCompleted
                      ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                      : ch.isUnlocked
                      ? 'bg-indigo-500/20 text-indigo-400 border border-indigo-500/30'
                      : 'bg-slate-800 text-slate-500'
                  }`}
                >
                  {ch.isCompleted ? <CheckCircle2 className="w-4 h-4" /> : ch.isUnlocked ? ch.chapterNumber : <Lock className="w-4 h-4" />}
                </div>
                <div>
                  <h3 className="text-sm font-semibold text-slate-200">{ch.title}</h3>
                  <p className="text-xs text-slate-400">
                    {ch.isCompleted
                      ? `Passed with ${ch.highestScorePercentage}%`
                      : ch.isUnlocked
                      ? 'Unlocked — Requires 80% to pass chapter'
                      : 'Locked — Pass previous chapter quiz to unlock'}
                  </p>
                </div>
              </div>

              {ch.isUnlocked && (
                <a
                  href="/quiz.html"
                  className="px-3 py-1.5 rounded-lg bg-indigo-600/20 hover:bg-indigo-600/30 text-indigo-300 border border-indigo-500/30 text-xs font-semibold transition-all shrink-0"
                >
                  Attempt Quiz
                </a>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
