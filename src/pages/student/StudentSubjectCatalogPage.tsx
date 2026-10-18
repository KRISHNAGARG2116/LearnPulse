import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getSubjects, getStudentEnrolledSubjects } from '../../api/lmsApi';
import { SubjectDTO, EnrolledSubjectDTO } from '../../types/lms';
import {
  BookOpen,
  Layers,
  ArrowRight,
  Loader2,
  AlertCircle,
  CheckCircle2,
  Lock,
  UserCheck,
} from 'lucide-react';

export const StudentSubjectCatalogPage: React.FC = () => {
  const [subjects, setSubjects] = useState<SubjectDTO[]>([]);
  const [enrolledSubjects, setEnrolledSubjects] = useState<EnrolledSubjectDTO[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchCatalogAndEnrollments = async () => {
      setLoading(true);
      setError(null);
      try {
        const [allSubjects, enrolled] = await Promise.all([
          getSubjects().catch(() => []),
          getStudentEnrolledSubjects().catch(() => []),
        ]);
        setSubjects(allSubjects);
        setEnrolledSubjects(enrolled);
      } catch (err: any) {
        setError(err.message || 'Failed to load subject catalog.');
      } finally {
        setLoading(false);
      }
    };
    fetchCatalogAndEnrollments();
  }, []);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
        <Loader2 className="w-10 h-10 text-indigo-400 animate-spin" />
        <p className="text-slate-400 text-sm font-medium">Loading academic course catalog & enrollments...</p>
      </div>
    );
  }

  // Map enrolled subjects by subjectId for O(1) lookup
  const enrollmentMap = new Map<string, EnrolledSubjectDTO>();
  enrolledSubjects.forEach((es) => {
    if (es.status === 'ACTIVE') {
      enrollmentMap.set(es.subjectId, es);
    }
  });

  return (
    <div className="space-y-8">
      {/* Header Banner */}
      <div className="glass-panel rounded-2xl p-6 md:p-8 border border-slate-800">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 text-xs font-semibold mb-3">
          <Layers className="w-3.5 h-3.5" />
          <span>Course Navigation</span>
        </div>
        <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight">
          Subject Catalog & Enrollments
        </h1>
        <p className="text-slate-400 text-sm mt-1 max-w-xl">
          Browse academic subjects. Actively enrolled courses grant access to chapter syllabus materials, document viewers, and practice quizzes.
        </p>
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center gap-3 text-rose-400 text-sm">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {subjects.length === 0 ? (
        <div className="glass-panel rounded-2xl p-12 text-center border border-dashed border-slate-800 space-y-3">
          <BookOpen className="w-12 h-12 text-slate-600 mx-auto" />
          <h3 className="text-lg font-bold text-white">No Subjects Found</h3>
          <p className="text-slate-400 text-sm max-w-md mx-auto">
            There are currently no academic subjects available in the catalog.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {subjects.map((sub) => {
            const enrollment = enrollmentMap.get(sub.id);
            const isEnrolled = !!enrollment;
            const teacherName = enrollment?.teacherFirstName
              ? `Prof. ${enrollment.teacherFirstName} ${enrollment.teacherLastName || ''}`
              : null;

            if (isEnrolled) {
              return (
                <Link
                  key={sub.id}
                  to={`/student/subjects/${sub.id}/chapters`}
                  className="glass-card rounded-2xl p-6 border border-emerald-500/30 hover:border-emerald-500/60 bg-emerald-950/10 hover:bg-emerald-950/20 transition-all group flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-center justify-between gap-2 mb-3">
                      <span className="px-3 py-1 rounded-lg bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 text-xs font-mono font-bold">
                        {sub.code}
                      </span>
                      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-[11px] font-bold">
                        <CheckCircle2 className="w-3 h-3" />
                        <span>Enrolled</span>
                      </span>
                    </div>

                    <h3 className="text-lg font-bold text-white group-hover:text-emerald-300 transition-colors">
                      {sub.name}
                    </h3>

                    {teacherName && (
                      <p className="text-xs text-indigo-300 font-medium mt-1.5 flex items-center gap-1.5">
                        <UserCheck className="w-3.5 h-3.5" />
                        <span>{teacherName}</span>
                      </p>
                    )}

                    {sub.description && (
                      <p className="text-xs text-slate-400 mt-2 line-clamp-3 leading-relaxed">
                        {sub.description}
                      </p>
                    )}
                  </div>

                  <div className="mt-6 pt-4 border-t border-slate-800/80 flex items-center justify-between text-xs font-semibold text-emerald-400">
                    <span>Access Chapters & Quizzes</span>
                    <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                  </div>
                </Link>
              );
            }

            return (
              <div
                key={sub.id}
                className="glass-card rounded-2xl p-6 border border-slate-800/80 opacity-75 hover:opacity-100 transition-all flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-center justify-between gap-2 mb-3">
                    <span className="px-3 py-1 rounded-lg bg-slate-800 text-slate-400 border border-slate-700 text-xs font-mono font-bold">
                      {sub.code}
                    </span>
                    <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/20 text-[11px] font-bold">
                      <Lock className="w-3 h-3" />
                      <span>Not Enrolled</span>
                    </span>
                  </div>

                  <h3 className="text-lg font-bold text-slate-300">
                    {sub.name}
                  </h3>

                  {sub.description && (
                    <p className="text-xs text-slate-400 mt-2 line-clamp-3 leading-relaxed">
                      {sub.description}
                    </p>
                  )}
                </div>

                <div className="mt-6 pt-4 border-t border-slate-800/80">
                  <p className="text-[11px] text-slate-400 italic">
                    Enrollment required to access course content. Contact your instructor.
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
