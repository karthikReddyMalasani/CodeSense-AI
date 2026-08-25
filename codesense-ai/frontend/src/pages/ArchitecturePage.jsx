import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { repositoryApi, parserApi, repoApi } from '../services/api';
import { Layers, RefreshCw, AlertCircle } from 'lucide-react';
import ProjectSubNav from '../components/common/ProjectSubNav';

// ── Parse mermaid output from backend into layer groups ──────────────────────
function parseMermaidLayers(mermaid) {
  if (!mermaid) return null;
  const layers = [];
  const subgraphRe = /subgraph\s+(\w+)([\s\S]*?)end/g;
  let match;
  while ((match = subgraphRe.exec(mermaid)) !== null) {
    const name = match[1];
    const body = match[2];
    const nodeRe = /\s+\w+\[([^\]]+)\]/g;
    const components = [];
    let nm;
    while ((nm = nodeRe.exec(body)) !== null) components.push(nm[1]);
    if (components.length > 0) layers.push({ name, components });
  }
  return layers.length > 0 ? layers : null;
}

// ── Infer layers from file list when parser has no classes ───────────────────
function inferLayersFromFiles(files) {
  const layers = {};
  files.forEach(f => {
    const p = (f.filePath || f.path || '').replace(/\\/g, '/');
    const parts = p.split('/');
    const dir = parts.length > 1 ? parts[0] : 'root';
    const ext = p.split('.').pop().toLowerCase();
    layers[dir] = layers[dir] || { components: new Set(), ext: new Set() };
    layers[dir].components.add(parts[parts.length - 1]);
    layers[dir].ext.add(ext);
  });
  return Object.entries(layers).map(([name, { components }]) => ({
    name,
    components: [...components].slice(0, 6)
  }));
}

// ── Layer colour palette ─────────────────────────────────────────────────────
const LAYER_COLORS = ['#38bdf8', '#4ade80', '#a78bfa', '#f43f5e', '#fbbf24', '#fb923c'];

function LayerNode({ label, components, color, isTop, isBottom }) {
  return (
    <div style={{
      border: `2px solid ${color}`,
      borderRadius: '10px',
      background: 'var(--cs-btn-secondary-bg)',
      padding: '14px 20px',
      width: '100%',
      maxWidth: '520px',
      margin: '0 auto',
      boxShadow: `0 0 14px ${color}22`
    }}>
      <div style={{ fontWeight: '800', fontSize: '14px', color, marginBottom: '8px', textAlign: 'center' }}>
        {label}
      </div>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', justifyContent: 'center' }}>
        {components.slice(0, 8).map((c, i) => (
          <span key={i} style={{
            padding: '3px 9px',
            background: `${color}18`,
            border: `1px solid ${color}44`,
            borderRadius: '6px',
            fontSize: '11px',
            fontFamily: 'var(--font-mono)',
            fontWeight: '600',
            color: 'var(--cs-text-main)'
          }}>{c}</span>
        ))}
      </div>
    </div>
  );
}

function ConnectorArrow({ label }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '1px', margin: '4px 0' }}>
      <div style={{ width: '2px', height: '18px', background: 'var(--cs-border-hover)' }} />
      {label && <div style={{ fontSize: '10px', color: 'var(--cs-text-muted)', whiteSpace: 'nowrap' }}>{label}</div>}
      <div style={{ fontSize: '16px', color: 'var(--cs-text-muted)', lineHeight: 1 }}>▼</div>
    </div>
  );
}

