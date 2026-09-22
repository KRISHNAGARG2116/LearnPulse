import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { MainLayout } from '../layouts/MainLayout';
import { LoginPage } from '../pages/auth/LoginPage';
import { RegisterPage } from '../pages/auth/RegisterPage';

import { StudentDashboardPage } from '../pages/student/StudentDashboardPage';
import { StudentSubjectCatalogPage } from '../pages/student/StudentSubjectCatalogPage';
import { StudentChapterListPage } from '../pages/student/StudentChapterListPage';
import { StudentMaterialViewerPage } from '../pages/student/StudentMaterialViewerPage';
import { StudentQuizExecutionPage } from '../pages/student/StudentQuizExecutionPage';
import { StudentQuizResultPage } from '../pages/student/StudentQuizResultPage';
import { StudentProgressPage } from '../pages/student/StudentProgressPage';
import { StudentProfilePage } from '../pages/student/StudentProfilePage';

import { TeacherDashboardPage } from '../pages/teacher/TeacherDashboardPage';
import { TeacherQuestionsPage } from '../pages/teacher/TeacherQuestionsPage';

import { AdminDashboardPage } from '../pages/admin/AdminDashboardPage';
import { AdminUsersPage } from '../pages/admin/AdminUsersPage';

import { NotFoundPage } from '../pages/fallback/NotFoundPage';
import { ForbiddenPage } from '../pages/fallback/ForbiddenPage';

export const AppRoutes: React.FC = () => {
  return (
    <Routes>
      {/* Public Routes */}
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
          <Route path="/student/profile" element={<StudentProfilePage />} />
        </Route>
      </Route>

      {/* Teacher Protected Routes */}
      <Route element={<ProtectedRoute allowedRoles={['TEACHER', 'ADMIN']} />}>
        <Route element={<MainLayout />}>
          <Route path="/teacher/dashboard" element={<TeacherDashboardPage />} />
          <Route path="/teacher/questions" element={<TeacherQuestionsPage />} />
          <Route path="/teacher/content" element={<TeacherDashboardPage />} />
          <Route path="/teacher/students" element={<TeacherDashboardPage />} />
          <Route path="/teacher/profile" element={<StudentProfilePage />} />
        </Route>
      </Route>

      {/* Admin Protected Routes */}
      <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
        <Route element={<MainLayout />}>
          <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
          <Route path="/admin/users" element={<AdminUsersPage />} />
          <Route path="/admin/teachers" element={<AdminUsersPage />} />
          <Route path="/admin/students" element={<AdminUsersPage />} />
          <Route path="/admin/system-settings" element={<AdminDashboardPage />} />
        </Route>
      </Route>

      {/* Root Redirect */}
      <Route path="/" element={<Navigate to="/login" replace />} />

      {/* Fallback Catch-all Route */}
      <Route path="*" element={<Navigate to="/404" replace />} />
    </Routes>
  );
};
