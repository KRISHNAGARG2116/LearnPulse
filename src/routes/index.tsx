import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { MainLayout } from '../layouts/MainLayout';
import { LoginPage } from '../pages/auth/LoginPage';
import { RegisterPage } from '../pages/auth/RegisterPage';

import { StudentDashboardPage } from '../pages/student/StudentDashboardPage';
import { StudentPracticePage } from '../pages/student/StudentPracticePage';
import { StudentResultsPage } from '../pages/student/StudentResultsPage';
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
          <Route path="/student/practice" element={<StudentPracticePage />} />
          <Route path="/student/results" element={<StudentResultsPage />} />
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
