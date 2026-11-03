import React, { Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { MainLayout } from '../layouts/MainLayout';

// --- Eagerly loaded (small, fast, always needed) ---
import { LoginPage } from '../pages/auth/LoginPage';
import { RegisterPage } from '../pages/auth/RegisterPage';
import { NotFoundPage } from '../pages/fallback/NotFoundPage';
import { ForbiddenPage } from '../pages/fallback/ForbiddenPage';

// --- Lazy-loaded Student pages ---
// Code-splitting reduces the initial JS bundle size.
// Each lazy import creates a separate chunk loaded on first navigation.
const StudentDashboardPage = React.lazy(() =>
  import('../pages/student/StudentDashboardPage').then(m => ({ default: m.StudentDashboardPage }))
);
const StudentSubjectCatalogPage = React.lazy(() =>
  import('../pages/student/StudentSubjectCatalogPage').then(m => ({ default: m.StudentSubjectCatalogPage }))
);
const StudentChapterListPage = React.lazy(() =>
  import('../pages/student/StudentChapterListPage').then(m => ({ default: m.StudentChapterListPage }))
);
const StudentMaterialViewerPage = React.lazy(() =>
  import('../pages/student/StudentMaterialViewerPage').then(m => ({ default: m.StudentMaterialViewerPage }))
);
const StudentQuizExecutionPage = React.lazy(() =>
  import('../pages/student/StudentQuizExecutionPage').then(m => ({ default: m.StudentQuizExecutionPage }))
);
const StudentQuizResultPage = React.lazy(() =>
  import('../pages/student/StudentQuizResultPage').then(m => ({ default: m.StudentQuizResultPage }))
);
const StudentProgressPage = React.lazy(() =>
  import('../pages/student/StudentProgressPage').then(m => ({ default: m.StudentProgressPage }))
);
const StudentProfilePage = React.lazy(() =>
  import('../pages/student/StudentProfilePage').then(m => ({ default: m.StudentProfilePage }))
);
// AI feature pages — heaviest content, highest benefit from lazy loading
const StudentAiTutorPage = React.lazy(() =>
  import('../pages/student/StudentAiTutorPage').then(m => ({ default: m.StudentAiTutorPage }))
);
const StudentPdfAssistantPage = React.lazy(() =>
  import('../pages/student/StudentPdfAssistantPage').then(m => ({ default: m.StudentPdfAssistantPage }))
);
const StudentFlashcardsPage = React.lazy(() =>
  import('../pages/student/StudentFlashcardsPage').then(m => ({ default: m.StudentFlashcardsPage }))
);
const StudentStudyPlannerPage = React.lazy(() =>
  import('../pages/student/StudentStudyPlannerPage').then(m => ({ default: m.StudentStudyPlannerPage }))
);
const StudentCodeExplainerPage = React.lazy(() =>
  import('../pages/student/StudentCodeExplainerPage').then(m => ({ default: m.StudentCodeExplainerPage }))
);

// --- Lazy-loaded Teacher pages ---
const TeacherDashboardPage = React.lazy(() =>
  import('../pages/teacher/TeacherDashboardPage').then(m => ({ default: m.TeacherDashboardPage }))
);
const TeacherQuestionsPage = React.lazy(() =>
  import('../pages/teacher/TeacherQuestionsPage').then(m => ({ default: m.TeacherQuestionsPage }))
);
const TeacherStudentsPage = React.lazy(() =>
  import('../pages/teacher/TeacherStudentsPage').then(m => ({ default: m.TeacherStudentsPage }))
);

// --- Lazy-loaded Admin pages (Chart.js dependency — largest chunk benefit) ---
const AdminDashboardPage = React.lazy(() =>
  import('../pages/admin/AdminDashboardPage').then(m => ({ default: m.AdminDashboardPage }))
);
const AdminUsersPage = React.lazy(() =>
  import('../pages/admin/AdminUsersPage').then(m => ({ default: m.AdminUsersPage }))
);
const AdminTeachersPage = React.lazy(() =>
  import('../pages/admin/AdminTeachersPage').then(m => ({ default: m.AdminTeachersPage }))
);
const AdminContentModerationPage = React.lazy(() =>
  import('../pages/admin/AdminContentModerationPage').then(m => ({ default: m.AdminContentModerationPage }))
);

/**
 * Shared loading fallback shown during dynamic chunk loading.
 * Kept intentionally minimal to avoid layout shift and flash.
 */
const PageLoadingFallback: React.FC = () => (
  <div
    style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: '60vh',
      flexDirection: 'column',
      gap: '12px',
      color: '#94a3b8',
      fontSize: '14px',
    }}
    role="status"
    aria-live="polite"
    aria-label="Loading page content"
  >
    <svg
      width="32"
      height="32"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      style={{ animation: 'spin 1s linear infinite' }}
    >
      <path d="M21 12a9 9 0 1 1-6.219-8.56" />
    </svg>
    <span>Loading...</span>
    <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
  </div>
);

