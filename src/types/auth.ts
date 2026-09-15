export type Role = 'STUDENT' | 'TEACHER' | 'ADMIN';

export interface User {
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInMs: number;
  userId: string;
  email: string;
  role: Role;
  firstName: string;
  lastName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role: Role; // Note: Public registration only allows 'STUDENT' or 'TEACHER'
  department?: string;
  enrollmentNumber?: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface ApiResponse<T> {
  status: 'success' | 'error';
  message: string;
  data: T;
  timestamp: string;
  errors: string[] | null;
}

export interface ChapterProgressionDTO {
  chapterId: string;
  title: string;
  chapterNumber: number;
  description: string;
  isUnlocked: boolean;
  isCompleted: boolean;
  quizId: string | null;
  quizTitle: string | null;
  highestScorePercentage: number;
  isQuizPassed: boolean;
}

export interface CourseProgressionDTO {
  courseId: string;
  courseName: string;
  courseCode: string;
  totalChapters: number;
  completedChapters: number;
  currentUnlockedChapterNumber: number;
  finalQuizId: string | null;
  finalQuizTitle: string | null;
  isFinalQuizUnlocked: boolean;
  isFinalQuizPassed: boolean;
  isCourseCompleted: boolean;
  chapters: ChapterProgressionDTO[];
}
