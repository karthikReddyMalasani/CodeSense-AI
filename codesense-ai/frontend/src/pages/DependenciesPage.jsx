import { useEffect, useState } from 'react';
import { useLocation, useParams } from 'react-router-dom';
import { repositoryApi, parserApi } from '../services/api';
import ProjectSubNav from '../components/common/ProjectSubNav';

export default function DependenciesPage() {
  const { id: projectId } = useParams();
  const location = useLocation();
  const [repositories, setRepositories] = useState([]);
  const [selectedRepo, setSelectedRepo] = useState(null);
  const [graph, setGraph] = useState(null);
  const [mermaid, setMermaid] = useState('');
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('graph');
  const [error, setError] = useState('');

  useEffect(() => {
    repositoryApi.list(projectId)
      .then(res => {
        const repos = res.data.data || [];
        setRepositories(repos);
        const preSelected = location.state?.repoId
          ? repos.find(r => r.id === location.state.repoId)
          : repos[0];
        if (preSelected) setSelectedRepo(preSelected);
      }).catch(() => { });
  }, [projectId, location.state]);

  const loadDependencies = async (repo) => {
    if (!repo) return;
    setLoading(true);
    setError('');
    setGraph(null);
    setMermaid('');
    try {
      const res = await parserApi.getDependencyGraph(repo.id);
      const data = res.data.data || res.data;
      setGraph(data.graph || null);
      const nextMermaid = (typeof data.mermaid === 'string' && data.mermaid.trim())
        ? data.mermaid
        : 'graph LR\n    repo["No dependency data available"]\n';
      setMermaid(nextMermaid);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load dependency graph for this repository.');
      setMermaid('graph LR\n    repo["No dependency data available"]\n');
    }
    setLoading(false);
  };

  useEffect(() => {
    if (selectedRepo) loadDependencies(selectedRepo);
  }, [selectedRepo]);

  return (
    <div>
      <div className="page-header">
        <div>
          <div className="page-title">🔗 Dependency Graph</div>
          <div className="page-subtitle">Code relationships and module dependencies</div>
        </div>
        {repositories.length > 0 && (
          <select className="input" style={{ width: 'auto' }}
            value={selectedRepo?.id || ''}
            onChange={e => setSelectedRepo(repositories.find(r => r.id === e.target.value))}>
            {repositories.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
          </select>
        )}
      </div>

      <ProjectSubNav activeTab="dependencies" />

      <div style={{ display: 'flex', gap: '8px', marginBottom: '16px' }}>
        {['graph', 'mermaid'].map(tab => (
          <button key={tab} className={`btn ${activeTab === tab ? 'btn-primary' : 'btn-secondary'} btn-sm`}
            onClick={() => setActiveTab(tab)}>
            {tab === 'graph' ? '📊 Graph Stats' : '📝 Mermaid Source'}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="loading-center"><div className="spinner" />        </div>
      ) : activeTab === 'graph' ? (
        <div>
          {error && <div className="alert alert-error" style={{ marginBottom: '16px' }}>{error}</div>}
          {graph && (
            <div className="stats-grid" style={{ gridTemplateColumns: 'repeat(3, 1fr)', marginBottom: '16px' }}>
              <div className="card stat-card">
                <div className="stat-value">{graph.nodeCount || 0}</div>
                <div className="stat-label">Nodes</div>
              </div>
              <div className="card stat-card">
                <div className="stat-value">{graph.edgeCount || 0}</div>
                <div className="stat-label">Edges</div>
              </div>
              <div className="card stat-card">
                <div className="stat-value">{graph.topDependencies?.length || 0}</div>
                <div className="stat-label">Top Dependencies</div>
              </div>
            </div>
          )}
          {!graph && !error && (
            <div className="empty-state">
              <h3>No dependency data</h3>
              <p>No relationships were detected for this repository yet.</p>
            </div>
          )}
          {graph?.topDependencies && graph.topDependencies.length > 0 && (
            <div className="card">
              <div style={{ fontWeight: '600', marginBottom: '12px' }}>🔝 Most Referenced Modules</div>
              {graph.topDependencies.map((dep, i) => (
                <div key={i} style={{
                  padding: '6px 0', borderBottom: '1px solid var(--border)',
                  fontFamily: 'var(--font-mono)', fontSize: '12px'
                }}>
                  {i + 1}. {dep}
                </div>
              ))}
            </div>
          )}
          {graph?.edges && (
            <div className="card" style={{ marginTop: '16px' }}>
              <div style={{ fontWeight: '600', marginBottom: '12px' }}>Dependency Edges ({graph.edges.length})</div>
              <div style={{ maxHeight: '400px', overflowY: 'auto' }}>
                {graph.edges.slice(0, 100).map((edge, i) => (
                  <div key={i} style={{
                    display: 'flex', gap: '8px', padding: '4px 0',
                    fontSize: '12px', borderBottom: '1px solid var(--border)', fontFamily: 'var(--font-mono)'
                  }}>
                    <span style={{ color: 'var(--primary-light)' }}>{edge.source}</span>
                    <span style={{ color: 'var(--text-muted)' }}>→ {edge.type} →</span>
                    <span>{edge.target}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      ) : (
        <div className="card">
          <div style={{ fontWeight: '600', marginBottom: '12px' }}>Mermaid Diagram Source</div>
          <pre style={{
            fontFamily: 'var(--font-mono)', fontSize: '12px', padding: '12px',
            background: 'var(--bg)', borderRadius: 'var(--radius)',
            overflowX: 'auto', maxHeight: '500px', whiteSpace: 'pre-wrap'
          }}>
            {mermaid || (error ? 'Unable to generate Mermaid source.' : 'No diagram data available.')}
          </pre>
        </div>
      )}
    </div>
  );
}
