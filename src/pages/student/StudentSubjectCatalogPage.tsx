import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getSubjects } from '../../api/lmsApi';
import { SubjectDTO } from '../../types/lms';
import { BookOpen, Layers, ArrowRight, Loader2, AlertCircle } from 'lucide-react';

export const StudentSubjectCatalogPage: React.FC = () => {
  const [subjects, setSubjects] = useState<SubjectDTO[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchCatalog = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await getSubjects();
        setSubjects(data);
      } catch (err: any) {
        setError(err.message || 'Failed to load subject catalog.');
      } finally {
        setLoading(false);
      }
    };
    fetchCatalog();
  }, []);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
        <Loader2 className="w-10 h-10 text-indigo-400 animate-spin" />
        <p className="text-slate-400 text-sm font-medium">Loading academic subject catalog...</p>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Header Banner */}
      <div className="glass-panel rounded-2xl p-6 md:p-8 border border-slate-800">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 text-xs font-semibold mb-3">
          <Layers className="w-3.5 h-3.5" />
          <span>Course Navigation</span>
        </div>
        <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight">
          Subject Catalog
        </h1>
        <p className="text-slate-400 text-sm mt-1 max-w-xl">
          Browse available academic subjects, access chapter lecture materials, and attempt chapter assessments.
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
          {subjects.map((sub) => (
            <Link
              key={sub.id}
              to={`/student/subjects/${sub.id}/chapters`}
              className="glass-card rounded-2xl p-6 border border-slate-800 hover:border-indigo-500/40 transition-all group flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between gap-2 mb-3">
                  <span className="px-3 py-1 rounded-lg bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 text-xs font-mono font-bold">
                    {sub.code}
                  </span>
                  <span className="text-xs text-slate-500 font-medium">Subject</span>
                </div>
                <h3 className="text-lg font-bold text-white group-hover:text-indigo-300 transition-colors">
                  {sub.name}
                </h3>
                {sub.description && (
                  <p className="text-xs text-slate-400 mt-2 line-clamp-3 leading-relaxed">
                    {sub.description}
                  </p>
                )}
              </div>

              <div className="mt-6 pt-4 border-t border-slate-800/80 flex items-center justify-between text-xs font-semibold text-indigo-400">
                <span>View Chapters</span>
                <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
};
