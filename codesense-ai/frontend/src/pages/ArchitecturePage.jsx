import { useEffect, useRef, useState } from 'react';
import { AlertCircle, Check, Circle, Layers, Loader2, RefreshCw, ShieldCheck, X } from 'lucide-react';
import { useParams } from 'react-router-dom';
import { repositoryApi, parserApi } from '../services/api';
import ProjectSubNav from '../components/common/ProjectSubNav';

const STAGES = ['Reading project structure', 'Scanning source files', 'Identifying application entry points', 'Detecting frameworks and technologies', 'Analyzing dependencies', 'Analyzing frontend architecture', 'Analyzing backend architecture', 'Analyzing API endpoints', 'Tracing frontend to backend communication', 'Analyzing services and business logic', 'Analyzing database/entities/repositories', 'Analyzing authentication and authorization', 'Detecting external APIs and third-party services', 'Analyzing file/data processing', 'Analyzing deployment and infrastructure configuration', 'Building component relationships', 'Understanding end-to-end application workflow', 'Designing system architecture', 'Generating architecture diagram', 'Finalizing architecture report'];

const toList = (value) => Array.isArray(value) ? value.filter(Boolean) : [];
const formatStatus = (value) => String(value || 'UNKNOWN').toUpperCase();

function StageIcon({ state }) {
  if (state === 'done') return <Check size={14} />;
  if (state === 'active') return <Loader2 size={14} className="architecture-spin" />;
  if (state === 'failed') return <X size={14} />;
  return <Circle size={11} />;
}

function MermaidDiagram({ title, diagram, caption }) {
  const [expanded, setExpanded] = useState(false);

  if (!diagram) return null;

  return (
    <div className="diagram-card">
      <div className="diagram-header">
        <div>
          <span className="eyebrow">DIAGRAM</span>
          <h4>{title}</h4>
        </div>
        <button type="button" className="btn btn-secondary btn-small" onClick={() => setExpanded(!expanded)}>
          {expanded ? 'Collapse' : 'Expand'}
        </button>
      </div>
      <div className={`mermaid-shell ${expanded ? 'expanded' : ''}`}>
        <pre className="mermaid-pre">{diagram}</pre>
      </div>
      {caption && <p className="diagram-caption">{caption}</p>}
    </div>
  );
}

