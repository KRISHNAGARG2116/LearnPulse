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
  TeacherSubjectDTO,
  EnrolledSubjectDTO,
  StudentSummaryDTO,
  EnrollmentDTO,
  AssignTeacherRequest,
  EnrollStudentRequest,
  GenerateQuizRequest,
  GeneratedQuizResponse,
  AiChatHistoryDTO,
  RagSourceDTO,
  SummarizeContentRequest,
  SummarizeResponse,
  CodeExplainerRequest,
  CodeExplainerResponse,
  StudyPlanRequest,
  StudyPlanResponse,
  FlashcardGenerateRequest,
  FlashcardGenerateResponse,
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

// Teacher Enrollment APIs
export const getTeacherSubjects = async (): Promise<TeacherSubjectDTO[]> => {
  const res = await apiClient.get<ApiResponse<TeacherSubjectDTO[]>>('/teacher/enrollment/subjects');
  return res.data.data || [];
};

export const getTeacherEnrolledStudents = async (subjectId: string): Promise<StudentSummaryDTO[]> => {
  const res = await apiClient.get<ApiResponse<StudentSummaryDTO[]>>(`/teacher/enrollment/subjects/${subjectId}/students`);
  return res.data.data || [];
};

export const getTeacherAvailableStudents = async (subjectId: string): Promise<StudentSummaryDTO[]> => {
  const res = await apiClient.get<ApiResponse<StudentSummaryDTO[]>>(`/teacher/enrollment/subjects/${subjectId}/available-students`);
  return res.data.data || [];
};

export const enrollStudentInSubject = async (subjectId: string, data: EnrollStudentRequest): Promise<EnrollmentDTO> => {
  const res = await apiClient.post<ApiResponse<EnrollmentDTO>>(`/teacher/enrollment/subjects/${subjectId}/students`, data);
  return res.data.data;
};

export const unenrollStudentFromSubject = async (subjectId: string, studentId: string): Promise<void> => {
  await apiClient.delete(`/teacher/enrollment/subjects/${subjectId}/students/${studentId}`);
};

// Student Enrollment APIs
export const getStudentEnrolledSubjects = async (): Promise<EnrolledSubjectDTO[]> => {
  const res = await apiClient.get<ApiResponse<EnrolledSubjectDTO[]>>('/student/enrollment/subjects');
  return res.data.data || [];
};

// Admin Subject Assignment APIs
export const assignTeacherToSubject = async (subjectId: string, data: AssignTeacherRequest): Promise<TeacherSubjectDTO> => {
  const res = await apiClient.post<ApiResponse<TeacherSubjectDTO>>(`/admin/subjects/${subjectId}/teachers`, data);
  return res.data.data;
};

export const removeTeacherFromSubject = async (subjectId: string, teacherId: string): Promise<void> => {
  await apiClient.delete(`/admin/subjects/${subjectId}/teachers/${teacherId}`);
};

export const getSubjectTeachers = async (subjectId: string): Promise<TeacherSubjectDTO[]> => {
  const res = await apiClient.get<ApiResponse<TeacherSubjectDTO[]>>(`/admin/subjects/${subjectId}/teachers`);
  return res.data.data || [];
};

// Week 12: AI Quiz Generator APIs
export const generateAiQuiz = async (data: GenerateQuizRequest): Promise<GeneratedQuizResponse> => {
  const res = await apiClient.post<ApiResponse<GeneratedQuizResponse>>('/ai/generate-quiz', data);
  return res.data.data;
};

export const publishQuiz = async (quizId: string): Promise<any> => {
  const res = await apiClient.post<ApiResponse<any>>(`/teacher/quizzes/${quizId}/publish`);
  return res.data.data;
};

// Week 12: AI Chat & Tutor APIs
export const sendAiChat = async (message: string, conversationId?: string, subjectName?: string): Promise<{ conversationId: string; userMessage: string; aiResponse: string; timestamp: string; fallback: boolean }> => {
  const res = await apiClient.post<ApiResponse<{ conversationId: string; userMessage: string; aiResponse: string; timestamp: string; fallback: boolean }>>('/ai/chat', {
    message,
    conversationId,
    subjectName,
  });
  return res.data.data;
};

export const getAiChatHistory = async (conversationId?: string): Promise<AiChatHistoryDTO[]> => {
  const params: Record<string, string> = {};
  if (conversationId) params.conversationId = conversationId;
  const res = await apiClient.get<ApiResponse<AiChatHistoryDTO[]>>('/ai/chat/history', { params });
  return res.data.data || [];
};

// Week 12: RAG PDF Assistant APIs
export const askDocumentRag = async (documentId: string, question: string): Promise<{ documentId: string; question: string; answer: string; sources: RagSourceDTO[]; fallback: boolean; timestamp: string }> => {
  const res = await apiClient.post<ApiResponse<{ documentId: string; question: string; answer: string; sources: RagSourceDTO[]; fallback: boolean; timestamp: string }>>('/ai/ask-document', {
    documentId,
    question,
  });
  return res.data.data;
};

// Week 12: Supplemental AI Learning APIs
export const summarizeContent = async (data: SummarizeContentRequest): Promise<SummarizeResponse> => {
  const res = await apiClient.post<ApiResponse<SummarizeResponse>>('/ai/summarize', data);
  return res.data.data;
};

export const explainCode = async (data: CodeExplainerRequest): Promise<CodeExplainerResponse> => {
  const res = await apiClient.post<ApiResponse<CodeExplainerResponse>>('/ai/explain-code', data);
  return res.data.data;
};

export const generateStudyPlan = async (data: StudyPlanRequest): Promise<StudyPlanResponse> => {
  const res = await apiClient.post<ApiResponse<StudyPlanResponse>>('/ai/study-plan', data);
  return res.data.data;
};

export const generateFlashcards = async (data: FlashcardGenerateRequest): Promise<FlashcardGenerateResponse> => {
  const res = await apiClient.post<ApiResponse<FlashcardGenerateResponse>>('/ai/flashcards', data);
  return res.data.data;
};