function ArchDiagram({ repoName, language, layers }) {
  // Derive a meaningful label for each layer
  const LAYER_LABELS = {
    'Controllers': '🌐 Controller Layer',
    'Services': '⚙️  Service Layer',
    'Repositories': '🗄️  Repository / Data Layer',
    'Models': '📦 Model / Entity Layer',
    'Components': '🧩 Component Layer',
    'Pages': '📄 Page Layer',
    'Utils': '🔧 Utility Layer',
  };

  const getLabel = (name) => LAYER_LABELS[name] || `📁 ${name}`;

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      gap: 0,
      padding: '28px 24px',
      width: '100%'
    }}>
      {/* Entry point node */}
      <div style={{
        border: '2px solid #818cf8',
        borderRadius: '10px',
        background: 'var(--cs-btn-secondary-bg)',
        padding: '10px 28px',
        fontWeight: '800',
        fontSize: '14px',
        color: '#818cf8',
        boxShadow: '0 0 14px #818cf822',
        textAlign: 'center'
      }}>
        👤 Client / User Request
      </div>
      <ConnectorArrow label="HTTP / REST" />

      {layers.map((layer, idx) => (
        <React.Fragment key={idx}>
          <LayerNode
            label={getLabel(layer.name)}
            components={layer.components}
            color={LAYER_COLORS[idx % LAYER_COLORS.length]}
          />
          {idx < layers.length - 1 && <ConnectorArrow label="" />}
        </React.Fragment>
      ))}

      <ConnectorArrow label="" />

      {/* Database node — always last */}
      <div style={{
        border: '2px solid #f43f5e',
        borderRadius: '10px',
        background: 'var(--cs-btn-secondary-bg)',
        padding: '10px 28px',
        fontWeight: '800',
        fontSize: '14px',
        color: '#f43f5e',
        boxShadow: '0 0 14px #f43f5e22',
        textAlign: 'center'
      }}>
        🗄️  Data Store / External Services
      </div>
    </div>
  );
}

// ── Empty state for repos with no parseable structure ────────────────────────
function SimpleFileDiagram({ files, repoName, language }) {
  const byDir = {};
  files.forEach(f => {
    const p = (f.filePath || f.path || '').replace(/\\/g, '/');
    const dir = p.includes('/') ? p.split('/')[0] : '(root)';
    byDir[dir] = byDir[dir] || [];
    byDir[dir].push(p.split('/').pop());
  });

  const dirs = Object.entries(byDir).slice(0, 6);

  if (dirs.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: '40px', color: 'var(--cs-text-muted)' }}>
        <AlertCircle style={{ width: 32, height: 32, marginBottom: 12 }} />
        <div>No parseable file structure found.</div>
      </div>
    );
  }

  return (
    <ArchDiagram
      repoName={repoName}
      language={language}
      layers={dirs.map(([dir, files]) => ({ name: dir, components: files.slice(0, 6) }))}
    />
  );
}

