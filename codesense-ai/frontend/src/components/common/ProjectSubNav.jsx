import React from 'react';
import { useNavigate, useParams, useLocation } from 'react-router-dom';

export default function ProjectSubNav({ activeTab }) {
    const { id: projectId } = useParams();
    const navigate = useNavigate();
    const location = useLocation();

    if (!projectId) return null;

    const tabs = [
        { key: 'overview', path: `/projects/${projectId}`, label: '📦 Overview' },
        { key: 'repository', path: `/projects/${projectId}/repository`, label: '📁 Repository' },
        { key: 'chat', path: `/projects/${projectId}/chat`, label: '💬 Chat' },
        { key: 'search', path: `/projects/${projectId}/search`, label: '🔍 Search' },
        { key: 'code-explanation', path: `/projects/${projectId}/code-explanation`, label: '💡 Code Explanation' },
        { key: 'readme', path: `/projects/${projectId}/readme`, label: '📖 Readme' },
        { key: 'api-docs', path: `/projects/${projectId}/api-docs`, label: '🔌 API Docs' },
        { key: 'architecture', path: `/projects/${projectId}/architecture`, label: '🏛️ Architecture' },
        { key: 'dependencies', path: `/projects/${projectId}/dependencies`, label: '🔗 Dependency Graph' },
        { key: 'metrics', path: `/projects/${projectId}/metrics`, label: '📈 Metrics' },
        { key: 'quality', path: `/projects/${projectId}/quality`, label: '📊 Quality Dashboard', primary: true }
    ];

    const currentTab = activeTab || tabs.find(t => location.pathname.endsWith(t.key) || (t.key === 'overview' && location.pathname === `/projects/${projectId}`))?.key || '';

    return (
        <div style={{
            display: 'flex',
            gap: '8px',
            marginBottom: '24px',
            flexWrap: 'wrap',
            padding: '8px 12px',
            backgroundColor: 'var(--bg-card, #161b22)',
            borderRadius: '8px',
            border: '1px solid var(--border-color, rgba(255,255,255,0.1))'
        }}>
            {tabs.map(tab => {
                const isActive = currentTab === tab.key;
                let btnClass = 'btn btn-sm ';
                if (isActive) {
                    btnClass += 'btn-primary';
                } else {
                    btnClass += 'btn-secondary';
                }
                return (
                    <button
                        key={tab.key}
                        className={btnClass}
                        style={isActive ? { backgroundColor: 'var(--accent-color, #3b82f6)', color: 'var(--primary-foreground, #ffffff)' } : {}}
                        onClick={() => navigate(tab.path)}
                    >
                        {tab.label}
                    </button>
                );
            })}
        </div>
    );
}
