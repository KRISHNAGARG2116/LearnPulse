import React, { useState, useEffect } from 'react';
import { getStudentEnrolledSubjects, getChapters, getDocuments, askDocumentRag } from '../../api/lmsApi';
import { EnrolledSubjectDTO, ChapterDTO, DocumentDTO, RagSourceDTO } from '../../types/lms';
import { PdfViewer } from '../../components/PdfViewer';
import {
  FileText,
  Send,
  Loader2,
  ChevronDown,
  ChevronUp,
  Sparkles,
  AlertCircle,
  HelpCircle,
  Layers
} from 'lucide-react';

interface QnAItem {
  id: string;
  question: string;
  answer: string;
  sources: RagSourceDTO[];
  timestamp: string;
}

export const StudentPdfAssistantPage: React.FC = () => {
  const [subjects, setSubjects] = useState<EnrolledSubjectDTO[]>([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState<string>('');
  const [chapters, setChapters] = useState<ChapterDTO[]>([]);
  const [selectedChapterId, setSelectedChapterId] = useState<string>('');
  const [documents, setDocuments] = useState<DocumentDTO[]>([]);
  const [selectedDoc, setSelectedDoc] = useState<DocumentDTO | null>(null);

  const [question, setQuestion] = useState('');
  const [qnaHistory, setQnaHistory] = useState<QnAItem[]>([]);
  const [loadingAnswer, setLoadingAnswer] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [expandedSources, setExpandedSources] = useState<Record<string, boolean>>({});

  // Fetch enrolled subjects on mount
  useEffect(() => {
    const fetchSubjects = async () => {
      try {
        const subList = await getStudentEnrolledSubjects();
        setSubjects(subList || []);
        if (subList && subList.length > 0) {
          setSelectedSubjectId(subList[0].subjectId);
        }
      } catch (err) {
        console.warn('Failed to load enrolled subjects:', err);
      }
    };
    fetchSubjects();
  }, []);

  // Fetch chapters when subject changes
  useEffect(() => {
    if (!selectedSubjectId) {
      setChapters([]);
      setSelectedChapterId('');
      return;
    }

    const fetchChapters = async () => {
      try {
        const chapList = await getChapters(selectedSubjectId);
        setChapters(chapList || []);
        if (chapList && chapList.length > 0) {
          setSelectedChapterId(chapList[0].id);
        } else {
          setSelectedChapterId('');
        }
      } catch (err) {
        console.warn('Failed to load chapters:', err);
        setChapters([]);
      }
    };
    fetchChapters();
  }, [selectedSubjectId]);

  // Fetch documents when chapter or subject changes
  useEffect(() => {
    const fetchDocs = async () => {
      try {
        const docList = await getDocuments(selectedSubjectId || undefined, selectedChapterId || undefined);
        setDocuments(docList || []);
        if (docList && docList.length > 0) {
          setSelectedDoc(docList[0]);
        } else {
          setSelectedDoc(null);
        }
      } catch (err) {
        console.warn('Failed to load documents:', err);
        setDocuments([]);
        setSelectedDoc(null);
      }
    };
    fetchDocs();
  }, [selectedSubjectId, selectedChapterId]);

  const handleAskQuestion = async () => {
    if (!selectedDoc || !question.trim() || loadingAnswer) return;

    const currentQ = question.trim();
    setQuestion('');
    setError(null);
    setLoadingAnswer(true);

    try {
      const response = await askDocumentRag(selectedDoc.id, currentQ);
      const newItem: QnAItem = {
        id: 'qna_' + Date.now(),
        question: currentQ,
        answer: response.answer || 'No response returned from assistant.',
        sources: response.sources || [],
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };

      setQnaHistory((prev) => [newItem, ...prev]);
    } catch (err: any) {
      console.error('Failed to ask document:', err);
      const errMsg =
        err.response?.data?.message ||
        err.message ||
        'Failed to query the document vector index. Please try again.';
      setError(errMsg);
    } finally {
      setLoadingAnswer(false);
    }
  };

  const toggleSource = (qnaId: string) => {
    setExpandedSources((prev) => ({
      ...prev,
      [qnaId]: !prev[qnaId],
    }));
  };

  return (
    <div className="p-4 sm:p-6 max-w-7xl mx-auto space-y-4">
      {/* Header and Document Picker */}
      <div className="bg-slate-900/60 border border-slate-800 p-4 rounded-2xl backdrop-blur-md flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-sky-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-sky-500/20">
            <FileText className="w-5 h-5 text-white" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-lg font-bold text-white">RAG PDF Assistant & Document Q&A</h1>
              <span className="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-sky-500/10 text-sky-400 border border-sky-500/20">
                Vector Grounded RAG
              </span>
            </div>
            <p className="text-xs text-slate-400">
              Split-screen interactive PDF viewer with retrieval-augmented AI citation.
            </p>
          </div>
        </div>

        {/* Dropdown Pickers */}
        <div className="flex flex-wrap items-center gap-2">
          {/* Subject Select */}
          <select
            value={selectedSubjectId}
            onChange={(e) => setSelectedSubjectId(e.target.value)}
            className="bg-slate-800 border border-slate-700 text-xs text-slate-200 rounded-xl px-3 py-2 focus:outline-none"
            aria-label="Select Subject"
          >
            <option value="">All Subjects</option>
            {subjects.map((sub) => (
              <option key={sub.subjectId} value={sub.subjectId}>
                {sub.subjectName}
              </option>
            ))}
          </select>

          {/* Chapter Select */}
          {chapters.length > 0 && (
            <select
              value={selectedChapterId}
              onChange={(e) => setSelectedChapterId(e.target.value)}
              className="bg-slate-800 border border-slate-700 text-xs text-slate-200 rounded-xl px-3 py-2 focus:outline-none"
              aria-label="Select Chapter"
            >
              <option value="">All Chapters</option>
              {chapters.map((chap) => (
                <option key={chap.id} value={chap.id}>
                  {chap.title}
                </option>
              ))}
            </select>
          )}

          {/* Document Select */}
          {documents.length > 0 ? (
            <select
              value={selectedDoc?.id || ''}
              onChange={(e) => {
                const found = documents.find((d) => d.id === e.target.value);
                if (found) setSelectedDoc(found);
              }}
              className="bg-indigo-950/60 border border-indigo-600/40 text-xs text-indigo-200 rounded-xl px-3 py-2 focus:outline-none font-medium"
              aria-label="Select Document"
            >
              {documents.map((doc) => (
                <option key={doc.id} value={doc.id}>
                  📄 {doc.originalFileName}
                </option>
              ))}
            </select>
          ) : (
            <span className="text-xs text-slate-500 italic">No materials found in this chapter</span>
          )}
        </div>
      </div>

      {/* Split-Screen Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
        {/* Left Side: Real PDF Viewer (7 cols on large screens) */}
        <div className="lg:col-span-7 bg-slate-900/40 border border-slate-800 rounded-2xl p-4 flex flex-col h-[700px] overflow-hidden">
          <div className="flex items-center justify-between pb-3 border-b border-slate-800/80 mb-3">
            <div className="flex items-center gap-2">
              <FileText className="w-4 h-4 text-sky-400" />
              <h2 className="text-sm font-semibold text-slate-200 truncate max-w-sm">
                {selectedDoc ? selectedDoc.originalFileName : 'Select a document'}
              </h2>
            </div>
            {selectedDoc && (
              <span className="text-[11px] text-slate-400 bg-slate-800 px-2 py-0.5 rounded-md">
                Streamed directly from LearnPulse Storage
              </span>
            )}
          </div>

          <div className="flex-1 overflow-hidden rounded-xl border border-slate-800 bg-slate-950">
            {selectedDoc ? (
              <PdfViewer documentId={selectedDoc.id} title={selectedDoc.originalFileName} />
            ) : (
              <div className="h-full flex flex-col items-center justify-center p-8 text-center text-slate-500">
                <FileText className="w-12 h-12 mb-2 text-slate-600" />
                <p className="text-sm font-medium">No document selected</p>
                <p className="text-xs text-slate-600 max-w-xs mt-1">
                  Choose an enrolled subject and chapter from the top bar to inspect materials.
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Right Side: Q&A Assistant with Genuine Source Citations (5 cols) */}
        <div className="lg:col-span-5 bg-slate-900/40 border border-slate-800 rounded-2xl p-4 flex flex-col h-[700px] overflow-hidden">
          <div className="pb-3 border-b border-slate-800/80 mb-3 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-indigo-400" />
              <h2 className="text-sm font-semibold text-white">Document Q&A & Citations</h2>
            </div>
            <span className="text-[11px] text-indigo-300 bg-indigo-950/60 border border-indigo-700/40 px-2 py-0.5 rounded-full">
              Qwen RAG Retrieval
            </span>
          </div>

          {/* Ask Input */}
          <div className="mb-4">
            <div className="flex gap-2">
              <input
                type="text"
                value={question}
                onChange={(e) => setQuestion(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') handleAskQuestion();
                }}
                disabled={!selectedDoc || loadingAnswer}
                placeholder={
                  selectedDoc
                    ? 'Ask a question grounded in this PDF...'
                    : 'Select a document to ask questions...'
                }
                className="flex-1 bg-slate-950 border border-slate-800 focus:border-indigo-500 rounded-xl px-3 py-2 text-xs sm:text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none"
              />
              <button
                onClick={handleAskQuestion}
                disabled={!selectedDoc || !question.trim() || loadingAnswer}
                className="px-3.5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 disabled:opacity-40 disabled:cursor-not-allowed text-white shadow-md shadow-indigo-600/30 transition flex items-center gap-1.5 text-xs font-semibold shrink-0"
              >
                {loadingAnswer ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                Ask
              </button>
            </div>

            {error && (
              <div className="mt-2 p-2.5 rounded-xl bg-rose-950/50 border border-rose-800/60 text-rose-200 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
                <span>{error}</span>
              </div>
            )}
          </div>

          {/* Q&A List */}
          <div className="flex-1 overflow-y-auto space-y-4 pr-1">
            {loadingAnswer && (
              <div className="p-4 rounded-xl bg-slate-800/80 border border-slate-700/60 flex items-center gap-3">
                <Loader2 className="w-5 h-5 text-indigo-400 animate-spin shrink-0" />
                <div>
                  <p className="text-xs font-medium text-slate-200">
                    Retrieving vector chunks and generating grounded answer...
                  </p>
                  <p className="text-[11px] text-slate-400 mt-0.5">
                    Analyzing semantic proximity to document contents.
                  </p>
                </div>
              </div>
            )}

            {qnaHistory.length === 0 && !loadingAnswer && (
              <div className="h-full flex flex-col items-center justify-center p-6 text-center text-slate-500">
                <HelpCircle className="w-10 h-10 mb-2 text-slate-600" />
                <p className="text-sm font-medium">No questions asked yet</p>
                <p className="text-xs text-slate-500 mt-1 max-w-xs">
                  Type any query about this document to receive an AI response grounded in its text.
                </p>
              </div>
            )}

            {qnaHistory.map((item) => {
              const isExpanded = !!expandedSources[item.id];
              return (
                <div
                  key={item.id}
                  className="rounded-xl bg-slate-800/90 border border-slate-700/70 p-3.5 space-y-2.5 shadow-sm"
                >
                  <div className="flex items-start justify-between gap-2">
                    <p className="text-xs font-bold text-indigo-300">Q: {item.question}</p>
                    <span className="text-[10px] text-slate-400 shrink-0">{item.timestamp}</span>
                  </div>

                  <p className="text-xs text-slate-200 leading-relaxed whitespace-pre-wrap">
                    {item.answer}
                  </p>

                  {/* Sources Accordion */}
                  {item.sources && item.sources.length > 0 && (
                    <div className="pt-2 border-t border-slate-700/50">
                      <button
                        onClick={() => toggleSource(item.id)}
                        className="flex items-center justify-between w-full text-[11px] font-semibold text-slate-400 hover:text-slate-200 transition"
                      >
                        <span className="flex items-center gap-1.5">
                          <Layers className="w-3.5 h-3.5 text-sky-400" />
                          Grounded in {item.sources.length} document chunk{item.sources.length > 1 ? 's' : ''}
                        </span>
                        {isExpanded ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
                      </button>

                      {isExpanded && (
                        <div className="mt-2 space-y-2">
                          {item.sources.map((src, sIdx) => (
                            <div
                              key={sIdx}
                              className="p-2 rounded-lg bg-slate-950/80 border border-slate-800 text-[11px] space-y-1"
                            >
                              <div className="flex items-center justify-between text-slate-400">
                                <span className="font-semibold text-indigo-400">
                                  Chunk #{src.chunkIndex !== undefined ? src.chunkIndex : sIdx + 1}
                                </span>
                                {src.similarity !== undefined && (
                                  <span className="text-[10px] px-1.5 py-0.2 rounded bg-indigo-950 text-indigo-300 border border-indigo-800/40">
                                    {(src.similarity * 100).toFixed(1)}% similarity
                                  </span>
                                )}
                              </div>
                              <p className="text-slate-300 italic font-mono text-[10px] line-clamp-3">
                                "{src.text}"
                              </p>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
};
