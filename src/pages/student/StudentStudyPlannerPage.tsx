import React, { useState, useEffect } from 'react';
import { generateStudyPlan, getStudentEnrolledSubjects } from '../../api/lmsApi';
import { StudyPlanResponse, EnrolledSubjectDTO } from '../../types/lms';
import {
  Calendar,
  Clock,
  BookOpen,
  Sparkles,
  Loader2,
  CheckCircle2,
  AlertCircle,
  Award,
  ListTodo,
  Printer,
  Plus,
  X
} from 'lucide-react';

export const StudentStudyPlannerPage: React.FC = () => {
  const [enrolledSubjects, setEnrolledSubjects] = useState<EnrolledSubjectDTO[]>([]);
  const [selectedSubjects, setSelectedSubjects] = useState<string[]>([]);
  const [customSubjectInput, setCustomSubjectInput] = useState<string>('');
  const [examDate, setExamDate] = useState<string>(() => {
    // Default to 14 days in future
    const d = new Date();
    d.setDate(d.getDate() + 14);
    return d.toISOString().split('T')[0];
  });
  const [hoursPerDay, setHoursPerDay] = useState<number>(4);

  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [planResult, setPlanResult] = useState<StudyPlanResponse | null>(null);

  // Fetch student enrolled subjects on mount
  useEffect(() => {
    const fetchEnrolled = async () => {
      try {
        const subs = await getStudentEnrolledSubjects();
        setEnrolledSubjects(subs || []);
        if (subs && subs.length > 0) {
          setSelectedSubjects(subs.slice(0, 3).map((s) => s.subjectName));
        } else {
          setSelectedSubjects(['Data Structures', 'Operating Systems']);
        }
      } catch (e) {
        console.warn('Failed to load enrolled subjects for planner:', e);
        setSelectedSubjects(['Data Structures', 'Operating Systems']);
      }
    };
    fetchEnrolled();
  }, []);

  const toggleSubject = (subName: string) => {
    if (selectedSubjects.includes(subName)) {
      setSelectedSubjects((prev) => prev.filter((s) => s !== subName));
    } else {
      setSelectedSubjects((prev) => [...prev, subName]);
    }
  };

  const handleAddCustomSubject = () => {
    const trimmed = customSubjectInput.trim();
    if (trimmed && !selectedSubjects.includes(trimmed)) {
      setSelectedSubjects((prev) => [...prev, trimmed]);
      setCustomSubjectInput('');
    }
  };

  const handleRemoveSubject = (subName: string) => {
    setSelectedSubjects((prev) => prev.filter((s) => s !== subName));
  };

  const handleGeneratePlan = async () => {
    if (selectedSubjects.length === 0) {
      setError('Please select at least one subject to plan.');
      return;
    }

    // Validate exam date
    const target = new Date(examDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (target <= today) {
      setError('Target exam date must be at least tomorrow.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await generateStudyPlan({
        examDate,
        subjects: selectedSubjects,
        availableHoursPerDay: hoursPerDay,
      });

      setPlanResult(response);
    } catch (err: any) {
      console.error('Study plan generation error:', err);
      setError(
        err.response?.data?.message ||
        err.message ||
        'Failed to generate study plan. Please verify the date and inputs.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-4 sm:p-6 max-w-5xl mx-auto space-y-6">
      {/* Header */}
      <div className="bg-slate-900/60 border border-slate-800 p-4 rounded-2xl backdrop-blur-md flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-emerald-500 to-teal-600 flex items-center justify-center shadow-lg shadow-emerald-500/20">
            <Calendar className="w-5 h-5 text-white" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-lg font-bold text-white">AI Personalized Study Planner</h1>
              <span className="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                Exam Preparation
              </span>
            </div>
            <p className="text-xs text-slate-400">
              Generate a realistic day-by-day revision schedule bounded by your target exam date and daily hours.
            </p>
          </div>
        </div>

        {planResult && (
          <button
            onClick={() => window.print()}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-200 border border-slate-700 transition self-start md:self-auto"
          >
            <Printer className="w-3.5 h-3.5 text-indigo-400" /> Print Plan
          </button>
        )}
      </div>

      {/* Configuration Form Card */}
      <div className="bg-slate-900/40 border border-slate-800 rounded-2xl p-5 space-y-5 shadow-lg">
        <h2 className="text-sm font-bold text-white flex items-center gap-2">
          <Sparkles className="w-4 h-4 text-emerald-400" />
          Configure Revision Parameters
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {/* Target Exam Date */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
              <Calendar className="w-3.5 h-3.5 text-indigo-400" /> Target Exam Date:
            </label>
            <input
              type="date"
              value={examDate}
              onChange={(e) => setExamDate(e.target.value)}
              min={new Date(Date.now() + 86400000).toISOString().split('T')[0]}
              className="w-full bg-slate-950 border border-slate-800 focus:border-emerald-500 rounded-xl px-3.5 py-2.5 text-xs sm:text-sm text-slate-100 focus:outline-none"
            />
            <p className="text-[11px] text-slate-400">
              AI distributes topics across the available preparation window.
            </p>
          </div>

          {/* Daily Available Hours */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 flex items-center justify-between">
              <span className="flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-emerald-400" /> Daily Study Commitment:
              </span>
              <span className="text-emerald-400 font-bold">{hoursPerDay} hours/day</span>
            </label>
            <input
              type="range"
              min={1}
              max={12}
              step={1}
              value={hoursPerDay}
              onChange={(e) => setHoursPerDay(Number(e.target.value))}
              className="w-full accent-emerald-500 cursor-pointer"
            />
            <div className="flex justify-between text-[10px] text-slate-400">
              <span>1 hr (Light review)</span>
              <span>4 hrs (Balanced)</span>
              <span>12 hrs (Intensive sprint)</span>
            </div>
          </div>
        </div>

        {/* Subjects Selector */}
        <div className="space-y-2">
          <label className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
            <BookOpen className="w-3.5 h-3.5 text-purple-400" /> Subjects to Cover:
          </label>

          {/* Enrolled Subjects Clickable Badges */}
          {enrolledSubjects.length > 0 && (
            <div className="flex flex-wrap gap-2 pt-1">
              {enrolledSubjects.map((sub) => {
                const isSelected = selectedSubjects.includes(sub.subjectName);
                return (
                  <button
                    key={sub.subjectId}
                    type="button"
                    onClick={() => toggleSubject(sub.subjectName)}
                    className={`px-3 py-1.5 rounded-xl text-xs font-medium border transition flex items-center gap-1.5 ${
                      isSelected
                        ? 'bg-emerald-600 text-white border-emerald-500 shadow-sm'
                        : 'bg-slate-950 text-slate-400 border-slate-800 hover:border-slate-700'
                    }`}
                  >
                    {isSelected && <CheckCircle2 className="w-3.5 h-3.5" />}
                    {sub.subjectName}
                  </button>
                );
              })}
            </div>
          )}

          {/* Active Selected Tags & Custom Add */}
          <div className="flex flex-wrap items-center gap-2 pt-2">
            {selectedSubjects.map((sub) => (
              <span
                key={sub}
                className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-semibold bg-slate-800 text-slate-200 border border-slate-700"
              >
                {sub}
                <button
                  onClick={() => handleRemoveSubject(sub)}
                  className="text-slate-400 hover:text-rose-400"
                >
                  <X className="w-3 h-3" />
                </button>
              </span>
            ))}

            <div className="flex items-center gap-1">
              <input
                type="text"
                placeholder="Add other subject..."
                value={customSubjectInput}
                onChange={(e) => setCustomSubjectInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    handleAddCustomSubject();
                  }
                }}
                className="bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-1 text-xs text-slate-200 placeholder:text-slate-400 focus:outline-none focus:border-emerald-500 w-36"
              />
              <button
                type="button"
                onClick={handleAddCustomSubject}
                className="p-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300"
              >
                <Plus className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>

        {error && (
          <div className="p-3 rounded-xl bg-rose-950/60 border border-rose-800/60 text-xs text-rose-200 flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <button
          onClick={handleGeneratePlan}
          disabled={loading || selectedSubjects.length === 0}
          className="w-full py-3 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 text-white font-bold text-xs sm:text-sm shadow-lg shadow-emerald-500/25 disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-2 transition"
        >
          {loading ? (
            <>
              <Loader2 className="w-4 h-4 animate-spin" />
              Synthesizing Optimal Schedule with Qwen AI...
            </>
          ) : (
            <>
              <Sparkles className="w-4 h-4" />
              Generate Daily Study Schedule
            </>
          )}
        </button>
      </div>

      {/* Generated Plan Agenda Timeline */}
      {planResult && (
        <div className="space-y-6 pt-2">
          {/* Summary Overview Banner */}
          <div className="bg-gradient-to-r from-slate-900 to-emerald-950/50 border border-emerald-500/30 p-5 rounded-2xl flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h3 className="text-base font-bold text-white">Your Personalized Study Roadmap</h3>
              <p className="text-xs text-slate-300 mt-0.5">
                Target Exam Date: <strong className="text-emerald-400">{planResult.examDate}</strong> • Total Roadmap Duration:{' '}
                <strong className="text-emerald-400">{planResult.totalDays} Days</strong>
              </p>
            </div>
            <div className="flex items-center gap-3">
              <span className="px-3 py-1 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-semibold">
                {planResult.dailyPlan?.length || 0} Scheduled Sessions
              </span>
            </div>
          </div>

          {/* Daily Schedule Timeline Cards */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
              <ListTodo className="w-4 h-4 text-emerald-400" /> Daily Breakdown & Action Checklist
            </h4>

            {planResult.dailyPlan && planResult.dailyPlan.length > 0 ? (
              planResult.dailyPlan.map((dayItem) => (
                <div
                  key={dayItem.day}
                  className="bg-slate-900/60 border border-slate-800 hover:border-slate-700/80 rounded-2xl p-4 sm:p-5 transition space-y-3"
                >
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-2 border-b border-slate-800">
                    <div className="flex items-center gap-2.5">
                      <span className="w-7 h-7 rounded-lg bg-emerald-500/20 text-emerald-400 font-black text-xs flex items-center justify-center border border-emerald-500/30">
                        D{dayItem.day}
                      </span>
                      <div>
                        <h5 className="text-sm font-bold text-white">Day {dayItem.day} Agenda</h5>
                        {dayItem.date && <p className="text-[11px] text-slate-400">{dayItem.date}</p>}
                      </div>
                    </div>

                    <span className="text-xs font-semibold text-emerald-400 bg-emerald-950/60 border border-emerald-800/40 px-2.5 py-1 rounded-lg self-start sm:self-auto flex items-center gap-1">
                      <Clock className="w-3.5 h-3.5" /> {dayItem.hours} Hours
                    </span>
                  </div>

                  {/* Topics Covered */}
                  {dayItem.topics && dayItem.topics.length > 0 && (
                    <div className="flex flex-wrap items-center gap-1.5">
                      <span className="text-[11px] font-semibold text-slate-400 mr-1">Topics:</span>
                      {dayItem.topics.map((t, tIdx) => (
                        <span
                          key={tIdx}
                          className="px-2.5 py-0.5 rounded-md bg-indigo-950/60 border border-indigo-700/40 text-[11px] font-medium text-indigo-200"
                        >
                          {t}
                        </span>
                      ))}
                    </div>
                  )}

                  {/* Tasks Checklist */}
                  {dayItem.tasks && dayItem.tasks.length > 0 && (
                    <div className="space-y-1.5 pt-1">
                      {dayItem.tasks.map((task, kIdx) => (
                        <div key={kIdx} className="flex items-start gap-2 text-xs text-slate-200">
                          <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                          <span className="leading-relaxed">{task}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ))
            ) : (
              <p className="text-xs text-slate-500 italic">No daily items generated.</p>
            )}
          </div>

          {/* Recommendations Card */}
          {planResult.recommendations && planResult.recommendations.length > 0 && (
            <div className="bg-slate-900/60 border border-indigo-500/30 rounded-2xl p-5 space-y-3">
              <h4 className="text-xs font-bold text-indigo-300 flex items-center gap-1.5 uppercase tracking-wider">
                <Award className="w-4 h-4 text-indigo-400" /> AI Pedagogical Advice & Retention Tips
              </h4>
              <ul className="space-y-2">
                {planResult.recommendations.map((rec, rIdx) => (
                  <li key={rIdx} className="text-xs text-slate-200 flex items-start gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-indigo-400 mt-1.5 shrink-0" />
                    <span>{rec}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
