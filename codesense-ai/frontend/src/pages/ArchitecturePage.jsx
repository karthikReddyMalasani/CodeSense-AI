import { useEffect, useRef, useState } from 'react';
import { AlertCircle, Check, ChevronDown, ChevronUp, Circle, Layers, Loader2, RefreshCw, ShieldCheck, X } from 'lucide-react';
import { useParams } from 'react-router-dom';
import { repositoryApi, parserApi } from '../services/api';
import ProjectSubNav from '../components/common/ProjectSubNav';

const STAGES = ['Reading project structure', 'Scanning source files', 'Identifying application entry points', 'Detecting frameworks and technologies', 'Analyzing dependencies', 'Analyzing frontend architecture', 'Analyzing backend architecture', 'Analyzing API endpoints', 'Tracing frontend to backend communication', 'Analyzing services and business logic', 'Analyzing database/entities/repositories', 'Analyzing authentication and authorization', 'Detecting external APIs and third-party services', 'Analyzing file/data processing', 'Analyzing deployment and infrastructure configuration', 'Building component relationships', 'Understanding end-to-end application workflow', 'Designing system architecture', 'Generating architecture diagram', 'Finalizing architecture report'];

function StageIcon({ state }) {
  if (state === 'done') return <Check size={14} />;
  if (state === 'active') return <Loader2 size={14} className="architecture-spin" />;
  if (state === 'failed') return <X size={14} />;
  return <Circle size={11} />;
}

function SystemMapGraph({ result }) {
  const flows = result?.flows || [];
  const components = result?.components || [];
  const nodes = components.length ? components.map((component, index) => ({
    id: component.name,
    label: component.name,
    x: 70 + (index % 2) * 220,
    y: 55 + Math.floor(index / 2) * 110,
    type: component.name
  })) : flows.reduce((acc, flow) => {
    if (!acc.some(node => node.id === flow.from)) acc.push({ id: flow.from, label: flow.from, x: 80, y: 70 + acc.length * 40, type: flow.from });
    if (!acc.some(node => node.id === flow.to)) acc.push({ id: flow.to, label: flow.to, x: 360, y: 70 + acc.length * 40, type: flow.to });
    return acc;
  }, []);

  if (!nodes.length) {
    return <div className="architecture-graph-empty">No component relationships were detected for this repository.</div>;
  }

  const lookup = new Map(nodes.map(node => [node.id, node]));

  return <div className="architecture-graph-wrap">
    <svg className="architecture-graph" viewBox="0 0 520 260" preserveAspectRatio="xMidYMid meet" aria-label="Component relationships graph">
      {flows.map((flow, index) => {
        const from = lookup.get(flow.from);
        const to = lookup.get(flow.to);
        if (!from || !to) return null;
        return <g key={`${flow.from}-${flow.to}-${index}`}>
          <line x1={from.x + 55} y1={from.y + 28} x2={to.x + 55} y2={to.y + 28} className="architecture-edge" />
          <circle cx={(from.x + to.x) / 2 + 55} cy={(from.y + to.y) / 2 + 28} r="4" className="architecture-edge-dot" />
        </g>;
      })}
      {nodes.map(node => <g key={node.id} transform={`translate(${node.x}, ${node.y})`}>
        <rect width="110" height="56" rx="12" className="architecture-node-rect" />
        <text x="55" y="25" textAnchor="middle" className="architecture-node-label">{node.label}</text>
      </g>)}
    </svg>
  </div>;
}

function ArchitectureResult({ result }) {
  const [expanded, setExpanded] = useState(null);
  if (!result) return null;
  return <div className="architecture-result">
    <section className="architecture-summary card"><div><span className="eyebrow">Evidence-backed assessment</span><h2>{result.architectureStyle}</h2><p>{result.summary}</p></div><ShieldCheck size={28} /></section>
    <div className="architecture-grid">
      <section className="card architecture-diagram"><div className="architecture-section-title"><div><span className="eyebrow">System map</span><h3>Component relationships</h3></div><span className="evidence-pill">Derived from source</span></div>{result.mermaid ? <SystemMapGraph result={result} /> : <div className="architecture-graph-empty">No relationship graph was generated.</div>} {result.mermaid && <pre className="architecture-mermaid-source">{result.mermaid}</pre>}</section>
      <section className="card"><div className="architecture-section-title"><div><span className="eyebrow">Detected stack</span><h3>Technologies</h3></div></div><div className="technology-cloud">{(result.technologies || []).map(technology => <span key={technology}>{technology}</span>)}</div><div className="flow-list">{(result.flows || []).map(flow => <div className="architecture-flow" key={`${flow.from}-${flow.to}`}><strong>{flow.from}</strong><span>→</span><strong>{flow.to}</strong><small>{flow.evidence}</small></div>)}</div></section>
    </div>
    <section className="card architecture-components"><div className="architecture-section-title"><div><span className="eyebrow">Source inventory</span><h3>Major components</h3></div><span className="panel-caption">{result.components?.length || 0} detected</span></div>{(result.components || []).map((component, index) => <div className="component-row" key={component.name}><button onClick={() => setExpanded(expanded === index ? null : index)}><span className="component-mark">{component.name.slice(0, 1)}</span><span><strong>{component.name}</strong><small>{component.purpose} · {component.technology}</small></span>{expanded === index ? <ChevronUp size={16} /> : <ChevronDown size={16} />}</button>{expanded === index && <div className="component-detail"><div>{component.files?.map(file => <code key={file}>{file}</code>)}</div>{component.evidence?.map(item => <p key={`${item.filePath}-${item.symbol}`}><b>{item.confidence}</b> {item.reason} <span>{item.filePath}{item.line ? `:${item.line}` : ''}</span></p>)}</div>}</div>)}</section>
    <section className="card"><div className="architecture-section-title"><div><span className="eyebrow">API inventory</span><h3>Discovered endpoints</h3></div><span className="panel-caption">{result.apis?.length || 0} detected</span></div>{result.apis?.length ? <div className="api-inventory">{result.apis.map((api, index) => <div key={`${api.sourceFile}-${index}`}><b>{api.method}</b><span>{api.endpoint}</span><small>{api.controller} · {api.sourceFile}</small></div>)}</div> : <p className="panel-placeholder">No mapping annotations were found in the parsed source.</p>}</section>
  </div>;
}

