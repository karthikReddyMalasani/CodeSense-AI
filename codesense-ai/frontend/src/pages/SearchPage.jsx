import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { repositoryApi, aiApi } from '../services/api';
import ProjectSubNav from '../components/common/ProjectSubNav';

export default function SearchPage() {
  const { id: projectId } = useParams();
  const [repositories, setRepositories] = useState([]);
  const [selectedRepo, setSelectedRepo] = useState(null);
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    repositoryApi.list(projectId).then(res => {
      const repos = (res.data.data || []).filter(r => r.status === 'READY');
      setRepositories(repos);
      if (repos.length > 0) setSelectedRepo(repos[0]);
    }).catch(() => { });
  }, [projectId]);

  const handleSearch = async (e) => {
    e?.preventDefault();
    if (!query.trim() || !selectedRepo) return;

    if (selectedRepo.status !== 'READY' || selectedRepo.ingestionStatus !== 'COMPLETED') {
      setSearched(true);
      setResults([]);
      setError('Repository indexing is still in progress.');
      return;
    }

    setLoading(true);
    setSearched(true);
    setError('');
    try {
      const res = await aiApi.search({ projectId, repositoryId: selectedRepo.id, query });
      const payload = res.data?.data || res.data || {};
      setResults(payload.results || []);
      if (!payload.results || payload.results.length === 0) {
        setError('No relevant results found for this query.');
      }
    } catch (err) {
      setResults([]);
      setError(err?.response?.data?.message || 'Unable to perform semantic search. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <div className="page-title">🔍 Semantic Search</div>
          <div className="page-subtitle">Search your codebase using natural language</div>
        </div>
        {repositories.length > 1 && (
          <select className="input" style={{ width: 'auto' }}
            value={selectedRepo?.id || ''} onChange={e => {
              const r = repositories.find(r => r.id === e.target.value);
              setSelectedRepo(r); setResults([]); setSearched(false);
            }}>
            {repositories.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
          </select>
        )}
      </div>

      <ProjectSubNav activeTab="search" />

      {selectedRepo && selectedRepo.status !== 'READY' || selectedRepo && selectedRepo.ingestionStatus !== 'COMPLETED' ? (
        <div className="alert alert-info" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '12px', flexWrap: 'wrap', marginBottom: '20px' }}>
          <span>
            Semantic search is waiting for repository ingestion to finish. Current status: {selectedRepo?.ingestionStatus || 'PENDING'}.
          </span>
          <button
            className="btn btn-secondary btn-sm"
            onClick={async () => {
              try {
                await aiApi.ingest({ projectId, repositoryId: selectedRepo.id });
              } catch (err) {
                // keep user on the page without breaking the search flow
              }
            }}
          >
            🔄 Ingest AI
          </button>
        </div>
      ) : null}

      <form onSubmit={handleSearch} style={{ marginBottom: '24px', display: 'flex', gap: '10px' }}>
        <input className="input" placeholder="e.g. JWT authentication, database connection, error handling..."
          value={query} onChange={e => setQuery(e.target.value)} style={{ flex: 1 }} />
        <button type="submit" className="btn btn-primary" disabled={loading || !query.trim() || !selectedRepo || selectedRepo.ingestionStatus !== 'COMPLETED'}>
          {loading ? <span className="spinner" /> : 'Search'}
        </button>
      </form>

      {/* Suggestions */}
      {!searched && (
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: '20px' }}>
          {['authentication', 'database connection', 'error handling', 'API endpoints', 'configuration'].map(s => (
            <button key={s} className="btn btn-secondary btn-sm"
              onClick={() => { setQuery(s); }}>
              {s}
            </button>
          ))}
        </div>
      )}

      {/* Results */}
      {results.length > 0 ? (
        <div>
          <div style={{ fontSize: '13px', color: 'var(--text-muted)', marginBottom: '12px' }}>
            {results.length} results for "{query}"
          </div>
          {results.map((r, i) => (
            <div key={i} className="search-result">
              <div className="search-result-header">
                <span className="search-result-path">{r.filePath}</span>
                {r.language && <span className="badge badge-blue">{r.language}</span>}
                {r.symbolName && <span className="badge badge-gray">{r.symbolType}: {r.symbolName}</span>}
                {r.startLine && (
                  <span style={{ fontSize: '11px', color: 'var(--text-muted)', marginLeft: 'auto' }}>
                    Lines {r.startLine}–{r.endLine}
                  </span>
                )}
              </div>
              <div className="search-result-code">{r.content}</div>
            </div>
          ))}
        </div>
      ) : searched && !loading ? (
        <div className="empty-state">
          <h3>{error ? 'Semantic search unavailable' : 'No relevant results found'}</h3>
          <p>
            {error || 'No relevant results found for this query. Try a more specific query or re-run repository ingestion.'}
          </p>
        </div>
      ) : null}
    </div>
  );
}
