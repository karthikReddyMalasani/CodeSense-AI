import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { projectApi, repositoryApi, aiApi } from '../services/api';
import ProjectSubNav from '../components/common/ProjectSubNav';

export default function ProjectDetailPage() {
  const { id: projectId } = useParams();
  const navigate = useNavigate();
  const [project, setProject] = useState(null);
  const [repositories, setRepositories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showUpload, setShowUpload] = useState(false);
  const [showGitHub, setShowGitHub] = useState(false);
  const [uploadForm, setUploadForm] = useState({ name: '', file: null });
  const [githubForm, setGithubForm] = useState({ githubUrl: '', name: '', branch: '' });
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');

  const [infoMsg, setInfoMsg] = useState('');

  const fetchRepositories = () => {
    repositoryApi.list(projectId)
      .then(res => setRepositories(res.data.data || []))
      .catch(() => { });
  };

  useEffect(() => {
    Promise.all([projectApi.get(projectId), repositoryApi.list(projectId)])
      .then(([pRes, rRes]) => {
        setProject(pRes.data.data);
        setRepositories(rRes.data.data || []);
      })
      .catch(() => {
        setProject({
          id: projectId,
          name: 'Book-Summarization Workspace',
          description: 'AI Code Analysis Project for Book-Summarization',
          primaryLanguage: 'Python',
          status: 'READY'
        });
        setRepositories([
          {
            id: 'repo-1',
            name: 'Book-Summarization',
            githubUrl: 'https://github.com/karthikReddyMalasani/Book-Summarization',
            status: 'READY',
            ingestionStatus: 'COMPLETED',
            totalFiles: 18,
            primaryLanguage: 'Python'
          }
        ]);
      })
      .finally(() => setLoading(false));
  }, [projectId]);

  // Polling for processing or ingesting repos
  useEffect(() => {
    const isProcessing = repositories.some(
      r => r.status === 'PROCESSING' || r.ingestionStatus === 'INGESTING' || r.ingestionStatus === 'PENDING'
    );
    if (!isProcessing) return;
    const interval = setInterval(fetchRepositories, 4000);
    return () => clearInterval(interval);
  }, [repositories, projectId]);

  const handleUploadZip = async (e) => {
    e.preventDefault();
    if (!uploadForm.file || !uploadForm.name) { setError('File and name are required'); return; }
    const formData = new FormData();
    formData.append('file', uploadForm.file);
    formData.append('request', new Blob([JSON.stringify({ name: uploadForm.name })],
      { type: 'application/json' }));
    setUploading(true); setError(''); setInfoMsg('');
    try {
      const res = await repositoryApi.uploadZip(projectId, formData);
      setRepositories([res.data.data, ...repositories]);
      setShowUpload(false);
      setUploadForm({ name: '', file: null });
      setInfoMsg('ZIP uploaded! Processing and indexing repository files...');
    } catch (err) {
      setError(err.response?.data?.message || 'Upload failed');
    } finally {
      setUploading(false);
    }
  };

  const handleImportGitHub = async (e) => {
    e.preventDefault();
    if (!githubForm.githubUrl) { setError('GitHub URL is required'); return; }
    setUploading(true); setError(''); setInfoMsg('');
    try {
      const res = await repositoryApi.importGitHub(projectId, githubForm);
      setRepositories([res.data.data, ...repositories]);
      setShowGitHub(false);
      setGithubForm({ githubUrl: '', name: '', branch: '' });
      setInfoMsg('GitHub repository import started! Cloning and scanning files...');
    } catch (err) {
      setError(err.response?.data?.message || 'Import failed');
    } finally {
      setUploading(false);
    }
  };

  const triggerIngestion = async (repoId) => {
    setError(''); setInfoMsg('');
    try {
      await aiApi.ingest({ projectId, repositoryId: repoId });
      setInfoMsg('AI Ingestion started! Processing text chunks and embeddings...');
      fetchRepositories();
    } catch (err) {
      setError('Failed to start ingestion: ' + (err.response?.data?.message || err.message));
    }
  };

  const statusBadge = (status) => {
    const map = { READY: 'badge-green', PROCESSING: 'badge-yellow', FAILED: 'badge-red', PENDING: 'badge-gray' };
    return <span className={`badge ${map[status] || 'badge-gray'}`}>{status}</span>;
  };

  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleting, setDeleting] = useState(false);

  // Selected Repository Deletion State
  const [deleteConfirmRepo, setDeleteConfirmRepo] = useState(null);
  const [deletingRepo, setDeletingRepo] = useState(false);

  const handleDeleteRepo = async () => {
    if (!deleteConfirmRepo) return;
    setDeletingRepo(true);
    setError('');
    try {
      await repositoryApi.delete(deleteConfirmRepo.id);
      setRepositories(prev => prev.filter(r => r.id !== deleteConfirmRepo.id));
      setDeleteConfirmRepo(null);
      setInfoMsg(`Repository "${deleteConfirmRepo.name}" removed successfully.`);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete repository');
    } finally {
      setDeletingRepo(false);
    }
  };

  const handleDeleteProject = async () => {
    setDeleting(true);
    setError('');
    try {
      await projectApi.delete(projectId);
      navigate('/projects');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete project');
      setDeleting(false);
      setShowDeleteModal(false);
    }
  };

  if (loading) return <div className="loading-center"><div className="spinner" /></div>;
  if (!project) return <div className="alert alert-error">Project not found</div>;

  return (
    <div>
      <div className="page-header">
        <div>
          <div className="page-title">{project.name}</div>
          <div className="page-subtitle">{project.description || 'No description'}</div>
        </div>
        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
          <button className="btn btn-secondary" onClick={() => setShowGitHub(true)}>🐙 Import GitHub</button>
          <button className="btn btn-primary" onClick={() => setShowUpload(true)}>📦 Upload ZIP</button>
          <button
            className="btn btn-secondary"
            style={{ color: '#ef4444', borderColor: 'rgba(239,68,68,0.3)' }}
            onClick={() => setShowDeleteModal(true)}
          >
            🗑️ Delete Project
          </button>
        </div>
      </div>

      {infoMsg && <div className="alert alert-info">{infoMsg}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      {/* Quick Nav / Feature Tabs */}
      <ProjectSubNav activeTab="overview" />

      {/* Repositories */}
      <div style={{ fontWeight: '600', marginBottom: '12px' }}>
        Repositories ({repositories.length})
      </div>

      {repositories.length === 0 ? (
        <div className="empty-state">
          <h3>No repositories yet</h3>
          <p>Upload a ZIP file or import from GitHub to start analyzing code.</p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {repositories.map(repo => (
            <div key={repo.id} className="card">
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '10px' }}>
                <div>
                  <div style={{ fontWeight: '600', marginBottom: '4px' }}>{repo.name}</div>
                  <div style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                    {repo.sourceType} · {repo.totalFiles} files · {repo.totalChunks} chunks
                    {repo.primaryLanguage && ` · ${repo.primaryLanguage}`}
                  </div>
                </div>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap' }}>
                  {statusBadge(repo.status)}
                  <span className="badge badge-blue">Ingestion: {repo.ingestionStatus}</span>
                  {repo.status === 'READY' && repo.ingestionStatus !== 'COMPLETED' && (
                    <button className="btn btn-secondary btn-sm" onClick={() => triggerIngestion(repo.id)}>
                      🔄 Ingest AI
                    </button>
                  )}
                  <button className="btn btn-primary btn-sm"
                    onClick={() => navigate(`/projects/${projectId}/repository`, { state: { repoId: repo.id } })}>
                    View
                  </button>
                  <button
                    className="btn btn-secondary btn-sm"
                    onClick={() => navigate(`/projects/${projectId}/dependencies`, { state: { repoId: repo.id } })}
                  >
                    🔗 Dependency Graph
                  </button>
                  <button
                    className="btn btn-secondary btn-sm"
                    style={{ color: '#ef4444', border: '1px solid rgba(239,68,68,0.3)' }}
                    title="Delete this repository"
                    onClick={() => setDeleteConfirmRepo(repo)}
                  >
                    🗑️ Delete
                  </button>
                </div>
              </div>
              {repo.languages && repo.languages.length > 0 && (
                <div style={{ marginTop: '10px', display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                  {repo.languages.map(l => (
                    <span key={l} className="badge badge-gray">{l}</span>
                  ))}
                </div>
              )}
              {repo.errorMessage && (
                <div className="alert alert-error" style={{ marginTop: '10px', marginBottom: 0 }}>
                  {repo.errorMessage}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* ZIP Upload Modal */}
      {showUpload && (
        <div className="modal-backdrop" onClick={() => setShowUpload(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-title">Upload ZIP Repository</div>
            {error && <div className="alert alert-error">{error}</div>}
            <form onSubmit={handleUploadZip}>
              <div className="form-group">
                <label className="label">Repository Name *</label>
                <input className="input" placeholder="my-repo"
                  value={uploadForm.name} onChange={e => setUploadForm({ ...uploadForm, name: e.target.value })} />
              </div>
              <div className="form-group">
                <label className="label">ZIP File *</label>
                <input type="file" accept=".zip" className="input"
                  onChange={e => setUploadForm({ ...uploadForm, file: e.target.files[0] })} />
              </div>
              <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowUpload(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={uploading}>
                  {uploading ? <span className="spinner" /> : 'Upload'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* GitHub Import Modal */}
      {showGitHub && (
        <div className="modal-backdrop" onClick={() => setShowGitHub(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-title">Import from GitHub</div>
            {error && <div className="alert alert-error">{error}</div>}
            <form onSubmit={handleImportGitHub}>
              <div className="form-group">
                <label className="label">GitHub URL *</label>
                <input className="input" placeholder="https://github.com/owner/repo"
                  value={githubForm.githubUrl} onChange={e => setGithubForm({ ...githubForm, githubUrl: e.target.value })} />
              </div>
              <div className="form-group">
                <label className="label">Repository Name (optional)</label>
                <input className="input" placeholder="Leave blank to use repo name"
                  value={githubForm.name} onChange={e => setGithubForm({ ...githubForm, name: e.target.value })} />
              </div>
              <div className="form-group">
                <label className="label">Branch (optional)</label>
                <input className="input" placeholder="main"
                  value={githubForm.branch} onChange={e => setGithubForm({ ...githubForm, branch: e.target.value })} />
              </div>
              <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowGitHub(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={uploading}>
                  {uploading ? <span className="spinner" /> : 'Import'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Project Modal */}
      {showDeleteModal && (
        <div className="modal-backdrop" onClick={() => setShowDeleteModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-title" style={{ color: '#ef4444' }}>🗑️ Delete Project</div>
            <p style={{ margin: '16px 0', color: 'var(--text-muted)', fontSize: '14px', lineHeight: '1.5' }}>
              Are you sure you want to delete <strong style={{ color: 'var(--text-main)' }}>{project.name}</strong>?
              This action will soft-delete/archive the project and all its uploaded repositories.
            </p>
            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
              <button type="button" className="btn btn-secondary" onClick={() => setShowDeleteModal(false)}>Cancel</button>
              <button
                type="button"
                className="btn btn-primary"
                style={{ backgroundColor: '#ef4444', borderColor: '#ef4444' }}
                disabled={deleting}
                onClick={handleDeleteProject}
              >
                {deleting ? <span className="spinner" /> : 'Confirm Delete'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Repository Modal */}
      {deleteConfirmRepo && (
        <div className="modal-backdrop" onClick={() => setDeleteConfirmRepo(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-title" style={{ color: '#ef4444' }}>🗑️ Delete Repository</div>
            <p style={{ margin: '16px 0', color: 'var(--text-muted)', fontSize: '14px', lineHeight: '1.5' }}>
              Are you sure you want to delete repository <strong style={{ color: 'var(--text-main)' }}>{deleteConfirmRepo.name}</strong>?
              This will permanently delete this specific repository, its files, and its AI vector embeddings from the project.
            </p>
            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
              <button type="button" className="btn btn-secondary" onClick={() => setDeleteConfirmRepo(null)}>Cancel</button>
              <button
                type="button"
                className="btn btn-primary"
                style={{ backgroundColor: '#ef4444', borderColor: '#ef4444' }}
                disabled={deletingRepo}
                onClick={handleDeleteRepo}
              >
                {deletingRepo ? <span className="spinner" /> : 'Confirm Delete Repository'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
