import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import SimpleMarkdown from '../components/common/SimpleMarkdown';
import { repositoryApi, aiApi } from '../services/api';
import ProjectSubNav from '../components/common/ProjectSubNav';

export default function ApiDocsPage() {
  const { id: projectId } = useParams();
  const [repositories, setRepositories] = useState([]);
  const [selectedRepo, setSelectedRepo] = useState(null);
  const [docs, setDocs] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [tab, setTab] = useState('preview');

  useEffect(() => {
    repositoryApi.list(projectId).then(res => {
      const repos = (res.data.data || []).filter(r => r.status === 'READY');
      setRepositories(repos);
      if (repos.length > 0) setSelectedRepo(repos[0]);
    }).catch(() => { });
  }, [projectId]);

  const generate = async () => {
    if (!selectedRepo) return;
    setLoading(true); setError('');
    try {
      const res = await aiApi.generateApiDocs({ projectId, repositoryId: selectedRepo.id });
      setDocs(res.data.data?.content || '');
    } catch (err) {
      setError(err.response?.data?.message || 'API docs generation failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <div className="page-title">📋 API Documentation</div>
          <div className="page-subtitle">AI-generated API documentation from source code</div>
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          {repositories.length > 1 && (
            <select className="input" style={{ width: 'auto' }}
              value={selectedRepo?.id || ''} onChange={e => {
                setSelectedRepo(repositories.find(r => r.id === e.target.value));
                setDocs('');
              }}>
              {repositories.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
            </select>
          )}
          <button className="btn btn-primary" onClick={generate} disabled={loading || !selectedRepo}>
            {loading ? <><span className="spinner" /> Generating...</> : '✨ Generate API Docs'}
          </button>
        </div>
      </div>

      <ProjectSubNav activeTab="api-docs" />

      {error && <div className="alert alert-error">{error}</div>}

      {docs ? (
        <div className="card" style={{ padding: '0' }}>
          <div style={{ display: 'flex', gap: '4px', padding: '12px 16px', borderBottom: '1px solid var(--border)' }}>
            <button className={`tab-btn ${tab === 'preview' ? 'active' : ''}`} onClick={() => setTab('preview')}>Preview</button>
            <button className={`tab-btn ${tab === 'source' ? 'active' : ''}`} onClick={() => setTab('source')}>Markdown Source</button>
          </div>
          <div style={{ padding: '20px' }}>
            {tab === 'preview' ? (
              <div className="markdown">
                <SimpleMarkdown>{docs}</SimpleMarkdown>
              </div>
            ) : (
              <pre style={{ fontFamily: 'var(--font-mono)', fontSize: '12px', whiteSpace: 'pre-wrap', color: 'var(--text)' }}>
                {docs}
              </pre>
            )}
          </div>
        </div>
      ) : (
        <div className="empty-state card">
          <h3>Generate API Documentation</h3>
          <p>Click "Generate API Docs" to extract and document APIs from {selectedRepo?.name || 'your repository'}.</p>
        </div>
      )}
    </div>
  );
}
