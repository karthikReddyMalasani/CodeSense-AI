import { useEffect, useMemo, useState } from 'react';
import { AlertTriangle, BarChart3, CheckCircle2, ChevronDown, ChevronUp, Code2, FileCode2, Gauge, RefreshCw, Search, ShieldCheck } from 'lucide-react';
import { useParams } from 'react-router-dom';
import { repositoryApi, parserApi } from '../services/api';
import ProjectSubNav from '../components/common/ProjectSubNav';

const number = value => Number(value || 0).toLocaleString();
const percent = value => `${((Number(value) || 0) * 100).toFixed(1)}%`;
const toneFor = value => value >= 10 ? 'danger' : value >= 5 ? 'warning' : 'good';

export default function MetricsPage() {
  const { id: projectId } = useParams();
  const [repositories, setRepositories] = useState([]);
  const [selectedRepo, setSelectedRepo] = useState(null);
  const [metrics, setMetrics] = useState(null);
  const [activeMetric, setActiveMetric] = useState('files');
  const [activeLanguage, setActiveLanguage] = useState('ALL');
  const [showAllSmells, setShowAllSmells] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const loadMetrics = async repo => {
    if (!repo) return;
    setLoading(true);
    setError('');
    try {
      const response = await parserApi.getMetrics(repo.id);
      const payload = response?.data?.data ?? response?.data ?? null;
      if (!payload || (typeof payload === 'object' && Object.keys(payload).length === 0)) {
        setMetrics(null);
        setError('No parsed metrics are available for this repository yet.');
        return;
      }
      setMetrics(payload);
    } catch (error) {
      setMetrics(null);
      const message = error?.response?.data?.message || 'Metrics could not be calculated. Please retry.';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    repositoryApi.list(projectId).then(response => {
      const repos = response.data.data || [];
      setRepositories(repos);
      if (repos.length) setSelectedRepo(repos[0]);
    }).catch(() => setError('Unable to load repositories.'));
  }, [projectId]);

  useEffect(() => { if (selectedRepo) loadMetrics(selectedRepo); }, [selectedRepo]);

  const languages = useMemo(() => Object.values(metrics?.languageBreakdown || {}).sort((a, b) => b.totalLines - a.totalLines), [metrics]);
  const totalLines = metrics?.totalLines || 0;
  const commentRatio = metrics?.commentRatio || 0;
  const complexity = metrics?.averageCyclomaticComplexity || 0;
  const smells = metrics?.codeSmells || [];
  const maxLanguageLines = Math.max(...languages.map(item => item.totalLines || 0), 1);
  const qualityScore = Math.max(0, Math.min(100, Math.round(100 - complexity * 4 - smells.length * 3 + commentRatio * 20)));
  const qualityTone = qualityScore >= 80 ? 'good' : qualityScore >= 60 ? 'warning' : 'danger';
  const visibleSmells = showAllSmells ? smells : smells.slice(0, 4);

  const cards = [
    { key: 'files', label: 'Files analyzed', value: number(metrics?.analyzedFiles), meta: `${number(metrics?.totalFiles)} discovered`, icon: FileCode2, tone: 'blue' },
    { key: 'lines', label: 'Total lines', value: number(totalLines), meta: `${percent(totalLines ? (metrics?.codeLines || 0) / totalLines : 0)} code`, icon: BarChart3, tone: 'cyan' },
    { key: 'code', label: 'Code lines', value: number(metrics?.codeLines), meta: `${number(metrics?.commentLines)} comments`, icon: Code2, tone: 'green' },
    { key: 'classes', label: 'Classes', value: number(metrics?.classCount), meta: `${number(metrics?.methodCount)} methods`, icon: ShieldCheck, tone: 'purple' },
    { key: 'methods', label: 'Methods', value: number(metrics?.methodCount), meta: 'Across analyzed files', icon: Gauge, tone: 'orange' },
    { key: 'comments', label: 'Comment ratio', value: percent(commentRatio), meta: commentRatio >= 0.15 ? 'Healthy documentation' : 'Room to document', icon: Search, tone: 'pink' },
    { key: 'complexity', label: 'Avg complexity', value: complexity.toFixed(1), meta: complexity < 5 ? 'Low branching risk' : 'Review hotspots', icon: AlertTriangle, tone: toneFor(complexity) },
  ];

  return <div className="metrics-page">
    <div className="page-header metrics-header">
      <div><div className="page-title"><BarChart3 size={22} /> Code intelligence</div><div className="page-subtitle">Explore structure, maintainability, and language mix</div></div>
      <div className="metrics-actions">{repositories.length > 0 && <select className="input" value={selectedRepo?.id || ''} onChange={event => setSelectedRepo(repositories.find(repo => repo.id === event.target.value))}>{repositories.map(repo => <option key={repo.id} value={repo.id}>{repo.name}</option>)}</select>}<button className="btn btn-secondary metrics-refresh" onClick={() => loadMetrics(selectedRepo)} disabled={loading} title="Refresh metrics"><RefreshCw size={15} className={loading ? 'metrics-spin' : ''} /> Refresh</button></div>
    </div>
    <ProjectSubNav activeTab="metrics" />
    {loading ? <div className="loading-center"><div className="spinner" /></div> : error ? <div className="metrics-empty card"><AlertTriangle size={28} /><h3>{error}</h3><button className="btn btn-primary" onClick={() => loadMetrics(selectedRepo)}>Try again</button></div> : !metrics ? <div className="metrics-empty card"><BarChart3 size={32} /><h3>No repository selected</h3><p>Select a repository to explore its metrics.</p></div> : <>
      <section className="metrics-hero"><div><span className="eyebrow">Repository pulse</span><h2>{selectedRepo?.name || 'Repository'} <span className="metrics-status">LIVE ANALYSIS</span></h2><p>{number(metrics.analyzedFiles)} of {number(metrics.totalFiles)} files parsed successfully.</p></div><div className={`quality-score ${qualityTone}`}><div className="quality-score-value">{qualityScore}</div><div><strong>Health score</strong><span>out of 100</span></div></div></section>
      <div className="metrics-card-grid">{cards.map(({ key, label, value, meta, icon: Icon, tone }) => <button key={key} className={`metric-tile ${activeMetric === key ? 'active' : ''}`} onClick={() => setActiveMetric(key)}><span className={`metric-tile-icon ${tone}`}><Icon size={17} /></span><span className="metric-tile-label">{label}</span><strong>{value}</strong><small>{meta}</small></button>)}</div>
      <div className="metrics-main-grid">
        <section className="card metrics-panel"><div className="panel-heading"><div><span className="eyebrow">Composition</span><h3>Language distribution</h3></div><span className="panel-caption">{languages.length} languages</span></div>{languages.length === 0 ? <div className="panel-placeholder">Language data will appear after files are analyzed.</div> : <div className="language-list"><button className={`language-filter ${activeLanguage === 'ALL' ? 'selected' : ''}`} onClick={() => setActiveLanguage('ALL')}><span>All languages</span><strong>{number(metrics.totalFiles)} files</strong></button>{languages.map(language => { const share = totalLines ? language.totalLines / totalLines * 100 : 0; return <button className={`language-row ${activeLanguage === language.language ? 'selected' : ''}`} key={language.language} onClick={() => setActiveLanguage(language.language)}><div className="language-row-top"><span>{language.language}</span><strong>{share.toFixed(0)}%</strong></div><div className="language-bar"><i style={{ width: `${language.totalLines / maxLanguageLines * 100}%` }} /></div><small>{number(language.fileCount)} files · {number(language.totalLines)} lines</small></button>; })}</div>}</section>
        <section className="card metrics-panel quality-panel"><div className="panel-heading"><div><span className="eyebrow">Maintainability</span><h3>Quality signals</h3></div><span className={`signal-dot ${qualityTone}`} /></div><div className="signal-row"><div><span>Documentation</span><small>{commentRatio >= 0.15 ? 'Good coverage' : 'Needs attention'}</small></div><strong>{percent(commentRatio)}</strong></div><div className="signal-track"><i style={{ width: `${Math.min(commentRatio * 100, 100)}%` }} /></div><div className="signal-row"><div><span>Branching complexity</span><small>{complexity < 5 ? 'Low risk' : complexity < 10 ? 'Moderate risk' : 'High risk'}</small></div><strong>{complexity.toFixed(1)}</strong></div><div className="signal-track orange"><i style={{ width: `${Math.min(complexity * 5, 100)}%` }} /></div><div className="quality-callout">{qualityScore >= 80 ? <CheckCircle2 size={18} /> : <AlertTriangle size={18} />}<span>{qualityScore >= 80 ? 'The repository is in strong shape.' : 'Review complexity and documentation hotspots.'}</span></div></section>
      </div>
      <section className="card observations-panel"><div className="panel-heading"><div><span className="eyebrow">Review queue</span><h3>Code observations <span className="count-badge">{smells.length}</span></h3></div>{smells.length > 4 && <button className="text-button" onClick={() => setShowAllSmells(!showAllSmells)}>{showAllSmells ? 'Show less' : 'View all'} {showAllSmells ? <ChevronUp size={15} /> : <ChevronDown size={15} />}</button>}</div>{smells.length === 0 ? <div className="quality-callout success"><CheckCircle2 size={18} /><span>No observations were raised by the analyzer.</span></div> : <div className="observation-list">{visibleSmells.map((smell, index) => <div className="observation" key={`${smell}-${index}`}><AlertTriangle size={16} /><span>{smell}</span><small>Review</small></div>)}</div>}</section>
    </>}
  </div>;
}