// ── Main page ─────────────────────────────────────────────────────────────────
export default function ArchitecturePage() {
  const { id: projectId } = useParams();
  const [repositories, setRepositories] = useState([]);
  const [selectedRepo, setSelectedRepo] = useState(null);
  const [archData, setArchData] = useState(null);   // { layers, files }
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    repositoryApi.list(projectId).then(res => {
      const repos = res.data.data || [];
      setRepositories(repos);
      if (repos.length > 0) {
        setSelectedRepo(repos[0]);
        loadArchitecture(repos[0]);
      } else {
        setLoading(false);
      }
    }).catch(() => setLoading(false));
  }, [projectId]);

  const loadArchitecture = async (repo) => {
    setGenerating(true);
    setError('');
    try {
      // 1. Try to get parsed architecture (mermaid layers) from the parser API
      const archRes = await parserApi.getArchitectureDiagrams(repo.id);
      const mermaid = archRes.data?.data?.mermaid || '';
      const layers = parseMermaidLayers(mermaid);

      // 2. Get the repo's file list for fallback / extra context
      let files = [];
      try {
        const fRes = await repoApi.getFiles(repo.id);
        files = fRes.data?.data || [];
      } catch { /* ok */ }

      if (layers && layers.length > 0) {
        setArchData({ layers, files, source: 'parsed' });
      } else {
        // Fallback: derive layers from the directory structure of the file list
        const dirLayers = inferLayersFromFiles(files);
        setArchData({ layers: dirLayers.length > 0 ? dirLayers : null, files, source: 'files' });
      }
    } catch {
      // Parser failed entirely — use file list only
      try {
        const fRes = await repoApi.getFiles(repo.id);
        const files = fRes.data?.data || [];
        const dirLayers = inferLayersFromFiles(files);
        setArchData({ layers: dirLayers.length > 0 ? dirLayers : null, files, source: 'files' });
      } catch {
        setError('Could not load repository structure. Make sure the repository is ingested.');
        setArchData(null);
      }
    } finally {
      setGenerating(false);
      setLoading(false);
    }
  };

  if (loading) return <div className="loading-center"><div className="cs-spinner" style={{ width: '24px', height: '24px' }} /></div>;

  return (
    <div style={{ maxWidth: '1100px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: '800', display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Layers style={{ color: 'var(--cs-primary)' }} /> Repository Architecture
          </h1>
          <p style={{ fontSize: '13px', color: 'var(--cs-text-muted)', marginTop: '4px' }}>
            Visual layer diagram generated from the repository's actual code structure
          </p>
        </div>

        {repositories.length > 1 && (
          <select className="input" style={{ width: '220px' }}
            value={selectedRepo?.id || ''}
            onChange={(e) => {
              const repo = repositories.find(r => r.id === e.target.value);
              if (repo) { setSelectedRepo(repo); loadArchitecture(repo); }
            }}
          >
            {repositories.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
          </select>
        )}
      </div>

      <ProjectSubNav activeTab="architecture" />

      {error && <div className="cs-alert cs-alert-danger">⚠️ {error}</div>}

      {selectedRepo && (
        <div className="cs-card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px 24px' }}>
          <div>
            <div style={{ fontSize: '11px', color: 'var(--cs-text-muted)', fontWeight: '600', textTransform: 'uppercase', letterSpacing: '0.06em' }}>Repository</div>
            <div style={{ fontSize: '16px', fontWeight: '700', marginTop: '2px' }}>{selectedRepo.name}</div>
            <div style={{ fontSize: '12px', color: 'var(--cs-text-muted)', marginTop: '4px' }}>
              Language: <strong>{selectedRepo.primaryLanguage || '—'}</strong>
              {' • '}Files: <strong>{selectedRepo.totalFiles || archData?.files?.length || '—'}</strong>
              {' • '}Source: <strong style={{ color: '#4ade80' }}>
                {archData?.source === 'parsed' ? '🔍 Live Parse' : '📁 File Structure'}
              </strong>
            </div>
          </div>
          <button className="cs-btn-gradient" onClick={() => loadArchitecture(selectedRepo)} disabled={generating}>
            <RefreshCw style={{ width: '16px', height: '16px' }} className={generating ? 'cs-spinner' : ''} />
            {generating ? 'Analyzing...' : 'Re-analyze'}
          </button>
        </div>
      )}

      {/* Architecture Diagram */}
      <div className="cs-card" style={{ padding: 0, overflow: 'hidden' }}>
        <div style={{ padding: '16px 24px', borderBottom: '1px solid var(--cs-border-color)' }}>
          <h3 style={{ fontSize: '15px', fontWeight: '700' }}>
            🏛️ {selectedRepo?.name || 'Repository'} — Architecture Diagram
          </h3>
          <p style={{ fontSize: '12px', color: 'var(--cs-text-muted)', marginTop: '4px' }}>
            {archData?.source === 'parsed'
              ? 'Layers detected from class annotations and naming conventions in your code'
              : 'Directory structure derived from repository files'}
          </p>
        </div>

        {generating ? (
          <div className="loading-center" style={{ padding: '48px' }}>
            <div className="cs-spinner" style={{ width: '24px', height: '24px' }} />
            <div style={{ marginTop: '12px', color: 'var(--cs-text-muted)', fontSize: '13px' }}>Parsing repository structure...</div>
          </div>
        ) : archData?.layers ? (
          <ArchDiagram
            repoName={selectedRepo?.name}
            language={selectedRepo?.primaryLanguage}
            layers={archData.layers}
          />
        ) : (
          <SimpleFileDiagram
            files={archData?.files || []}
            repoName={selectedRepo?.name}
            language={selectedRepo?.primaryLanguage}
          />
        )}
      </div>

      {/* Layer component breakdown cards */}
      {archData?.layers && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '16px' }}>
          {archData.layers.map((layer, idx) => (
            <div key={idx} className="cs-card" style={{ borderTop: `3px solid ${LAYER_COLORS[idx % LAYER_COLORS.length]}` }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                <h4 style={{ fontWeight: '700', fontSize: '14px' }}>{layer.name}</h4>
                <span style={{
                  fontSize: '10px', padding: '2px 8px', borderRadius: '12px',
                  background: `${LAYER_COLORS[idx % LAYER_COLORS.length]}22`,
                  color: LAYER_COLORS[idx % LAYER_COLORS.length], fontWeight: '700'
                }}>{layer.components.length} components</span>
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                {layer.components.map((c, i) => (
                  <span key={i} style={{
                    padding: '4px 10px', background: 'var(--cs-btn-secondary-bg)',
                    border: '1px solid var(--cs-border-color)', borderRadius: '6px',
                    fontSize: '11px', fontFamily: 'var(--font-mono)', fontWeight: '600',
                    color: 'var(--cs-text-main)'
                  }}>{c}</span>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
