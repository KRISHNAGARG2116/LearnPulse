import React, { useEffect, useState } from 'react';
import apiClient from '../api/axios';
import { Loader2, AlertCircle, FileText } from 'lucide-react';

interface PdfViewerProps {
  documentId: string;
  title?: string;
}

export const PdfViewer: React.FC<PdfViewerProps> = ({ documentId, title }) => {
  const [blobUrl, setBlobUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    let currentBlobUrl: string | null = null;

    const loadPdfBlob = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await apiClient.get(`/documents/${documentId}/stream`, {
          responseType: 'blob',
        });

        if (!active) return;

        const rawContentType = response.headers['content-type'];
        const contentType = typeof rawContentType === 'string' ? rawContentType : 'application/pdf';
        const blob = new Blob([response.data], { type: contentType });
        currentBlobUrl = URL.createObjectURL(blob);
        setBlobUrl(currentBlobUrl);
      } catch (err: any) {
        if (!active) return;
        setError(err.message || 'Failed to stream educational document.');
      } finally {
        if (active) setLoading(false);
      }
    };

    loadPdfBlob();

    return () => {
      active = false;
      if (currentBlobUrl) {
        URL.revokeObjectURL(currentBlobUrl);
      }
    };
  }, [documentId]);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center p-12 bg-slate-900/60 border border-slate-800 rounded-2xl gap-3">
        <Loader2 className="w-8 h-8 text-indigo-400 animate-spin" />
        <p className="text-xs text-slate-400 font-medium">Loading document stream into viewer...</p>
      </div>
    );
  }

  if (error || !blobUrl) {
    return (
      <div className="p-8 bg-rose-500/10 border border-rose-500/20 rounded-2xl text-center space-y-3">
        <AlertCircle className="w-8 h-8 text-rose-400 mx-auto" />
        <h4 className="text-sm font-bold text-rose-300">Document Stream Error</h4>
        <p className="text-xs text-rose-400/80 max-w-md mx-auto">
          {error || 'Unable to render embedded document preview. The file may be unavailable or inaccessible.'}
        </p>
      </div>
    );
  }

  return (
    <div className="glass-panel rounded-2xl border border-slate-800 overflow-hidden flex flex-col">
      {/* Header bar */}
      <div className="p-4 bg-slate-900/90 border-b border-slate-800 flex items-center justify-between">
        <div className="flex items-center gap-2 text-sm font-bold text-white">
          <FileText className="w-4 h-4 text-indigo-400" />
          <span>{title || 'Embedded PDF Document Viewer'}</span>
        </div>
      </div>

      {/* Embedded Iframe Stream */}
      <div className="w-full h-[600px] bg-slate-950">
        <iframe
          src={blobUrl}
          title={title || 'PDF Document Stream'}
          className="w-full h-full border-none"
        />
      </div>
    </div>
  );
};