function ArchitectureResult({ result }) {
  if (!result) return null;

  const hld = result.hld || {};
  const lld = result.lld || {};
  const databaseDesign = result.databaseDesign || {};
  const insights = result.insights || {};
  const overview = hld.systemOverview || {};
  const components = toList(hld.components);
  const communications = toList(hld.communications);
  const technologies = toList(result.technologies || overview.mainTechnologies || []);
  const packageList = toList(lld.packages);
  const serviceList = toList(lld.services);
  const controllerList = toList(lld.controllers);
  const entityList = toList(lld.entities);
  const sequenceList = toList(lld.sequenceDiagrams);
  const tables = toList(databaseDesign.tables);
  const dbRelationships = toList(databaseDesign.relationships);
  const warnings = toList(insights.warnings);
  const insightItems = toList(insights.insights);

  return (
    <div className="architecture-result">
      <section className="card architecture-overview">
        <div className="section-header">
          <div>
            <span className="eyebrow">ARCHITECTURE OVERVIEW</span>
            <h2>{overview.applicationName || 'System architecture'}</h2>
          </div>
          <span className="status-badge"><ShieldCheck size={14} /> Evidence-backed</span>
        </div>

        <div className="overview-grid">
          <div className="overview-copy">
            <p>{overview.description || result.summary || 'Architecture summary is not available yet.'}</p>
            <div className="chip-list">
              {(overview.majorCapabilities || []).map((capability) => (
                <span key={capability} className="chip">{capability}</span>
              ))}
            </div>
          </div>

          <div className="stats-grid">
            <div className="stat-card">
              <span>Architecture style</span>
              <strong>{hld.architecturalStyle || result.architectureStyle || 'Layered'}</strong>
            </div>
            <div className="stat-card">
              <span>Technology stack</span>
              <strong>{technologies.length || 'Detected'}</strong>
            </div>
            <div className="stat-card">
              <span>Components</span>
              <strong>{components.length || '0'}</strong>
            </div>
            <div className="stat-card">
              <span>Database tables</span>
              <strong>{tables.length || '0'}</strong>
            </div>
          </div>
        </div>
      </section>

      <section className="card architecture-section">
        <div className="section-header">
          <div>
            <span className="eyebrow">HIGH-LEVEL DESIGN</span>
            <h3>System composition</h3>
          </div>
          <span className="soft-pill">{components.length} major components</span>
        </div>

        {hld.architectureDiagram && (
          <MermaidDiagram title="System architecture" diagram={hld.architectureDiagram} caption={hld.architecturalStyle || 'System-wide architecture diagram'} />
        )}

        <div className="two-column-grid">
          <div className="panel-block">
            <h4>Primary components</h4>
            <div className="stack-list">
              {components.length ? components.map((component) => (
                <div className="stack-item" key={`${component.name}-${component.type}`}>
                  <div className="stack-topline">
                    <strong>{component.name}</strong>
                    <span className="confidence-badge confidence-neutral">{formatStatus(component.confidence)}</span>
                  </div>
                  <small>{component.type}</small>
                  <p>{component.description || 'No description available from parser metadata.'}</p>
                  {component.responsibilities?.length ? (
                    <ul>
                      {component.responsibilities.map((item) => <li key={item}>{item}</li>)}
                    </ul>
                  ) : null}
                </div>
              )) : <p className="empty-state">No high-level components were extracted for this repository.</p>}
            </div>
          </div>

          <div className="panel-block">
            <h4>Communication flow</h4>
            <div className="stack-list">
              {communications.length ? communications.map((flow) => (
                <div className="stack-item" key={`${flow.from}-${flow.to}-${flow.protocol}`}>
                  <div className="stack-topline">
                    <strong>{flow.from} → {flow.to}</strong>
                    <span className="confidence-badge confidence-variant">{formatStatus(flow.confidence)}</span>
                  </div>
                  <small>{flow.protocol || 'Unknown protocol'}</small>
                  <p>{flow.description || 'No description available.'}</p>
                </div>
              )) : <p className="empty-state">No cross-component communication was detected.</p>}
            </div>
          </div>
        </div>
      </section>

      <section className="card architecture-section">
        <div className="section-header">
          <div>
            <span className="eyebrow">LOW-LEVEL DESIGN</span>
            <h3>Implementation structure</h3>
          </div>
          <span className="soft-pill">{packageList.length} packages</span>
        </div>

        {lld.classDiagram && (
          <MermaidDiagram title="Class relationships" diagram={lld.classDiagram} caption="Class-level structure inferred from parser relationships" />
        )}

        <div className="three-column-grid">
          <div className="panel-block">
            <h4>Packages</h4>
            <div className="stack-list compact">
              {packageList.length ? packageList.slice(0, 8).map((pkg) => (
                <div className="stack-item compact" key={pkg.packageName}>
                  <strong>{pkg.packageName}</strong>
                  <p>{pkg.purpose || 'Package purpose inferred from naming and folder structure.'}</p>
                </div>
              )) : <p className="empty-state">No package structure detected.</p>}
            </div>
          </div>

          <div className="panel-block">
            <h4>Services</h4>
            <div className="stack-list compact">
              {serviceList.length ? serviceList.slice(0, 8).map((service) => (
                <div className="stack-item compact" key={service.name || service.className}>
                  <strong>{service.name || service.className}</strong>
                  <p>{service.purpose || 'Business service layer component.'}</p>
                </div>
              )) : <p className="empty-state">No service layer objects were detected.</p>}
            </div>
          </div>

          <div className="panel-block">
            <h4>Controllers</h4>
            <div className="stack-list compact">
              {controllerList.length ? controllerList.slice(0, 8).map((controller) => (
                <div className="stack-item compact" key={controller.name || controller.className}>
                  <strong>{controller.name || controller.className}</strong>
                  <p>{(controller.endpoints || []).slice(0, 3).map((endpoint) => `${endpoint.method} ${endpoint.path}`).join(' • ') || 'No endpoints captured.'}</p>
                </div>
              )) : <p className="empty-state">No controller definitions were detected.</p>}
            </div>
          </div>
        </div>

        {sequenceList.length ? (
          <div className="sequence-grid">
            {sequenceList.slice(0, 2).map((sequence) => (
              <MermaidDiagram key={sequence.name || sequence.description} title={sequence.name || 'Sequence flow'} diagram={sequence.mermaidDiagram} caption={sequence.description || 'Workflow sequence'} />
            ))}
          </div>
        ) : null}
      </section>

      <section className="card architecture-section">
        <div className="section-header">
          <div>
            <span className="eyebrow">DATABASE DESIGN</span>
            <h3>Data model</h3>
          </div>
          <span className="soft-pill">{databaseDesign.databaseType || 'Database'} detected</span>
        </div>

        {databaseDesign.erDiagram && (
          <MermaidDiagram title="Entity relationship diagram" diagram={databaseDesign.erDiagram} caption="ER structure inferred from entities and relationship metadata" />
        )}

        <div className="table-grid">
          {tables.length ? tables.map((table) => (
            <div className="table-card" key={`${table.entityName}-${table.tableName}`}>
              <div className="table-card-head">
                <strong>{table.tableName || table.entityName}</strong>
                <span>{table.entityName}</span>
              </div>
              <ul>
                {(table.columns || []).map((column) => (
                  <li key={`${table.tableName}-${column.fieldName}`}>
                    <span>{column.fieldName}</span>
                    <small>{column.type || 'unknown'}</small>
                  </li>
                ))}
              </ul>
            </div>
          )) : <p className="empty-state">No persistence entities were detected in the repository.</p>}
        </div>

        {dbRelationships.length ? (
          <div className="relationship-list">
            {dbRelationships.map((relationship, index) => (
              <div className="relationship-row" key={`${relationship.fromTable}-${relationship.toTable}-${index}`}>
                <strong>{relationship.fromTable}</strong>
                <span>{relationship.type || 'RELATIONSHIP'}</span>
                <strong>{relationship.toTable}</strong>
              </div>
            ))}
          </div>
        ) : null}
      </section>

      <section className="card architecture-section">
        <div className="section-header">
          <div>
            <span className="eyebrow">ARCHITECTURE INSIGHTS</span>
            <h3>Observations and warnings</h3>
          </div>
          <span className="soft-pill">{warningCount(warnings)} warnings</span>
        </div>

        <div className="insight-grid">
          <div className="panel-block">
            <h4>Insights</h4>
            <div className="insight-list">
              {insightItems.length ? insightItems.map((entry) => (
                <div className="insight-item" key={`${entry.category}-${entry.description}`}>
                  <div className="insight-head">
                    <strong>{entry.category || 'Observation'}</strong>
                    <span className={`severity-tag severity-${(entry.severity || 'INFO').toLowerCase()}`}>
                      {formatStatus(entry.severity)}
                    </span>
                  </div>
                  <p>{entry.description}</p>
                  <small>{entry.evidence || 'Observed from repository evidence.'}</small>
                </div>
              )) : <p className="empty-state">No architectural insights were generated.</p>}
            </div>
          </div>

          <div className="panel-block">
            <h4>Warnings</h4>
            <div className="insight-list">
              {warnings.length ? warnings.map((warning) => (
                <div className="warning-item" key={`${warning.type}-${warning.description}`}>
                  <div className="insight-head">
                    <strong>{warning.type || 'WARNING'}</strong>
                    <span className={`severity-tag severity-${(warning.severity || 'MEDIUM').toLowerCase()}`}>
                      {formatStatus(warning.severity)}
                    </span>
                  </div>
                  <p>{warning.description}</p>
                  <small>{warning.recommendation || warning.evidence || 'Review recommended.'}</small>
                </div>
              )) : <p className="empty-state">No warnings were identified.</p>}
            </div>
          </div>
        </div>
      </section>

      <section className="card architecture-section">
        <div className="section-header">
          <div>
            <span className="eyebrow">TECHNOLOGY STACK</span>
            <h3>Detected tooling</h3>
          </div>
        </div>
        <div className="chip-list large">
          {(technologies.length ? technologies : overview.mainTechnologies || []).map((technology) => (
            <span key={technology} className="chip strong-chip">{technology}</span>
          ))}
        </div>
      </section>
    </div>
  );
}

