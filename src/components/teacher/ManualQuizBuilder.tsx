import React, { useEffect, useState } from 'react';
import { getSubjects, getChapters, createQuiz } from '../../api/lmsApi';
import { SubjectDTO, ChapterDTO, CreateQuestionRequest, CreateQuizRequest } from '../../types/lms';
import {
  Plus,
  Trash2,
  CheckCircle2,
  AlertCircle,
  Loader2,
  HelpCircle,
  FileCheck,
} from 'lucide-react';

interface ManualQuizBuilderProps {
  onSuccess?: () => void;
}

const emptyQuestion = (): CreateQuestionRequest => ({
  questionText: '',
  optionA: '',
  optionB: '',
  optionC: '',
  optionD: '',
  correctAnswer: 'A',
  marks: 1,
});

export const ManualQuizBuilder: React.FC<ManualQuizBuilderProps> = ({ onSuccess }) => {
  const [subjects, setSubjects] = useState<SubjectDTO[]>([]);
  const [chapters, setChapters] = useState<ChapterDTO[]>([]);

  const [title, setTitle] = useState<string>('');
  const [description, setDescription] = useState<string>('');
  const [selectedSubjectId, setSelectedSubjectId] = useState<string>('');
  const [selectedChapterId, setSelectedChapterId] = useState<string>('');

  const [questions, setQuestions] = useState<CreateQuestionRequest[]>([emptyQuestion()]);

  const [loadingSubjects, setLoadingSubjects] = useState<boolean>(true);
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadSubjects = async () => {
      try {
        const data = await getSubjects();
        setSubjects(data);
      } catch {
        setError('Failed to load academic subjects.');
      } finally {
        setLoadingSubjects(false);
      }
    };
    loadSubjects();
  }, []);

  useEffect(() => {
    if (!selectedSubjectId) {
      setChapters([]);
      setSelectedChapterId('');
      return;
    }

    const loadChapters = async () => {
      try {
        const data = await getChapters(selectedSubjectId);
        setChapters(data);
      } catch {
        setError('Failed to load subject chapters.');
      }
    };

    loadChapters();
  }, [selectedSubjectId]);

  const handleAddQuestion = () => {
    setQuestions((prev) => [...prev, emptyQuestion()]);
  };

  const handleRemoveQuestion = (index: number) => {
    if (questions.length <= 1) return;
    setQuestions((prev) => prev.filter((_, i) => i !== index));
  };

  const handleQuestionChange = (index: number, field: keyof CreateQuestionRequest, value: any) => {
    setQuestions((prev) => {
      const updated = [...prev];
      updated[index] = { ...updated[index], [field]: value };
      return updated;
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMessage(null);

    if (!title.trim() || title.length < 3) {
      setError('Quiz title must be at least 3 characters.');
      return;
    }

    if (!selectedSubjectId) {
      setError('Please select an academic subject for the quiz.');
      return;
    }

    // Validate questions
    for (let i = 0; i < questions.length; i++) {
      const q = questions[i];
      if (!q.questionText.trim()) {
        setError(`Question ${i + 1} text is required.`);
        return;
      }
      if (!q.optionA.trim() || !q.optionB.trim() || !q.optionC.trim() || !q.optionD.trim()) {
        setError(`Question ${i + 1} requires all 4 options (A, B, C, D).`);
        return;
      }
      if (!['A', 'B', 'C', 'D'].includes(q.correctAnswer)) {
        setError(`Question ${i + 1} requires a valid correct answer (A, B, C, or D).`);
        return;
      }
    }

    setSubmitting(true);

    try {
      const payload: CreateQuizRequest = {
        title: title.trim(),
        description: description.trim() || undefined,
        subjectId: selectedSubjectId,
        chapterId: selectedChapterId || undefined,
        isPublished: true,
        status: 'PUBLISHED',
        questions: questions.map((q) => ({
          ...q,
          questionText: q.questionText.trim(),
          optionA: q.optionA.trim(),
          optionB: q.optionB.trim(),
          optionC: q.optionC.trim(),
          optionD: q.optionD.trim(),
          correctAnswer: q.correctAnswer.toUpperCase(),
          marks: q.marks && q.marks > 0 ? q.marks : 1,
        })),
      };

      await createQuiz(payload);

      setSuccessMessage(`Quiz "${title}" created and published successfully!`);
      setTitle('');
      setDescription('');
      setSelectedSubjectId('');
      setSelectedChapterId('');
      setQuestions([emptyQuestion()]);
      if (onSuccess) onSuccess();
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to create quiz.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {successMessage && (
        <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center gap-3 text-emerald-400 text-sm">
          <CheckCircle2 className="w-5 h-5 shrink-0" />
          <span>{successMessage}</span>
        </div>
      )}

      {error && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center gap-3 text-rose-400 text-sm">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Basic Metadata */}
      <div className="space-y-4">
        <div>
          <label className="text-xs font-bold text-slate-300 uppercase tracking-wider block mb-1">
            Quiz Title *
          </label>
          <input
            type="text"
            placeholder="e.g. Chapter 1 Basics & Architecture Quiz"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            disabled={submitting}
            className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-800 text-white text-sm focus:outline-none focus:border-indigo-500 transition-colors"
          />
        </div>

        <div>
          <label className="text-xs font-bold text-slate-300 uppercase tracking-wider block mb-1">
            Description (Optional)
          </label>
          <textarea
            placeholder="Brief instructions or summary of topics assessed..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            disabled={submitting}
            rows={2}
            className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-800 text-white text-sm focus:outline-none focus:border-indigo-500 transition-colors"
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="text-xs font-bold text-slate-300 uppercase tracking-wider block mb-1">
              Subject *
            </label>
            <select
              value={selectedSubjectId}
              onChange={(e) => setSelectedSubjectId(e.target.value)}
              disabled={loadingSubjects || submitting}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-800 text-white text-sm focus:outline-none focus:border-indigo-500 transition-colors"
            >
              <option value="">-- Select Subject --</option>
              {subjects.map((sub) => (
                <option key={sub.id} value={sub.id}>
                  {sub.code} - {sub.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="text-xs font-bold text-slate-300 uppercase tracking-wider block mb-1">
              Chapter (Optional)
            </label>
            <select
              value={selectedChapterId}
              onChange={(e) => setSelectedChapterId(e.target.value)}
              disabled={!selectedSubjectId || submitting}
              className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-800 text-white text-sm focus:outline-none focus:border-indigo-500 transition-colors disabled:opacity-50"
            >
              <option value="">-- Final Course Quiz / General --</option>
              {chapters.map((chap) => (
                <option key={chap.id} value={chap.id}>
                  Ch.{chap.chapterNumber}: {chap.title}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Dynamic Questions Builder */}
      <div className="space-y-6 pt-4 border-t border-slate-800">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <HelpCircle className="w-5 h-5 text-indigo-400" />
            <span>Quiz Questions ({questions.length})</span>
          </h3>
          <button
            type="button"
            onClick={handleAddQuestion}
            className="px-3.5 py-1.5 rounded-xl bg-indigo-600/20 hover:bg-indigo-600/30 text-indigo-300 border border-indigo-500/30 text-xs font-bold inline-flex items-center gap-1.5 transition-all"
          >
            <Plus className="w-4 h-4" />
            <span>Add Question</span>
          </button>
        </div>

        {questions.map((q, idx) => (
          <div key={idx} className="p-5 rounded-xl border border-slate-800 bg-slate-900/60 space-y-4 relative">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-indigo-400 uppercase tracking-wider">
                Question {idx + 1}
              </span>
              {questions.length > 1 && (
                <button
                  type="button"
                  onClick={() => handleRemoveQuestion(idx)}
                  className="text-slate-400 hover:text-rose-400 text-xs inline-flex items-center gap-1 transition-colors"
                >
                  <Trash2 className="w-4 h-4" />
                  <span>Remove</span>
                </button>
              )}
            </div>

            <input
              type="text"
              placeholder={`Enter Question ${idx + 1} text...`}
              value={q.questionText}
              onChange={(e) => handleQuestionChange(idx, 'questionText', e.target.value)}
              disabled={submitting}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-900 border border-slate-800 text-white text-sm focus:outline-none focus:border-indigo-500"
            />

            {/* Options A, B, C, D */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {(['A', 'B', 'C', 'D'] as const).map((opt) => {
                const fieldName = `option${opt}` as keyof CreateQuestionRequest;
                return (
                  <div key={opt} className="flex items-center gap-2">
                    <span className="w-7 h-7 rounded-lg bg-slate-800 border border-slate-700 text-slate-300 text-xs font-bold flex items-center justify-center shrink-0">
                      {opt}
                    </span>
                    <input
                      type="text"
                      placeholder={`Option ${opt} text`}
                      value={q[fieldName] as string}
                      onChange={(e) => handleQuestionChange(idx, fieldName, e.target.value)}
                      disabled={submitting}
                      className="w-full px-3 py-2 rounded-xl bg-slate-900 border border-slate-800 text-white text-xs focus:outline-none focus:border-indigo-500"
                    />
                  </div>
                );
              })}
            </div>

            {/* Correct Answer & Marks Selector */}
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pt-2 border-t border-slate-800/80">
              <div className="flex items-center gap-2">
                <label className="text-xs font-semibold text-slate-300">Correct Answer:</label>
                <select
                  value={q.correctAnswer}
                  onChange={(e) => handleQuestionChange(idx, 'correctAnswer', e.target.value)}
                  disabled={submitting}
                  className="px-3 py-1.5 rounded-lg bg-slate-800 border border-slate-700 text-emerald-400 font-bold text-xs focus:outline-none"
                >
                  <option value="A">Option A</option>
                  <option value="B">Option B</option>
                  <option value="C">Option C</option>
                  <option value="D">Option D</option>
                </select>
              </div>

              <div className="flex items-center gap-2">
                <label className="text-xs font-semibold text-slate-300">Marks:</label>
                <input
                  type="number"
                  min={1}
                  max={10}
                  value={q.marks || 1}
                  onChange={(e) => handleQuestionChange(idx, 'marks', parseInt(e.target.value) || 1)}
                  disabled={submitting}
                  className="w-16 px-3 py-1.5 rounded-lg bg-slate-800 border border-slate-700 text-white text-xs font-bold"
                />
              </div>
            </div>
          </div>
        ))}
      </div>

      <button
        type="submit"
        disabled={submitting}
        className="w-full py-3.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed text-white text-sm font-bold inline-flex items-center justify-center gap-2 shadow-lg shadow-indigo-500/25 transition-all"
      >
        {submitting ? (
          <>
            <Loader2 className="w-4 h-4 animate-spin" />
            <span>Persisting Quiz & Questions...</span>
          </>
        ) : (
          <>
            <FileCheck className="w-4 h-4" />
            <span>Create & Publish Manual Quiz</span>
          </>
        )}
      </button>
    </form>
  );
};
