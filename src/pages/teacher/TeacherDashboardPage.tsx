import React, { useEffect, useState } from 'react';
import { MaterialUploadForm } from '../../components/teacher/MaterialUploadForm';
import { ManualQuizBuilder } from '../../components/teacher/ManualQuizBuilder';
import { getDocuments, getQuizzes } from '../../api/lmsApi';
import { DocumentDTO, StudentQuizDTO } from '../../types/lms';
import {
  Sparkles,
  Upload,
  PlusCircle,
  FileText,
  HelpCircle,
  Layers,
  Loader2,
} from 'lucide-react';

export const TeacherDashboardPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'upload' | 'quiz' | 'content'>('upload');

  const [documents, setDocuments] = useState<DocumentDTO[]>([]);
  const [quizzes, setQuizzes] = useState<StudentQuizDTO[]>([]);
  const [loading, setLoading] = useState<boolean>(false);

  const fetchContent = async () => {
    setLoading(true);
    try {
      const [docData, quizData] = await Promise.all([
        getDocuments().catch(() => []),
        getQuizzes().catch(() => []),
      ]);
      setDocuments(docData);
      setQuizzes(quizData);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchContent();
  }, []);

  return (
    <div className="space-y-8">
      {/* Welcome Banner */}
      <div className="glass-panel rounded-2xl p-6 md:p-8 border border-indigo-500/20 relative overflow-hidden">
        <div className="absolute -right-10 -bottom-10 w-64 h-64 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div>
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 text-xs font-semibold mb-3">
              <Sparkles className="w-3.5 h-3.5" />
              <span>Teacher Workspace</span>
            </div>
            <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight">
              Course Content & Quiz Management
            </h1>
            <p className="text-slate-400 text-sm mt-1 max-w-xl">
              Upload educational lecture materials and author custom chapter assessments for student learning.
            </p>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-800 pb-2 overflow-x-auto">
        <button
          onClick={() => setActiveTab('upload')}
          className={`px-4 py-2.5 rounded-xl text-xs font-bold inline-flex items-center gap-2 transition-all ${
            activeTab === 'upload'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-500/25'
              : 'bg-slate-900 text-slate-400 hover:text-white border border-slate-800'
          }`}
        >
          <Upload className="w-4 h-4" />
          <span>Upload Study Material</span>
        </button>

        <button
          onClick={() => setActiveTab('quiz')}
          className={`px-4 py-2.5 rounded-xl text-xs font-bold inline-flex items-center gap-2 transition-all ${
            activeTab === 'quiz'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-500/25'
              : 'bg-slate-900 text-slate-400 hover:text-white border border-slate-800'
          }`}
        >
          <PlusCircle className="w-4 h-4" />
          <span>Manual Quiz Builder</span>
        </button>

        <button
          onClick={() => setActiveTab('content')}
          className={`px-4 py-2.5 rounded-xl text-xs font-bold inline-flex items-center gap-2 transition-all ${
            activeTab === 'content'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-500/25'
              : 'bg-slate-900 text-slate-400 hover:text-white border border-slate-800'
          }`}
        >
          <Layers className="w-4 h-4" />
          <span>Content Overview ({documents.length + quizzes.length})</span>
        </button>
      </div>

      {/* Tab Panels */}
      {activeTab === 'upload' && (
        <div className="glass-panel rounded-2xl p-6 md:p-8 border border-slate-800 space-y-4">
          <h2 className="text-lg font-bold text-white flex items-center gap-2 mb-2">
            <Upload className="w-5 h-5 text-indigo-400" />
            <span>Upload Educational Material (.PDF, .DOC, .DOCX)</span>
          </h2>
          <MaterialUploadForm onSuccess={fetchContent} />
        </div>
      )}

      {activeTab === 'quiz' && (
        <div className="glass-panel rounded-2xl p-6 md:p-8 border border-slate-800 space-y-4">
          <h2 className="text-lg font-bold text-white flex items-center gap-2 mb-2">
            <PlusCircle className="w-5 h-5 text-indigo-400" />
            <span>Manual Quiz Builder</span>
          </h2>
          <ManualQuizBuilder onSuccess={fetchContent} />
        </div>
      )}

      {activeTab === 'content' && (
        <div className="space-y-8">
          {/* Uploaded Documents List */}
          <div className="glass-panel rounded-2xl p-6 border border-slate-800 space-y-4">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <FileText className="w-5 h-5 text-indigo-400" />
              <span>Uploaded Educational Documents ({documents.length})</span>
            </h2>

            {loading ? (
              <div className="py-8 text-center text-slate-400 text-xs">
                <Loader2 className="w-6 h-6 animate-spin mx-auto text-indigo-400 mb-2" />
                Loading content...
              </div>
            ) : documents.length === 0 ? (
              <div className="p-6 text-center text-slate-500 text-xs">
                No materials uploaded yet.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="text-xs text-slate-400 uppercase bg-slate-900/80 border-b border-slate-800">
                    <tr>
                      <th className="py-3 px-4">Filename</th>
                      <th className="py-3 px-4">Subject</th>
                      <th className="py-3 px-4">Status</th>
                      <th className="py-3 px-4">Date</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {documents.map((doc) => (
                      <tr key={doc.id} className="hover:bg-slate-900/40">
                        <td className="py-3 px-4 font-semibold text-white">{doc.originalFileName}</td>
                        <td className="py-3 px-4 text-xs text-slate-400">{doc.subjectName || 'General'}</td>
                        <td className="py-3 px-4">
                          <span className="px-2.5 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-xs font-bold">
                            {doc.processingStatus || 'PROCESSED'}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-xs text-slate-400">
                          {doc.createdAt ? new Date(doc.createdAt).toLocaleDateString() : 'Recent'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Created Quizzes List */}
          <div className="glass-panel rounded-2xl p-6 border border-slate-800 space-y-4">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <HelpCircle className="w-5 h-5 text-indigo-400" />
              <span>Created Assessment Quizzes ({quizzes.length})</span>
            </h2>

            {loading ? (
              <div className="py-8 text-center text-slate-400 text-xs">
                <Loader2 className="w-6 h-6 animate-spin mx-auto text-indigo-400 mb-2" />
                Loading quizzes...
              </div>
            ) : quizzes.length === 0 ? (
              <div className="p-6 text-center text-slate-500 text-xs">
                No quizzes created yet.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="text-xs text-slate-400 uppercase bg-slate-900/80 border-b border-slate-800">
                    <tr>
                      <th className="py-3 px-4">Quiz Title</th>
                      <th className="py-3 px-4">Subject</th>
                      <th className="py-3 px-4">Total Marks</th>
                      <th className="py-3 px-4">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {quizzes.map((q) => (
                      <tr key={q.id} className="hover:bg-slate-900/40">
                        <td className="py-3 px-4 font-semibold text-white">{q.title}</td>
                        <td className="py-3 px-4 text-xs text-slate-400">{q.subjectName || 'General'}</td>
                        <td className="py-3 px-4 font-bold text-slate-200">{q.totalMarks} pts</td>
                        <td className="py-3 px-4">
                          <span className="px-2.5 py-0.5 rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 text-xs font-bold">
                            {q.status || 'PUBLISHED'}
                          </span>
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
