import { useEffect, useMemo, useRef, useState } from 'react';
import { Check, Circle, Crosshair, Filter, GitBranch, Loader2, RefreshCw, Search, X } from 'lucide-react';
import { useLocation, useParams } from 'react-router-dom';
import { repositoryApi, parserApi } from '../services/api';
import ProjectSubNav from '../components/common/ProjectSubNav';

const STAGES = [
  'Reading project structure',
  'Detecting project type',
  'Detecting programming languages',
  'Reading dependency configuration',
  'Scanning source files',
  'Identifying modules/packages',
  'Identifying classes/components',
  'Extracting imports',
  'Extracting exports',
  'Resolving internal dependencies',
  'Resolving external dependencies',
  'Building dependency relationships',
  'Detecting circular dependencies',
  'Detecting highly coupled modules',
  'Calculating dependency metrics',
  'Building dependency graph',
  'Organizing graph hierarchy',
  'Generating dependency analysis',
  'Rendering interactive graph',
  'Finalizing report'
];

const FILTERS = ['ALL', 'INTERNAL', 'EXTERNAL', 'FRONTEND', 'BACKEND', 'DATABASE', 'FRAMEWORK', 'AI'];
const LEVEL_OPTIONS = ['ALL', 'FILE', 'CLASS'];

const EMPTY_METRICS = {
  totalNodes: 0,
  internalNodes: 0,
  externalNodes: 0,
  totalRelationships: 0,
  circularDependencies: 0,
  highCouplingNodes: 0
};

function normalizeGraphResult(result) {
  const payload = result && result.result ? result.result : result;
  const nodesInput = Array.isArray(payload?.nodes) ? payload.nodes : [];
  const edgesInput = Array.isArray(payload?.edges)
    ? payload.edges
    : Array.isArray(payload?.relationships)
      ? payload.relationships
      : [];

  const metricsSource = payload?.metrics || {};

  const nodes = nodesInput
    .filter((node) => node && node.id)
    .map((node) => ({
      id: String(node.id),
      name: node.name || node.label || 'Unnamed node',
      label: node.label || node.name || 'Unnamed node',
      type: String(node.type || 'CLASS').toUpperCase(),
      layer: String(node.layer || node.module || inferNodeLayer(node)).toUpperCase(),
      module: node.module || node.package || '',
      file: node.file || node.path || '',
      path: node.path || node.file || '',
      language: node.language || 'Unknown',
      external: Boolean(node.external)
    }));

  const edges = edgesInput
    .filter((edge) => edge && edge.source && edge.target)
    .map((edge, index) => ({
      id: edge.id || `${edge.source}-${edge.target}-${edge.type || 'DEPENDS_ON'}-${index}`,
      source: String(edge.source),
      target: String(edge.target),
      type: String(edge.type || edge.relationshipType || 'DEPENDS_ON').toUpperCase(),
      confidence: edge.confidence || 'CONFIRMED'
    }));

  const metrics = {
    totalNodes: Number(metricsSource.totalNodes ?? payload?.totalNodes ?? nodes.length ?? 0),
    internalNodes: Number(metricsSource.internalNodes ?? payload?.internalNodes ?? nodes.filter((node) => !node.external).length ?? 0),
    externalNodes: Number(metricsSource.externalNodes ?? payload?.externalNodes ?? nodes.filter((node) => node.external).length ?? 0),
    totalRelationships: Number(metricsSource.totalRelationships ?? metricsSource.relationships ?? payload?.totalRelationships ?? payload?.relationships?.length ?? payload?.edges?.length ?? edges.length ?? 0),
    circularDependencies: Number(metricsSource.circularDependencies ?? payload?.circularDependencies ?? 0),
    highCouplingNodes: Number(metricsSource.highCouplingNodes ?? payload?.highCouplingNodes ?? 0)
  };

  return {
    nodes,
    edges,
    externalDependencies: Array.isArray(payload?.externalDependencies) ? payload.externalDependencies : [],
    metrics
  };
}

