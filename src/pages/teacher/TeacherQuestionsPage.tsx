import React, { useState, useEffect } from 'react';
import { getTeacherSubjects, getDocuments, generateAiQuiz, publishQuiz } from '../../api/lmsApi';
import { TeacherSubjectDTO, DocumentDTO, GeneratedQuizResponse } from '../../types/lms';
import {
  Sparkles,
  BookOpen,
  Loader2,
  CheckCircle2,
  AlertCircle,
  HelpCircle,
  Layers,
  Send,
  Edit3,
  Check,
  X
} from 'lucide-react';

export const TeacherQuestionsPage: React.FC = () => {
  const [subjects, setSubjects] = useState<TeacherSubjectDTO[]>([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState<string>('');
  const [documents, setDocuments] = useState<DocumentDTO[]>([]);
  const [selectedDocId, setSelectedDocId] = useState<string>('');

  // AI Quiz Generator Form State
  const [questionCount, setQuestionCount] = useState<number>(5);
  const [difficulty, setDifficulty] = useState<'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'>('INTERMEDIATE');
  const [quizTitle, setQuizTitle] = useState<string>('');
  const [teacherPriorities, setTeacherPriorities] = useState<string>('');
  const [customInstructions, setCustomInstructions] = useState<string>('');

  const [generating, setGenerating] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [successResponse, setSuccessResponse] = useState<GeneratedQuizResponse | null>(null);

  // Publishing State
  const [publishing, setPublishing] = useState<boolean>(false);
  const [publishSuccessMessage, setPublishSuccessMessage] = useState<string | null>(null);
  const [publishError, setPublishError] = useState<string | null>(null);

  // Question Editing State
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [editedQuestionText, setEditedQuestionText] = useState<string>('');
  const [editedOptionA, setEditedOptionA] = useState<string>('');
  const [editedOptionB, setEditedOptionB] = useState<string>('');
  const [editedOptionC, setEditedOptionC] = useState<string>('');
  const [editedOptionD, setEditedOptionD] = useState<string>('');
  const [editedCorrectAnswer, setEditedCorrectAnswer] = useState<string>('A');
  const [editedExplanation, setEditedExplanation] = useState<string>('');

  // Fetch teacher's assigned subjects on mount
  useEffect(() => {
    const fetchSubjects = async () => {
      try {
        const subList = await getTeacherSubjects();
        setSubjects(subList || []);
        if (subList && subList.length > 0) {
          setSelectedSubjectId(subList[0].subjectId);
        }
      } catch (err) {
        console.warn('Failed to load teacher assigned subjects:', err);
      }
    };
    fetchSubjects();
  }, []);

  // Fetch documents when subject changes
  useEffect(() => {
    if (!selectedSubjectId) {
      setDocuments([]);
      setSelectedDocId('');
      return;
    }

    const fetchDocs = async () => {
      try {
        const docs = await getDocuments(selectedSubjectId);
        setDocuments(docs || []);
        if (docs && docs.length > 0) {
          setSelectedDocId(docs[0].id);
        } else {
          setSelectedDocId('');
        }
      } catch (err) {
        console.warn('Failed to load documents for subject:', err);
        setDocuments([]);
        setSelectedDocId('');
      }
    };
    fetchDocs();
  }, [selectedSubjectId]);

  const handleGenerateQuiz = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDocId || generating) return;

    setGenerating(true);
    setError(null);
    setSuccessResponse(null);
    setPublishSuccessMessage(null);
    setPublishError(null);
    setEditingIndex(null);

    try {
      const response = await generateAiQuiz({
        documentId: selectedDocId,
        questionCount,
        difficulty,
        quizTitle: quizTitle.trim() || undefined,
        teacherPriorities: teacherPriorities.trim() || undefined,
        customInstructions: customInstructions.trim() || undefined,
      });

      setSuccessResponse(response);
    } catch (err: any) {
      console.error('Quiz generation error:', err);
      setError(
        err.response?.data?.message ||
        err.message ||
        'Failed to generate AI quiz. Please verify that the document contains text content.'
      );
    } finally {
      setGenerating(false);
    }
  };

  const handlePublishQuiz = async () => {
    if (!successResponse || publishing) return;

    setPublishing(true);
    setPublishError(null);

    try {
      await publishQuiz(successResponse.quizId);
      setSuccessResponse((prev) => {
        if (!prev) return null;
        return {
          ...prev,
          status: 'PUBLISHED' as any,
          isPublished: true,
        };
      });
      setPublishSuccessMessage('Quiz published successfully! Students enrolled in this subject can now take this assessment.');
    } catch (err: any) {
      console.error('Failed to publish quiz:', err);
      setPublishError(
        err.response?.data?.message ||
        err.message ||
        'Failed to transition quiz to PUBLISHED status.'
      );
    } finally {
      setPublishing(false);
    }
  };

  const startEditQuestion = (index: number) => {
    if (!successResponse) return;
    const q = successResponse.questions[index];
    setEditingIndex(index);
    setEditedQuestionText(q.questionText);
    setEditedOptionA(q.optionA);
    setEditedOptionB(q.optionB);
    setEditedOptionC(q.optionC);
    setEditedOptionD(q.optionD);
    setEditedCorrectAnswer(q.correctAnswer);
    setEditedExplanation(q.explanation || '');
  };

  const saveEditQuestion = () => {
    if (editingIndex === null || !successResponse) return;

    const updatedQuestions = [...successResponse.questions];
    updatedQuestions[editingIndex] = {
      ...updatedQuestions[editingIndex],
      questionText: editedQuestionText.trim(),
      optionA: editedOptionA.trim(),
      optionB: editedOptionB.trim(),
      optionC: editedOptionC.trim(),
      optionD: editedOptionD.trim(),
      correctAnswer: editedCorrectAnswer,
      explanation: editedExplanation.trim() || undefined,
    };

    setSuccessResponse({
      ...successResponse,
      questions: updatedQuestions,
    });
    setEditingIndex(null);
  };

  return (
    <div className="p-4 sm:p-6 max-w-6xl mx-auto space-y-6">
      {/* Header */}
      <div className="bg-slate-900/60 border border-slate-800 p-4 rounded-2xl backdrop-blur-md flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-purple-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-purple-500/20">
            <Sparkles className="w-5 h-5 text-white" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-lg font-bold text-white">AI Quiz & Assessment Generator</h1>
              <span className="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-purple-500/10 text-purple-400 border border-purple-500/20">
                Teacher Engine • Day 67
              </span>
            </div>
            <p className="text-xs text-slate-400">
              Generate curriculum-grounded quizzes from uploaded course materials with teacher priority steering.
            </p>
          </div>
        </div>
      </div>

      {/* Generator Configuration Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Form Panel (5 cols) */}
        <div className="lg:col-span-5 bg-slate-900/40 border border-slate-800 rounded-2xl p-5 space-y-4 shadow-xl">
          <h2 className="text-sm font-bold text-white flex items-center gap-2 pb-2 border-b border-slate-800">
            <Layers className="w-4 h-4 text-purple-400" />
            Generation Parameters
          </h2>

          <form onSubmit={handleGenerateQuiz} className="space-y-4">
            {/* Subject Selector */}
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-300">Teaching Subject:</label>
              <select
                value={selectedSubjectId}
                onChange={(e) => setSelectedSubjectId(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 focus:border-purple-500 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none"
              >
                {subjects.map((sub) => (
                  <option key={sub.subjectId} value={sub.subjectId}>
                    {sub.subjectName} ({sub.subjectCode})
                  </option>
                ))}
              </select>
            </div>

            {/* Document Selector */}
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-300">Source Document Material:</label>
              {documents.length > 0 ? (
                <select
                  value={selectedDocId}
                  onChange={(e) => setSelectedDocId(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 focus:border-purple-500 rounded-xl px-3 py-2 text-xs text-indigo-300 focus:outline-none font-medium"
                >
                  {documents.map((doc) => (
                    <option key={doc.id} value={doc.id}>
                      📄 {doc.originalFileName}
                    </option>
                  ))}
                </select>
              ) : (
                <p className="text-xs text-amber-400 bg-amber-950/30 p-2.5 rounded-xl border border-amber-800/40">
                  No documents found for this subject. Please upload material in Content first.
                </p>
              )}
            </div>

            {/* Quiz Title */}
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-300">Quiz Title (Optional):</label>
              <input
                type="text"
                placeholder="e.g. Chapter 4 OSI Architecture Assessment"
                value={quizTitle}
                onChange={(e) => setQuizTitle(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 focus:border-purple-500 rounded-xl px-3 py-2 text-xs text-slate-200 placeholder:text-slate-500 focus:outline-none"
              />
            </div>

            {/* Questions Count & Difficulty */}
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-300">Question Count:</label>
                <select
                  value={questionCount}
                  onChange={(e) => setQuestionCount(Number(e.target.value))}
                  className="w-full bg-slate-950 border border-slate-800 focus:border-purple-500 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none"
                >
                  <option value={3}>3 Questions</option>
                  <option value={5}>5 Questions</option>
                  <option value={10}>10 Questions</option>
                  <option value={15}>15 Questions</option>
                  <option value={20}>20 Questions</option>
                </select>
              </div>

              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-300">Target Difficulty:</label>
                <select
                  value={difficulty}
                  onChange={(e) => setDifficulty(e.target.value as any)}
                  className="w-full bg-slate-950 border border-slate-800 focus:border-purple-500 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none"
                >
                  <option value="BEGINNER">Beginner</option>
                  <option value="INTERMEDIATE">Intermediate</option>
                  <option value="ADVANCED">Advanced</option>
                </select>
              </div>
            </div>

            {/* Teacher Priorities */}
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-300">Teacher Topic Priorities:</label>
              <input
                type="text"
                placeholder="e.g. Emphasize Transport layer headers & TCP handshake"
                value={teacherPriorities}
                onChange={(e) => setTeacherPriorities(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 focus:border-purple-500 rounded-xl px-3 py-2 text-xs text-slate-200 placeholder:text-slate-500 focus:outline-none"
              />
            </div>

            {/* Custom Instructions */}
            <div className="space-y-1">
              <label className="text-xs font-semibold text-slate-300">Custom Instructions (Prompt Steering):</label>
              <textarea
                rows={2}
                placeholder="e.g. Include scenario-based network troubleshooting questions."
                value={customInstructions}
                onChange={(e) => setCustomInstructions(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 focus:border-purple-500 rounded-xl p-2.5 text-xs text-slate-200 placeholder:text-slate-500 focus:outline-none"
              />
            </div>

            {error && (
              <div className="p-3 rounded-xl bg-rose-950/60 border border-rose-800/60 text-xs text-rose-200 flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
                <span>{error}</span>
              </div>
            )}

            <button
              type="submit"
              disabled={!selectedDocId || generating}
              className="w-full py-3 rounded-xl bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white font-bold text-xs sm:text-sm shadow-lg shadow-purple-600/25 disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-2 transition"
            >
              {generating ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Generating Questions Grounded in Material...
                </>
              ) : (
                <>
                  <Sparkles className="w-4 h-4" />
                  Generate AI Quiz & Save as DRAFT
                </>
              )}
            </button>
          </form>
        </div>

        {/* Results Preview Panel (7 cols) */}
        <div className="lg:col-span-7 bg-slate-900/40 border border-slate-800 rounded-2xl p-5 flex flex-col min-h-[500px] shadow-xl">
          <div className="pb-3 border-b border-slate-800 flex items-center justify-between">
            <h2 className="text-sm font-bold text-white flex items-center gap-2">
              <BookOpen className="w-4 h-4 text-indigo-400" />
              Generated Quiz Output & Management
            </h2>
            {successResponse && (
              <span
                className={`px-2.5 py-0.5 rounded-full text-[11px] font-semibold border ${
                  successResponse.isPublished
                    ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                    : 'bg-amber-500/10 text-amber-400 border-amber-500/30'
                }`}
              >
                {successResponse.isPublished ? 'PUBLISHED' : 'DRAFT'}
              </span>
            )}
          </div>

          <div className="flex-1 py-3 overflow-y-auto">
            {generating && (
              <div className="h-full flex flex-col items-center justify-center p-8 text-center space-y-3">
                <Loader2 className="w-10 h-10 text-purple-400 animate-spin" />
                <p className="text-sm font-semibold text-slate-200">
                  Spring AI ChatClient is analyzing document content...
                </p>
                <p className="text-xs text-slate-400 max-w-sm">
                  Grounding questions in material text, validating 4 distinct options per question, and assigning explanations.
                </p>
              </div>
            )}

            {!generating && !successResponse && (
              <div className="h-full flex flex-col items-center justify-center p-8 text-center text-slate-500 space-y-2">
                <HelpCircle className="w-12 h-12 text-slate-600" />
                <p className="text-sm font-medium">No quiz generated yet</p>
                <p className="text-xs text-slate-600 max-w-sm">
                  Select a course document on the left and configure your topic priorities to synthesize a draft quiz.
                </p>
              </div>
            )}

            {!generating && successResponse && (
              <div className="space-y-4">
                {/* Quiz Summary Card */}
                <div className="p-4 rounded-xl bg-slate-950/80 border border-purple-500/30 space-y-3">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                    <div>
                      <h3 className="text-base font-bold text-white">{successResponse.title}</h3>
                      <p className="text-xs text-slate-300 mt-0.5">
                        Subject: <strong className="text-indigo-400">{successResponse.subjectName}</strong> • Material:{' '}
                        <strong className="text-indigo-400">{successResponse.documentFileName}</strong>
                      </p>
                    </div>

                    {/* Publish Control */}
                    {!successResponse.isPublished ? (
                      <button
                        onClick={handlePublishQuiz}
                        disabled={publishing}
                        className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-xs font-bold text-white shadow-md shadow-emerald-600/30 transition disabled:opacity-40 self-start sm:self-auto shrink-0"
                      >
                        {publishing ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Send className="w-3.5 h-3.5" />}
                        Publish Quiz
                      </button>
                    ) : (
                      <span className="flex items-center gap-1.5 px-3 py-1 rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 text-xs font-bold">
                        <CheckCircle2 className="w-4 h-4" /> Live for Students
                      </span>
                    )}
                  </div>

                  {publishSuccessMessage && (
                    <div className="p-2.5 rounded-lg bg-emerald-950/40 border border-emerald-800/40 text-xs text-emerald-300 flex items-center gap-2">
                      <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                      <span>{publishSuccessMessage}</span>
                    </div>
                  )}

                  {publishError && (
                    <div className="p-2.5 rounded-lg bg-rose-950/40 border border-rose-800/40 text-xs text-rose-300 flex items-center gap-2">
                      <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
                      <span>{publishError}</span>
                    </div>
                  )}

                  <div className="flex items-center justify-between text-[11px] text-slate-400 pt-2 border-t border-slate-800/80">
                    <span>{successResponse.questionsCount} Questions • {successResponse.totalMarks} Total Marks</span>
                    <span>Status: <strong className={successResponse.isPublished ? 'text-emerald-400' : 'text-amber-400'}>{successResponse.isPublished ? 'PUBLISHED' : 'DRAFT'}</strong></span>
                  </div>
                </div>

                {/* Questions List */}
                <div className="space-y-3">
                  {successResponse.questions.map((q, idx) => {
                    const isEditing = editingIndex === idx;

                    return (
                      <div
                        key={idx}
                        className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 space-y-3"
                      >
                        <div className="flex items-start justify-between gap-2">
                          <span className="text-xs font-bold text-slate-200">
                            Question {idx + 1}
                          </span>
                          <div className="flex items-center gap-2">
                            <span className="text-[10px] font-semibold text-purple-300 bg-purple-950 px-2 py-0.5 rounded shrink-0">
                              AI_GENERATED
                            </span>
                            {!isEditing ? (
                              <button
                                onClick={() => startEditQuestion(idx)}
                                className="p-1 text-slate-400 hover:text-indigo-400 transition"
                                title="Edit Question"
                              >
                                <Edit3 className="w-3.5 h-3.5" />
                              </button>
                            ) : (
                              <div className="flex items-center gap-1">
                                <button
                                  onClick={saveEditQuestion}
                                  className="p-1 text-emerald-400 hover:text-emerald-300"
                                  title="Save Changes"
                                >
                                  <Check className="w-4 h-4" />
                                </button>
                                <button
                                  onClick={() => setEditingIndex(null)}
                                  className="p-1 text-slate-400 hover:text-rose-400"
                                  title="Cancel"
                                >
                                  <X className="w-4 h-4" />
                                </button>
                              </div>
                            )}
                          </div>
                        </div>

                        {/* Question Text */}
                        {!isEditing ? (
                          <p className="text-xs text-slate-200 font-medium">{q.questionText}</p>
                        ) : (
                          <div className="space-y-1">
                            <label className="text-[10px] font-semibold text-slate-400">Question Text:</label>
                            <input
                              type="text"
                              value={editedQuestionText}
                              onChange={(e) => setEditedQuestionText(e.target.value)}
                              className="w-full bg-slate-950 border border-indigo-500/50 rounded-lg p-2 text-xs text-slate-100 focus:outline-none"
                            />
                          </div>
                        )}

                        {/* Options Grid */}
                        {!isEditing ? (
                          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
                            <div
                              className={`p-2 rounded-lg border ${
                                q.correctAnswer === 'A'
                                  ? 'bg-emerald-950/40 border-emerald-500/50 text-emerald-200 font-semibold'
                                  : 'bg-slate-950/60 border-slate-800 text-slate-300'
                              }`}
                            >
                              A) {q.optionA}
                            </div>
                            <div
                              className={`p-2 rounded-lg border ${
                                q.correctAnswer === 'B'
                                  ? 'bg-emerald-950/40 border-emerald-500/50 text-emerald-200 font-semibold'
                                  : 'bg-slate-950/60 border-slate-800 text-slate-300'
                              }`}
                            >
                              B) {q.optionB}
                            </div>
                            <div
                              className={`p-2 rounded-lg border ${
                                q.correctAnswer === 'C'
                                  ? 'bg-emerald-950/40 border-emerald-500/50 text-emerald-200 font-semibold'
                                  : 'bg-slate-950/60 border-slate-800 text-slate-300'
                              }`}
                            >
                              C) {q.optionC}
                            </div>
                            <div
                              className={`p-2 rounded-lg border ${
                                q.correctAnswer === 'D'
                                  ? 'bg-emerald-950/40 border-emerald-500/50 text-emerald-200 font-semibold'
                                  : 'bg-slate-950/60 border-slate-800 text-slate-300'
                              }`}
                            >
                              D) {q.optionD}
                            </div>
                          </div>
                        ) : (
                          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
                            <div className="space-y-1">
                              <label className="text-[10px] text-slate-400">Option A:</label>
                              <input
                                type="text"
                                value={editedOptionA}
                                onChange={(e) => setEditedOptionA(e.target.value)}
                                className="w-full bg-slate-950 border border-slate-800 rounded p-1.5 text-xs text-slate-200"
                              />
                            </div>
                            <div className="space-y-1">
                              <label className="text-[10px] text-slate-400">Option B:</label>
                              <input
                                type="text"
                                value={editedOptionB}
                                onChange={(e) => setEditedOptionB(e.target.value)}
                                className="w-full bg-slate-950 border border-slate-800 rounded p-1.5 text-xs text-slate-200"
                              />
                            </div>
                            <div className="space-y-1">
                              <label className="text-[10px] text-slate-400">Option C:</label>
                              <input
                                type="text"
                                value={editedOptionC}
                                onChange={(e) => setEditedOptionC(e.target.value)}
                                className="w-full bg-slate-950 border border-slate-800 rounded p-1.5 text-xs text-slate-200"
                              />
                            </div>
                            <div className="space-y-1">
                              <label className="text-[10px] text-slate-400">Option D:</label>
                              <input
                                type="text"
                                value={editedOptionD}
                                onChange={(e) => setEditedOptionD(e.target.value)}
                                className="w-full bg-slate-950 border border-slate-800 rounded p-1.5 text-xs text-slate-200"
                              />
                            </div>
                            <div className="sm:col-span-2 space-y-1 pt-1">
                              <label className="text-[10px] text-slate-400">Correct Answer Option:</label>
                              <select
                                value={editedCorrectAnswer}
                                onChange={(e) => setEditedCorrectAnswer(e.target.value)}
                                className="bg-slate-950 border border-slate-800 rounded px-2 py-1 text-xs text-emerald-400 font-bold"
                              >
                                <option value="A">Option A</option>
                                <option value="B">Option B</option>
                                <option value="C">Option C</option>
                                <option value="D">Option D</option>
                              </select>
                            </div>
                          </div>
                        )}

                        {/* Explanation */}
                        {!isEditing ? (
                          q.explanation && (
                            <div className="p-2.5 rounded-lg bg-indigo-950/40 border border-indigo-900/40 text-xs text-indigo-200">
                              <strong className="text-indigo-300">Explanation: </strong>
                              {q.explanation}
                            </div>
                          )
                        ) : (
                          <div className="space-y-1">
                            <label className="text-[10px] text-slate-400">Explanation:</label>
                            <input
                              type="text"
                              value={editedExplanation}
                              onChange={(e) => setEditedExplanation(e.target.value)}
                              className="w-full bg-slate-950 border border-slate-800 rounded p-1.5 text-xs text-indigo-200"
                            />
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
