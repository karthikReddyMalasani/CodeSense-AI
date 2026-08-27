import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useTheme } from '../../context/ThemeContext';
import {
  Code2,
  LayoutDashboard,
  FolderGit2,
  FolderPlus,
  DownloadCloud,
  MessageSquare,
  Search,
  Code,
  FileText,
  BookOpen,
  Layers,
  BarChart3,
  GitFork,
  Settings,
  Moon,
  Sun
} from 'lucide-react';

const Sidebar = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { theme, toggleTheme } = useTheme();

  const isActive = (path) => {
    if (path === '/dashboard') return location.pathname === '/dashboard' || location.pathname === '/';
    return location.pathname === path;
  };

  return (
    <aside className="cs-sidebar">
      {/* Brand Header */}
      <div className="cs-sidebar-brand" onClick={() => navigate('/dashboard')} style={{ cursor: 'pointer' }}>
        <div className="cs-brand-icon-box">
          <Code2 className="cs-brand-icon" />
        </div>
        <div className="cs-brand-info">
          <div className="cs-brand-title">CodeSense AI</div>
          <div className="cs-brand-subtitle">AI-Powered Code Intelligence</div>
        </div>
      </div>

      {/* Main Navigation Scroll Area */}
      <div className="cs-sidebar-scroll">
        {/* Main Dashboard Link */}
        <Link to="/dashboard" className={`cs-nav-item cs-nav-primary ${isActive('/dashboard') ? 'active' : ''}`}>
          <LayoutDashboard className="cs-nav-icon" />
          <span>Dashboard</span>
        </Link>

        {/* PROJECTS */}
        <div className="cs-nav-group">
          <div className="cs-nav-header">PROJECTS</div>
          <Link to="/projects" className={`cs-nav-item ${isActive('/projects') ? 'active' : ''}`}>
            <FolderGit2 className="cs-nav-icon" />
            <span>All Projects</span>
          </Link>
          <Link to="/projects" className="cs-nav-item" onClick={() => navigate('/projects', { state: { openNew: true } })}>
            <FolderPlus className="cs-nav-icon" />
            <span>New Project</span>
          </Link>
          <Link to="/dashboard#import" className="cs-nav-item" onClick={() => {
            navigate('/dashboard');
            setTimeout(() => {
              document.getElementById('import-section')?.scrollIntoView({ behavior: 'smooth' });
            }, 100);
          }}>
            <DownloadCloud className="cs-nav-icon" />
            <span>Import Repository</span>
          </Link>
        </div>

        {/* AI ASSISTANT */}
        <div className="cs-nav-group">
          <div className="cs-nav-header">AI ASSISTANT</div>
          <Link to="/projects" className={`cs-nav-item ${location.pathname.includes('/chat') ? 'active' : ''}`}>
            <MessageSquare className="cs-nav-icon" />
            <span>AI Chat</span>
          </Link>
          <Link to="/projects" className={`cs-nav-item ${location.pathname.includes('/search') ? 'active' : ''}`}>
            <Search className="cs-nav-icon" />
            <span>Semantic Search</span>
          </Link>
          <Link to="/projects" className={`cs-nav-item ${location.pathname.includes('/code-explanation') ? 'active' : ''}`}>
            <Code className="cs-nav-icon" />
            <span>Code Explanation</span>
          </Link>
        </div>

        {/* DOCUMENTATION */}
        <div className="cs-nav-group">
          <div className="cs-nav-header">DOCUMENTATION</div>
          <Link to="/projects" className={`cs-nav-item ${location.pathname.includes('/readme') ? 'active' : ''}`}>
            <FileText className="cs-nav-icon" />
            <span>README Generator</span>
          </Link>
          <Link to="/projects" className={`cs-nav-item ${location.pathname.includes('/api-docs') ? 'active' : ''}`}>
            <BookOpen className="cs-nav-icon" />
            <span>API Documentation</span>
          </Link>
        </div>

        {/* ANALYSIS */}
        <div className="cs-nav-group">
          <div className="cs-nav-header">ANALYSIS</div>
          <Link to="/projects" className={`cs-nav-item ${location.pathname.includes('/architecture') ? 'active' : ''}`}>
            <Layers className="cs-nav-icon" />
            <span>Architecture</span>
          </Link>
          <Link to="/projects" className={`cs-nav-item ${location.pathname.includes('/metrics') ? 'active' : ''}`}>
            <BarChart3 className="cs-nav-icon" />
            <span>Metrics</span>
          </Link>
          <Link to="/projects" className={`cs-nav-item ${location.pathname.includes('/dependencies') ? 'active' : ''}`}>
            <GitFork className="cs-nav-icon" />
            <span>Dependencies</span>
          </Link>
        </div>
      </div>

      {/* Sidebar Footer Widgets */}
      <div className="cs-sidebar-footer">
        {/* Controls */}
        <div className="cs-footer-actions">
          <button className="cs-theme-toggle" onClick={toggleTheme}>
            {theme === 'dark' ? <Moon className="cs-footer-icon" /> : <Sun className="cs-footer-icon" />}
            <span>Dark Mode</span>
            <div className={`cs-toggle-switch ${theme === 'dark' ? 'active' : ''}`}>
              <div className="cs-toggle-thumb" />
            </div>
          </button>

          <Link to="/settings" className="cs-nav-item cs-settings-link">
            <Settings className="cs-nav-icon" />
            <span>Settings</span>
          </Link>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
