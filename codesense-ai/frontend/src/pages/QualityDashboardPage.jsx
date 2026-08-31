import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { repositoryApi, aiApi } from '../services/api';
import ProjectSubNav from '../components/common/ProjectSubNav';

const SEVERITIES = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
const TYPES = ['BUG', 'SECURITY', 'CODE_SMELL', 'PERFORMANCE', 'MAINTAINABILITY'];

const TYPE_META = {
    BUG: { label: 'Bug', color: 'var(--destructive)', bg: 'var(--destructive-surface)', icon: '🐛' },
    SECURITY: { label: 'Security', color: 'var(--warning)', bg: 'var(--warning-surface)', icon: '🔒' },
    CODE_SMELL: { label: 'Code Smell', color: 'var(--warning)', bg: 'var(--warning-surface)', icon: '⚠️' },
    PERFORMANCE: { label: 'Performance', color: 'var(--primary)', bg: 'var(--accent)', icon: '⚡' },
    MAINTAINABILITY: { label: 'Maintain.', color: 'var(--info)', bg: 'var(--info-surface)', icon: '🔧' },
};

const SEV_META = {
    CRITICAL: { color: 'var(--destructive)', text: 'Critical' },
    HIGH: { color: 'var(--warning)', text: 'High' },
    MEDIUM: { color: 'var(--warning)', text: 'Medium' },
    LOW: { color: 'var(--success)', text: 'Low' },
};

function GradeRing({ score, grade }) {
    const color = score >= 80 ? 'var(--success)' : score >= 60 ? 'var(--warning)' : 'var(--destructive)';
    const r = 48, circ = 2 * Math.PI * r;
    const dash = (score / 100) * circ;
    return (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
            <svg width="128" height="128" viewBox="0 0 128 128">
                <circle cx="64" cy="64" r={r} fill="none" stroke="var(--border)" strokeWidth="10" />
                <circle cx="64" cy="64" r={r} fill="none" stroke={color} strokeWidth="10"
                    strokeDasharray={`${dash} ${circ}`} strokeLinecap="round"
                    transform="rotate(-90 64 64)" style={{ transition: 'stroke-dasharray 1s ease' }} />
                <text x="64" y="58" textAnchor="middle" fontSize="26" fontWeight="800" fill={color}>{score}</text>
                <text x="64" y="76" textAnchor="middle" fontSize="11" fill="var(--muted-foreground)">/100</text>
            </svg>
            <div style={{
                background: color, color: 'var(--primary-foreground)', borderRadius: '8px',
                padding: '4px 20px', fontSize: '20px', fontWeight: '800', letterSpacing: '2px'
            }}>{grade}</div>
        </div>
    );
}

function SeverityBar({ count, severity, max }) {
    const meta = SEV_META[severity];
    const pct = max > 0 ? Math.round((count / max) * 100) : 0;
    return (
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '10px' }}>
            <span style={{ width: '70px', fontSize: '12px', color: meta.color, fontWeight: '600' }}>{meta.text}</span>
            <div style={{ flex: 1, background: 'var(--muted)', borderRadius: '99px', height: '10px', overflow: 'hidden' }}>
                <div style={{
                    width: `${pct}%`, background: meta.color, height: '100%', borderRadius: '99px',
                    transition: 'width 1s ease'
                }} />
            </div>
            <span style={{ fontSize: '13px', fontWeight: '700', minWidth: '24px', textAlign: 'right' }}>{count}</span>
        </div>
    );
}

