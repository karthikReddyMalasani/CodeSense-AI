import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import SimpleMarkdown from '../components/common/SimpleMarkdown';
import { repositoryApi, aiApi } from '../services/api';
import ProjectSubNav from '../components/common/ProjectSubNav';

export default function CodeExplainPage() {
  const { id: projectId } = useParams();
  const [repositories, setRepositories] = useState([]);
  const [selectedRepo, setSelectedRepo] = useState(null);
  const [files, setFiles] = useState([]);
  const [code, setCode] = useState('');
  const [language, setLanguage] = useState('');
  const [filePath, setFilePath] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    repositoryApi.list(projectId).then(res => {
      const repos = (res.data.data || []).filter(r => r.status === 'READY');
      setRepositories(repos);
      if (repos.length > 0) {
        setSelectedRepo(repos[0]);
        loadFiles(repos[0].id);
      }
    }).catch(() => { });
  }, [projectId]);

  const loadFiles = (repoId) => {
    repositoryApi.files(repoId).then(res => setFiles(res.data.data || [])).catch(() => { });
  };

  const loadFileContent = async (fileId) => {
    if (!selectedRepo) return;
    const f = files.find(f => f.id === fileId);
    if (!f) return;
    setFilePath(f.filePath);
    setLanguage(f.language || '');
    if (f.content) { setCode(f.content); return; }
    try {
      const res = await repositoryApi.file(selectedRepo.id, fileId);
      setCode(res.data.data?.content || '');
    } catch { }
  };

  const handleExplain = async () => {
    if (!code.trim()) { setError('Please enter or select code to explain'); return; }
    setLoading(true); setError(''); setResult(null);
    try {
      const res = await aiApi.explainCode({ projectId, repositoryId: selectedRepo?.id, code, language, filePath });
      setResult(res.data.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Explanation failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <div className="page-title">💡 Code Explanation</div>
          <div className="page-subtitle">Understand any code with AI</div>
        </div>
      </div>

      <ProjectSubNav activeTab="code-explanation" />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', alignItems: 'start' }}>
        {/* Input panel */}
        <div>
          <div className="card" style={{ marginBottom: '0' }}>
            {repositories.length > 0 && (
              <div style={{ display: 'flex', gap: '10px', marginBottom: '12px', flexWrap: 'wrap' }}>
                <select className="input" style={{ flex: 1 }}
                  value={selectedRepo?.id || ''} onChange={e => {
                    const r = repositories.find(r => r.id === e.target.value);
                    setSelectedRepo(r); loadFiles(r.id);
                  }}>
                  {repositories.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
                </select>
                <select className="input" style={{ flex: 1 }}
                  onChange={e => loadFileContent(e.target.value)}>
                  <option value="">Select file...</option>
                  {files.filter(f => !f.binary).map(f => (
                    <option key={f.id} value={f.id}>{f.filePath}</option>
                  ))}
                </select>
              </div>
            )}

            <div className="form-group">
              <label className="label">Language</label>
              <input className="input" placeholder="Java, Python, JavaScript..."
                value={language} onChange={e => setLanguage(e.target.value)} />
            </div>

            <div className="form-group">
              <label className="label">Code *</label>
              <textarea className="input" rows={20} placeholder="Paste code here or select a file above..."
                value={code} onChange={e => setCode(e.target.value)}
                style={{ fontFamily: 'var(--font-mono)', fontSize: '12px', resize: 'vertical' }} />
            </div>

            {error && <div className="alert alert-error">{error}</div>}
            <button className="btn btn-primary" onClick={handleExplain} disabled={loading}
              style={{ width: '100%', justifyContent: 'center' }}>
              {loading ? <><span className="spinner" /> Analyzing...</> : '💡 Explain Code'}
            </button>
          </div>
        </div>

        {/* Result panel */}
        <div>
          {result ? (
            <div className="card">
              <div style={{ fontWeight: '600', marginBottom: '16px' }}>AI Explanation</div>
              {result.rawExplanation ? (
                <div className="markdown">
                  <SimpleMarkdown>{result.rawExplanation}</SimpleMarkdown>
                </div>
              ) : (
                <>
                  {result.summary && <div style={{ marginBottom: '12px' }}><strong>Summary:</strong> {result.summary}</div>}
                  {result.purpose && <div style={{ marginBottom: '12px' }}><strong>Purpose:</strong> {result.purpose}</div>}
                  {result.keyComponents?.length > 0 && (
                    <div style={{ marginBottom: '12px' }}>
                      <strong>Key Components:</strong>
                      <ul style={{ marginTop: '6px', paddingLeft: '20px' }}>
                        {result.keyComponents.map((c, i) => <li key={i}>{c}</li>)}
                      </ul>
                    </div>
                  )}
                </>
              )}
            </div>
          ) : (
            <div className="empty-state card">
              <h3>Explanation will appear here</h3>
              <p>Select or paste code and click "Explain Code"</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
