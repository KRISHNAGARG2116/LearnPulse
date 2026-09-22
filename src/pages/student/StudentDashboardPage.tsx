import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { getStudentProgress, getSubjects } from '../../api/lmsApi';
import { StudentProgressDTO, SubjectDTO } from '../../types/lms';
import {
  Sparkles,
  BookOpen,
  Award,
  BarChart3,
  ArrowRight,
  Loader2,
  AlertCircle,
  Clock,
  Layers,
  CheckCircle2,
} from 'lucide-react';

export const StudentDashboardPage: React.FC = () => {
  const { user } = useAuth();
  const [progress, setProgress] = useState<StudentProgressDTO | null>(null);
  const [subjects, setSubjects] = useState<SubjectDTO[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      setError(null);
      try {
        const [progressData, subjectsData] = await Promise.all([
          getStudentProgress().catch(() => null),
          getSubjects().catch(() => []),
        ]);
        setProgress(progressData);
        setSubjects(subjectsData);
      } catch (err: any) {
        setError(err.message || 'Failed to load dashboard data. Please try again.');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
        <Loader2 className="w-10 h-10 text-indigo-400 animate-spin" />
        <p className="text-slate-400 text-sm font-medium">Loading student dashboard...</p>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Error Alert */}
      {error && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center gap-3 text-rose-400 text-sm">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Welcome Banner */}
      <div className="glass-panel rounded-2xl p-6 md:p-8 border border-indigo-500/20 relative overflow-hidden">
        <div className="absolute -right-10 -bottom-10 w-64 h-64 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 text-xs font-semibold mb-3">
              <Sparkles className="w-3.5 h-3.5" />
              <span>Student Learning Workspace</span>
            </div>
            <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight">
              Welcome back, {user?.firstName ? `${user.firstName} ${user.lastName || ''}` : 'Student'}!
            </h1>
            <p className="text-slate-400 text-sm mt-1 max-w-xl">
              Track your course progression, attempt chapter quizzes, and analyze your performance history.
            </p>
          </div>
          <div className="flex items-center gap-3 shrink-0">
            <Link
              to="/student/subjects"
              className="glow-button px-5 py-3 rounded-xl font-bold text-white text-sm inline-flex items-center gap-2 shadow-lg shadow-indigo-500/25"
            >
              <BookOpen className="w-4 h-4" />
              <span>Browse Subjects</span>
            </Link>
          </div>
        </div>
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="glass-card rounded-2xl p-6 border border-slate-800 flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-indigo-500/20 border border-indigo-500/30 flex items-center justify-center text-indigo-400 shrink-0">
            <BookOpen className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Quizzes Attempted</p>
            <p className="text-2xl font-extrabold text-white mt-0.5">
              {progress?.totalQuizzesAttempted ?? 0}
            </p>
          </div>
        </div>

        <div className="glass-card rounded-2xl p-6 border border-slate-800 flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center text-emerald-400 shrink-0">
            <CheckCircle2 className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Completed Quizzes</p>
            <p className="text-2xl font-extrabold text-emerald-400 mt-0.5">
              {progress?.totalQuizzesCompleted ?? 0}
            </p>
          </div>
        </div>

        <div className="glass-card rounded-2xl p-6 border border-slate-800 flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-purple-500/20 border border-purple-500/30 flex items-center justify-center text-purple-400 shrink-0">
            <BarChart3 className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Average Score</p>
            <p className="text-2xl font-extrabold text-purple-300 mt-0.5">
              {progress?.averagePercentage != null ? `${progress.averagePercentage.toFixed(1)}%` : '0%'}
            </p>
          </div>
        </div>

        <div className="glass-card rounded-2xl p-6 border border-slate-800 flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-amber-500/20 border border-amber-500/30 flex items-center justify-center text-amber-400 shrink-0">
            <Award className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Highest Score</p>
            <p className="text-2xl font-extrabold text-amber-300 mt-0.5">
              {progress?.highestScore ?? 0} pts
            </p>
          </div>
        </div>
      </div>

      {/* Available Subjects Grid */}
      <div className="glass-panel rounded-2xl p-6 border border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            <Layers className="w-5 h-5 text-indigo-400" />
            <span>Academic Subjects</span>
          </h2>
          <Link
            to="/student/subjects"
            className="text-xs font-semibold text-indigo-400 hover:text-indigo-300 inline-flex items-center gap-1"
          >
            <span>View All</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {subjects.length === 0 ? (
          <div className="p-8 text-center border border-dashed border-slate-800 rounded-xl space-y-2">
            <BookOpen className="w-8 h-8 text-slate-600 mx-auto" />
            <p className="text-slate-400 text-sm font-medium">No subjects available yet.</p>
            <p className="text-slate-500 text-xs">Check back later when teachers publish academic subjects.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {subjects.map((sub) => (
              <Link
                key={sub.id}
                to={`/student/subjects/${sub.id}/chapters`}
                className="p-5 rounded-xl border border-slate-800 bg-slate-900/60 hover:bg-slate-900 hover:border-indigo-500/40 transition-all group flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-center justify-between gap-2 mb-2">
                    <span className="px-2.5 py-1 rounded-md bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 text-xs font-mono font-bold">
                      {sub.code}
                    </span>
                  </div>
                  <h3 className="text-base font-bold text-white group-hover:text-indigo-300 transition-colors">
                    {sub.name}
                  </h3>
                  {sub.description && (
                    <p className="text-xs text-slate-400 mt-1 line-clamp-2">{sub.description}</p>
                  )}
                </div>
                <div className="mt-4 pt-3 border-t border-slate-800/80 flex items-center justify-between text-xs font-semibold text-indigo-400">
                  <span>Explore Chapters & Materials</span>
                  <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>

      {/* Recent Quiz Attempts */}
      <div className="glass-panel rounded-2xl p-6 border border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            <Clock className="w-5 h-5 text-indigo-400" />
            <span>Recent Quiz Attempts</span>
          </h2>
          <Link
            to="/student/progress"
            className="text-xs font-semibold text-indigo-400 hover:text-indigo-300 inline-flex items-center gap-1"
          >
            <span>View Full Analytics</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        {!progress?.recentAttempts || progress.recentAttempts.length === 0 ? (
          <div className="p-8 text-center border border-dashed border-slate-800 rounded-xl space-y-2">
            <BarChart3 className="w-8 h-8 text-slate-600 mx-auto" />
            <p className="text-slate-400 text-sm font-medium">No quiz attempts recorded yet.</p>
            <p className="text-slate-500 text-xs">Select a subject and chapter to attempt your first quiz!</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-xs text-slate-400 uppercase bg-slate-900/80 border-b border-slate-800">
                <tr>
                  <th className="py-3 px-4">Quiz Title</th>
                  <th className="py-3 px-4">Score</th>
                  <th className="py-3 px-4">Percentage</th>
                  <th className="py-3 px-4">Date</th>
                  <th className="py-3 px-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {progress.recentAttempts.map((res) => (
                  <tr key={res.id} className="hover:bg-slate-900/40 transition-colors">
                    <td className="py-3 px-4 font-semibold text-white">{res.quizTitle || 'Quiz Attempt'}</td>
                    <td className="py-3 px-4 font-bold text-slate-200">
                      {res.score} / {res.totalMarks}
                    </td>
                    <td className="py-3 px-4">
                      <span className="font-semibold text-indigo-400">
                        {res.percentage != null ? `${res.percentage.toFixed(1)}%` : '0%'}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-xs text-slate-400">
                      {res.attemptedAt ? new Date(res.attemptedAt).toLocaleDateString() : 'Recent'}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <Link
                        to={`/student/quiz/result/${res.id}`}
                        className="px-3 py-1.5 rounded-lg bg-indigo-600/20 hover:bg-indigo-600/30 text-indigo-300 border border-indigo-500/30 text-xs font-semibold transition-all"
                      >
                        View Result
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