export default function ArchitecturePage() {
  const { id: projectId } = useParams();
  const [repositories, setRepositories] = useState([]);
  const [selectedRepo, setSelectedRepo] = useState(null);
  const [job, setJob] = useState(null);
  const [error, setError] = useState('');
  const pollRef = useRef(null);
  const poll = async (repo, jobId) => { try { const response = await parserApi.getArchitectureAnalysis(repo.id, jobId); const next = response.data.data || response.data; setJob(next); if (next.status === 'QUEUED' || next.status === 'RUNNING') pollRef.current = setTimeout(() => poll(repo, jobId), 900); } catch { setError('Architecture analysis could not be read. Retry the analysis.'); } };
  const startAnalysis = async repo => { if (!repo || job?.status === 'RUNNING' || job?.status === 'QUEUED') return; setError(''); setJob(null); try { const response = await parserApi.startArchitectureAnalysis(repo.id); const started = response.data.data || response.data; setJob(started); poll(repo, started.jobId); } catch { setError('Architecture analysis failed to start.'); } };
  useEffect(() => { repositoryApi.list(projectId).then(response => { const repos = response.data.data || []; setRepositories(repos); if (repos.length) setSelectedRepo(repos[0]); }).catch(() => setError('Unable to load repositories.')); return () => clearTimeout(pollRef.current); }, [projectId]);
  useEffect(() => { if (selectedRepo) startAnalysis(selectedRepo); }, [selectedRepo]);
  const running = job?.status === 'QUEUED' || job?.status === 'RUNNING';
  const progress = job ? Math.round((job.completedStages / job.totalStages) * 100) : 0;
  return <div className="architecture-page">
    <div className="page-header architecture-header"><div><div className="page-title"><Layers size={22} /> System architecture</div><div className="page-subtitle">A source-derived map of components, flows, APIs, and infrastructure</div></div><div className="architecture-controls">{repositories.length > 0 && <select className="input" value={selectedRepo?.id || ''} disabled={running} onChange={event => setSelectedRepo(repositories.find(repo => repo.id === event.target.value))}>{repositories.map(repo => <option key={repo.id} value={repo.id}>{repo.name}</option>)}</select>}{job?.status === 'FAILED' && <button className="btn btn-secondary" onClick={() => startAnalysis(selectedRepo)}><RefreshCw size={15} /> Retry analysis</button>}</div></div>
    <div className={running ? 'architecture-locked' : ''}><ProjectSubNav activeTab="architecture" /></div>
    {error && <div className="cs-alert cs-alert-danger">{error}</div>}
    {running ? <section className="architecture-progress card"><div className="progress-heading"><div><span className="eyebrow">Deep analysis in progress</span><h2>{selectedRepo?.name}</h2><p>Reading evidence from the uploaded project. Navigation is locked until this run completes.</p></div><div className="progress-number">{progress}<small>%</small></div></div><div className="architecture-progress-track"><i style={{ width: `${progress}%` }} /></div><div className="stage-list">{STAGES.map((stage, index) => { const state = index < (job?.completedStages || 0) ? 'done' : index === (job?.completedStages || 0) ? 'active' : 'pending'; return <div className={`stage-item ${state}`} key={stage}><span className="stage-icon"><StageIcon state={state} /></span><span>{stage}</span>{state !== 'pending' && <small>{state === 'done' ? 'Complete' : 'Analyzing'}</small>}</div>; })}</div></section> : job?.status === 'FAILED' ? <section className="architecture-progress card failed-progress"><AlertCircle size={30} /><h2>Analysis failed</h2><p>{job.error || 'The backend could not complete this analysis.'}</p><button className="btn btn-primary" onClick={() => startAnalysis(selectedRepo)}>Retry analysis</button></section> : job?.status === 'COMPLETED' ? <ArchitectureResult result={job.result} /> : <div className="architecture-progress card"><Loader2 className="architecture-spin" /><p>Preparing analysis...</p></div>}
  </div>;
}
