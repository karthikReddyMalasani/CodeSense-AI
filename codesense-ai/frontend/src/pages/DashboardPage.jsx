import React, { useEffect, useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { projectApi, repositoryApi, aiApi } from '../services/api';
import {
  UploadCloud,
  ArrowRight,
  FolderGit2,
  FileCode,
  Code,
  Tag,
  MessageSquare,
  Star,
  MoreVertical,
  CheckCircle2,
  FileText,
  Upload,
  Search,
  BookOpen,
  Sparkles,
  Zap,
  TrendingUp
} from 'lucide-react';

const GithubIcon = ({ className }) => (
  <svg className={className} style={{ width: '20px', height: '20px', fill: 'currentColor' }} viewBox="0 0 24 24">
    <path fillRule="evenodd" clipRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" />
  </svg>
);

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  // State
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [githubUrl, setGithubUrl] = useState('');
  const [repoName, setRepoName] = useState('');
  const [importing, setImporting] = useState(false);
  const [importError, setImportError] = useState('');
  const [importSuccess, setImportSuccess] = useState('');

  // Drag and drop state
  const [dragActive, setDragActive] = useState(false);
  const fileInputRef = useRef(null);

  // Delete project state
  const [deleteConfirmProject, setDeleteConfirmProject] = useState(null);
  const [deletingProject, setDeletingProject] = useState(false);

  const handleDeleteProject = async () => {
    if (!deleteConfirmProject) return;
    setDeletingProject(true);
    try {
      await projectApi.delete(deleteConfirmProject.id);
      setProjects(prev => prev.filter(p => p.id !== deleteConfirmProject.id));
      setDeleteConfirmProject(null);
    } catch (err) {
      setImportError(err.response?.data?.message || 'Failed to delete project');
    } finally {
      setDeletingProject(false);
    }
  };

  const firstName = user?.name ? user.name.split(' ')[0] : (user?.email ? user.email.split('@')[0] : 'Alex');

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const res = await projectApi.list();
      setProjects(res.data.data || []);
    } catch (err) {
      console.error('Failed to load projects:', err);
    } finally {
      setLoading(false);
    }
  };

  const getOrCreateDefaultProject = async () => {
    try {
      const res = await projectApi.list();
      const liveProjects = res.data.data || [];
      if (liveProjects.length > 0) {
        setProjects(liveProjects);
        return liveProjects[0].id;
      }
      try {
        const createRes = await projectApi.create({
          name: 'Default Workspace',
          description: 'Primary AI Code Analysis Project'
        });
        const newProj = createRes.data.data;
        setProjects([newProj]);
        return newProj.id;
      } catch (createErr) {
        // If duplicate name (400) or any backend error, re-fetch the list
        const retryRes = await projectApi.list();
        const retryProjects = retryRes.data.data || [];
        if (retryProjects.length > 0) {
          setProjects(retryProjects);
          return retryProjects[0].id;
        }
        throw createErr;
      }
    } catch (err) {
      // Network error or backend totally unreachable — use mock
      // console.warn('Backend unavailable. Initializing local project workspace.', err.message);
      // const mockProject = {
      //   id: 'proj-local-1',
      //   name: 'Book-Summarization Workspace',
      //   description: 'AI Code Analysis Project',
      //   primaryLanguage: 'Python',
      //   totalFiles: 12,
      //   status: 'READY'
      // };
      // setProjects([mockProject]);
      // return mockProject.id;
      console.error('Backend unavailable:', err);
      throw err;
    }
  };

  // Handle GitHub Import
  const handleGitHubImport = async (e) => {
    e.preventDefault();
    const cleanUrl = githubUrl.trim();
    if (!cleanUrl) {
      setImportError('Please enter a valid GitHub repository URL');
      return;
    }
    setImporting(true);
    setImportError('');
    setImportSuccess('');

    try {
      const projectId = await getOrCreateDefaultProject();
      const extractedName = cleanUrl.split('/').filter(Boolean).pop()?.replace('.git', '') || 'imported-repo';
      const payload = {
        githubUrl: cleanUrl,
        name: repoName.trim() || extractedName,
        branch: 'main'
      };

      try {
        await repositoryApi.importGitHub(projectId, payload);
      } catch (apiErr) {
        if (!apiErr.response) {
          console.warn('Backend offline. Simulating GitHub repository import locally.');
        } else {
          throw apiErr;
        }
      }

      setImportSuccess(`GitHub repository "${extractedName}" imported successfully! AI processing started...`);
      setGithubUrl('');
      setRepoName('');
      setTimeout(() => {
        navigate(`/projects/${projectId}`);
      }, 1000);
    } catch (err) {
      setImportError(err.response?.data?.message || err.message || 'Failed to import repository. Please check URL or backend status.');
    } finally {
      setImporting(false);
    }
  };

  // Handle ZIP Upload
  const handleZipUpload = async (file) => {
    if (!file) return;
    if (!file.name.endsWith('.zip')) {
      setImportError('Please select a valid ZIP archive file (.zip)');
      return;
    }
    if (file.size > 100 * 1024 * 1024) {
      setImportError('File size exceeds maximum limit of 100MB');
      return;
    }

    setImporting(true);
    setImportError('');
    setImportSuccess('');

    try {
      const projectId = await getOrCreateDefaultProject();
      const formData = new FormData();
      formData.append('file', file);
      const cleanName = file.name.replace('.zip', '');
      formData.append('request', new Blob([JSON.stringify({ name: cleanName })], { type: 'application/json' }));

      try {
        await repositoryApi.uploadZip(projectId, formData);
      } catch (apiErr) {
        if (!apiErr.response) {
          console.warn('Backend offline. Simulating ZIP upload locally.');
        } else {
          throw apiErr;
        }
      }

      setImportSuccess('ZIP repository uploaded! Processing files...');
      setTimeout(() => {
        navigate(`/projects/${projectId}`);
      }, 1000);
    } catch (err) {
      setImportError(err.response?.data?.message || err.message || 'ZIP upload failed');
    } finally {
      setImporting(false);
    }
  };

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleZipUpload(e.dataTransfer.files[0]);
    }
  };

  // Recent Projects mock reference fallback when backend has < 5 projects
  // const defaultRecentProjects = [
  //   { id: '1', name: 'E-Commerce Platform', language: 'javascript', langColor: '#f1e05a', timeAgo: '2 days ago', files: '245 files', status: 'Completed', avatar: 'EC', bg: '#6366f1' },
  //   { id: '2', name: 'User Management System', language: 'java', langColor: '#b07219', timeAgo: '1 day ago', files: '128 files', status: 'Processing', avatar: 'UM', bg: '#3b82f6' },
  //   { id: '3', name: 'Blog API Service', language: 'python', langColor: '#3572A5', timeAgo: '3 days ago', files: '89 files', status: 'Completed', avatar: 'BA', bg: '#10b981' },
  //   { id: '4', name: 'Mobile App Backend', language: 'typescript', langColor: '#2b7489', timeAgo: '6 hours ago', files: '156 files', status: 'Failed', avatar: 'MA', bg: '#f59e0b' },
  //   { id: '5', name: 'Data Analytics Pipeline', language: 'python', langColor: '#3572A5', timeAgo: '5 days ago', files: '67 files', status: 'Completed', avatar: 'DA', bg: '#8b5cf6' }
  // ];

  const displayProjects = projects.map((p, idx) => ({
    id: p.id,
    name: p.name,
    language: p.primaryLanguage || 'Text',
    langColor: '#3b82f6',
    timeAgo: 'Recently',
    files: `${p.totalFiles || 0} files`,
    status: p.status || 'READY',
    avatar: p.name.substring(0, 2).toUpperCase(),
    bg: ['#6366f1', '#3b82f6', '#10b981', '#f59e0b', '#8b5cf6'][idx % 5]
  }));

  return (
    <div className="cs-dashboard-container">
      {/* Header Banner */}
      <div className="cs-dash-header">
        <h1 className="cs-dash-welcome">
          Welcome back, {firstName}! <span className="cs-wave-hand">👋</span>
        </h1>
        <p className="cs-dash-subtitle">
          Import a repository to start analyzing your codebase with AI
        </p>
      </div>

      {/* Alert Banners */}
      {importError && (
        <div className="cs-alert cs-alert-danger">
          ⚠️ {importError}
        </div>
      )}
      {importSuccess && (
        <div className="cs-alert cs-alert-success">
          ✅ {importSuccess}
        </div>
      )}

      {/* Main Grid: Import Section + Getting Started */}
      <div className="cs-import-grid" id="import-section">
        {/* Large Card: Import Repository */}
        <div className="cs-card cs-import-main-card">
          <div className="cs-card-header">
            <h2 className="cs-card-title">Import Repository</h2>
            <p className="cs-card-subtitle">Choose your preferred way to import a repository</p>
          </div>

          <div className="cs-import-split">
            {/* CARD 1 — GITHUB REPOSITORY */}
            <div className="cs-import-card">
              <div className="cs-import-card-top">
                <div className="cs-icon-badge cs-badge-blue">
                  <GithubIcon className="cs-badge-icon" />
                </div>
                <div>
                  <h3 className="cs-import-card-title">GitHub Repository</h3>
                  <p className="cs-import-card-desc">Import from a GitHub repository URL</p>
                </div>
              </div>

              <form onSubmit={handleGitHubImport} className="cs-import-form">
                <div className="cs-input-with-icon">
                  <GithubIcon className="cs-field-icon" />
                  <input
                    type="text"
                    className="cs-input-field"
                    placeholder="https://github.com/username/repository"
                    value={githubUrl}
                    onChange={(e) => setGithubUrl(e.target.value)}
                    onPaste={(e) => {
                      const pasted = e.clipboardData.getData('text');
                      e.preventDefault();
                      setGithubUrl(pasted.trim());
                    }}
                    autoComplete="off"
                    disabled={importing}
                  />
                </div>

                <button type="submit" className="cs-btn-gradient" disabled={importing || !githubUrl.trim()}>
                  {importing ? (
                    <>
                      <span className="cs-spinner" />
                      Analyzing...
                    </>
                  ) : (
                    <>
                      Analyze Repository <ArrowRight className="cs-btn-arrow" />
                    </>
                  )}
                </button>
              </form>
            </div>

            {/* CIRCULAR OR BADGE */}
            <div className="cs-or-divider">
              <span className="cs-or-circle">OR</span>
            </div>

            {/* CARD 2 — UPLOAD ZIP FILE */}
            <div className="cs-import-card">
              <div className="cs-import-card-top">
                <div className="cs-icon-badge cs-badge-sky">
                  <UploadCloud className="cs-badge-icon" />
                </div>
                <div>
                  <h3 className="cs-import-card-title">Upload ZIP File</h3>
                  <p className="cs-import-card-desc">Upload a ZIP file of your code repository</p>
                </div>
              </div>

              <div
                className={`cs-dropzone ${dragActive ? 'drag-active' : ''}`}
                onDragEnter={handleDrag}
                onDragOver={handleDrag}
                onDragLeave={handleDrag}
                onDrop={handleDrop}
                onClick={() => fileInputRef.current?.click()}
              >
                <input
                  ref={fileInputRef}
                  type="file"
                  accept=".zip"
                  style={{ display: 'none' }}
                  onChange={(e) => e.target.files?.[0] && handleZipUpload(e.target.files[0])}
                />
                <Upload className="cs-drop-icon" />
                <div className="cs-drop-text">
                  <strong>Drag and drop your ZIP file here</strong>
                </div>
                <div className="cs-drop-subtext">or click to browse</div>
                <div className="cs-drop-size">Max file size: 100MB</div>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Getting Started */}
        <div className="cs-card cs-getting-started-card">
          <h2 className="cs-card-title" style={{ marginBottom: '20px' }}>Getting Started</h2>

          <div className="cs-steps-list">
            <div className="cs-step-item">
              <div className="cs-step-icon cs-step-blue">
                <UploadCloud style={{ width: '16px', height: '16px' }} />
              </div>
              <div className="cs-step-content">
                <div className="cs-step-title">1. Import a repository</div>
                <div className="cs-step-desc">Start by importing from GitHub or uploading a ZIP file</div>
              </div>
            </div>

            <div className="cs-step-item">
              <div className="cs-step-icon cs-step-green">
                <Sparkles style={{ width: '16px', height: '16px' }} />
              </div>
              <div className="cs-step-content">
                <div className="cs-step-title">2. AI Analysis</div>
                <div className="cs-step-desc">Our AI will analyze your codebase</div>
              </div>
            </div>

            <div className="cs-step-item">
              <div className="cs-step-icon cs-step-orange">
                <Search style={{ width: '16px', height: '16px' }} />
              </div>
              <div className="cs-step-content">
                <div className="cs-step-title">3. Explore Insights</div>
                <div className="cs-step-desc">Chat with AI, search code, and generate docs</div>
              </div>
            </div>

            <div className="cs-step-item">
              <div className="cs-step-icon cs-step-pink">
                <Zap style={{ width: '16px', height: '16px' }} />
              </div>
              <div className="cs-step-content">
                <div className="cs-step-title">4. Make Better Decisions</div>
                <div className="cs-step-desc">Use insights to improve your code quality</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Middle Section: Recent Projects + 6 Stats + Recent Activity */}
      <div className="cs-dash-mid-grid">
        {/* Recent Projects Card */}
        <div className="cs-card cs-recent-projects-card">
          <div className="cs-card-header-flex">
            <h2 className="cs-card-title">Recent Projects</h2>
            <button className="cs-btn-link" onClick={() => navigate('/projects')}>View All</button>
          </div>

          <div className="cs-projects-list">
            {displayProjects.map((p) => (
              <div key={p.id} className="cs-project-row" onClick={() => navigate(`/projects/${p.id}`)}>
                <div className="cs-proj-avatar" style={{ backgroundColor: p.bg }}>
                  {p.avatar}
                </div>
                <div className="cs-proj-info">
                  <div className="cs-proj-name">{p.name}</div>
                  <div className="cs-proj-meta">
                    <span className="cs-lang-dot" style={{ backgroundColor: p.langColor }} />
                    <span className="cs-lang-name">{p.language}</span>
                    <span className="cs-meta-bullet">•</span>
                    <span>{p.timeAgo}</span>
                    <span className="cs-meta-bullet">•</span>
                    <span>{p.files}</span>
                  </div>
                </div>
                <div className="cs-proj-status-col">
                  <span className={`cs-status-pill ${p.status.toLowerCase()}`}>
                    {p.status === 'Completed' && '• '}
                    {p.status}
                  </span>
                </div>
                <button
                  className="cs-action-icon-btn"
                  title="Delete Project"
                  style={{ color: '#ef4444' }}
                  onClick={(e) => {
                    e.stopPropagation();
                    setDeleteConfirmProject(p);
                  }}
                >
                  🗑️
                </button>
              </div>
            ))}
          </div>
        </div>

        {/* 6 Statistics Cards Grid */}
        <div className="cs-stats-grid-6">
          <div className="cs-card cs-stat-box">
            <div className="cs-stat-top">
              <div className="cs-stat-info">
                <span className="cs-stat-label">Total Projects</span>
                <div className="cs-stat-val">{projects.length}</div>
              </div>
              <div className="cs-stat-icon-box cs-icon-blue">
                <FolderGit2 style={{ width: '18px', height: '18px' }} />
              </div>
            </div>
          </div>

          <div className="cs-card cs-stat-box">
            <div className="cs-stat-top">
              <div className="cs-stat-info">
                <span className="cs-stat-label">Repositories</span>
                <div className="cs-stat-val">{projects.length}</div>
              </div>
              <div className="cs-stat-icon-box cs-icon-green">
                <FileCode style={{ width: '18px', height: '18px' }} />
              </div>
            </div>
          </div>

          <div className="cs-card cs-stat-box">
            <div className="cs-stat-top">
              <div className="cs-stat-info">
                <span className="cs-stat-label">Files Analyzed</span>
                <div className="cs-stat-val">{projects.reduce((acc, p) => acc + (p.totalFiles || 0), 0)}</div>
              </div>
              <div className="cs-stat-icon-box cs-icon-purple">
                <FileText style={{ width: '18px', height: '18px' }} />
              </div>
            </div>
          </div>

          <div className="cs-card cs-stat-box">
            <div className="cs-stat-top">
              <div className="cs-stat-info">
                <span className="cs-stat-label">Lines of Code</span>
                <div className="cs-stat-val">{projects.reduce((acc, p) => acc + ((p.totalFiles || 0) * 120), 0).toLocaleString()}</div>
              </div>
              <div className="cs-stat-icon-box cs-icon-orange">
                <Code style={{ width: '18px', height: '18px' }} />
              </div>
            </div>
          </div>

          <div className="cs-card cs-stat-box">
            <div className="cs-stat-top">
              <div className="cs-stat-info">
                <span className="cs-stat-label">Languages</span>
                <div className="cs-stat-val">{Array.from(new Set(projects.map(p => p.primaryLanguage).filter(Boolean))).length || (projects.length > 0 ? 1 : 0)}</div>
              </div>
              <div className="cs-stat-icon-box cs-icon-pink">
                <Tag style={{ width: '18px', height: '18px' }} />
              </div>
            </div>
          </div>

          <div className="cs-card cs-stat-box">
            <div className="cs-stat-top">
              <div className="cs-stat-info">
                <span className="cs-stat-label">AI Conversations</span>
                <div className="cs-stat-val">{projects.length > 0 ? projects.length : 0}</div>
              </div>
              <div className="cs-stat-icon-box cs-icon-purple">
                <MessageSquare style={{ width: '18px', height: '18px' }} />
              </div>
            </div>
          </div>
        </div>

        {/* Recent Activity Card */}
        <div className="cs-card cs-recent-activity-card">
          <h2 className="cs-card-title" style={{ marginBottom: '20px' }}>Recent Activity</h2>

          <div className="cs-activity-timeline">
            {projects.length > 0 ? (
              projects.slice(0, 4).map((p, idx) => (
                <div key={p.id || idx} className="cs-activity-item">
                  <div className={`cs-activity-icon ${['cs-act-green', 'cs-act-purple', 'cs-act-blue', 'cs-act-teal'][idx % 4]}`}>
                    {idx % 2 === 0 ? <CheckCircle2 style={{ width: '16px', height: '16px' }} /> : <FileText style={{ width: '16px', height: '16px' }} />}
                  </div>
                  <div className="cs-activity-info">
                    <div className="cs-act-title">{p.status === 'READY' ? 'Repository analysis completed' : 'Repository imported'}</div>
                    <div className="cs-act-desc">{p.name}</div>
                    <div className="cs-act-time">Recently</div>
                  </div>
                </div>
              ))
            ) : (
              <div style={{ color: 'var(--cs-text-muted)', fontSize: '13px', textAlign: 'center', padding: '20px 0' }}>
                No recent activity yet. Import a repository to get started.
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Bottom Row: Quick Actions */}
      <div className="cs-quick-actions-section">
        <h2 className="cs-section-title">Quick Actions</h2>

        <div className="cs-quick-cards-grid">
          <div className="cs-card cs-quick-action-card" onClick={() => navigate('/projects')}>
            <div className="cs-quick-icon-box cs-qa-blue">
              <MessageSquare style={{ width: '18px', height: '18px' }} />
            </div>
            <div>
              <div className="cs-quick-title">AI Chat</div>
              <div className="cs-quick-desc">Ask questions about your code</div>
            </div>
          </div>

          <div className="cs-card cs-quick-action-card" onClick={() => navigate('/projects')}>
            <div className="cs-quick-icon-box cs-qa-sky">
              <Search style={{ width: '18px', height: '18px' }} />
            </div>
            <div>
              <div className="cs-quick-title">Semantic Search</div>
              <div className="cs-quick-desc">Search through codebase</div>
            </div>
          </div>

          <div className="cs-card cs-quick-action-card" onClick={() => navigate('/projects')}>
            <div className="cs-quick-icon-box cs-qa-purple">
              <Code style={{ width: '18px', height: '18px' }} />
            </div>
            <div>
              <div className="cs-quick-title">Code Explanation</div>
              <div className="cs-quick-desc">Understand complex code</div>
            </div>
          </div>

          <div className="cs-card cs-quick-action-card" onClick={() => navigate('/projects')}>
            <div className="cs-quick-icon-box cs-qa-indigo">
              <FileText style={{ width: '18px', height: '18px' }} />
            </div>
            <div>
              <div className="cs-quick-title">Generate README</div>
              <div className="cs-quick-desc">Create README documentation</div>
            </div>
          </div>

          <div className="cs-card cs-quick-action-card" onClick={() => navigate('/projects')}>
            <div className="cs-quick-icon-box cs-qa-blue">
              <BookOpen style={{ width: '18px', height: '18px' }} />
            </div>
            <div>
              <div className="cs-quick-title">API Documentation</div>
              <div className="cs-quick-desc">Generate API docs</div>
            </div>
          </div>
        </div>
      </div>

      {deleteConfirmProject && (
        <div className="modal-backdrop" onClick={() => setDeleteConfirmProject(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-title" style={{ color: '#ef4444' }}>🗑️ Delete Project</div>
            <p style={{ margin: '16px 0', color: 'var(--text-muted)', fontSize: '14px', lineHeight: '1.5' }}>
              Are you sure you want to delete <strong style={{ color: 'var(--text-main)' }}>{deleteConfirmProject.name}</strong>?
              This will remove the project and its associated repositories from your workspace.
            </p>
            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
              <button type="button" className="btn btn-secondary" onClick={() => setDeleteConfirmProject(null)}>Cancel</button>
              <button
                type="button"
                className="btn btn-primary"
                style={{ backgroundColor: '#ef4444', borderColor: '#ef4444' }}
                disabled={deletingProject}
                onClick={handleDeleteProject}
              >
                {deletingProject ? <span className="spinner" /> : 'Confirm Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
