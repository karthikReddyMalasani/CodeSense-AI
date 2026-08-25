import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { projectApi } from '../services/api';

export default function ProjectsPage() {
  const navigate = useNavigate();
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [form, setForm] = useState({ name: '', description: '' });
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    loadProjects();
  }, []);

  const loadProjects = () => {
    setLoading(true);
    projectApi.list()
      .then(res => setProjects(res.data.data || []))
      .catch(() => { })
      .finally(() => setLoading(false));
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    if (!form.name.trim()) { setError('Project name is required'); return; }
    setCreating(true); setError('');
    try {
      const res = await projectApi.create(form);
      const newProject = res.data.data;
      setProjects([newProject, ...projects]);
      setShowModal(false);
      setForm({ name: '', description: '' });
      navigate(`/projects/${newProject.id}`);
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || 'Failed to create project';
      setError(msg);
    } finally {
      setCreating(false);
    }
  };

  const [deleteConfirmProject, setDeleteConfirmProject] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const handleDelete = async () => {
    if (!deleteConfirmProject) return;
    setDeleting(true);
    setError('');
    try {
      await projectApi.delete(deleteConfirmProject.id);
      setProjects(projects.filter(p => p.id !== deleteConfirmProject.id));
      setDeleteConfirmProject(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete project');
    } finally {
      setDeleting(false);
    }
  };

  if (loading) return <div className="loading-center"><div className="spinner" /></div>;

  return (
    <div>
      <div className="page-header">
        <div>
          <div className="page-title">Projects</div>
          <div className="page-subtitle">{projects.length} projects</div>
        </div>
        <button className="btn btn-primary" onClick={() => setShowModal(true)}>+ New Project</button>
      </div>

      {error && <div className="alert alert-error" style={{ marginBottom: '16px' }}>{error}</div>}

      {projects.length === 0 ? (
        <div className="empty-state">
          <h3>No projects yet</h3>
          <p>Create a project to start analyzing your repositories with AI.</p>
          <button className="btn btn-primary" style={{ marginTop: '16px' }} onClick={() => setShowModal(true)}>
            Create First Project
          </button>
        </div>
      ) : (
        <div className="projects-grid">
          {projects.map(p => (
            <div key={p.id} className="card project-card" style={{ position: 'relative' }} onClick={() => navigate(`/projects/${p.id}`)}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div className="project-card-title">{p.name}</div>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  style={{ color: '#ef4444', border: '1px solid rgba(239,68,68,0.3)', padding: '4px 8px' }}
                  title="Delete project"
                  onClick={(e) => {
                    e.stopPropagation();
                    setDeleteConfirmProject(p);
                  }}
                >
                  🗑️ Delete
                </button>
              </div>
              <div className="project-card-desc">{p.description || 'No description'}</div>
              <div className="project-card-meta">
                <span className="badge badge-blue">{p.repositoryCount || 0} repos</span>
                <span className="badge badge-gray">{p.status}</span>
              </div>
            </div>
          ))}
        </div>
      )}

      {showModal && (
        <div className="modal-backdrop" onClick={() => setShowModal(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-title">Create New Project</div>
            {error && <div className="alert alert-error">{error}</div>}
            <form onSubmit={handleCreate}>
              <div className="form-group">
                <label className="label">Project Name *</label>
                <input className="input" placeholder="My Project"
                  value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
              </div>
              <div className="form-group">
                <label className="label">Description</label>
                <textarea className="input" rows={3} placeholder="Optional description"
                  value={form.description} onChange={e => setForm({ ...form, description: e.target.value })}
                  style={{ resize: 'vertical' }} />
              </div>
              <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={creating}>
                  {creating ? <span className="spinner" /> : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
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
                disabled={deleting}
                onClick={handleDelete}
              >
                {deleting ? <span className="spinner" /> : 'Confirm Delete'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
