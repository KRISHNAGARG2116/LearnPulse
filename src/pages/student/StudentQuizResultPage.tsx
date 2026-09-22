import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getQuizResult } from '../../api/lmsApi';
import { StudentQuizResultDTO } from '../../types/lms';
import {
  Award,
  CheckCircle2,
  XCircle,
  BarChart3,
  ArrowRight,
  BookOpen,
  Loader2,
  AlertCircle,
  Calendar,
} from 'lucide-react';

export const StudentQuizResultPage: React.FC = () => {
  const { resultId } = useParams<{ resultId: string }>();
  const [result, setResult] = useState<StudentQuizResultDTO | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!resultId) return;

    const fetchResult = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await getQuizResult(resultId);
        setResult(data);
      } catch (err: any) {
        setError(err.message || 'Failed to load quiz result.');
      } finally {
        setLoading(false);
      }
    };

    fetchResult();
  }, [resultId]);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
        <Loader2 className="w-10 h-10 text-indigo-400 animate-spin" />
        <p className="text-slate-400 text-sm font-medium">Fetching quiz assessment result...</p>
      </div>
    );
  }

  if (error || !result) {
    return (
      <div className="glass-panel rounded-2xl p-12 text-center border border-slate-800 space-y-4 max-w-lg mx-auto">
        <AlertCircle className="w-12 h-12 text-rose-400 mx-auto" />
        <h2 className="text-lg font-bold text-white">Result Not Found</h2>
        <p className="text-slate-400 text-sm max-w-sm mx-auto">
          {error || 'Unable to load the requested quiz attempt result.'}
        </p>
        <Link
          to="/student/dashboard"
          className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-indigo-600 text-white text-xs font-semibold hover:bg-indigo-500"
        >
          <span>Return to Dashboard</span>
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-8 max-w-3xl mx-auto">
      {/* Header Banner */}
      <div className="glass-panel rounded-2xl p-6 md:p-8 border border-indigo-500/20 text-center relative overflow-hidden space-y-3">
        <div className="w-16 h-16 rounded-2xl bg-indigo-500/20 border border-indigo-500/30 text-indigo-400 flex items-center justify-center mx-auto shadow-lg shadow-indigo-500/10">
          <Award className="w-8 h-8" />
        </div>

        <h1 className="text-2xl md:text-3xl font-extrabold text-white">
          {result.quizTitle || 'Quiz Assessment Result'}
        </h1>

        <div className="flex items-center justify-center gap-3 text-xs text-slate-400 pt-1">
          {result.attemptedAt && (
            <span className="flex items-center gap-1">
              <Calendar className="w-3.5 h-3.5 text-indigo-400" />
              Completed on {new Date(result.attemptedAt).toLocaleString()}
            </span>
          )}

          {/* Render Pass/Fail badge ONLY if provided by backend */}
          {result.isPassed !== undefined && result.isPassed !== null && (
            <span
              className={`px-3 py-1 rounded-full text-xs font-bold border ${
                result.isPassed
                  ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                  : 'bg-amber-500/10 text-amber-400 border-amber-500/20'
              }`}
            >
              {result.isPassed ? 'Passed' : 'Attempt Complete'}
            </span>
          )}
        </div>
      </div>

      {/* Score Summary Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <div className="glass-card rounded-2xl p-6 border border-slate-800 text-center">
          <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Score Achieved</p>
          <p className="text-3xl font-extrabold text-white mt-1">
            {result.score} <span className="text-sm font-normal text-slate-400">/ {result.totalMarks}</span>
          </p>
        </div>

        <div className="glass-card rounded-2xl p-6 border border-slate-800 text-center">
          <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Percentage</p>
          <p className="text-3xl font-extrabold text-indigo-400 mt-1">
            {result.percentage != null ? `${result.percentage.toFixed(1)}%` : '0%'}
          </p>
        </div>

        <div className="glass-card rounded-2xl p-6 border border-slate-800 text-center">
          <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Correct / Wrong</p>
          <div className="flex items-center justify-center gap-3 mt-1 text-lg font-bold">
            <span className="text-emerald-400 flex items-center gap-1">
              <CheckCircle2 className="w-4 h-4" />
              {result.correctAnswers}
            </span>
            <span className="text-slate-600">/</span>
            <span className="text-rose-400 flex items-center gap-1">
              <XCircle className="w-4 h-4" />
              {result.wrongAnswers}
            </span>
          </div>
        </div>
      </div>

      {/* Action Navigation Bar */}
      <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4">
        <Link
          to="/student/dashboard"
          className="w-full sm:w-auto px-6 py-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-white text-xs font-semibold border border-slate-700 inline-flex items-center justify-center gap-2 transition-all"
        >
          <span>Return to Dashboard</span>
        </Link>

        <Link
          to="/student/subjects"
          className="w-full sm:w-auto px-6 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold inline-flex items-center justify-center gap-2 shadow-lg shadow-indigo-500/25 transition-all"
        >
          <BookOpen className="w-4 h-4" />
          <span>Browse Subjects</span>
        </Link>

        <Link
          to="/student/progress"
          className="w-full sm:w-auto px-6 py-3 rounded-xl bg-purple-600 hover:bg-purple-500 text-white text-xs font-bold inline-flex items-center justify-center gap-2 shadow-lg shadow-purple-500/25 transition-all"
        >
          <BarChart3 className="w-4 h-4" />
          <span>View All Analytics</span>
          <ArrowRight className="w-4 h-4" />
        </Link>
      </div>
    </div>
  );
};
