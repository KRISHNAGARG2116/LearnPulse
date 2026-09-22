import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getChapterById, getDocuments } from '../../api/lmsApi';
import { ChapterDTO, DocumentDTO } from '../../types/lms';
import { PdfViewer } from '../../components/PdfViewer';
import {
  FileText,
  ArrowLeft,
  Loader2,
  AlertCircle,
  Download,
  Calendar,
  HardDrive,
} from 'lucide-react';

export const StudentMaterialViewerPage: React.FC = () => {
  const { chapterId } = useParams<{ chapterId: string }>();
  const [chapter, setChapter] = useState<ChapterDTO | null>(null);
  const [documents, setDocuments] = useState<DocumentDTO[]>([]);
  const [selectedDoc, setSelectedDoc] = useState<DocumentDTO | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!chapterId) return;

    const fetchMaterials = async () => {
      setLoading(true);
      setError(null);
      try {
        const [chapData, docData] = await Promise.all([
          getChapterById(chapterId).catch(() => null),
          getDocuments(undefined, chapterId).catch(() => []),
        ]);
        setChapter(chapData);
        setDocuments(docData);
        if (docData.length > 0) {
          setSelectedDoc(docData[0]);
        }
      } catch (err: any) {
        setError(err.message || 'Failed to load course study materials.');
      } finally {
        setLoading(false);
      }
    };

    fetchMaterials();
  }, [chapterId]);

  const formatFileSize = (bytes: number) => {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  };

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
        <Loader2 className="w-10 h-10 text-indigo-400 animate-spin" />
        <p className="text-slate-400 text-sm font-medium">Loading chapter materials...</p>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Back Link */}
      {chapter?.subjectId && (
        <Link
          to={`/student/subjects/${chapter.subjectId}/chapters`}
          className="inline-flex items-center gap-2 text-xs font-semibold text-slate-400 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to Chapter List</span>
        </Link>
      )}

      {/* Header */}
      <div className="glass-panel rounded-2xl p-6 md:p-8 border border-slate-800 space-y-2">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 text-xs font-semibold mb-1">
          <FileText className="w-3.5 h-3.5" />
          <span>Study Material Viewer</span>
        </div>
        <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight">
          {chapter?.title || 'Chapter Materials'}
        </h1>
        {chapter?.description && (
          <p className="text-slate-400 text-sm max-w-2xl">{chapter.description}</p>
        )}
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center gap-3 text-rose-400 text-sm">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {documents.length === 0 ? (
        <div className="glass-panel rounded-2xl p-12 text-center border border-dashed border-slate-800 space-y-3">
          <FileText className="w-12 h-12 text-slate-600 mx-auto" />
          <h3 className="text-lg font-bold text-white">No Materials Uploaded</h3>
          <p className="text-slate-400 text-sm max-w-md mx-auto">
            No lecture documents or study materials have been uploaded for this chapter yet.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Document List Selector */}
          <div className="space-y-4 lg:col-span-1">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <FileText className="w-4 h-4 text-indigo-400" />
              <span>Available Documents ({documents.length})</span>
            </h2>

            <div className="space-y-3">
              {documents.map((doc) => {
                const isSelected = selectedDoc?.id === doc.id;
                return (
                  <div
                    key={doc.id}
                    onClick={() => setSelectedDoc(doc)}
                    className={`p-4 rounded-xl border cursor-pointer transition-all ${
                      isSelected
                        ? 'bg-indigo-600/20 border-indigo-500/50 text-white'
                        : 'bg-slate-900/60 border-slate-800 hover:border-slate-700 text-slate-300'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <h4 className="text-sm font-bold truncate">{doc.originalFileName}</h4>
                    </div>

                    <div className="mt-3 flex items-center gap-4 text-xs text-slate-400">
                      <span className="flex items-center gap-1">
                        <HardDrive className="w-3.5 h-3.5" />
                        {formatFileSize(doc.fileSize)}
                      </span>
                      {doc.createdAt && (
                        <span className="flex items-center gap-1">
                          <Calendar className="w-3.5 h-3.5" />
                          {new Date(doc.createdAt).toLocaleDateString()}
                        </span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Embedded Viewer & Metadata Area */}
          <div className="lg:col-span-2 space-y-6">
            {selectedDoc ? (
              <div className="space-y-4">
                <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 flex items-center justify-between gap-4">
                  <div>
                    <h3 className="text-sm font-bold text-white">{selectedDoc.originalFileName}</h3>
                    <p className="text-xs text-slate-400 mt-0.5">
                      Format: {selectedDoc.contentType || 'Document'} • Size: {formatFileSize(selectedDoc.fileSize)}
                    </p>
                  </div>
                  <a
                    href={`/api/documents/${selectedDoc.id}/download`}
                    download={selectedDoc.originalFileName}
                    className="px-4 py-2 rounded-xl bg-indigo-600/20 hover:bg-indigo-600/30 text-indigo-300 border border-indigo-500/30 text-xs font-semibold inline-flex items-center gap-2 transition-all shrink-0"
                  >
                    <Download className="w-4 h-4" />
                    <span>Download</span>
                  </a>
                </div>

                {selectedDoc.contentType === 'application/pdf' || selectedDoc.originalFileName.endsWith('.pdf') ? (
                  <PdfViewer documentId={selectedDoc.id} title={selectedDoc.originalFileName} />
                ) : (
                  <div className="p-12 bg-slate-900/60 border border-slate-800 rounded-2xl text-center space-y-3">
                    <FileText className="w-12 h-12 text-indigo-400 mx-auto" />
                    <h4 className="text-base font-bold text-white">Office Document View</h4>
                    <p className="text-xs text-slate-400 max-w-md mx-auto">
                      This is a Microsoft Office document ({selectedDoc.originalFileName}). Click the download button above to view or download the file locally.
                    </p>
                  </div>
                )}
              </div>
            ) : (
              <div className="p-12 text-center text-slate-500">
                Select a document from the list to view.
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
