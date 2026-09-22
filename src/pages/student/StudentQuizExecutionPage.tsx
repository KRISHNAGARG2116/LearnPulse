import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { getQuizById, submitQuiz } from '../../api/lmsApi';
import { StudentQuizDTO, QuestionAnswerRequest } from '../../types/lms';
import { QuizTimer } from '../../components/QuizTimer';
import {
  HelpCircle,
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  AlertCircle,
  Loader2,
  Send,
} from 'lucide-react';

export const StudentQuizExecutionPage: React.FC = () => {
  const { quizId } = useParams<{ quizId: string }>();
  const navigate = useNavigate();

  const [quiz, setQuiz] = useState<StudentQuizDTO | null>(null);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState<number>(0);
  const [selectedAnswers, setSelectedAnswers] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState<boolean>(true);
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [showConfirmModal, setShowConfirmModal] = useState<boolean>(false);

  useEffect(() => {
    if (!quizId) return;

    const fetchQuiz = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await getQuizById(quizId);
        setQuiz(data);
      } catch (err: any) {
        setError(err.message || 'Failed to load quiz details.');
      } finally {
        setLoading(false);
      }
    };

    fetchQuiz();
  }, [quizId]);

  const handleOptionSelect = (questionId: string, optionLetter: string) => {
    setSelectedAnswers((prev) => ({
      ...prev,
      [questionId]: optionLetter,
    }));
  };

  const handleFinalSubmit = useCallback(async () => {
    if (!quiz || submitting) return;

    setSubmitting(true);
    setError(null);

    try {
      const answerPayload: QuestionAnswerRequest[] = Object.entries(selectedAnswers).map(
        ([questionId, selectedAnswer]) => ({
          questionId,
          selectedAnswer,
        })
      );

      // If any questions were un-answered, assign default option if backend validation requires at least 1 answer
      if (answerPayload.length === 0 && quiz.questions && quiz.questions.length > 0) {
        answerPayload.push({
          questionId: quiz.questions[0].id,
          selectedAnswer: 'A',
        });
      }

      const result = await submitQuiz({
        quizId: quiz.id,
        answers: answerPayload,
      });

      navigate(`/student/quiz/result/${result.id}`, { replace: true });
    } catch (err: any) {
      setError(err.message || 'Quiz submission failed. Please try again.');
      setSubmitting(false);
      setShowConfirmModal(false);
    }
  }, [quiz, selectedAnswers, submitting, navigate]);

  const handleTimerExpire = useCallback(() => {
    handleFinalSubmit();
  }, [handleFinalSubmit]);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
        <Loader2 className="w-10 h-10 text-indigo-400 animate-spin" />
        <p className="text-slate-400 text-sm font-medium">Preparing quiz assessment environment...</p>
      </div>
    );
  }

  if (error || !quiz || !quiz.questions || quiz.questions.length === 0) {
    return (
      <div className="glass-panel rounded-2xl p-12 text-center border border-slate-800 space-y-4">
        <AlertCircle className="w-12 h-12 text-rose-400 mx-auto" />
        <h2 className="text-lg font-bold text-white">Quiz Unavailable</h2>
        <p className="text-slate-400 text-sm max-w-md mx-auto">
          {error || 'This quiz currently has no questions available for execution.'}
        </p>
        <Link
          to="/student/subjects"
          className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-slate-800 text-white text-xs font-semibold hover:bg-slate-700"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Return to Catalog</span>
        </Link>
      </div>
    );
  }

  const currentQuestion = quiz.questions[currentQuestionIndex];
  const totalQuestions = quiz.questions.length;
  const answeredCount = Object.keys(selectedAnswers).length;

  return (
    <div className="space-y-8 max-w-4xl mx-auto">
      {/* Quiz Top Bar */}
      <div className="glass-panel rounded-2xl p-6 border border-slate-800 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 text-xs font-semibold mb-2">
            <HelpCircle className="w-3.5 h-3.5" />
            <span>{quiz.subjectName || 'Course Quiz'}</span>
          </div>
          <h1 className="text-xl md:text-2xl font-extrabold text-white tracking-tight">
            {quiz.title}
          </h1>
        </div>

        <div className="flex items-center gap-4 shrink-0">
          <QuizTimer durationMinutes={15} onExpire={handleTimerExpire} />
        </div>
      </div>

      {/* Question Navigator Dots */}
      <div className="glass-panel rounded-2xl p-4 border border-slate-800 flex items-center justify-between gap-4 overflow-x-auto">
        <span className="text-xs font-semibold text-slate-400 shrink-0">
          Question Navigator ({answeredCount}/{totalQuestions} Answered):
        </span>
        <div className="flex items-center gap-2 overflow-x-auto py-1">
          {quiz.questions.map((q, idx) => {
            const isAnswered = !!selectedAnswers[q.id];
            const isCurrent = idx === currentQuestionIndex;

            return (
              <button
                key={q.id}
                onClick={() => setCurrentQuestionIndex(idx)}
                className={`w-8 h-8 rounded-lg text-xs font-bold transition-all shrink-0 border ${
                  isCurrent
                    ? 'bg-indigo-600 text-white border-indigo-400 shadow-md shadow-indigo-500/30 ring-2 ring-indigo-500/50'
                    : isAnswered
                    ? 'bg-emerald-500/20 text-emerald-400 border-emerald-500/40'
                    : 'bg-slate-800/80 text-slate-400 border-slate-700 hover:bg-slate-700'
                }`}
              >
                {idx + 1}
              </button>
            );
          })}
        </div>
      </div>

      {/* Main Question Card */}
      <div className="glass-panel rounded-2xl p-6 md:p-8 border border-slate-800 space-y-6">
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <span className="text-xs font-bold uppercase tracking-wider text-indigo-400">
            Question {currentQuestionIndex + 1} of {totalQuestions}
          </span>
          <span className="text-xs font-semibold text-slate-400">
            {currentQuestion.marks} {currentQuestion.marks === 1 ? 'Mark' : 'Marks'}
          </span>
        </div>

        {/* Question Text */}
        <h2 className="text-lg md:text-xl font-bold text-white leading-relaxed">
          {currentQuestion.questionText}
        </h2>

        {/* Options Grid */}
        <div className="space-y-3 pt-2">
          {[
            { key: 'A', text: currentQuestion.optionA },
            { key: 'B', text: currentQuestion.optionB },
            { key: 'C', text: currentQuestion.optionC },
            { key: 'D', text: currentQuestion.optionD },
          ].map((opt) => {
            const isSelected = selectedAnswers[currentQuestion.id] === opt.key;

            return (
              <div
                key={opt.key}
                onClick={() => handleOptionSelect(currentQuestion.id, opt.key)}
                className={`p-4 rounded-xl border cursor-pointer flex items-center justify-between gap-4 transition-all ${
                  isSelected
                    ? 'bg-indigo-600/20 border-indigo-500 text-white shadow-lg shadow-indigo-500/10'
                    : 'bg-slate-900/60 border-slate-800 hover:border-slate-700 text-slate-300'
                }`}
              >
                <div className="flex items-center gap-3">
                  <div
                    className={`w-7 h-7 rounded-lg flex items-center justify-center text-xs font-bold border transition-colors ${
                      isSelected
                        ? 'bg-indigo-600 text-white border-indigo-400'
                        : 'bg-slate-800 text-slate-400 border-slate-700'
                    }`}
                  >
                    {opt.key}
                  </div>
                  <span className="text-sm font-medium">{opt.text}</span>
                </div>
                {isSelected && <CheckCircle2 className="w-5 h-5 text-indigo-400 shrink-0" />}
              </div>
            );
          })}
        </div>

        {/* Question Footer Navigation */}
        <div className="pt-6 border-t border-slate-800 flex items-center justify-between gap-4">
          <button
            onClick={() => setCurrentQuestionIndex((prev) => Math.max(0, prev - 1))}
            disabled={currentQuestionIndex === 0}
            className="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed text-white text-xs font-semibold border border-slate-700 inline-flex items-center gap-2 transition-all"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Previous</span>
          </button>

          {currentQuestionIndex < totalQuestions - 1 ? (
            <button
              onClick={() => setCurrentQuestionIndex((prev) => Math.min(totalQuestions - 1, prev + 1))}
              className="glow-button px-5 py-2.5 rounded-xl text-white text-xs font-bold inline-flex items-center gap-2"
            >
              <span>Next Question</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          ) : (
            <button
              onClick={() => setShowConfirmModal(true)}
              className="px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold inline-flex items-center gap-2 shadow-lg shadow-emerald-500/25 transition-all"
            >
              <Send className="w-4 h-4" />
              <span>Review & Submit</span>
            </button>
          )}
        </div>
      </div>

      {/* Confirmation Modal */}
      {showConfirmModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
          <div className="glass-panel rounded-2xl p-6 md:p-8 max-w-md w-full border border-slate-800 space-y-6 animate-in fade-in zoom-in-95">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-indigo-500/20 border border-indigo-500/30 text-indigo-400 flex items-center justify-center shrink-0">
                <Send className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-lg font-bold text-white">Submit Quiz Assessment</h3>
                <p className="text-xs text-slate-400">Are you sure you want to finish?</p>
              </div>
            </div>

            <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 text-xs text-slate-300 space-y-2">
              <div className="flex justify-between">
                <span>Total Questions:</span>
                <span className="font-bold text-white">{totalQuestions}</span>
              </div>
              <div className="flex justify-between">
                <span>Questions Answered:</span>
                <span className="font-bold text-emerald-400">{answeredCount}</span>
              </div>
              <div className="flex justify-between">
                <span>Unanswered Questions:</span>
                <span className="font-bold text-amber-400">{totalQuestions - answeredCount}</span>
              </div>
            </div>

            <div className="flex items-center gap-3 pt-2">
              <button
                onClick={() => setShowConfirmModal(false)}
                disabled={submitting}
                className="flex-1 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold border border-slate-700 transition-all"
              >
                Continue Answering
              </button>
              <button
                onClick={handleFinalSubmit}
                disabled={submitting}
                className="flex-1 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold inline-flex items-center justify-center gap-2 shadow-lg shadow-emerald-500/25 transition-all"
              >
                {submitting ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Grading...</span>
                  </>
                ) : (
                  <span>Confirm Submit</span>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
