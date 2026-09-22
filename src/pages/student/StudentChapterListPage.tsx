import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getSubjectById, getChapters, getQuizzes } from '../../api/lmsApi';
import { SubjectDTO, ChapterDTO, StudentQuizDTO } from '../../types/lms';
import {
  BookOpen,
  FileText,
  HelpCircle,
  ArrowLeft,
  Loader2,
  AlertCircle,
  Layers,
} from 'lucide-react';

export const StudentChapterListPage: React.FC = () => {
  const { subjectId } = useParams<{ subjectId: string }>();
  const [subject, setSubject] = useState<SubjectDTO | null>(null);
  const [chapters, setChapters] = useState<ChapterDTO[]>([]);
  const [quizzes, setQuizzes] = useState<StudentQuizDTO[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!subjectId) return;

    const fetchSubjectData = async () => {
      setLoading(true);
      setError(null);
      try {
        const [subData, chapData, quizData] = await Promise.all([
          getSubjectById(subjectId).catch(() => null),
          getChapters(subjectId).catch(() => []),
          getQuizzes(subjectId).catch(() => []),
        ]);
        setSubject(subData);
        setChapters(chapData);
        setQuizzes(quizData);
      } catch (err: any) {
        setError(err.message || 'Failed to load chapter list.');
      } finally {
        setLoading(false);
      }
    };

    fetchSubjectData();
  }, [subjectId]);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
        <Loader2 className="w-10 h-10 text-indigo-400 animate-spin" />
        <p className="text-slate-400 text-sm font-medium">Loading chapter list...</p>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Back Button */}
      <Link
        to="/student/subjects"
        className="inline-flex items-center gap-2 text-xs font-semibold text-slate-400 hover:text-white transition-colors"
      >
        <ArrowLeft className="w-4 h-4" />
        <span>Back to Subject Catalog</span>
      </Link>

      {/* Header Banner */}
      <div className="glass-panel rounded-2xl p-6 md:p-8 border border-slate-800 space-y-3">
        <div className="flex items-center gap-3">
          <span className="px-3 py-1 rounded-lg bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 text-xs font-mono font-bold">
            {subject?.code || 'SUBJECT'}
          </span>
        </div>
        <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight">
          {subject?.name || 'Academic Subject'}
        </h1>
        {subject?.description && (
          <p className="text-slate-400 text-sm max-w-2xl">{subject.description}</p>
        )}
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center gap-3 text-rose-400 text-sm">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Chapters List */}
      <div className="glass-panel rounded-2xl p-6 border border-slate-800 space-y-4">
        <h2 className="text-lg font-bold text-white flex items-center gap-2 mb-4">
          <Layers className="w-5 h-5 text-indigo-400" />
          <span>Course Chapters ({chapters.length})</span>
        </h2>

        {chapters.length === 0 ? (
          <div className="p-8 text-center border border-dashed border-slate-800 rounded-xl space-y-2">
            <BookOpen className="w-8 h-8 text-slate-600 mx-auto" />
            <p className="text-slate-400 text-sm font-medium">No chapters available for this subject.</p>
          </div>
        ) : (
          <div className="space-y-4">
            {chapters.map((chap) => {
              // Find quiz for this chapter if any
              const chapterQuiz = quizzes.find((q) => q.chapterId === chap.id);

              return (
                <div
                  key={chap.id}
                  className="p-5 rounded-xl border border-slate-800 bg-slate-900/60 hover:border-indigo-500/40 transition-all flex flex-col md:flex-row md:items-center justify-between gap-4"
                >
                  <div className="flex items-start gap-4">
                    <div className="w-10 h-10 rounded-xl bg-indigo-500/20 border border-indigo-500/30 text-indigo-400 flex items-center justify-center text-sm font-bold shrink-0">
                      Ch.{chap.chapterNumber}
                    </div>
                    <div>
                      <h3 className="text-base font-bold text-white">{chap.title}</h3>
                      {chap.description && (
                        <p className="text-xs text-slate-400 mt-1">{chap.description}</p>
                      )}
                    </div>
                  </div>

                  <div className="flex items-center gap-3 shrink-0">
                    <Link
                      to={`/student/chapters/${chap.id}/materials`}
                      className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-white text-xs font-semibold border border-slate-700 inline-flex items-center gap-2 transition-all"
                    >
                      <FileText className="w-4 h-4 text-indigo-400" />
                      <span>Study Materials</span>
                    </Link>

                    {chapterQuiz && (
                      <Link
                        to={`/student/quiz/${chapterQuiz.id}`}
                        className="glow-button px-4 py-2 rounded-xl text-white text-xs font-bold inline-flex items-center gap-2 shadow-lg shadow-indigo-500/20"
                      >
                        <HelpCircle className="w-4 h-4" />
                        <span>Take Quiz</span>
                      </Link>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
