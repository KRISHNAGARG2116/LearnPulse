import React, { useEffect, useState } from 'react';
import { getSubjects, getChapters, uploadDocument } from '../../api/lmsApi';
import { SubjectDTO, ChapterDTO } from '../../types/lms';
import { Upload, FileText, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';

interface MaterialUploadFormProps {
  onSuccess?: () => void;
}

export const MaterialUploadForm: React.FC<MaterialUploadFormProps> = ({ onSuccess }) => {
  const [subjects, setSubjects] = useState<SubjectDTO[]>([]);
  const [chapters, setChapters] = useState<ChapterDTO[]>([]);

  const [selectedSubjectId, setSelectedSubjectId] = useState<string>('');
  const [selectedChapterId, setSelectedChapterId] = useState<string>('');
  const [file, setFile] = useState<File | null>(null);

  const [loadingSubjects, setLoadingSubjects] = useState<boolean>(true);
  const [loadingChapters, setLoadingChapters] = useState<boolean>(false);
  const [uploading, setUploading] = useState<boolean>(false);

  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadSubjects = async () => {
      try {
        const data = await getSubjects();
        setSubjects(data);
      } catch (err: any) {
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
      setLoadingChapters(true);
      try {
        const data = await getChapters(selectedSubjectId);
        setChapters(data);
      } catch (err: any) {
        setError('Failed to load subject chapters.');
      } finally {
        setLoadingChapters(false);
      }
    };

    loadChapters();
  }, [selectedSubjectId]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setError(null);
    setSuccessMessage(null);
    if (e.target.files && e.target.files[0]) {
      const selectedFile = e.target.files[0];
      const maxSizeBytes = 20 * 1024 * 1024; // 20 MB

      const ext = selectedFile.name.split('.').pop()?.toLowerCase();
      if (!ext || !['pdf', 'doc', 'docx'].includes(ext)) {
        setError('Invalid file type. Only PDF, DOC, and DOCX documents are permitted.');
        setFile(null);
        return;
      }

      if (selectedFile.size > maxSizeBytes) {
        setError('File size exceeds 20 MB limit.');
        setFile(null);
        return;
      }

      setFile(selectedFile);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) {
      setError('Please select a valid document file to upload.');
      return;
    }

    setUploading(true);
    setError(null);
    setSuccessMessage(null);

    try {
      const formData = new FormData();
      formData.append('file', file);
      if (selectedSubjectId) formData.append('subjectId', selectedSubjectId);
      if (selectedChapterId) formData.append('chapterId', selectedChapterId);

      await uploadDocument(formData);

      setSuccessMessage(`Successfully uploaded and processed "${file.name}"!`);
      setFile(null);
      setSelectedSubjectId('');
      setSelectedChapterId('');
      if (onSuccess) onSuccess();
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'File upload failed.');
    } finally {
      setUploading(false);
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

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Subject Dropdown */}
        <div className="space-y-2">
          <label className="text-xs font-bold text-slate-300 uppercase tracking-wider">
            Select Subject (Optional)
          </label>
          <select
            value={selectedSubjectId}
            onChange={(e) => setSelectedSubjectId(e.target.value)}
            disabled={loadingSubjects || uploading}
            className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-800 text-white text-sm focus:outline-none focus:border-indigo-500 transition-colors"
          >
            <option value="">-- All / General Subject --</option>
            {subjects.map((sub) => (
              <option key={sub.id} value={sub.id}>
                {sub.code} - {sub.name}
              </option>
            ))}
          </select>
        </div>

        {/* Chapter Dropdown */}
        <div className="space-y-2">
          <label className="text-xs font-bold text-slate-300 uppercase tracking-wider">
            Select Chapter (Optional)
          </label>
          <select
            value={selectedChapterId}
            onChange={(e) => setSelectedChapterId(e.target.value)}
            disabled={!selectedSubjectId || loadingChapters || uploading}
            className="w-full px-4 py-3 rounded-xl bg-slate-900 border border-slate-800 text-white text-sm focus:outline-none focus:border-indigo-500 transition-colors disabled:opacity-50"
          >
            <option value="">-- All / General Chapter --</option>
            {chapters.map((chap) => (
              <option key={chap.id} value={chap.id}>
                Ch.{chap.chapterNumber}: {chap.title}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* File Dropzone */}
      <div className="space-y-2">
        <label className="text-xs font-bold text-slate-300 uppercase tracking-wider">
          Document File (.PDF, .DOC, .DOCX)
        </label>
        <div className="p-8 border-2 border-dashed border-slate-800 hover:border-indigo-500/50 bg-slate-900/40 rounded-2xl text-center space-y-3 transition-colors relative cursor-pointer">
          <input
            type="file"
            accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            onChange={handleFileChange}
            disabled={uploading}
            className="absolute inset-0 opacity-0 cursor-pointer w-full h-full"
          />
          <Upload className="w-10 h-10 text-indigo-400 mx-auto" />
          <div>
            <p className="text-sm font-bold text-white">
              {file ? file.name : 'Click or Drag & Drop Document File'}
            </p>
            <p className="text-xs text-slate-500 mt-1">
              Supports PDF, DOC, DOCX up to 20 MB
            </p>
          </div>
        </div>
      </div>

      <button
        type="submit"
        disabled={!file || uploading}
        className="w-full py-3.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed text-white text-sm font-bold inline-flex items-center justify-center gap-2 shadow-lg shadow-indigo-500/25 transition-all"
      >
        {uploading ? (
          <>
            <Loader2 className="w-4 h-4 animate-spin" />
            <span>Uploading & Processing Text Extraction...</span>
          </>
        ) : (
          <>
            <FileText className="w-4 h-4" />
            <span>Upload Educational Material</span>
          </>
        )}
      </button>
    </form>
  );
};
