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

// ==========================================
// Week 13: Admin Governance & Analytics APIs
// ==========================================

// User Management
export const getAdminUsers = async (params?: {
  role?: string;
  enabled?: boolean;
  search?: string;
  page?: number;
  size?: number;
}): Promise<{ content: import('../types/lms').AdminUserDTO[]; totalElements: number; totalPages: number }> => {
  const res = await apiClient.get<ApiResponse<any>>('/admin/users', { params });
  return res.data.data;
};

export const getAdminUserDetail = async (id: string): Promise<import('../types/lms').AdminUserDetailDTO> => {
  const res = await apiClient.get<ApiResponse<import('../types/lms').AdminUserDetailDTO>>(`/admin/users/${id}`);
  return res.data.data;
};

export const updateAdminUserProfile = async (id: string, data: import('../types/lms').UpdateUserProfileRequest): Promise<import('../types/lms').AdminUserDTO> => {
  const res = await apiClient.patch<ApiResponse<import('../types/lms').AdminUserDTO>>(`/admin/users/${id}`, data);
  return res.data.data;
};

export const updateAdminUserRole = async (id: string, role: string): Promise<import('../types/lms').AdminUserDTO> => {
  const res = await apiClient.patch<ApiResponse<import('../types/lms').AdminUserDTO>>(`/admin/users/${id}/role`, { role });
  return res.data.data;
};

export const initiatePasswordReset = async (id: string): Promise<import('../types/lms').PasswordResetResponse> => {
  const res = await apiClient.post<ApiResponse<import('../types/lms').PasswordResetResponse>>(`/admin/users/${id}/password-reset`);
  return res.data.data;
};

export const deactivateAdminUser = async (id: string): Promise<import('../types/lms').AdminUserDTO> => {
  const res = await apiClient.patch<ApiResponse<import('../types/lms').AdminUserDTO>>(`/admin/users/${id}/deactivate`);
  return res.data.data;
};

export const reactivateAdminUser = async (id: string): Promise<import('../types/lms').AdminUserDTO> => {
  const res = await apiClient.patch<ApiResponse<import('../types/lms').AdminUserDTO>>(`/admin/users/${id}/reactivate`);
  return res.data.data;
};

// Platform Analytics
export const getPlatformSummary = async (): Promise<import('../types/lms').PlatformSummaryDTO> => {
  const res = await apiClient.get<ApiResponse<import('../types/lms').PlatformSummaryDTO>>('/admin/analytics/summary');
  return res.data.data;
};

export const getUserActivityTrends = async (): Promise<import('../types/lms').UserActivityTrendDTO[]> => {
  const res = await apiClient.get<ApiResponse<import('../types/lms').UserActivityTrendDTO[]>>('/admin/analytics/user-activity');
  return res.data.data;
};

export const getCourseActivity = async (): Promise<import('../types/lms').CourseActivityDTO[]> => {
  const res = await apiClient.get<ApiResponse<import('../types/lms').CourseActivityDTO[]>>('/admin/analytics/course-activity');
  return res.data.data;
};

export const getAiUsageSummary = async (): Promise<import('../types/lms').AiUsageSummaryDTO> => {
  const res = await apiClient.get<ApiResponse<import('../types/lms').AiUsageSummaryDTO>>('/admin/analytics/ai-usage');
  return res.data.data;
};

export const getAiTelemetry = async (page = 0, size = 10): Promise<{ content: import('../types/lms').AiTelemetryRecord[]; totalElements: number; totalPages: number }> => {
  const res = await apiClient.get<ApiResponse<any>>('/admin/analytics/ai-telemetry', { params: { page, size } });
  return res.data.data;
};

// Teacher Verification
export const getAdminTeachers = async (status?: string): Promise<import('../types/lms').TeacherVerificationDTO[]> => {
  const params: Record<string, string> = {};
  if (status) params.status = status;
  const res = await apiClient.get<ApiResponse<import('../types/lms').TeacherVerificationDTO[]>>('/admin/teachers', { params });
  return res.data.data;
};

export const getAdminPendingTeachers = async (): Promise<import('../types/lms').TeacherVerificationDTO[]> => {
  const res = await apiClient.get<ApiResponse<import('../types/lms').TeacherVerificationDTO[]>>('/admin/teachers/pending');
  return res.data.data;
};

export const approveTeacher = async (id: string): Promise<import('../types/lms').TeacherVerificationDTO> => {
  const res = await apiClient.post<ApiResponse<import('../types/lms').TeacherVerificationDTO>>(`/admin/teachers/${id}/approve`);
  return res.data.data;
};

export const rejectTeacher = async (id: string, reason?: string): Promise<import('../types/lms').TeacherVerificationDTO> => {
  const res = await apiClient.post<ApiResponse<import('../types/lms').TeacherVerificationDTO>>(`/admin/teachers/${id}/reject`, { reason });
  return res.data.data;
};

// Content Moderation
export const getAdminDocuments = async (active?: boolean): Promise<import('../types/lms').AdminDocumentDTO[]> => {
  const params: Record<string, boolean> = {};
  if (active !== undefined) params.active = active;
  const res = await apiClient.get<ApiResponse<import('../types/lms').AdminDocumentDTO[]>>('/admin/content/documents', { params });
  return res.data.data;
};

export const updateAdminDocumentStatus = async (id: string, active: boolean): Promise<import('../types/lms').AdminDocumentDTO> => {
  const res = await apiClient.patch<ApiResponse<import('../types/lms').AdminDocumentDTO>>(`/admin/content/documents/${id}/status`, { active });
  return res.data.data;
};

export const getAdminQuizzes = async (status?: string): Promise<import('../types/lms').AdminQuizDTO[]> => {
  const params: Record<string, string> = {};
  if (status) params.status = status;
  const res = await apiClient.get<ApiResponse<import('../types/lms').AdminQuizDTO[]>>('/admin/content/quizzes', { params });
  return res.data.data;
};

export const updateAdminQuizStatus = async (id: string, status?: string, active?: boolean): Promise<import('../types/lms').AdminQuizDTO> => {
  const res = await apiClient.patch<ApiResponse<import('../types/lms').AdminQuizDTO>>(`/admin/content/quizzes/${id}/status`, { status, active });
  return res.data.data;
};