function inferNodeLayer(node) {
  const haystack = `${node?.name || ''} ${node?.type || ''} ${node?.file || ''} ${node?.module || ''}`.toLowerCase();

  if (haystack.includes('controller') || haystack.includes('api') || haystack.includes('route')) return 'BACKEND';
  if (haystack.includes('component') || haystack.includes('page') || haystack.includes('jsx') || haystack.includes('tsx')) return 'FRONTEND';
  if (haystack.includes('database') || haystack.includes('postgres') || haystack.includes('redis') || haystack.includes('entity')) return 'DATABASE';
  if (haystack.includes('gemini') || haystack.includes('openai') || haystack.includes('groq') || haystack.includes('ai')) return 'AI';
  if (haystack.includes('react') || haystack.includes('vite') || haystack.includes('framework')) return 'FRAMEWORK';
  return 'BACKEND';
}

function matchesLevel(node, level) {
  if (level === 'ALL') return true;
  if (level === 'FILE') return node.type === 'FILE';
  if (level === 'CLASS') return ['CLASS', 'INTERFACE', 'SERVICE', 'CONTROLLER', 'REPOSITORY', 'ENTITY', 'COMPONENT'].includes(node.type);
  return node.layer === level || node.type === level;
}

function matchesFilter(node, filterValue) {
  if (filterValue === 'ALL') return true;
  if (filterValue === 'INTERNAL') return !node.external;
  if (filterValue === 'EXTERNAL') return node.external;

  const haystack = `${node.label} ${node.type} ${node.layer} ${node.module} ${node.file}`.toLowerCase();

  if (filterValue === 'FRONTEND') return node.layer === 'FRONTEND' || haystack.includes('frontend');
  if (filterValue === 'BACKEND') return node.layer === 'BACKEND' || haystack.includes('backend');
  if (filterValue === 'DATABASE') return node.layer === 'DATABASE' || haystack.includes('database') || haystack.includes('postgres') || haystack.includes('redis');
  if (filterValue === 'FRAMEWORK') return node.layer === 'FRAMEWORK' || haystack.includes('framework') || haystack.includes('react');
  if (filterValue === 'AI') return node.layer === 'AI' || haystack.includes('ai') || haystack.includes('gemini') || haystack.includes('openai');

  return haystack.includes(filterValue.toLowerCase());
}

function StageIcon({ state }) {
  if (state === 'done') return <Check size={13} />;
  if (state === 'active') return <Loader2 size={13} className="dependency-spin" />;
  if (state === 'failed') return <X size={13} />;
  return <Circle size={10} />;
}

function Progress({ job, repo, onRetry }) {
  const progress = job ? Math.round((job.completedStages / job.totalStages) * 100) : 0;
  const running = job?.status === 'RUNNING' || job?.status === 'QUEUED';

  return (
    <section className={`dependency-progress card ${job?.status === 'FAILED' ? 'dependency-failed' : ''}`}>
      <div className="dependency-progress-head">
        <div>
          <span className="eyebrow">{job?.status === 'FAILED' ? 'Analysis stopped' : 'Deep dependency analysis'}</span>
          <h2>{repo?.name || 'Repository'}</h2>
          <p>{job?.status === 'FAILED' ? job.error : running ? 'Resolving actual source relationships. Navigation is locked until this run completes.' : 'Preparing dependency analysis...'}</p>
        </div>
        <strong>{progress}<small>%</small></strong>
      </div>

      {running && (
        <div className="dependency-progress-track">
          <i style={{ width: `${progress}%` }} />
        </div>
      )}

      {job?.status === 'FAILED' && (
        <button className="btn btn-primary" onClick={onRetry}>
          <RefreshCw size={15} /> Retry analysis
        </button>
      )}

      <div className="dependency-stage-list">
        {STAGES.map((stage, index) => {
          const state = index < (job?.completedStages || 0) ? 'done' : index === (job?.completedStages || 0) && running ? 'active' : 'pending';
          return (
            <div className={`dependency-stage ${state}`} key={stage}>
              <span><StageIcon state={state} /></span>
              {stage}
              <small>{state === 'done' ? 'Complete' : state === 'active' ? 'Analyzing' : ''}</small>
            </div>
          );
        })}
      </div>
    </section>
  );
}

