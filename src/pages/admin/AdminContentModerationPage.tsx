import React, { useState, useEffect, useCallback } from 'react';
import {
  getAdminDocuments,
  updateAdminDocumentStatus,
  getAdminQuizzes,
  updateAdminQuizStatus
} from '../../api/lmsApi';
import { AdminDocumentDTO, AdminQuizDTO } from '../../types/lms';
import {
  FolderKanban,
  FileText,
  FileQuestion,
  CheckCircle2,
  AlertCircle,
  Loader2,
  X,
  Eye,
  EyeOff,
  Filter
} from 'lucide-react';

export const AdminContentModerationPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'DOCUMENTS' | 'QUIZZES'>('DOCUMENTS');

  // Documents state
  const [documents, setDocuments] = useState<AdminDocumentDTO[]>([]);
  const [docsLoading, setDocsLoading] = useState(true);
  const [docFilter, setDocFilter] = useState<string>('ALL');

  // Quizzes state
  const [quizzes, setQuizzes] = useState<AdminQuizDTO[]>([]);
  const [quizzesLoading, setQuizzesLoading] = useState(true);
  const [quizFilter, setQuizFilter] = useState<string>('ALL');

  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [actionPending, setActionPending] = useState(false);

  const fetchDocuments = useCallback(async () => {
    try {
      setDocsLoading(true);
      const active = docFilter === 'ACTIVE' ? true : docFilter === 'INACTIVE' ? false : undefined;
      const data = await getAdminDocuments(active);
      setDocuments(data || []);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to fetch uploaded documents');
    } finally {
      setDocsLoading(false);
    }
  }, [docFilter]);

  const fetchQuizzes = useCallback(async () => {
    try {
      setQuizzesLoading(true);
      const status = quizFilter !== 'ALL' ? quizFilter : undefined;
      const data = await getAdminQuizzes(status);
      setQuizzes(data || []);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to fetch quizzes');
    } finally {
      setQuizzesLoading(false);
    }
  }, [quizFilter]);

  useEffect(() => {
    if (activeTab === 'DOCUMENTS') {
      fetchDocuments();
    } else {
      fetchQuizzes();
    }
  }, [activeTab, fetchDocuments, fetchQuizzes]);

  const handleToggleDocStatus = async (doc: AdminDocumentDTO) => {
    try {
      setActionPending(true);
      setError(null);
      const updated = await updateAdminDocumentStatus(doc.id, !doc.active);
      setSuccessMsg(`Document "${updated.originalFileName}" is now ${updated.active ? 'ACTIVE' : 'INACTIVE'}.`);
      fetchDocuments();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to update document status');
    } finally {
      setActionPending(false);
    }
  };

  const handleToggleQuizPublish = async (quiz: AdminQuizDTO) => {
    try {
      setActionPending(true);
      setError(null);
      const newPublished = !quiz.isPublished;
      const newStatus = newPublished ? 'PUBLISHED' : 'DRAFT';
      const updated = await updateAdminQuizStatus(quiz.id, newStatus, newPublished);
      setSuccessMsg(`Quiz "${updated.title}" status updated to ${updated.status}.`);
      fetchQuizzes();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to update quiz status');
    } finally {
      setActionPending(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-black text-white tracking-tight flex items-center gap-3">
          <FolderKanban className="w-7 h-7 text-indigo-400" />
          Content Catalog Moderation
        </h1>
        <p className="text-xs text-slate-400 mt-1">
          Review, activate, deactivate, or unpublish syllabus documents and academic quizzes
        </p>
      </div>

      {/* Alerts */}
      {error && (
        <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/30 text-red-400 flex items-center justify-between text-xs">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{error}</span>
          </div>
          <button onClick={() => setError(null)} className="text-red-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {successMsg && (
        <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 flex items-center justify-between text-xs">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 shrink-0" />
            <span>{successMsg}</span>
          </div>
          <button onClick={() => setSuccessMsg(null)} className="text-emerald-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Tabs */}
      <div className="flex items-center gap-3 border-b border-slate-800 pb-3">
        <button
          onClick={() => setActiveTab('DOCUMENTS')}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            activeTab === 'DOCUMENTS'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/30'
              : 'bg-slate-900 text-slate-400 hover:text-white'
          }`}
        >
          <FileText className="w-4 h-4" />
          Curriculum Documents ({documents.length})
        </button>
        <button
          onClick={() => setActiveTab('QUIZZES')}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all ${
            activeTab === 'QUIZZES'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/30'
              : 'bg-slate-900 text-slate-400 hover:text-white'
          }`}
        >
          <FileQuestion className="w-4 h-4" />
          Academic Quizzes ({quizzes.length})
        </button>
      </div>

      {/* Content Area */}
      {activeTab === 'DOCUMENTS' ? (
        <div className="space-y-4">
          <div className="glass-panel p-4 rounded-2xl border border-slate-800 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Filter className="w-4 h-4 text-slate-400" />
              <span className="text-xs text-slate-400">Filter Documents:</span>
              <select
                value={docFilter}
                onChange={(e) => setDocFilter(e.target.value)}
                className="bg-slate-900 border border-slate-800 rounded-xl px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-indigo-500"
              >
                <option value="ALL">All Documents</option>
                <option value="ACTIVE">Active Documents</option>
                <option value="INACTIVE">Inactive / Deactivated</option>
              </select>
            </div>
          </div>

          <div className="glass-panel rounded-2xl border border-slate-800 overflow-hidden">
            {docsLoading ? (
              <div className="p-12 flex flex-col items-center justify-center gap-3">
                <Loader2 className="w-6 h-6 text-indigo-400 animate-spin" />
                <p className="text-xs text-slate-400">Loading curriculum documents...</p>
              </div>
            ) : documents.length === 0 ? (
              <div className="p-12 text-center text-slate-500 text-xs">
                No uploaded curriculum documents found.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-900/60 border-b border-slate-800 text-slate-400 uppercase tracking-wider font-semibold">
                    <tr>
                      <th className="py-3 px-4">File Name</th>
                      <th className="py-3 px-4">Subject</th>
                      <th className="py-3 px-4">Uploader Faculty</th>
                      <th className="py-3 px-4">File Size</th>
                      <th className="py-3 px-4">Status</th>
                      <th className="py-3 px-4 text-right">Moderation</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {documents.map((d) => (
                      <tr key={d.id} className="hover:bg-slate-900/40 transition-colors">
                        <td className="py-3 px-4 font-medium text-white flex items-center gap-2">
                          <FileText className="w-4 h-4 text-slate-400 shrink-0" />
                          <span className="truncate max-w-xs" title={d.originalFileName}>
                            {d.originalFileName}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-slate-300">{d.subjectName}</td>
                        <td className="py-3 px-4 text-slate-400">{d.teacherEmail}</td>
                        <td className="py-3 px-4 text-slate-400">
                          {(d.fileSize / 1024).toFixed(1)} KB
                        </td>
                        <td className="py-3 px-4">
                          {d.active ? (
                            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                              Active
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-red-500/10 text-red-400 border border-red-500/30">
                              Deactivated
                            </span>
                          )}
                        </td>
                        <td className="py-3 px-4 text-right">
                          <button
                            disabled={actionPending}
                            onClick={() => handleToggleDocStatus(d)}
                            className={`px-3 py-1.5 rounded-xl font-semibold text-[11px] flex items-center gap-1.5 ml-auto transition-colors ${
                              d.active
                                ? 'bg-red-500/10 text-red-400 hover:bg-red-500 hover:text-white border border-red-500/30'
                                : 'bg-emerald-600 hover:bg-emerald-500 text-white'
                            }`}
                          >
                            {d.active ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                            {d.active ? 'Deactivate' : 'Activate'}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          <div className="glass-panel p-4 rounded-2xl border border-slate-800 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Filter className="w-4 h-4 text-slate-400" />
              <span className="text-xs text-slate-400">Filter Quizzes:</span>
              <select
                value={quizFilter}
                onChange={(e) => setQuizFilter(e.target.value)}
                className="bg-slate-900 border border-slate-800 rounded-xl px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-indigo-500"
              >
                <option value="ALL">All Statuses</option>
                <option value="PUBLISHED">Published Only</option>
                <option value="DRAFT">Drafts Only</option>
              </select>
            </div>
          </div>

          <div className="glass-panel rounded-2xl border border-slate-800 overflow-hidden">
            {quizzesLoading ? (
              <div className="p-12 flex flex-col items-center justify-center gap-3">
                <Loader2 className="w-6 h-6 text-indigo-400 animate-spin" />
                <p className="text-xs text-slate-400">Loading quizzes catalog...</p>
              </div>
            ) : quizzes.length === 0 ? (
              <div className="p-12 text-center text-slate-500 text-xs">
                No academic quizzes found.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-900/60 border-b border-slate-800 text-slate-400 uppercase tracking-wider font-semibold">
                    <tr>
                      <th className="py-3 px-4">Quiz Title</th>
                      <th className="py-3 px-4">Subject</th>
                      <th className="py-3 px-4">Creator</th>
                      <th className="py-3 px-4">Total Marks</th>
                      <th className="py-3 px-4">Status</th>
                      <th className="py-3 px-4 text-right">Moderation</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {quizzes.map((q) => (
                      <tr key={q.id} className="hover:bg-slate-900/40 transition-colors">
                        <td className="py-3 px-4 font-medium text-white flex items-center gap-2">
                          <FileQuestion className="w-4 h-4 text-slate-400 shrink-0" />
                          <span className="truncate max-w-xs" title={q.title}>
                            {q.title}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-slate-300">{q.subjectName}</td>
                        <td className="py-3 px-4 text-slate-400">{q.creatorEmail}</td>
                        <td className="py-3 px-4 text-slate-300">{q.totalMarks} pts</td>
                        <td className="py-3 px-4">
                          {q.isPublished ? (
                            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                              Published
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/10 text-amber-400 border border-amber-500/30">
                              Draft
                            </span>
                          )}
                        </td>
                        <td className="py-3 px-4 text-right">
                          <button
                            disabled={actionPending}
                            onClick={() => handleToggleQuizPublish(q)}
                            className={`px-3 py-1.5 rounded-xl font-semibold text-[11px] flex items-center gap-1.5 ml-auto transition-colors ${
                              q.isPublished
                                ? 'bg-amber-500/10 text-amber-400 hover:bg-amber-500 hover:text-white border border-amber-500/30'
                                : 'bg-emerald-600 hover:bg-emerald-500 text-white'
                            }`}
                          >
                            {q.isPublished ? 'Unpublish to Draft' : 'Publish to Students'}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
