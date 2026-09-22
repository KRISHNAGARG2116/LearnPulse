import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getStudentProgress } from '../../api/lmsApi';
import { StudentProgressDTO } from '../../types/lms';
import {
  BarChart3,
  Award,
  CheckCircle2,
  Clock,
  Loader2,
  AlertCircle,
  TrendingUp,
} from 'lucide-react';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  ArcElement,
} from 'chart.js';
import { Line, Doughnut } from 'react-chartjs-2';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  ArcElement
);

export const StudentProgressPage: React.FC = () => {
  const [progress, setProgress] = useState<StudentProgressDTO | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchProgress = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await getStudentProgress();
        setProgress(data);
      } catch (err: any) {
        setError(err.message || 'Failed to load progress analytics.');
      } finally {
        setLoading(false);
      }
    };

    fetchProgress();
  }, []);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
        <Loader2 className="w-10 h-10 text-indigo-400 animate-spin" />
        <p className="text-slate-400 text-sm font-medium">Loading performance analytics & charts...</p>
      </div>
    );
  }

  const attempts = progress?.recentAttempts || [];

  // Chart 1: Line Chart Data (Percentage Trend)
  const lineLabels = attempts.length > 0
    ? attempts.map((a, i) => a.quizTitle || `Attempt ${i + 1}`).reverse()
    : ['No Data'];

  const lineDataValues = attempts.length > 0
    ? attempts.map((a) => a.percentage || 0).reverse()
    : [0];

  const lineChartData = {
    labels: lineLabels,
    datasets: [
      {
        label: 'Score Percentage (%)',
        data: lineDataValues,
        borderColor: '#6366f1',
        backgroundColor: 'rgba(99, 102, 241, 0.2)',
        tension: 0.3,
        fill: true,
        pointBackgroundColor: '#818cf8',
        pointRadius: 5,
      },
    ],
  };

  const lineChartOptions = {
    responsive: true,
    plugins: {
      legend: {
        labels: { color: '#94a3b8', font: { size: 12 } },
      },
    },
    scales: {
      y: {
        min: 0,
        max: 100,
        ticks: { color: '#94a3b8' },
        grid: { color: 'rgba(51, 65, 85, 0.4)' },
      },
      x: {
        ticks: { color: '#94a3b8' },
        grid: { color: 'rgba(51, 65, 85, 0.4)' },
      },
    },
  };

  // Chart 2: Doughnut Chart Data (Correct vs Wrong)
  const totalCorrect = progress?.totalCorrectAnswers ?? 0;
  const totalWrong = progress?.totalWrongAnswers ?? 0;

  const doughnutData = {
    labels: ['Correct Answers', 'Wrong Answers'],
    datasets: [
      {
        data: [totalCorrect, totalWrong],
        backgroundColor: ['#10b981', '#f43f5e'],
        borderColor: ['#059669', '#e11d48'],
        borderWidth: 1,
      },
    ],
  };

  const doughnutOptions = {
    responsive: true,
    plugins: {
      legend: {
        position: 'bottom' as const,
        labels: { color: '#94a3b8', font: { size: 12 } },
      },
    },
  };

  return (
    <div className="space-y-8">
      {/* Header Banner */}
      <div className="glass-panel rounded-2xl p-6 md:p-8 border border-slate-800">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 text-xs font-semibold mb-3">
          <TrendingUp className="w-3.5 h-3.5" />
          <span>Performance Analytics</span>
        </div>
        <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight">
          Historical Learning Performance
        </h1>
        <p className="text-slate-400 text-sm mt-1 max-w-xl">
          Review score trends, answer accuracy, and attempt history across all quiz assessments.
        </p>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center gap-3 text-rose-400 text-sm">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Analytics Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="glass-card rounded-2xl p-6 border border-slate-800 flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-indigo-500/20 border border-indigo-500/30 flex items-center justify-center text-indigo-400 shrink-0">
            <BarChart3 className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Total Attempts</p>
            <p className="text-2xl font-extrabold text-white mt-0.5">
              {progress?.totalQuizzesAttempted ?? 0}
            </p>
          </div>
        </div>

        <div className="glass-card rounded-2xl p-6 border border-slate-800 flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-purple-500/20 border border-purple-500/30 flex items-center justify-center text-purple-400 shrink-0">
            <TrendingUp className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Average Percentage</p>
            <p className="text-2xl font-extrabold text-purple-300 mt-0.5">
              {progress?.averagePercentage != null ? `${progress.averagePercentage.toFixed(1)}%` : '0%'}
            </p>
          </div>
        </div>

        <div className="glass-card rounded-2xl p-6 border border-slate-800 flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center text-emerald-400 shrink-0">
            <CheckCircle2 className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Total Correct</p>
            <p className="text-2xl font-extrabold text-emerald-400 mt-0.5">
              {totalCorrect}
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

      {/* Chart.js Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Line Chart */}
        <div className="lg:col-span-2 glass-panel rounded-2xl p-6 border border-slate-800 space-y-4">
          <h2 className="text-base font-bold text-white flex items-center gap-2">
            <TrendingUp className="w-4 h-4 text-indigo-400" />
            <span>Score Percentage Trend</span>
          </h2>
          {attempts.length === 0 ? (
            <div className="h-64 flex items-center justify-center text-slate-500 text-xs">
              No attempt data to chart.
            </div>
          ) : (
            <div className="h-64">
              <Line data={lineChartData} options={lineChartOptions} />
            </div>
          )}
        </div>

        {/* Doughnut Chart */}
        <div className="lg:col-span-1 glass-panel rounded-2xl p-6 border border-slate-800 space-y-4">
          <h2 className="text-base font-bold text-white flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-400" />
            <span>Accuracy Breakdown</span>
          </h2>
          {totalCorrect === 0 && totalWrong === 0 ? (
            <div className="h-64 flex items-center justify-center text-slate-500 text-xs">
              No answer breakdown data available.
            </div>
          ) : (
            <div className="h-64 flex items-center justify-center p-4">
              <Doughnut data={doughnutData} options={doughnutOptions} />
            </div>
          )}
        </div>
      </div>

      {/* Historical Attempts Table */}
      <div className="glass-panel rounded-2xl p-6 border border-slate-800 space-y-4">
        <h2 className="text-lg font-bold text-white flex items-center gap-2">
          <Clock className="w-5 h-5 text-indigo-400" />
          <span>Historical Attempt Records</span>
        </h2>

        {attempts.length === 0 ? (
          <div className="p-8 text-center border border-dashed border-slate-800 rounded-xl text-slate-400 text-sm">
            No quiz attempts recorded.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-xs text-slate-400 uppercase bg-slate-900/80 border-b border-slate-800">
                <tr>
                  <th className="py-3 px-4">Quiz Title</th>
                  <th className="py-3 px-4">Score</th>
                  <th className="py-3 px-4">Percentage</th>
                  <th className="py-3 px-4">Attempt Date</th>
                  <th className="py-3 px-4 text-right">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {attempts.map((res) => (
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
