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

export type EnrollmentStatus = 'ACTIVE' | 'INACTIVE';

export interface TeacherSubjectDTO {
  subjectId: string;
  subjectName: string;
  subjectCode: string;
  subjectDescription?: string;
  chapterCount: number;
  assignedAt: string;
}

export interface EnrolledSubjectDTO {
  subjectId: string;
  subjectName: string;
  subjectCode: string;
  subjectDescription?: string;
  chapterCount: number;
  status: EnrollmentStatus;
  enrolledAt: string;
  teacherFirstName?: string;
  teacherLastName?: string;
  teacherEmail?: string;
}

export interface StudentSummaryDTO {
  userId: string;
  email: string;
  firstName?: string;
  lastName?: string;
  department?: string;
  enrollmentNumber?: string;
  enrolledAt?: string;
  status?: EnrollmentStatus;
}

export interface EnrollmentDTO {
  id: string;
  studentId: string;
  studentEmail: string;
  studentFirstName?: string;
  studentLastName?: string;
  enrollmentNumber?: string;
  subjectId: string;
  subjectName: string;
  status: EnrollmentStatus;
  enrolledAt: string;
}

export interface AssignTeacherRequest {
  teacherId: string;
}

export interface EnrollStudentRequest {
  studentId: string;
}

// Week 12: AI Quiz Generator Types
export interface GenerateQuizRequest {
  documentId: string;
  questionCount?: number;
  difficulty?: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
  quizTitle?: string;
  teacherPriorities?: string;
  customInstructions?: string;
}

export interface GeneratedQuizQuestionDto {
  id?: string;
  questionText: string;
  optionA: string;
  optionB: string;
  optionC: string;
  optionD: string;
  correctAnswer: string;
  explanation?: string;
  marks?: number;
  source?: string;
}

export interface GeneratedQuizResponse {
  quizId: string;
  title: string;
  description?: string;
  subjectId: string;
  subjectName: string;
  chapterId: string;
  chapterTitle: string;
  documentId: string;
  documentFileName: string;
  status: string;
  isPublished: boolean;
  passingScorePercentage: number;
  totalMarks: number;
  questionsCount: number;
  questions: GeneratedQuizQuestionDto[];
  createdAt: string;
}

// Week 12: AI Chat & Tutor Types
export interface AiChatMessage {
  id?: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp?: string;
  error?: boolean;
}

export interface AiChatHistoryDTO {
  id: string;
  role: string;
  content: string;
  createdAt: string;
}

// Week 12: RAG PDF Assistant Types
export interface RagSourceDTO {
  documentId?: string;
  documentTitle?: string;
  chunkIndex?: number;
  text: string;
  similarity?: number;
}

export interface AskDocumentRequest {
  documentId: string;
  question: string;
}

export interface AskDocumentResponse {
  answer: string;
  sources?: RagSourceDTO[];
}

// Week 12: Flashcard Types
export interface FlashcardItem {
  id: string;
  question: string;
  answer: string;
  subjectId?: string;
  chapterId?: string;
}

export interface FlashcardGenerateRequest {
  content: string;
  cardCount?: number;
}

export interface FlashcardDto {
  question: string;
  answer: string;
}

export interface FlashcardGenerateResponse {
  flashcards: FlashcardDto[];
}

// Week 12: AI Study Planner Types
export interface StudyPlanRequest {
  examDate: string;
  subjects: string[];
  availableHoursPerDay: number;
}

export interface StudyPlanDay {
  day: number;
  date: string;
  topics: string[];
  hours: number;
  tasks: string[];
}

export interface StudyPlanResponse {
  examDate: string;
  totalDays: number;
  dailyPlan: StudyPlanDay[];
  recommendations: string[];
}

// Week 12: AI Code Explainer Types
export interface CodeVariableItem {
  name: string;
  type?: string;
  purpose: string;
}

export interface CodeExplainerRequest {
  code: string;
  language?: string;
}

export interface CodeExplainerResponse {
  purpose: string;
  stepByStepLogic: string[];
  variables?: string[];
  methods?: string[];
  controlFlow?: string;
  algorithm?: string;
  timeComplexity: string;
  spaceComplexity: string;
  potentialImprovements?: string[];
}

// Week 12: AI Summarizer Types
export interface SummarizeContentRequest {
  content: string;
  maxLength?: number;
}

export interface SummarizeResponse {
  summary: string;
  keyTakeaways?: string[];
  keywords?: string[];
}

