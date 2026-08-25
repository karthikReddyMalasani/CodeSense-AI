import { Outlet, NavLink, useParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const icon = {
  dashboard: '⊞',
  projects: '📁',
  repo: '🗂',
  chat: '💬',
  search: '🔍',
  explain: '💡',
  readme: '📖',
  apidocs: '📋',
  arch: '🏗',
  metrics: '📊',
  deps: '🔗',
};

export default function Layout() {
  const { user, logout } = useAuth();
  const { projectId } = useParams();

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar-logo">
          CodeSense <span>AI</span>
        </div>
        <nav className="sidebar-nav">
          <div className="sidebar-section">
            <div className="sidebar-section-title">Main</div>
            <NavLink to="/dashboard" className={({isActive}) => `nav-item${isActive ? ' active' : ''}`}>
              {icon.dashboard} Dashboard
            </NavLink>
            <NavLink to="/projects" className={({isActive}) => `nav-item${isActive ? ' active' : ''}`}>
              {icon.projects} Projects
            </NavLink>
          </div>

          {projectId && (
            <div className="sidebar-section">
              <div className="sidebar-section-title">Project</div>
              <NavLink to={`/projects/${projectId}/repository`}
                className={({isActive}) => `nav-item${isActive ? ' active' : ''}`}>
                {icon.repo} Repository
              </NavLink>
              <NavLink to={`/projects/${projectId}/chat`}
                className={({isActive}) => `nav-item${isActive ? ' active' : ''}`}>
                {icon.chat} AI Chat
              </NavLink>
              <NavLink to={`/projects/${projectId}/search`}
                className={({isActive}) => `nav-item${isActive ? ' active' : ''}`}>
                {icon.search} Search
              </NavLink>
              <NavLink to={`/projects/${projectId}/code-explanation`}
                className={({isActive}) => `nav-item${isActive ? ' active' : ''}`}>
                {icon.explain} Explain Code
              </NavLink>
              <NavLink to={`/projects/${projectId}/readme`}
                className={({isActive}) => `nav-item${isActive ? ' active' : ''}`}>
                {icon.readme} README
              </NavLink>
              <NavLink to={`/projects/${projectId}/api-docs`}
                className={({isActive}) => `nav-item${isActive ? ' active' : ''}`}>
                {icon.apidocs} API Docs
              </NavLink>
              <NavLink to={`/projects/${projectId}/architecture`}
                className={({isActive}) => `nav-item${isActive ? ' active' : ''}`}>
                {icon.arch} Architecture
              </NavLink>
              <NavLink to={`/projects/${projectId}/metrics`}
                className={({isActive}) => `nav-item${isActive ? ' active' : ''}`}>
                {icon.metrics} Metrics
              </NavLink>
              <NavLink to={`/projects/${projectId}/dependencies`}
                className={({isActive}) => `nav-item${isActive ? ' active' : ''}`}>
                {icon.deps} Dependencies
              </NavLink>
            </div>
          )}
        </nav>

        <div style={{ padding: '12px 16px', borderTop: '1px solid var(--border)' }}>
          <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginBottom: '8px' }}>
            {user?.name || user?.email}
          </div>
          <button className="btn btn-secondary btn-sm" onClick={logout} style={{ width: '100%' }}>
            Sign Out
          </button>
        </div>
      </aside>

      <main className="main-content">
        <div className="topbar">
          <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
            CodeSense AI — Code Intelligence Platform
          </span>
          <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
            {user?.role}
          </span>
        </div>
        <div className="page-content">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
