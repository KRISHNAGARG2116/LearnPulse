export interface SubjectDTO {
  id: string;
  name: string;
  code: string;
  description?: string;
  createdAt?: string;
}

export interface CreateSubjectRequest {
  name: string;
  code: string;
  description?: string;
}

export interface ChapterDTO {
  id: string;
  title: string;
  chapterNumber: number;
  description?: string;
  subjectId: string;
  subjectName?: string;
  createdAt?: string;
}

export interface CreateChapterRequest {
  title: string;
  chapterNumber: number;
  description?: string;
}

export interface DocumentDTO {
  id: string;
  originalFileName: string;
  storedFileName: string;
  fileSize: number;
  contentType: string;
  teacherId?: string;
  teacherEmail?: string;
  subjectId?: string;
  subjectName?: string;
  chapterId?: string;
  chapterTitle?: string;
  processingStatus?: string;
  extractedText?: string;
  createdAt?: string;
}

export interface StudentQuestionDTO {
  id: string;
  questionText: string;
  optionA: string;
  optionB: string;
  optionC: string;
  optionD: string;
  marks: number;
}

export interface StudentQuizDTO {
  id: string;
  title: string;
  description?: string;
  subjectId?: string;
  subjectName?: string;
  chapterId?: string;
  chapterTitle?: string;
  totalMarks: number;
  quizType?: string;
  status?: string;
  isPublished?: boolean;
  passingScorePercentage?: number;
  createdById?: string;
  createdByEmail?: string;
  questions?: StudentQuestionDTO[];
  createdAt?: string;
}

export interface QuestionAnswerRequest {
  questionId: string;
  selectedAnswer: string;
}

export interface QuizSubmissionRequest {
  quizId: string;
  answers: QuestionAnswerRequest[];
}

export interface StudentQuizResultDTO {
  id: string;
  quizId: string;
  quizTitle: string;
  studentId: string;
  studentEmail: string;
  score: number;
  totalMarks: number;
  percentage: number;
  correctAnswers: number;
  wrongAnswers: number;
  isPassed?: boolean;
  passingScorePercentage?: number;
  attemptedAt: string;
}

export interface StudentProgressDTO {
  studentId: string;
  studentEmail: string;
  totalQuizzesAttempted: number;
  totalQuizzesCompleted: number;
  averageScore: number;
  averagePercentage: number;
  highestScore: number;
  totalCorrectAnswers: number;
  totalWrongAnswers: number;
  recentAttempts: StudentQuizResultDTO[];
}

export interface CreateQuestionRequest {
  questionText: string;
  optionA: string;
  optionB: string;
  optionC: string;
  optionD: string;
  correctAnswer: string;
  marks?: number;
}

export interface CreateQuizRequest {
  title: string;
  description?: string;
  subjectId?: string;
  chapterId?: string;
  quizType?: string;
  status?: string;
  isPublished?: boolean;
  passingScorePercentage?: number;
  questions: CreateQuestionRequest[];
}