function GraphPanel({ result }) {
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState('ALL');
  const [level, setLevel] = useState('ALL');
  const [selectedId, setSelectedId] = useState(null);
  const [showSource, setShowSource] = useState(false);
  const [scale, setScale] = useState(1);

  const graph = useMemo(() => normalizeGraphResult(result), [result]);

  const visibleNodes = useMemo(() => {
    const term = query.trim().toLowerCase();
    return graph.nodes.filter((node) => {
      const haystack = `${node.label} ${node.type} ${node.layer} ${node.module} ${node.file}`.toLowerCase();
      const matchesQuery = !term || haystack.includes(term);
      const matchesLevel = matchesLevel(node, level);
      const matchesType = matchesFilter(node, filter);
      return matchesQuery && matchesLevel && matchesType;
    });
  }, [graph.nodes, query, level, filter]);

  const visibleIds = useMemo(() => new Set(visibleNodes.map((node) => node.id)), [visibleNodes]);

  const visibleEdges = useMemo(
    () => graph.edges.filter((edge) => visibleIds.has(edge.source) && visibleIds.has(edge.target)),
    [graph.edges, visibleIds]
  );

  const selectedNode = useMemo(() => {
    if (!selectedId) return null;
    return graph.nodes.find((node) => node.id === selectedId) || null;
  }, [graph.nodes, selectedId]);

  const selectedIncoming = useMemo(() => {
    if (!selectedNode) return [];
    return graph.edges.filter((edge) => edge.target === selectedNode.id);
  }, [graph.edges, selectedNode]);

  const selectedOutgoing = useMemo(() => {
    if (!selectedNode) return [];
    return graph.edges.filter((edge) => edge.source === selectedNode.id);
  }, [graph.edges, selectedNode]);

  useEffect(() => {
    if (!selectedId && visibleNodes[0]) {
      setSelectedId(visibleNodes[0].id);
    }
  }, [selectedId, visibleNodes]);

  const layout = useMemo(() => {
    const nodes = visibleNodes.slice(0, 250);
    const columns = Math.min(5, Math.max(2, Math.ceil(Math.sqrt(nodes.length || 1))));

    return nodes.reduce((accumulator, node, index) => {
      const row = Math.floor(index / columns);
      const col = index % columns;
      accumulator[node.id] = { x: 20 + col * 170, y: 20 + row * 110 };
      return accumulator;
    }, {});
  }, [visibleNodes]);

  const summaryMetrics = {
    totalNodes: Number(graph.metrics.totalNodes || graph.nodes.length || 0),
    visibleNodes: visibleNodes.length,
    internalNodes: Number(graph.metrics.internalNodes || graph.nodes.filter((node) => !node.external).length),
    externalNodes: Number(graph.metrics.externalNodes || graph.nodes.filter((node) => node.external).length),
    totalRelationships: Number(graph.metrics.totalRelationships || graph.edges.length || 0),
    visibleRelationships: visibleEdges.length,
    circularDependencies: Number(graph.metrics.circularDependencies || 0),
    highCouplingNodes: Number(graph.metrics.highCouplingNodes || 0)
  };

  const sourceData = JSON.stringify({
    nodes: graph.nodes,
    edges: graph.edges,
    externalDependencies: graph.externalDependencies,
    metrics: summaryMetrics
  }, null, 2);

  return (
    <>
      <div className="dependency-toolbar">
        <div className="dependency-search">
          <Search size={15} />
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search node, package, file, type..." />
        </div>

        <select value={level} onChange={(event) => setLevel(event.target.value)}>
          {LEVEL_OPTIONS.map((option) => (
            <option key={option} value={option}>
              {option === 'ALL' ? 'All levels' : option === 'FILE' ? 'Files' : 'Classes / Components'}
            </option>
          ))}
        </select>

        <div className="dependency-filters">
          {FILTERS.map((item) => (
            <button className={filter === item ? 'active' : ''} key={item} onClick={() => setFilter(item)}>
              <Filter size={12} />{item[0] + item.slice(1).toLowerCase()}
            </button>
          ))}
        </div>

        <button className="icon-button" onClick={() => setScale(1)} title="Reset view"><Crosshair size={16} /></button>
        <button className="icon-button" onClick={() => setScale(Math.min(scale + 0.1, 1.8))} title="Zoom in">+</button>
        <button className="icon-button" onClick={() => setScale(Math.max(scale - 0.1, 0.6))} title="Zoom out">−</button>
        <button className="icon-button" onClick={() => setShowSource((value) => !value)} title="View Graph Source">Source</button>
      </div>

      <section className="dependency-summary">
        {[
          ['totalNodes', 'Total nodes', summaryMetrics.totalNodes],
          ['visibleNodes', 'Visible nodes', summaryMetrics.visibleNodes],
          ['internalNodes', 'Internal nodes', summaryMetrics.internalNodes],
          ['externalNodes', 'External nodes', summaryMetrics.externalNodes],
          ['totalRelationships', 'Relationships', summaryMetrics.totalRelationships],
          ['visibleRelationships', 'Visible relationships', summaryMetrics.visibleRelationships]
        ].map(([key, label, value]) => (
          <div className="dependency-stat card" key={key}>
            <strong>{value}</strong>
            <span>{label}</span>
          </div>
        ))}
      </section>

      <div className="dependency-workspace">
        <div className="dependency-canvas">
          <div className="dependency-canvas-inner" style={{ transform: `scale(${scale})` }}>
            {visibleNodes.length === 0 ? (
              <div className="dependency-detail-empty">
                <GitBranch size={24} />
                <strong>No semantic dependencies found.</strong>
              </div>
            ) : (
              <svg width="100%" height="900" viewBox="0 0 1200 900" role="img" aria-label="Dependency graph">
                {visibleEdges.map((edge) => {
                  const start = layout[edge.source];
                  const end = layout[edge.target];
                  if (!start || !end) return null;

                  const x1 = start.x + 70;
                  const x2 = end.x + 70;
                  const y1 = start.y + 35;
                  const y2 = end.y + 35;

                  return (
                    <g key={edge.id}>
                      <path
                        d={`M ${x1} ${y1} C ${x1 + 40} ${y1}, ${x2 - 40} ${y2}, ${x2} ${y2}`}
                        fill="none"
                        stroke={edge.type === 'CALLS' ? '#60a5fa' : edge.type === 'IMPORTS' ? '#a78bfa' : edge.type === 'EXTENDS' ? '#fbbf24' : '#94a3b8'}
                        strokeWidth={2}
                        opacity={0.75}
                      />
                      <text x={(x1 + x2) / 2} y={(y1 + y2) / 2 - 6} fontSize="10" fill="var(--cs-text-muted)" textAnchor="middle">
                        {edge.type}
                      </text>
                    </g>
                  );
                })}

                {visibleNodes.map((node) => {
                  const position = layout[node.id];
                  if (!position) return null;

                  const selected = selectedId === node.id;
                  const x = position.x;
                  const y = position.y;

                  return (
                    <g key={node.id} onClick={() => setSelectedId(node.id)} style={{ cursor: 'pointer' }}>
                      <rect
                        x={x}
                        y={y}
                        width={140}
                        height={70}
                        rx={10}
                        fill={node.external ? '#fff7ed' : '#f8fafc'}
                        stroke={selected ? '#2563eb' : node.external ? '#f97316' : '#cbd5e1'}
                        strokeWidth={selected ? 2 : 1}
                      />
                      <text x={x + 12} y={y + 20} fontSize="11" fontWeight="700" fill="var(--cs-text-main)" dominantBaseline="hanging">
                        {node.label}
                      </text>
                      <text x={x + 12} y={y + 36} fontSize="9" fill="var(--cs-text-muted)" dominantBaseline="hanging">
                        {node.type}
                      </text>
                      <text x={x + 12} y={y + 52} fontSize="8" fill="var(--cs-text-muted)" dominantBaseline="hanging">
                        {node.layer}
                      </text>
                    </g>
                  );
                })}
              </svg>
            )}
          </div>
        </div>

        <aside className="dependency-details">
          {selectedNode ? (
            <>
              <div className="dependency-detail-title">
                <span className="eyebrow">Selected node</span>
                <button onClick={() => setSelectedId(null)}><X size={15} /></button>
                <h3>{selectedNode.label}</h3>
                <p>{selectedNode.type} · {selectedNode.language || 'Unknown language'}</p>
              </div>

              <dl>
                <dt>Layer</dt>
                <dd>{selectedNode.layer || 'Unknown'}</dd>
                <dt>Module</dt>
                <dd>{selectedNode.module || '—'}</dd>
                <dt>File</dt>
                <dd>{selectedNode.file || '—'}</dd>
                <dt>External</dt>
                <dd>{selectedNode.external ? 'Yes' : 'No'}</dd>
                <dt>Incoming dependencies</dt>
                <dd>{selectedIncoming.length}</dd>
                <dt>Outgoing dependencies</dt>
                <dd>{selectedOutgoing.length}</dd>
                <dt>Fan-in</dt>
                <dd>{selectedIncoming.length}</dd>
                <dt>Fan-out</dt>
                <dd>{selectedOutgoing.length}</dd>
                <dt>Coupling</dt>
                <dd>{selectedIncoming.length + selectedOutgoing.length}</dd>
                <dt>Relationship types</dt>
                <dd>{Array.from(new Set(selectedIncoming.concat(selectedOutgoing).map((edge) => edge.type))).join(', ') || 'None'}</dd>
              </dl>
            </>
          ) : (
            <div className="dependency-detail-empty">
              <GitBranch size={24} />
              <strong>Select a node</strong>
              <span>Inspect its location, relationships, and coupling metrics.</span>
            </div>
          )}
        </aside>
      </div>

      {showSource && (
        <div className="dependency-mermaid card">
          <div className="dependency-section-heading">
            <h3>Graph source</h3>
            <span>Developer view</span>
          </div>
          <pre>{sourceData}</pre>
        </div>
      )}
    </>
  );
}

