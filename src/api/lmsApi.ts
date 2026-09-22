import apiClient from './axios';
import { ApiResponse } from '../types/auth';
import {
  SubjectDTO,
  CreateSubjectRequest,
  ChapterDTO,
  CreateChapterRequest,
  DocumentDTO,
  StudentQuizDTO,
  QuizSubmissionRequest,
  StudentQuizResultDTO,
  StudentProgressDTO,
  CreateQuizRequest,
} from '../types/lms';

// Subject APIs
export const getSubjects = async (): Promise<SubjectDTO[]> => {
  const res = await apiClient.get<ApiResponse<SubjectDTO[]>>('/subjects');
  return res.data.data || [];
};

export const getSubjectById = async (id: string): Promise<SubjectDTO> => {
  const res = await apiClient.get<ApiResponse<SubjectDTO>>(`/subjects/${id}`);
  return res.data.data;
};

export const createSubject = async (data: CreateSubjectRequest): Promise<SubjectDTO> => {
  const res = await apiClient.post<ApiResponse<SubjectDTO>>('/subjects', data);
  return res.data.data;
};

// Chapter APIs
export const getChapters = async (subjectId: string): Promise<ChapterDTO[]> => {
  const res = await apiClient.get<ApiResponse<ChapterDTO[]>>(`/subjects/${subjectId}/chapters`);
  return res.data.data || [];
};

export const getChapterById = async (id: string): Promise<ChapterDTO> => {
  const res = await apiClient.get<ApiResponse<ChapterDTO>>(`/chapters/${id}`);
  return res.data.data;
};

export const createChapter = async (subjectId: string, data: CreateChapterRequest): Promise<ChapterDTO> => {
  const res = await apiClient.post<ApiResponse<ChapterDTO>>(`/subjects/${subjectId}/chapters`, data);
  return res.data.data;
};

// Document APIs
export const getDocuments = async (subjectId?: string, chapterId?: string): Promise<DocumentDTO[]> => {
  const params: Record<string, string> = {};
  if (subjectId) params.subjectId = subjectId;
  if (chapterId) params.chapterId = chapterId;
  const res = await apiClient.get<ApiResponse<DocumentDTO[]>>('/documents', { params });
  return res.data.data || [];
};

export const getDocumentById = async (id: string): Promise<DocumentDTO> => {
  const res = await apiClient.get<ApiResponse<DocumentDTO>>(`/documents/${id}`);
  return res.data.data;
};

// Student Quiz APIs
export const getQuizzes = async (subjectId?: string, chapterId?: string): Promise<StudentQuizDTO[]> => {
  const params: Record<string, string> = {};
  if (subjectId) params.subjectId = subjectId;
  if (chapterId) params.chapterId = chapterId;
  const res = await apiClient.get<ApiResponse<StudentQuizDTO[]>>('/quizzes', { params });
  return res.data.data || [];
};

export const getQuizById = async (id: string): Promise<StudentQuizDTO> => {
  const res = await apiClient.get<ApiResponse<StudentQuizDTO>>(`/quizzes/${id}`);
  return res.data.data;
};

export const submitQuiz = async (data: QuizSubmissionRequest): Promise<StudentQuizResultDTO> => {
  const res = await apiClient.post<ApiResponse<StudentQuizResultDTO>>('/quizzes/submit', data);
  return res.data.data;
};

export const getQuizResult = async (id: string): Promise<StudentQuizResultDTO> => {
  const res = await apiClient.get<ApiResponse<StudentQuizResultDTO>>(`/quizzes/result/${id}`);
  return res.data.data;
};

export const getStudentProgress = async (): Promise<StudentProgressDTO> => {
  const res = await apiClient.get<ApiResponse<StudentProgressDTO>>('/student/progress');
  return res.data.data;
};

export const getStudentProfile = async (): Promise<any> => {
  const res = await apiClient.get<ApiResponse<any>>('/student/profile');
  return res.data.data;
};

// Teacher APIs
export const uploadDocument = async (formData: FormData): Promise<DocumentDTO> => {
  const res = await apiClient.post<ApiResponse<DocumentDTO>>('/teacher/upload-pdf', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return res.data.data;
};

export const createQuiz = async (data: CreateQuizRequest): Promise<any> => {
  const res = await apiClient.post<ApiResponse<any>>('/teacher/create-quiz', data);
  return res.data.data;
};
