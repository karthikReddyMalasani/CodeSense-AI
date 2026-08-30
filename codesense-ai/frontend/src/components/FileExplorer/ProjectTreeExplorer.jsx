import React, { useState, useEffect } from 'react';
import {
  ChevronDown,
  ChevronRight,
  Folder,
  FolderOpen,
  FileCode,
  FileText,
  FileJson,
  Archive,
  Zap
} from 'lucide-react';
import { projectApi, repositoryApi } from '../../services/api';
import './ProjectTreeExplorer.css';

const ProjectTreeExplorer = ({ onFileSelect }) => {
  const [projects, setProjects] = useState([]);
  const [expandedNodes, setExpandedNodes] = useState({});
  const [repoFiles, setRepoFiles] = useState({});
  const [loading, setLoading] = useState(true);
  const [loadingRepos, setLoadingRepos] = useState({});

  // Fetch all projects on mount
  useEffect(() => {
    loadProjects();
  }, []);

  const loadProjects = async () => {
    try {
      const res = await projectApi.list();
      setProjects(res.data.data || []);
      // Expand first project by default
      if (res.data.data?.length > 0) {
        setExpandedNodes({ [`project-${res.data.data[0].id}`]: true });
      }
    } catch (err) {
      console.error('Failed to load projects:', err);
    } finally {
      setLoading(false);
    }
  };

  const toggleNode = async (nodeId) => {
    setExpandedNodes(prev => ({
      ...prev,
      [nodeId]: !prev[nodeId]
    }));

    // If toggling a project node, load its repositories
    if (nodeId.startsWith('project-')) {
      const projectId = nodeId.replace('project-', '');
      if (!expandedNodes[nodeId]) {
        await loadRepositories(projectId);
      }
    }

    // If toggling a repository node, load its files
    if (nodeId.startsWith('repo-')) {
      const repoId = nodeId.replace('repo-', '');
      if (!expandedNodes[nodeId] && !repoFiles[repoId]) {
        await loadRepositoryFiles(repoId);
      }
    }
  };

  const loadRepositories = async (projectId) => {
    try {
      setLoadingRepos(prev => ({ ...prev, [projectId]: true }));
      const res = await repositoryApi.list(projectId);
      const repos = res.data.data || [];
      // Store repos by project (update project object)
      setProjects(prevProjects =>
        prevProjects.map(p =>
          p.id === projectId ? { ...p, repositories: repos } : p
        )
      );
    } catch (err) {
      console.error(`Failed to load repositories for project ${projectId}:`, err);
    } finally {
      setLoadingRepos(prev => ({ ...prev, [projectId]: false }));
    }
  };

  const buildFileTree = (files) => {
    if (!Array.isArray(files)) return [];

    const root = { name: 'root', type: 'folder', children: {}, files: [] };

    files.forEach(file => {
      const pathParts = (file.path || file.name || '').split('/').filter(Boolean);
      if (pathParts.length === 0) return;

      let current = root;
      for (let i = 0; i < pathParts.length - 1; i++) {
        const folderName = pathParts[i];
        if (!current.children[folderName]) {
          current.children[folderName] = {
            name: folderName,
            type: 'folder',
            children: {},
            files: []
          };
        }
        current = current.children[folderName];
      }

      const fileName = pathParts[pathParts.length - 1];
      current.files.push({ ...file, displayName: fileName });
    });

    return root;
  };

  const loadRepositoryFiles = async (repoId) => {
    try {
      const res = await repositoryApi.getFiles(repoId);
      const files = res.data.data || [];
      setRepoFiles(prev => ({
        ...prev,
        [repoId]: buildFileTree(files)
      }));
    } catch (err) {
      console.error(`Failed to load files for repository ${repoId}:`, err);
      setRepoFiles(prev => ({ ...prev, [repoId]: null }));
    }
  };

  const getFileIcon = (fileName) => {
    if (!fileName) return <FileText className="tree-file-icon doc" />;
    if (fileName.endsWith('.java') || fileName.endsWith('.py') || fileName.endsWith('.js')
      || fileName.endsWith('.ts') || fileName.endsWith('.tsx') || fileName.endsWith('.jsx')
      || fileName.endsWith('.cpp') || fileName.endsWith('.c')) {
      return <FileCode className="tree-file-icon code" />;
    }
    if (fileName.endsWith('.json')) {
      return <FileJson className="tree-file-icon json" />;
    }
    if (fileName.endsWith('.zip') || fileName.endsWith('.tar') || fileName.endsWith('.gz')) {
      return <Archive className="tree-file-icon archive" />;
    }
    return <FileText className="tree-file-icon doc" />;
  };

  const RecursiveTreeNode = ({ node, repoId, pathPrefix = '', depth = 0 }) => {
    if (!node) return null;

    const folderEntries = Object.entries(node.children || {});
    const sortedFolders = folderEntries.sort(([a], [b]) => a.localeCompare(b));
    const sortedFiles = [...(node.files || [])].sort((a, b) => a.displayName.localeCompare(b.displayName));

    return (
      <div className="tree-node-group">
        {/* Render Folders */}
        {sortedFolders.map(([folderName, folderNode]) => {
          const folderPath = `${pathPrefix}/${folderName}`;
          const folderId = `folder-${repoId}-${folderPath}`;
          const isExpanded = expandedNodes[folderId];
          const totalChildFiles = folderNode.files.length + Object.keys(folderNode.children).length;

          return (
            <div key={folderId} className="tree-folder-wrapper">
              <div
                className="tree-folder-item"
                style={{ paddingLeft: `${depth * 14 + 8}px` }}
                onClick={() => toggleNode(folderId)}
              >
                <div className="tree-node-content">
                  <button className="tree-toggle-btn">
                    {isExpanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                  </button>
                  {isExpanded ? <FolderOpen size={16} className="folder-icon open" /> : <Folder size={16} className="folder-icon" />}
                  <span className="tree-item-name">{folderName}</span>
                </div>
              </div>

              {isExpanded && (
                <RecursiveTreeNode
                  node={folderNode}
                  repoId={repoId}
                  pathPrefix={folderPath}
                  depth={depth + 1}
                />
              )}
            </div>
          );
        })}

        {/* Render Files */}
        {sortedFiles.map(file => (
          <div
            key={`file-${file.id}`}
            className="tree-file-item"
            style={{ paddingLeft: `${depth * 14 + 26}px` }}
            onClick={() => onFileSelect?.(file)}
          >
            <div className="tree-node-content">
              {getFileIcon(file.displayName || file.name)}
              <span className="tree-item-name">{file.displayName || file.name}</span>
            </div>
          </div>
        ))}
      </div>
    );
  };

  const renderFileTree = (rootNode, repoId) => {
    if (!rootNode) return null;
    return <RecursiveTreeNode node={rootNode} repoId={repoId} depth={0} />;
  };

  if (loading) {
    return (
      <div className="project-tree-explorer">
        <div className="tree-loading">Loading projects...</div>
      </div>
    );
  }

  if (projects.length === 0) {
    return (
      <div className="project-tree-explorer">
        <div className="tree-empty">No projects found</div>
      </div>
    );
  }

  return (
    <div className="project-tree-explorer">
      <div className="tree-header">
        <h3 className="tree-title">Projects</h3>
        <span className="tree-badge">{projects.length}</span>
      </div>

      <div className="tree-container">
        {projects.map(project => {
          const projectNodeId = `project-${project.id}`;
          const isProjectExpanded = expandedNodes[projectNodeId];
          const repositories = project.repositories || [];
          const isLoadingRepos = loadingRepos[project.id];

          return (
            <div key={projectNodeId}>
              {/* Project Node */}
              <div
                className="tree-project-item"
                onClick={() => toggleNode(projectNodeId)}
              >
                <div className="tree-node-content">
                  <button className="tree-toggle-btn">
                    {isProjectExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                  </button>
                  <Folder size={16} className="tree-project-icon" />
                  <span className="tree-item-name">{project.name}</span>
                  <span className="tree-item-count">({repositories.length})</span>
                </div>
              </div>

              {/* Repositories */}
              {isProjectExpanded && (
                <div className="tree-project-contents">
                  {isLoadingRepos && <div className="tree-loading-inline">Loading repositories...</div>}

                  {repositories.map(repo => {
                    const repoNodeId = `repo-${repo.id}`;
                    const isRepoExpanded = expandedNodes[repoNodeId];
                    const fileTree = repoFiles[repo.id];
                    const fileCount = repo.totalFiles || 0;

                    return (
                      <div key={repoNodeId}>
                        {/* Repository Node */}
                        <div
                          className="tree-repository-item"
                          onClick={() => toggleNode(repoNodeId)}
                        >
                          <div className="tree-node-content">
                            <button className="tree-toggle-btn">
                              {isRepoExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                            </button>
                            {isRepoExpanded ? <FolderOpen size={16} /> : <Folder size={16} />}
                            <span className="tree-item-name">{repo.name}</span>
                            <span className="tree-item-count">({fileCount})</span>
                            {repo.status === 'READY' && (
                              <Zap size={12} className="tree-status-icon ready" title="Ready" />
                            )}
                          </div>
                        </div>

                        {/* Files */}
                        {isRepoExpanded && (
                          <div className="tree-repository-contents">
                            {!fileTree ? (
                              <div className="tree-loading-inline">Loading files...</div>
                            ) : (
                              renderFileTree(fileTree, repo.id)
                            )}
                          </div>
                        )}
                      </div>
                    );
                  })}

                  {repositories.length === 0 && !isLoadingRepos && (
                    <div className="tree-empty-inline">No repositories</div>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default ProjectTreeExplorer;
