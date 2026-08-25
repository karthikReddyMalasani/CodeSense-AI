import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { INITIAL_RECENT_PROJECTS } from '../utils/constants';
import ImportProjectModal from '../components/common/ImportProjectModal';
import {
  Sparkles,
  Plus,
  FolderOpen,
  Code2,
  Wand2,
  Search,
  Bug,
  Zap,
  TestTube,
  ArrowRight,
  Clock,
  Files,
  FolderGit2,
  FileArchive
} from 'lucide-react';

const Dashboard = () => {
  const navigate = useNavigate();
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);

  return (
    <div className="dashboard-container">
      {/* Welcome Banner */}
      <section className="welcome-banner">
        <div className="banner-content">
          <div className="badge-pill">
            <Sparkles className="pill-icon glow" />
            <span>AI-Powered Developer Studio</span>
          </div>
          <h1 className="hero-title">Welcome to CodeAssist AI</h1>
          <p className="hero-subtitle">
            Write better code. Understand your codebase. Fix bugs faster.
          </p>

          <div className="hero-actions">
            <button className="btn btn-primary btn-hero" onClick={() => setIsImportModalOpen(true)}>
              <FolderGit2 className="btn-icon" />
              <span>Import GitHub Repo</span>
            </button>

            <button className="btn btn-secondary btn-hero" onClick={() => setIsImportModalOpen(true)}>
              <FileArchive className="btn-icon" />
              <span>Upload ZIP Folder</span>
            </button>

            <button className="btn btn-accent btn-hero" onClick={() => navigate('/workspace')}>
              <Code2 className="btn-icon" />
              <span>Start Coding</span>
            </button>
          </div>
        </div>
      </section>

      {/* Quick Action Cards */}
      <section className="dashboard-section">
        <h2 className="section-heading">Quick Actions</h2>
        <div className="quick-actions-grid">
          <div className="action-card" onClick={() => navigate('/generator')}>
            <div className="card-icon-box wand">
              <Wand2 className="card-icon" />
            </div>
            <h3 className="card-title">1. Generate Code</h3>
            <p className="card-desc">Generate production-grade functions from plain natural language descriptions.</p>
          </div>

          <div className="action-card" onClick={() => navigate('/analyzer')}>
            <div className="card-icon-box code">
              <Code2 className="card-icon" />
            </div>
            <h3 className="card-title">2. Explain Code</h3>
            <p className="card-desc">Get step-by-step logic breakdowns and architectural explanations.</p>
          </div>

          <div className="action-card" onClick={() => navigate('/analyzer')}>
            <div className="card-icon-box bug">
              <Bug className="card-icon" />
            </div>
            <h3 className="card-title">3. Debug Code</h3>
            <p className="card-desc">Identify NullPointer exceptions, memory leaks, and logic faults automatically.</p>
          </div>

          <div className="action-card" onClick={() => navigate('/analyzer')}>
            <div className="card-icon-box search">
              <Search className="card-icon" />
            </div>
            <h3 className="card-title">4. Analyze Code</h3>
            <p className="card-desc">Calculate code quality, cyclomatic complexity scores, and security risks.</p>
          </div>

          <div className="action-card" onClick={() => navigate('/analyzer')}>
            <div className="card-icon-box zap">
              <Zap className="card-icon" />
            </div>
            <h3 className="card-title">5. Optimize Code</h3>
            <p className="card-desc">Refactor algorithms for maximum speed ($O(n)$ time) and lower RAM footprint.</p>
          </div>

          <div className="action-card" onClick={() => navigate('/workspace')}>
            <div className="card-icon-box test">
              <TestTube className="card-icon" />
            </div>
            <h3 className="card-title">6. Generate Test Cases</h3>
            <p className="card-desc">Auto-create comprehensive unit test suites with boundary condition checks.</p>
          </div>
        </div>
      </section>

      {/* Recent Projects */}
      <section className="dashboard-section">
        <h2 className="section-heading">Recent Projects</h2>
        <div className="recent-projects-grid">
          {INITIAL_RECENT_PROJECTS.map((proj) => (
            <div key={proj.id} className="project-card">
              <div className="project-card-header">
                <h3 className="proj-name">{proj.name}</h3>
                <span className="lang-tag">{proj.language}</span>
              </div>
              <p className="proj-desc">{proj.description}</p>
              <div className="proj-meta">
                <span>
                  <Files className="meta-icon" /> {proj.fileCount} files
                </span>
                <span>
                  <Clock className="meta-icon" /> {proj.lastModified}
                </span>
              </div>
              <button className="btn btn-outline btn-open-proj" onClick={() => navigate('/workspace')}>
                <span>Open Project</span>
                <ArrowRight className="btn-icon" />
              </button>
            </div>
          ))}
        </div>
      </section>

      <ImportProjectModal isOpen={isImportModalOpen} onClose={() => setIsImportModalOpen(false)} />
    </div>
  );
};

export default Dashboard;
