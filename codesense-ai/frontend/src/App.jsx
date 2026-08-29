import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';

import { ThemeProvider } from './context/ThemeContext';
import { AuthProvider } from './context/AuthContext';
import { ProjectProvider } from './context/ProjectContext';
import { EditorProvider } from './context/EditorContext';
import { AIProvider } from './context/AIContext';

import PrivateRoute from './components/common/PrivateRoute';
import Sidebar from './components/Sidebar/Sidebar';
import Navbar from './components/Navbar/Navbar';

import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import OAuthConsentPage from './pages/OAuthConsentPage';
import DashboardPage from './pages/DashboardPage';
import ProjectsPage from './pages/ProjectsPage';
import ProjectDetailPage from './pages/ProjectDetailPage';
import RepositoryPage from './pages/RepositoryPage';
import ChatPage from './pages/ChatPage';
import SearchPage from './pages/SearchPage';
import CodeExplainPage from './pages/CodeExplainPage';
import ReadmePage from './pages/ReadmePage';
import ApiDocsPage from './pages/ApiDocsPage';
import ArchitecturePage from './pages/ArchitecturePage';
import MetricsPage from './pages/MetricsPage';
import DependenciesPage from './pages/DependenciesPage';
import QualityDashboardPage from './pages/QualityDashboardPage';

import Workspace from './pages/Workspace';
import Settings from './pages/Settings';

const AppLayout = () => {
  return (
    <div className="cs-app-layout">
      <Sidebar />
      <div className="cs-main-wrapper">
        <Navbar />
        <main className="cs-content-area">
          <Routes>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/projects" element={<ProjectsPage />} />
            <Route path="/projects/:id" element={<ProjectDetailPage />} />
            <Route path="/projects/:id/repository" element={<RepositoryPage />} />
            <Route path="/projects/:id/chat" element={<ChatPage />} />
            <Route path="/projects/:id/search" element={<SearchPage />} />
            <Route path="/projects/:id/code-explanation" element={<CodeExplainPage />} />
            <Route path="/projects/:id/readme" element={<ReadmePage />} />
            <Route path="/projects/:id/api-docs" element={<ApiDocsPage />} />
            <Route path="/projects/:id/architecture" element={<ArchitecturePage />} />
            <Route path="/projects/:id/metrics" element={<MetricsPage />} />
            <Route path="/projects/:id/dependencies" element={<DependenciesPage />} />
            <Route path="/projects/:id/quality" element={<QualityDashboardPage />} />

            <Route path="/workspace" element={<Workspace />} />
            <Route path="/settings" element={<Settings />} />
          </Routes>
        </main>
      </div>
    </div>
  );
};

function App() {
  return (
    <BrowserRouter>
      <ThemeProvider>
        <AuthProvider>
          <ProjectProvider>
            <EditorProvider>
              <AIProvider>
                <Routes>
                  {/* Public Auth Routes (No Sidebar/Navbar) */}
                  <Route path="/login" element={<LoginPage />} />
                  <Route path="/register" element={<RegisterPage />} />
                  <Route path="/oauth/consent" element={<OAuthConsentPage />} />

                  {/* Protected Main Application Layout Shell */}
                  <Route element={<PrivateRoute />}>
                    <Route path="/*" element={<AppLayout />} />
                  </Route>

                  <Route path="*" element={<Navigate to="/dashboard" replace />} />
                </Routes>
              </AIProvider>
            </EditorProvider>
          </ProjectProvider>
        </AuthProvider>
      </ThemeProvider>
    </BrowserRouter>
  );
}

export default App;
