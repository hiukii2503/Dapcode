import React, { Suspense, lazy } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Spinner } from 'react-bootstrap';
import ProblemsPage from './pages/ProblemsPage';
import AdminRoute from './components/AdminRoute';
import BannedModal from './components/BannedModal';
import { ThemeProvider } from './context/ThemeContext';

const WorkspacePage = lazy(() => import('./pages/WorkspacePage'));
const TheoriesPage = lazy(() => import('./pages/TheoriesPage'));
const TheoryDetailPage = lazy(() => import('./pages/TheoryDetailPage'));
const AuthCallbackPage = lazy(() => import('./pages/AuthCallbackPage'));

const AdminLayout = lazy(() => import('./layouts/AdminLayout'));
const AdminDashboardPage = lazy(() => import('./pages/admin/AdminDashboardPage'));
const AdminProblemsPage = lazy(() => import('./pages/admin/AdminProblemsPage'));
const AdminTestCasesPage = lazy(() => import('./pages/admin/AdminTestCasesPage'));
const AdminTheoriesPage = lazy(() => import('./pages/admin/AdminTheoriesPage'));
const AdminUsersPage = lazy(() => import('./pages/admin/AdminUsersPage'));

function App() {
  return (
    <ThemeProvider>
      <BrowserRouter>
        <BannedModal />
        <Suspense
          fallback={
            <div className="d-flex vh-100 vw-100 align-items-center justify-content-center gap-2 bg-canvas fs-xs text-body-secondary">
              <Spinner animation="border" size="sm" />
              <span>Đang tải trang...</span>
            </div>
          }
        >
          <Routes>
            <Route path="/" element={<Navigate to="/roadmap" replace />} />
            <Route path="/roadmap" element={<ProblemsPage initialView="graph" />} />
            <Route path="/problems" element={<ProblemsPage initialView="accordion" />} />
            <Route path="/practice" element={<ProblemsPage initialView="accordion" />} />
            <Route path="/problems/:slug" element={<WorkspacePage />} />
            <Route path="/theories" element={<TheoriesPage />} />
            <Route path="/theories/:slug" element={<TheoryDetailPage />} />
            <Route path="/auth/callback" element={<AuthCallbackPage />} />

            {/* Admin routes */}
            <Route
              path="/admin"
              element={
                <AdminRoute>
                  <AdminLayout />
                </AdminRoute>
              }
            >
              <Route index element={<AdminDashboardPage />} />
              <Route path="problems" element={<AdminProblemsPage />} />
              <Route path="testcases" element={<AdminTestCasesPage />} />
              <Route path="theories" element={<AdminTheoriesPage />} />
              <Route path="users" element={<AdminUsersPage />} />
            </Route>
            <Route path="/admin/login" element={<Navigate to="/admin" replace />} />

            <Route path="*" element={<Navigate to="/roadmap" replace />} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </ThemeProvider>
  );
}

export default App;