export const AppRoutes: React.FC = () => {
  return (
    <Suspense fallback={<PageLoadingFallback />}>
      <Routes>
        {/* Public Routes — eagerly loaded for fast initial render */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/404" element={<NotFoundPage />} />
        <Route path="/403" element={<ForbiddenPage />} />

        {/* Student Protected Routes */}
        <Route element={<ProtectedRoute allowedRoles={['STUDENT']} />}>
          <Route element={<MainLayout />}>
            <Route path="/student/dashboard" element={<StudentDashboardPage />} />
            <Route path="/student/subjects" element={<StudentSubjectCatalogPage />} />
            <Route path="/student/subjects/:subjectId/chapters" element={<StudentChapterListPage />} />
            <Route path="/student/chapters/:chapterId/materials" element={<StudentMaterialViewerPage />} />
            <Route path="/student/quiz/:quizId" element={<StudentQuizExecutionPage />} />
            <Route path="/student/quiz/result/:resultId" element={<StudentQuizResultPage />} />
            <Route path="/student/progress" element={<StudentProgressPage />} />
            <Route path="/student/practice" element={<StudentSubjectCatalogPage />} />
            <Route path="/student/results" element={<StudentProgressPage />} />
            <Route path="/student/tutor" element={<StudentAiTutorPage />} />
            <Route path="/student/pdf-assistant" element={<StudentPdfAssistantPage />} />
            <Route path="/student/flashcards" element={<StudentFlashcardsPage />} />
            <Route path="/student/study-plan" element={<StudentStudyPlannerPage />} />
            <Route path="/student/code-explainer" element={<StudentCodeExplainerPage />} />
            <Route path="/student/profile" element={<StudentProfilePage />} />
          </Route>
        </Route>

        {/* Teacher Protected Routes */}
        <Route element={<ProtectedRoute allowedRoles={['TEACHER', 'ADMIN']} />}>
          <Route element={<MainLayout />}>
            <Route path="/teacher/dashboard" element={<TeacherDashboardPage />} />
            <Route path="/teacher/questions" element={<TeacherQuestionsPage />} />
            <Route path="/teacher/content" element={<TeacherDashboardPage />} />
            <Route path="/teacher/students" element={<TeacherStudentsPage />} />
            <Route path="/teacher/profile" element={<StudentProfilePage />} />
          </Route>
        </Route>

        {/* Admin Protected Routes */}
        <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
          <Route element={<MainLayout />}>
            <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
            <Route path="/admin/users" element={<AdminUsersPage />} />
            <Route path="/admin/teachers" element={<AdminTeachersPage />} />
            <Route path="/admin/students" element={<AdminUsersPage />} />
            <Route path="/admin/content-moderation" element={<AdminContentModerationPage />} />
            <Route path="/admin/system-settings" element={<AdminContentModerationPage />} />
          </Route>
        </Route>

        {/* Root Redirect */}
        <Route path="/" element={<Navigate to="/login" replace />} />

        {/* Fallback Catch-all Route */}
        <Route path="*" element={<Navigate to="/404" replace />} />
      </Routes>
    </Suspense>
  );
};