function warningCount(list) {
  return Array.isArray(list) ? list.length : 0;
}

export default function ArchitecturePage() {
  const { id: projectId } = useParams();
  const [repositories, setRepositories] = useState([]);
  const [selectedRepo, setSelectedRepo] = useState(null);
  const [job, setJob] = useState(null);
  const [error, setError] = useState('');
  const pollRef = useRef(null);

  const poll = async (repo, jobId) => {
    try {
      const response = await parserApi.getArchitectureAnalysis(repo.id, jobId);
      const next = response.data.data || response.data;
      setJob(next);
      if (next.status === 'QUEUED' || next.status === 'RUNNING') {
        pollRef.current = setTimeout(() => poll(repo, jobId), 900);
      }
    } catch {
      setError('Architecture analysis could not be read. Retry the analysis.');
    }
  };

  const startAnalysis = async (repo) => {
    if (!repo || job?.status === 'RUNNING' || job?.status === 'QUEUED') return;
    setError('');
    setJob(null);
    try {
      const response = await parserApi.startArchitectureAnalysis(repo.id);
      const started = response.data.data || response.data;
      setJob(started);
      poll(repo, started.jobId);
    } catch {
      setError('Architecture analysis failed to start.');
    }
  };

  useEffect(() => {
    repositoryApi.list(projectId)
      .then((response) => {
        const repos = response.data.data || [];
        setRepositories(repos);
        if (repos.length) setSelectedRepo(repos[0]);
      })
      .catch(() => setError('Unable to load repositories.'));

    return () => clearTimeout(pollRef.current);
  }, [projectId]);

  useEffect(() => {
    if (selectedRepo) startAnalysis(selectedRepo);
  }, [selectedRepo]);

  const running = job?.status === 'QUEUED' || job?.status === 'RUNNING';
  const progress = job ? Math.round((job.completedStages / job.totalStages) * 100) : 0;

  return (
    <div className="architecture-page">
      <div className="page-header architecture-header">
        <div>
          <div className="page-title"><Layers size={22} /> System architecture</div>
          <div className="page-subtitle">A source-derived map of components, flows, APIs, and infrastructure</div>
        </div>
        <div className="architecture-controls">
          {repositories.length > 0 && (
            <select className="input" value={selectedRepo?.id || ''} disabled={running} onChange={(event) => setSelectedRepo(repositories.find((repo) => repo.id === event.target.value))}>
              {repositories.map((repo) => <option key={repo.id} value={repo.id}>{repo.name}</option>)}
            </select>
          )}
          {job?.status === 'FAILED' && (
            <button className="btn btn-secondary" onClick={() => startAnalysis(selectedRepo)}>
              <RefreshCw size={15} /> Retry analysis
            </button>
          )}
        </div>
      </div>

      <div className={running ? 'architecture-locked' : ''}><ProjectSubNav activeTab="architecture" /></div>

      {error && <div className="cs-alert cs-alert-danger">{error}</div>}

      {running ? (
        <section className="architecture-progress card">
          <div className="progress-heading">
            <div>
              <span className="eyebrow">DEEP ANALYSIS IN PROGRESS</span>
              <h2>{selectedRepo?.name}</h2>
              <p>Reading evidence from the uploaded project. Navigation is locked until this run completes.</p>
            </div>
            <div className="progress-number">{progress}<small>%</small></div>
          </div>
          <div className="architecture-progress-track"><i style={{ width: `${progress}%` }} /></div>
          <div className="stage-list">
            {STAGES.map((stage, index) => {
              const state = index < (job?.completedStages || 0) ? 'done' : index === (job?.completedStages || 0) ? 'active' : 'pending';
              return (
                <div className={`stage-item ${state}`} key={stage}>
                  <span className="stage-icon"><StageIcon state={state} /></span>
                  <span>{stage}</span>
                  {state !== 'pending' && <small>{state === 'done' ? 'Complete' : 'Analyzing'}</small>}
                </div>
              );
            })}
          </div>
        </section>
      ) : job?.status === 'FAILED' ? (
        <section className="architecture-progress card failed-progress">
          <AlertCircle size={30} />
          <h2>Analysis failed</h2>
          <p>{job.error || 'The backend could not complete this analysis.'}</p>
          <button className="btn btn-primary" onClick={() => startAnalysis(selectedRepo)}>Retry analysis</button>
        </section>
      ) : job?.status === 'COMPLETED' ? (
        <ArchitectureResult result={job.result} />
      ) : (
        <div className="architecture-progress card">
          <Loader2 className="architecture-spin" />
          <p>Preparing analysis...</p>
        </div>
      )}
    </div>
  );
}

