import React, { useEffect, useState } from 'react';
import apiClient from '../../api/axios';

export const StudentResultsPage: React.FC = () => {
  const [progress, setProgress] = useState<any>(null);

  useEffect(() => {
    const fetchProgress = async () => {
      try {
        const res = await apiClient.get('/student/progress');
        if (res.data?.data) {
          setProgress(res.data.data);
        }
      } catch {
        // Fallback default
      }
    };
    fetchProgress();
  }, []);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-extrabold text-white tracking-tight">My Assessment Results</h1>
        <p className="text-xs text-slate-400 mt-1">Performance analytics and attempt history</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="glass-card p-5 rounded-xl border border-slate-800">
          <p className="text-xs text-slate-400 font-semibold uppercase">Total Attempts</p>
          <p className="text-2xl font-black text-white mt-1">{progress?.totalQuizzesAttempted || 0}</p>
        </div>
        <div className="glass-card p-5 rounded-xl border border-slate-800">
          <p className="text-xs text-slate-400 font-semibold uppercase">Average Score</p>
          <p className="text-2xl font-black text-indigo-400 mt-1">{progress?.averageScore || '0.0'}</p>
        </div>
        <div className="glass-card p-5 rounded-xl border border-slate-800">
          <p className="text-xs text-slate-400 font-semibold uppercase">Average Percentage</p>
          <p className="text-2xl font-black text-emerald-400 mt-1">{progress?.averagePercentage || '0.0'}%</p>
        </div>
        <div className="glass-card p-5 rounded-xl border border-slate-800">
          <p className="text-xs text-slate-400 font-semibold uppercase">Total Correct Choices</p>
          <p className="text-2xl font-black text-purple-400 mt-1">{progress?.totalCorrectAnswers || 0}</p>
        </div>
      </div>
    </div>
  );
};