export default function DependenciesPage() {
  const { id: projectId } = useParams();
  const location = useLocation();
  const [repositories, setRepositories] = useState([]);
  const [selectedRepo, setSelectedRepo] = useState(null);
  const [job, setJob] = useState(null);
  const pollRef = useRef(null);
  const [error, setError] = useState('');

  const poll = async (repo, id) => {
    try {
      const response = await parserApi.getDependencyAnalysis(repo.id, id);
      const next = response.data.data || response.data;
      setJob(next);

      if (next.status === 'RUNNING' || next.status === 'QUEUED') {
        pollRef.current = setTimeout(() => poll(repo, id), 900);
      }
    } catch {
      setError('Unable to load dependency graph.');
    }
  };

  const start = async (repo) => {
    if (!repo || job?.status === 'RUNNING' || job?.status === 'QUEUED') return;
    setError('');
    setJob(null);

    try {
      const response = await parserApi.startDependencyAnalysis(repo.id);
      const next = response.data.data || response.data;
      setJob(next);
      if (next?.jobId) {
        poll(repo, next.jobId);
      }
    } catch {
      setError('Dependency graph data is unavailable.');
    }
  };

  useEffect(() => {
    repositoryApi.list(projectId)
      .then((response) => {
        const repos = response.data.data || [];
        setRepositories(repos);
        const selected = location.state?.repoId ? repos.find((repo) => repo.id === location.state.repoId) : repos[0];
        if (selected) {
          setSelectedRepo(selected);
        }
      })
      .catch(() => setError('Unable to load repositories.'));

    return () => clearTimeout(pollRef.current);
  }, [projectId, location.state]);

  useEffect(() => {
    if (selectedRepo) {
      start(selectedRepo);
    }
  }, [selectedRepo]);

  const running = job?.status === 'RUNNING' || job?.status === 'QUEUED';

  return (
    <div className="dependency-page">
      <div className="page-header dependency-header">
        <div>
          <div className="page-title"><GitBranch size={22} /> Dependency intelligence</div>
          <div className="page-subtitle">Actual relationships across source files, components, modules, and libraries</div>
        </div>

        <div className="dependency-controls">
          {repositories.length > 0 && (
            <select
              className="input"
              disabled={running}
              value={selectedRepo?.id || ''}
              onChange={(event) => setSelectedRepo(repositories.find((repo) => repo.id === event.target.value))}
            >
              {repositories.map((repo) => (
                <option key={repo.id} value={repo.id}>{repo.name}</option>
              ))}
            </select>
          )}

          {job?.status === 'COMPLETED' && (
            <button className="btn btn-secondary" onClick={() => start(selectedRepo)}>
              <RefreshCw size={15} /> Re-analyze
            </button>
          )}
        </div>
      </div>

      <div className={running ? 'dependency-locked' : ''}>
        <ProjectSubNav activeTab="dependencies" />
      </div>

      {error && <div className="cs-alert cs-alert-danger">{error}</div>}

      {running || job?.status === 'FAILED' ? (
        <Progress job={job} repo={selectedRepo} onRetry={() => start(selectedRepo)} />
      ) : job?.status === 'COMPLETED' ? (
        <GraphPanel result={job.result} />
      ) : (
        <Progress job={job} repo={selectedRepo} onRetry={() => start(selectedRepo)} />
      )}
    </div>
  );
}
