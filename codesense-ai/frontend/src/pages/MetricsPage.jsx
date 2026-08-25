import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { repositoryApi, parserApi } from '../services/api';
import ProjectSubNav from '../components/common/ProjectSubNav';

export default function MetricsPage() {
  const { id: projectId } = useParams();
  const [repositories, setRepositories] = useState([]);
  const [selectedRepo, setSelectedRepo] = useState(null);
  const [metrics, setMetrics] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    repositoryApi.list(projectId)
      .then(res => {
        const repos = res.data.data || [];
        setRepositories(repos);
        if (repos.length > 0) setSelectedRepo(repos[0]);
      })
      .catch(() => { });
  }, [projectId]);

  const loadMetrics = async (repo) => {
    if (!repo) return;
    setLoading(true);
    setError('');
    setMetrics(null);
    try {
      const res = await parserApi.getMetrics(repo.id);
      setMetrics(res.data.data || res.data);
    } catch (err) {
      // Fallback: show basic metrics from repository data
      setMetrics({
        totalFiles: repo.totalFiles || 0,
        analyzedFiles: repo.totalFiles || 0,
        totalLines: 0,
        codeLines: 0,
        classCount: 0,
        methodCount: 0,
        averageCyclomaticComplexity: 0,
        commentRatio: 0,
        languageBreakdown: {},
        codeSmells: []
      });
    }
    setLoading(false);
  };

  useEffect(() => {
    if (selectedRepo) loadMetrics(selectedRepo);
  }, [selectedRepo]);

  const pct = (val) => (val ? (val * 100).toFixed(1) + '%' : '0%');
  const fmt = (val) => (val ? val.toFixed(2) : '0.00');

  return (
    <div>
      <div className="page-header">
        <div>
          <div className="page-title">📊 Code Metrics</div>
          <div className="page-subtitle">Complexity, size, and quality indicators</div>
        </div>
        {repositories.length > 0 && (
          <select className="input" style={{ width: 'auto' }}
            value={selectedRepo?.id || ''}
            onChange={e => setSelectedRepo(repositories.find(r => r.id === e.target.value))}>
            {repositories.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
          </select>
        )}
      </div>

      <ProjectSubNav activeTab="metrics" />

      {loading ? (
        <div className="loading-center"><div className="spinner" /></div>
      ) : !metrics ? (
        <div className="empty-state">
          <h3>No repository selected</h3>
          <p>Select a repository to view code metrics.</p>
        </div>
      ) : (
        <>
          {/* Overview stats */}
          <div className="stats-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))' }}>
            {[
              { label: 'Total Files', value: metrics.totalFiles || 0 },
              { label: 'Total Lines', value: (metrics.totalLines || 0).toLocaleString() },
              { label: 'Code Lines', value: (metrics.codeLines || 0).toLocaleString() },
              { label: 'Classes', value: metrics.classCount || 0 },
              { label: 'Methods', value: metrics.methodCount || 0 },
              { label: 'Comment Ratio', value: pct(metrics.commentRatio) },
              { label: 'Avg Complexity', value: fmt(metrics.averageCyclomaticComplexity) },
            ].map(({ label, value }) => (
              <div key={label} className="card stat-card">
                <div className="stat-value" style={{ fontSize: '20px' }}>{value}</div>
                <div className="stat-label">{label}</div>
              </div>
            ))}
          </div>

          {/* Language breakdown */}
          {metrics.languageBreakdown && Object.keys(metrics.languageBreakdown).length > 0 && (
            <div className="card" style={{ marginBottom: '16px' }}>
              <div style={{ fontWeight: '600', marginBottom: '12px' }}>Language Breakdown</div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {Object.entries(metrics.languageBreakdown).map(([lang, lm]) => (
                  <div key={lang} style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <span style={{ minWidth: '100px', fontSize: '13px', fontWeight: '500' }}>{lang}</span>
                    <span className="badge badge-blue">{lm.fileCount} files</span>
                    <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>{(lm.totalLines || 0).toLocaleString()} lines</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Code smells */}
          {metrics.codeSmells && metrics.codeSmells.length > 0 && (
            <div className="card">
              <div style={{ fontWeight: '600', marginBottom: '12px' }}>⚠️ Code Quality Observations</div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                {metrics.codeSmells.map((smell, i) => (
                  <div key={i} className="alert alert-warning" style={{ marginBottom: 0, padding: '8px 12px' }}>
                    {smell}
                  </div>
                ))}
              </div>
            </div>
          )}

          {metrics.codeSmells && metrics.codeSmells.length === 0 && (
            <div className="card">
              <div className="alert alert-success" style={{ marginBottom: 0 }}>
                ✅ No code quality issues detected.
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