export default function QualityDashboardPage() {
    const { id: projectId } = useParams();
    const [repositories, setRepositories] = useState([]);
    const [selectedRepo, setSelectedRepo] = useState(null);
    const [report, setReport] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [activeFilter, setActiveFilter] = useState('ALL');
    const [expandedIssue, setExpandedIssue] = useState(null);

    useEffect(() => {
        repositoryApi.list(projectId)
            .then(res => {
                const repos = (res.data.data || []).filter(r => r.status === 'READY');
                setRepositories(repos);
                if (repos.length > 0) setSelectedRepo(repos[0]);
            })
            .catch(() => { });
    }, [projectId]);

    const runAnalysis = async (repo) => {
        if (!repo) return;
        setLoading(true);
        setError('');
        setReport(null);
        setExpandedIssue(null);
        try {
            const res = await aiApi.analyzeQuality(projectId, repo.id);
            setReport(res.data.data);
        } catch (err) {
            setError(err.response?.data?.message || 'Analysis failed. Ensure the repository has been ingested.');
        } finally {
            setLoading(false);
        }
    };

    const filteredIssues = report?.issues?.filter(i =>
        activeFilter === 'ALL' || i.type === activeFilter) || [];

    const maxSeverity = report
        ? Math.max(report.criticalCount, report.highCount, report.mediumCount, report.lowCount, 1)
        : 1;

    return (
        <div>
            {/* Header */}
            <div className="page-header">
                <div>
                    <div className="page-title">📊 Code Quality Dashboard</div>
                    <div className="page-subtitle">Static analysis + AI-powered issue detection and scoring</div>
                </div>
                <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                    {repositories.length > 0 && (
                        <select className="input" style={{ width: 'auto' }}
                            value={selectedRepo?.id || ''}
                            onChange={e => {
                                const r = repositories.find(r => r.id === e.target.value);
                                setSelectedRepo(r);
                            }}>
                            {repositories.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
                        </select>
                    )}
                    <button className="btn btn-primary" disabled={loading || !selectedRepo}
                        onClick={() => runAnalysis(selectedRepo)}>
                        {loading ? <><span className="spinner" style={{ width: '14px', height: '14px', marginRight: '8px' }} />Analyzing...</> : '⚡ Run Analysis'}
                    </button>
                </div>
            </div>

            <ProjectSubNav activeTab="quality" />

            {error && <div className="alert alert-error">{error}</div>}

            {!report && !loading && (
                <div className="empty-state">
                    <div style={{ fontSize: '48px', marginBottom: '16px' }}>📊</div>
                    <h3>Ready for Analysis</h3>
                    <p>Select a repository and click <strong>Run Analysis</strong> to get your code quality score, detected issues, and AI-powered fix suggestions.</p>
                    {repositories.length === 0 && (
                        <p style={{ color: 'var(--text-muted)', marginTop: '8px', fontSize: '13px' }}>
                            No ready repositories found. Import a repository and trigger AI ingestion first.
                        </p>
                    )}
                </div>
            )}

            {loading && (
                <div style={{ textAlign: 'center', padding: '60px 20px' }}>
                    <div className="spinner" style={{ width: '36px', height: '36px', margin: '0 auto 16px' }} />
                    <div style={{ fontWeight: '600', fontSize: '16px' }}>Running AI-powered analysis...</div>
                    <div style={{ color: 'var(--text-muted)', fontSize: '13px', marginTop: '8px' }}>
                        Scanning files · Detecting issues · Generating AI explanations
                    </div>
                </div>
            )}

            {report && (
                <>
                    {/* Score + Overview Row */}
                    <div style={{ display: 'grid', gridTemplateColumns: '200px 1fr 1fr', gap: '16px', marginBottom: '16px' }}>

                        {/* Quality Score */}
                        <div className="card" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '24px 16px' }}>
                            <GradeRing score={report.qualityScore} grade={report.grade} />
                            <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '10px', textAlign: 'center' }}>
                                Quality Score
                            </div>
                            {report.modelId && (
                                <div style={{ fontSize: '10px', color: 'var(--text-muted)', marginTop: '4px' }}>{report.modelId}</div>
                            )}
                        </div>

                        {/* Issue Type Counts */}
                        <div className="card">
                            <div style={{ fontWeight: '600', marginBottom: '14px' }}>Issues by Type</div>
                            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '10px' }}>
                                {[
                                    { label: 'Bugs', count: report.bugCount, meta: TYPE_META.BUG },
                                    { label: 'Security', count: report.securityCount, meta: TYPE_META.SECURITY },
                                    { label: 'Code Smells', count: report.codeSmellCount, meta: TYPE_META.CODE_SMELL },
                                    { label: 'Performance', count: report.performanceCount, meta: TYPE_META.PERFORMANCE },
                                    { label: 'Maintainability', count: (report.issues || []).filter(i => i.type === 'MAINTAINABILITY').length, meta: TYPE_META.MAINTAINABILITY },
                                    { label: 'Total Issues', count: (report.issues || []).length, meta: { color: 'var(--muted-foreground)', bg: 'var(--muted)', icon: '📋' } },
                                ].map(({ label, count, meta }) => (
                                    <div key={label} style={{ background: meta.bg, borderRadius: '10px', padding: '12px', textAlign: 'center' }}>
                                        <div style={{ fontSize: '22px', marginBottom: '2px' }}>{meta.icon}</div>
                                        <div style={{ fontSize: '24px', fontWeight: '800', color: meta.color }}>{count}</div>
                                        <div style={{ fontSize: '11px', color: 'var(--muted-foreground)' }}>{label}</div>
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* Severity Bars */}
                        <div className="card">
                            <div style={{ fontWeight: '600', marginBottom: '14px' }}>Issues by Severity</div>
                            {SEVERITIES.map(sev => (
                                <SeverityBar key={sev} severity={sev}
                                    count={sev === 'CRITICAL' ? report.criticalCount : sev === 'HIGH' ? report.highCount :
                                        sev === 'MEDIUM' ? report.mediumCount : report.lowCount}
                                    max={maxSeverity} />
                            ))}
                        </div>
                    </div>

                    {/* Metrics Row */}
                    <div className="stats-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))', marginBottom: '16px' }}>
                        {[
                            { label: 'Files', value: report.totalFiles },
                            { label: 'Lines', value: (report.totalLines || 0).toLocaleString() },
                            { label: 'Classes', value: report.classCount },
                            { label: 'Methods', value: report.methodCount },
                            { label: 'Avg Complexity', value: (report.averageComplexity || 0).toFixed(1) },
                        ].map(({ label, value }) => (
                            <div key={label} className="card stat-card">
                                <div className="stat-value" style={{ fontSize: '20px' }}>{value}</div>
                                <div className="stat-label">{label}</div>
                            </div>
                        ))}
                    </div>

                    {/* AI Recommendations */}
                    {report.aiRecommendations?.length > 0 && (
                        <div className="card" style={{ marginBottom: '16px' }}>
                            <div style={{ fontWeight: '600', marginBottom: '12px' }}>💡 AI Recommendations</div>
                            <ul style={{ margin: 0, paddingLeft: '18px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                                {report.aiRecommendations.map((rec, i) => (
                                    <li key={i} style={{ fontSize: '14px', color: 'var(--text)' }}>{rec}</li>
                                ))}
                            </ul>
                        </div>
                    )}

                    {/* Issues Table */}
                    {(report.issues?.length ?? 0) > 0 && (
                        <div className="card">
                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
                                <div style={{ fontWeight: '600' }}>🔍 Detected Issues ({filteredIssues.length})</div>
                                <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                                    {['ALL', ...TYPES].map(f => (
                                        <button key={f} onClick={() => setActiveFilter(f)}
                                            style={{
                                                padding: '4px 10px', borderRadius: '6px', fontSize: '11px', fontWeight: '600',
                                                border: '1px solid', cursor: 'pointer',
                                                background: activeFilter === f ? 'var(--primary)' : 'transparent',
                                                color: activeFilter === f ? 'var(--primary-foreground, #ffffff)' : 'var(--text-muted)',
                                                borderColor: activeFilter === f ? 'var(--primary)' : 'var(--border)'
                                            }}>
                                            {f === 'ALL' ? 'All' : TYPE_META[f]?.label || f}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                {filteredIssues.map((issue, i) => {
                                    const tm = TYPE_META[issue.type] || TYPE_META.CODE_SMELL;
                                    const sm = SEV_META[issue.severity] || SEV_META.MEDIUM;
                                    const isExpanded = expandedIssue === i;
                                    return (
                                        <div key={i} style={{
                                            border: `1px solid ${tm.color}`, borderRadius: '10px',
                                            overflow: 'hidden', background: tm.bg
                                        }}>
                                            <div style={{
                                                display: 'flex', alignItems: 'center', gap: '12px', padding: '12px 14px',
                                                cursor: 'pointer'
                                            }} onClick={() => setExpandedIssue(isExpanded ? null : i)}>
                                                <span style={{ fontSize: '18px' }}>{tm.icon}</span>
                                                <div style={{ flex: 1, minWidth: 0 }}>
                                                    <div style={{ fontWeight: '600', fontSize: '14px' }}>{issue.title}</div>
                                                    <div style={{ fontSize: '12px', color: 'var(--muted-foreground, #64748b)', marginTop: '2px' }}>
                                                        {issue.filePath && <span style={{ fontFamily: 'monospace' }}>{issue.filePath}</span>}
                                                        {issue.line && <span> · Line {issue.line}</span>}
                                                    </div>
                                                </div>
                                                <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }}>
                                                    <span style={{
                                                        padding: '2px 8px', borderRadius: '999px', fontSize: '11px', fontWeight: '700',
                                                        background: 'var(--muted)', color: sm.color, border: `1px solid ${sm.color}`
                                                    }}>{sm.text}</span>
                                                    <span style={{ fontSize: '11px', color: 'var(--muted-foreground, #64748b)' }}>{isExpanded ? '▲' : '▼'}</span>
                                                </div>
                                            </div>

                                            {isExpanded && (
                                                <div style={{
                                                    padding: '0 14px 14px', borderTop: `1px solid ${tm.color}`,
                                                    display: 'flex', flexDirection: 'column', gap: '10px', marginTop: '2px', paddingTop: '12px'
                                                }}>
                                                    {issue.description && (
                                                        <div>
                                                            <div style={{ fontSize: '11px', fontWeight: '700', color: 'var(--muted-foreground, #64748b)', textTransform: 'uppercase', marginBottom: '4px' }}>Problem</div>
                                                            <div style={{ fontSize: '13px' }}>{issue.description}</div>
                                                        </div>
                                                    )}
                                                    {issue.explanation && (
                                                        <div>
                                                            <div style={{ fontSize: '11px', fontWeight: '700', color: 'var(--muted-foreground, #64748b)', textTransform: 'uppercase', marginBottom: '4px' }}>Why it matters</div>
                                                            <div style={{ fontSize: '13px' }}>{issue.explanation}</div>
                                                        </div>
                                                    )}
                                                    {issue.suggestion && (
                                                        <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: '8px', padding: '10px 12px' }}>
                                                            <div style={{ fontSize: '11px', fontWeight: '700', color: '#16a34a', textTransform: 'uppercase', marginBottom: '4px' }}>✅ Suggestion</div>
                                                            <div style={{ fontSize: '13px', color: '#15803d' }}>{issue.suggestion}</div>
                                                        </div>
                                                    )}
                                                </div>
                                            )}
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    )}
                </>
            )}
        </div>
    );
}
