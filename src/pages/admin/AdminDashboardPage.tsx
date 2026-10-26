import React, { useState, useEffect } from 'react';
import {
  getPlatformSummary,
  getUserActivityTrends,
  getCourseActivity,
  getAiUsageSummary,
  getAiTelemetry
} from '../../api/lmsApi';
import {
  PlatformSummaryDTO,
  UserActivityTrendDTO,
  CourseActivityDTO,
  AiUsageSummaryDTO,
  AiTelemetryRecord
} from '../../types/lms';
import {
  Users,
  BookOpen,
  Bot,
  TrendingUp,
  Award,
  Loader2,
  AlertCircle,
  Clock,
  Sparkles,
  Activity,
  Layers
} from 'lucide-react';

import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
  Filler
} from 'chart.js';
import { Line, Bar, Doughnut } from 'react-chartjs-2';

// Register Chart.js components
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
  Filler
);

export const AdminDashboardPage: React.FC = () => {
  const [summary, setSummary] = useState<PlatformSummaryDTO | null>(null);
  const [trends, setTrends] = useState<UserActivityTrendDTO[]>([]);
  const [courses, setCourses] = useState<CourseActivityDTO[]>([]);
  const [aiUsage, setAiUsage] = useState<AiUsageSummaryDTO | null>(null);

  // AI Operational Telemetry Table State
  const [telemetryRecords, setTelemetryRecords] = useState<AiTelemetryRecord[]>([]);
  const [telemetryPage, setTelemetryPage] = useState(0);
  const [telemetryTotalPages, setTelemetryTotalPages] = useState(0);
  const [telemetryTotalCount, setTelemetryTotalCount] = useState(0);
  const [telemetryLoading, setTelemetryLoading] = useState(false);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchTelemetry = async (page: number) => {
    try {
      setTelemetryLoading(true);
      const res = await getAiTelemetry(page, 10);
      setTelemetryRecords(res.content || []);
      setTelemetryTotalPages(res.totalPages || 0);
      setTelemetryTotalCount(res.totalElements || 0);
      setTelemetryPage(page);
    } catch (err) {
      console.error('Failed to load AI telemetry logs', err);
    } finally {
      setTelemetryLoading(false);
    }
  };

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        setLoading(true);
        setError(null);

        const [summaryData, trendsData, coursesData, aiData] = await Promise.all([
          getPlatformSummary(),
          getUserActivityTrends(),
          getCourseActivity(),
          getAiUsageSummary()
        ]);

        setSummary(summaryData);
        setTrends(trendsData);
        setCourses(coursesData);
        setAiUsage(aiData);
        await fetchTelemetry(0);
      } catch (err: any) {
        setError(err?.response?.data?.message || 'Failed to load platform analytics');
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

  if (loading) {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center gap-3">
        <Loader2 className="w-8 h-8 text-indigo-400 animate-spin" />
        <p className="text-xs text-slate-400">Aggregating platform metrics & telemetry...</p>
      </div>
    );
  }

  if (error || !summary) {
    return (
      <div className="p-8 rounded-2xl bg-red-500/10 border border-red-500/30 text-red-400 flex flex-col items-center text-center gap-2">
        <AlertCircle className="w-8 h-8 shrink-0 text-red-400" />
        <h2 className="text-sm font-bold text-white">Analytics Unavailable</h2>
        <p className="text-xs text-red-300">{error || 'Could not load platform metrics'}</p>
      </div>
    );
  }

  // --- Chart 1: User Distribution by Role (Doughnut) ---
  const userRoleData = {
    labels: ['Students', 'Teachers', 'Administrators'],
    datasets: [
      {
        data: [summary.studentsCount, summary.teachersCount, summary.adminsCount],
        backgroundColor: [
          'rgba(99, 102, 241, 0.8)', // indigo
          'rgba(245, 158, 11, 0.8)', // amber
          'rgba(168, 85, 247, 0.8)'  // purple
        ],
        borderColor: [
          '#6366f1',
          '#f59e0b',
          '#a855f7'
        ],
        borderWidth: 1.5,
      },
    ],
  };

  // --- Chart 2: User Registrations Over Time (Line) ---
  const trendLabels = trends.map((t) => t.date.substring(5)); // MM-DD
  const trendValues = trends.map((t) => t.registrationsCount);
  const registrationLineData = {
    labels: trendLabels,
    datasets: [
      {
        label: 'New Registrations',
        data: trendValues,
        fill: true,
        backgroundColor: 'rgba(99, 102, 241, 0.15)',
        borderColor: '#6366f1',
        tension: 0.35,
        pointBackgroundColor: '#6366f1',
        pointRadius: 3,
      },
    ],
  };

  // --- Chart 3: AI Operation Volume by Feature (Bar) ---
  const aiFeatureLabels = aiUsage?.operationsByFeature
    ? Object.keys(aiUsage.operationsByFeature).map((f) =>
        f.replace('AI_', '').replace('_', ' ')
      )
    : [];
  const aiFeatureCounts = aiUsage?.operationsByFeature
    ? Object.values(aiUsage.operationsByFeature)
    : [];
  const aiBarData = {
    labels: aiFeatureLabels.length > 0 ? aiFeatureLabels : ['None Recorded'],
    datasets: [
      {
        label: 'AI Invocations',
        data: aiFeatureCounts.length > 0 ? aiFeatureCounts : [0],
        backgroundColor: 'rgba(168, 85, 247, 0.7)',
        borderColor: '#a855f7',
        borderWidth: 1,
        borderRadius: 6,
      },
    ],
  };

  // --- Chart 4: AI Status Breakdown (Doughnut) ---
  const aiStatusLabels = aiUsage?.operationsByStatus
    ? Object.keys(aiUsage.operationsByStatus)
    : [];
  const aiStatusCounts = aiUsage?.operationsByStatus
    ? Object.values(aiUsage.operationsByStatus)
    : [];
  const aiStatusData = {
    labels: aiStatusLabels.length > 0 ? aiStatusLabels : ['SUCCESS'],
    datasets: [
      {
        data: aiStatusCounts.length > 0 ? aiStatusCounts : [0],
        backgroundColor: [
          'rgba(16, 185, 129, 0.8)', // emerald
          'rgba(245, 158, 11, 0.8)', // amber
          'rgba(239, 68, 68, 0.8)'   // red
        ],
        borderWidth: 1,
      },
    ],
  };

  const chartOptions: any = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom' as const,
        labels: {
          color: '#94a3b8',
          font: { size: 10, family: 'sans-serif' },
        },
      },
    },
    scales: {
      x: {
        ticks: { color: '#64748b', font: { size: 9 } },
        grid: { color: 'rgba(51, 65, 85, 0.2)' },
      },
      y: {
        ticks: { color: '#64748b', font: { size: 9 }, stepSize: 1 },
        grid: { color: 'rgba(51, 65, 85, 0.2)' },
        beginAtZero: true,
      },
    },
  };

  const doughnutOptions: any = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom' as const,
        labels: {
          color: '#94a3b8',
          font: { size: 10, family: 'sans-serif' },
        },
      },
    },
  };

  const passRate =
    summary.completedQuizAttempts > 0
      ? Math.round((summary.passedQuizAttempts / summary.completedQuizAttempts) * 100)
      : 0;

  return (
    <div className="space-y-8">
      {/* Page Title */}
      <div>
        <h1 className="text-2xl font-black text-white tracking-tight flex items-center gap-3">
          <Activity className="w-7 h-7 text-indigo-400" />
          Platform Analytics & System Intelligence
        </h1>
        <p className="text-xs text-slate-400 mt-1">
          Live database aggregations, user distribution, academic progression, and local AI telemetry
        </p>
      </div>

      {/* Summary Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        {/* Total Users */}
        <div className="glass-card p-5 rounded-2xl border border-slate-800 bg-slate-900/60 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Total Registered Users</span>
            <div className="w-8 h-8 rounded-lg bg-indigo-500/20 text-indigo-400 border border-indigo-500/30 flex items-center justify-center">
              <Users className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-extrabold text-white mt-2">{summary.totalUsers}</p>
          <div className="flex items-center gap-2 mt-1 text-[11px] text-slate-400">
            <span className="text-emerald-400 font-semibold">{summary.activeUsers} enabled accounts</span>
            <span>•</span>
            <span className="text-slate-500">{summary.deactivatedUsers} deactivated</span>
          </div>
        </div>

        {/* Academic Content */}
        <div className="glass-card p-5 rounded-2xl border border-slate-800 bg-slate-900/60 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Courses & Docs</span>
            <div className="w-8 h-8 rounded-lg bg-blue-500/20 text-blue-400 border border-blue-500/30 flex items-center justify-center">
              <BookOpen className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-extrabold text-white mt-2">{summary.totalSubjects}</p>
          <div className="flex items-center gap-2 mt-1 text-[11px] text-slate-400">
            <span>{summary.totalChapters} chapters</span>
            <span>•</span>
            <span>{summary.activeDocuments} active docs</span>
          </div>
        </div>

        {/* Assessment Performance */}
        <div className="glass-card p-5 rounded-2xl border border-slate-800 bg-slate-900/60 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Quizzes & Pass Rate</span>
            <div className="w-8 h-8 rounded-lg bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 flex items-center justify-center">
              <Award className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-extrabold text-white mt-2">{summary.completedQuizAttempts}</p>
          <div className="flex items-center gap-2 mt-1 text-[11px] text-slate-400">
            <span className="text-emerald-400 font-semibold">{passRate}% pass rate</span>
            <span>•</span>
            <span>avg {summary.averageQuizScore}%</span>
          </div>
        </div>

        {/* AI Telemetry Snapshot */}
        <div className="glass-card p-5 rounded-2xl border border-slate-800 bg-slate-900/60 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">AI Operations</span>
            <div className="w-8 h-8 rounded-lg bg-purple-500/20 text-purple-400 border border-purple-500/30 flex items-center justify-center">
              <Bot className="w-4 h-4" />
            </div>
          </div>
          <p className="text-2xl font-extrabold text-purple-300 mt-2">{aiUsage?.totalOperations || 0}</p>
          <div className="flex items-center gap-2 mt-1 text-[11px] text-slate-400">
            <span className="text-purple-400 font-semibold">{aiUsage?.totalTokens || 0} tokens</span>
            <span>•</span>
            <span>{aiUsage?.averageLatencyMs || 0} ms avg</span>
          </div>
        </div>
      </div>

      {/* Chart Rows */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Chart 1: User Distribution by Role */}
        <div className="glass-panel p-5 rounded-2xl border border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="text-xs font-bold text-white uppercase tracking-wider flex items-center gap-2">
              <Users className="w-4 h-4 text-indigo-400" />
              User Role Distribution
            </h2>
            <span className="text-[10px] text-slate-400">Real DB Counts</span>
          </div>
          <div className="h-64">
            <Doughnut data={userRoleData} options={doughnutOptions} />
          </div>
        </div>

        {/* Chart 2: Registration Trend */}
        <div className="glass-panel p-5 rounded-2xl border border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="text-xs font-bold text-white uppercase tracking-wider flex items-center gap-2">
              <TrendingUp className="w-4 h-4 text-emerald-400" />
              Registration Trajectory (Last 14 Days)
            </h2>
            <span className="text-[10px] text-slate-400">Daily Volume</span>
          </div>
          <div className="h-64">
            <Line data={registrationLineData} options={chartOptions} />
          </div>
        </div>

        {/* Chart 3: AI Operation Volume by Feature */}
        <div className="glass-panel p-5 rounded-2xl border border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="text-xs font-bold text-white uppercase tracking-wider flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-purple-400" />
              AI Invocations by Feature
            </h2>
            <span className="text-[10px] text-purple-400 font-semibold">
              {aiUsage?.successRatePercentage || 100}% Success Rate
            </span>
          </div>
          <div className="h-64">
            <Bar data={aiBarData} options={chartOptions} />
          </div>
        </div>

        {/* Chart 4: AI Operations Status Breakdown */}
        <div className="glass-panel p-5 rounded-2xl border border-slate-800 space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="text-xs font-bold text-white uppercase tracking-wider flex items-center gap-2">
              <Clock className="w-4 h-4 text-amber-400" />
              AI Status & Reliability Breakdown
            </h2>
            <span className="text-[10px] text-slate-400">
              Est. Cost: ${aiUsage?.estimatedCostUsd || '0.00'}
            </span>
          </div>
          <div className="h-64">
            <Doughnut data={aiStatusData} options={doughnutOptions} />
          </div>
        </div>
      </div>

      {/* Course Resource Allocation & Subject Enrollment Table */}
      <div className="glass-panel p-5 rounded-2xl border border-slate-800 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-xs font-bold text-white uppercase tracking-wider flex items-center gap-2">
            <Layers className="w-4 h-4 text-indigo-400" />
            Course Resource Allocation Table
          </h2>
          <span className="text-[10px] text-slate-400">Tabular Distribution of Curricula, Quizzes & Documents</span>
        </div>
        {courses.length === 0 ? (
          <p className="text-xs text-slate-500 py-4 text-center">No academic courses registered yet.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-900/60 border-b border-slate-800 text-slate-400 uppercase font-semibold">
                <tr>
                  <th className="py-2.5 px-3">Subject Name</th>
                  <th className="py-2.5 px-3">Code</th>
                  <th className="py-2.5 px-3 text-center">Active Enrollments</th>
                  <th className="py-2.5 px-3 text-center">Chapters</th>
                  <th className="py-2.5 px-3 text-center">Quizzes</th>
                  <th className="py-2.5 px-3 text-center">Uploaded Docs</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {courses.map((c) => (
                  <tr key={c.subjectId} className="hover:bg-slate-900/40">
                    <td className="py-2.5 px-3 font-semibold text-white">{c.subjectName}</td>
                    <td className="py-2.5 px-3 text-slate-400 font-mono text-[11px]">{c.subjectCode}</td>
                    <td className="py-2.5 px-3 text-center text-indigo-400 font-bold">{c.enrolledStudentsCount}</td>
                    <td className="py-2.5 px-3 text-center text-slate-300">{c.chaptersCount}</td>
                    <td className="py-2.5 px-3 text-center text-slate-300">{c.quizzesCount}</td>
                    <td className="py-2.5 px-3 text-center text-slate-300">{c.documentsCount}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* AI Operational Telemetry Table */}
      <div className="glass-panel p-5 rounded-2xl border border-slate-800 space-y-4">
        <div className="flex items-center justify-between flex-wrap gap-2">
          <div>
            <h2 className="text-xs font-bold text-white uppercase tracking-wider flex items-center gap-2">
              <Bot className="w-4 h-4 text-purple-400" />
              AI Operational Telemetry Log
            </h2>
            <p className="text-[11px] text-slate-400 mt-0.5">
              Live audit trail of Ollama LLM invocations, token provenance, execution latencies, and fault status
            </p>
          </div>
          <div className="flex items-center gap-2 text-xs text-slate-400">
            <span>Total Logged Operations: <strong className="text-white">{telemetryTotalCount}</strong></span>
          </div>
        </div>

        {telemetryLoading ? (
          <div className="flex items-center justify-center py-8">
            <Loader2 className="w-6 h-6 animate-spin text-purple-400" />
          </div>
        ) : telemetryRecords.length === 0 ? (
          <p className="text-xs text-slate-500 py-6 text-center">No AI operations recorded yet.</p>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-900/60 border-b border-slate-800 text-slate-400 uppercase font-semibold">
                  <tr>
                    <th className="py-2.5 px-3">Timestamp</th>
                    <th className="py-2.5 px-3">Feature</th>
                    <th className="py-2.5 px-3">Model / Provider</th>
                    <th className="py-2.5 px-3">User</th>
                    <th className="py-2.5 px-3 text-center">Latency</th>
                    <th className="py-2.5 px-3 text-center">Prompt / Comp (Total)</th>
                    <th className="py-2.5 px-3 text-center">Provenance</th>
                    <th className="py-2.5 px-3 text-center">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60 font-mono text-[11px]">
                  {telemetryRecords.map((rec) => (
                    <tr key={rec.id} className="hover:bg-slate-900/40">
                      <td className="py-2 px-3 text-slate-400 whitespace-nowrap">
                        {new Date(rec.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                      </td>
                      <td className="py-2 px-3 font-sans font-semibold text-white">
                        {rec.feature}
                      </td>
                      <td className="py-2 px-3 text-slate-300">
                        {rec.model} <span className="text-[10px] text-slate-500">({rec.provider})</span>
                      </td>
                      <td className="py-2 px-3 text-slate-300 font-sans truncate max-w-[140px]" title={rec.userEmail || 'System'}>
                        {rec.userEmail || 'System'}
                      </td>
                      <td className="py-2 px-3 text-center text-slate-300">
                        {rec.executionLatencyMs} ms
                      </td>
                      <td className="py-2 px-3 text-center text-slate-300">
                        {rec.promptTokens || 0} / {rec.completionTokens || 0} ({rec.totalTokens || 0})
                      </td>
                      <td className="py-2 px-3 text-center">
                        <span className={`inline-flex px-1.5 py-0.5 rounded text-[9px] font-sans font-medium uppercase tracking-wider ${
                          rec.usageProvenance === 'PROVIDER_REPORTED'
                            ? 'bg-blue-500/20 text-blue-300 border border-blue-500/30'
                            : 'bg-slate-700/50 text-slate-400 border border-slate-600/30'
                        }`}>
                          {rec.usageProvenance}
                        </span>
                      </td>
                      <td className="py-2 px-3 text-center">
                        <span className={`inline-flex px-1.5 py-0.5 rounded text-[9px] font-sans font-medium uppercase tracking-wider ${
                          rec.status === 'SUCCESS'
                            ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                            : rec.status === 'FALLBACK'
                            ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                            : 'bg-red-500/20 text-red-300 border border-red-500/30'
                        }`}>
                          {rec.status}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination Controls */}
            {telemetryTotalPages > 1 && (
              <div className="flex items-center justify-between pt-2 border-t border-slate-800 text-xs">
                <span className="text-slate-400 text-[11px]">
                  Page {telemetryPage + 1} of {telemetryTotalPages}
                </span>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => fetchTelemetry(telemetryPage - 1)}
                    disabled={telemetryPage === 0 || telemetryLoading}
                    className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed text-white text-[11px]"
                  >
                    Previous
                  </button>
                  <button
                    onClick={() => fetchTelemetry(telemetryPage + 1)}
                    disabled={telemetryPage >= telemetryTotalPages - 1 || telemetryLoading}
                    className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed text-white text-[11px]"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};
