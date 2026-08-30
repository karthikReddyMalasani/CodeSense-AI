import React, { useMemo, useState } from 'react';
import { useProject } from '../../context/ProjectContext';
import ImportProjectModal from '../common/ImportProjectModal';
import ProjectTreeExplorer from './ProjectTreeExplorer';
import {
  Folder,
  FolderOpen,
  FileCode,
  FileText,
  FileJson,
  ChevronDown,
  ChevronRight,
  Search,
  FolderPlus,
  FilePlus,
  Layers
} from 'lucide-react';
import './FileExplorer.css';

const normalizePath = (value = '') => String(value || '').replace(/\\/g, '/').replace(/^\/+/, '').replace(/\/+$/, '').replace(/\/+/g, '/');

const buildExplorerTree = (projectName, files = []) => {
  const root = { name: projectName, type: 'folder', children: {}, files: [] };

  files.forEach((entry) => {
    const rawPath = normalizePath(entry.path || entry.name || '');
    if (!rawPath) return;

    const parts = rawPath.split('/').filter(Boolean);
    let current = root;

    for (let i = 0; i < parts.length - 1; i += 1) {
      const folderName = parts[i];
      if (!current.children[folderName]) {
        current.children[folderName] = { name: folderName, type: 'folder', children: {}, files: [] };
      }
      current = current.children[folderName];
    }

    const finalName = parts[parts.length - 1];
    if (entry.type === 'directory' || (!entry.path && entry.type === 'folder')) {
      if (!current.children[finalName]) {
        current.children[finalName] = { name: finalName, type: 'folder', children: {}, files: [] };
      }
      return;
    }

    current.files.push({ ...entry, displayName: finalName });
  });

  return root;
};

const FileExplorer = () => {
  const { currentProject, selectFile, activeFileId, searchQuery, setSearchQuery, createNewFile, createFolder } = useProject();
  const [expandedFolders, setExpandedFolders] = useState({});
  const [showNewFileInput, setShowNewFileInput] = useState(false);
  const [showNewFolderInput, setShowNewFolderInput] = useState(false);
  const [newEntryName, setNewEntryName] = useState('');
  const [newEntryType, setNewEntryType] = useState('file');
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [viewMode, setViewMode] = useState('current');

  const explorerTree = useMemo(
    () => buildExplorerTree(currentProject.name, currentProject.files || []),
    [currentProject.name, currentProject.files]
  );

  const filteredFiles = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) return currentProject.files || [];
    return (currentProject.files || []).filter((file) => (file.path || file.name || '').toLowerCase().includes(query));
  }, [currentProject.files, searchQuery]);

  const toggleFolder = (folderPath) => {
    setExpandedFolders((prev) => ({ ...prev, [folderPath]: !prev[folderPath] }));
  };

  const renderNode = (node, depth = 0, currentPath = '') => {
    const folders = Object.entries(node.children || {}).sort(([a], [b]) => a.localeCompare(b));
    const files = [...(node.files || [])].filter((file) => {
      const path = normalizePath(file.path || file.name || '');
      return !searchQuery || path.toLowerCase().includes(searchQuery.toLowerCase());
    }).sort((a, b) => (a.displayName || a.name).localeCompare(b.displayName || b.name));

    return (
      <div key={`${currentPath || 'root'}-${depth}`} className="tree-branch">
        {folders.map(([folderName, folderNode]) => {
          const folderPath = currentPath ? `${currentPath}/${folderName}` : folderName;
          const isExpanded = expandedFolders[folderPath] !== false;

          return (
            <div key={`folder-${folderPath}`} className="tree-folder-block">
              <div className="folder-node" style={{ paddingLeft: `${depth * 14 + 10}px` }} onClick={() => toggleFolder(folderPath)}>
                {isExpanded ? <ChevronDown className="arrow-icon" /> : <ChevronRight className="arrow-icon" />}
                {isExpanded ? <FolderOpen className="folder-icon" /> : <Folder className="folder-icon" />}
                <span className="folder-name">{folderName}</span>
              </div>
              {isExpanded && renderNode(folderNode, depth + 1, folderPath)}
            </div>
          );
        })}

        {files.map((file) => {
          const filePath = normalizePath(file.path || file.name || '');
          const fileName = file.displayName || file.name;
          const isActive = activeFileId === file.id;
          const icon = fileName.endsWith('.json')
            ? <FileJson className="file-icon json" />
            : fileName.endsWith('.java') || fileName.endsWith('.py') || fileName.endsWith('.js') || fileName.endsWith('.ts') || fileName.endsWith('.cpp')
              ? <FileCode className="file-icon code" />
              : <FileText className="file-icon doc" />;

          return (
            <div
              key={file.id || filePath}
              className={`file-node ${isActive ? 'active' : ''}`}
              style={{ paddingLeft: `${depth * 14 + 26}px` }}
              onClick={() => selectFile(file)}
            >
              {icon}
              <span className="file-name">{fileName}</span>
            </div>
          );
        })}
      </div>
    );
  };

  const handleCreateEntrySubmit = (e) => {
    e.preventDefault();
    const trimmed = newEntryName.trim();
    if (!trimmed) return;

    if (newEntryType === 'folder') {
      const basePath = normalizePath(trimmed);
      createFolder(basePath);
    } else {
      const targetPath = normalizePath(trimmed.includes('/') ? trimmed.substring(0, trimmed.lastIndexOf('/')) : 'src');
      const fileName = trimmed.includes('/') ? trimmed.substring(trimmed.lastIndexOf('/') + 1) : trimmed;
      createNewFile(fileName, currentProject.language || 'java', targetPath);
    }

    setNewEntryName('');
    setShowNewFileInput(false);
    setShowNewFolderInput(false);
  };

  const filteredTree = useMemo(() => {
    if (!searchQuery.trim()) {
      return explorerTree;
    }

    const tree = { name: currentProject.name, type: 'folder', children: {}, files: [] };
    filteredFiles.forEach((file) => {
      const normalized = normalizePath(file.path || file.name || '');
      if (!normalized) return;
      const parts = normalized.split('/').filter(Boolean);
      let current = tree;

      for (let i = 0; i < parts.length - 1; i += 1) {
        const folderName = parts[i];
        if (!current.children[folderName]) {
          current.children[folderName] = { name: folderName, type: 'folder', children: {}, files: [] };
        }
        current = current.children[folderName];
      }

      current.files.push({ ...file, displayName: parts[parts.length - 1] });
    });

    return tree;
  }, [currentProject.name, filteredFiles, searchQuery]);

  return (
    <div className="file-explorer-panel">
      <div className="explorer-header">
        <span className="explorer-title">EXPLORER</span>
        <div className="explorer-actions">
          <button
            className={`icon-action-btn ${viewMode === 'projects' ? 'active' : ''}`}
            title="Toggle Projects Tree View"
            onClick={() => setViewMode(viewMode === 'projects' ? 'current' : 'projects')}
          >
            <Layers className="action-icon" />
          </button>
          <button className="icon-action-btn" title="New File" onClick={() => { setNewEntryType('file'); setShowNewFileInput(true); setShowNewFolderInput(false); }}>
            <FilePlus className="action-icon" />
          </button>
          <button className="icon-action-btn" title="New Folder" onClick={() => { setNewEntryType('folder'); setShowNewFolderInput(true); setShowNewFileInput(false); }}>
            <FolderPlus className="action-icon" />
          </button>
        </div>
      </div>

      {viewMode === 'projects' ? (
        <ProjectTreeExplorer onFileSelect={selectFile} />
      ) : (
        <>
          <div className="project-actions-bar">
            <button className="btn-small btn-secondary" onClick={() => setIsImportModalOpen(true)}>
              + Import Repo
            </button>
            <button className="btn-small btn-outline" onClick={() => setIsImportModalOpen(true)}>
              Upload ZIP
            </button>
          </div>

          <div className="search-box">
            <Search className="search-box-icon" />
            <input
              type="text"
              className="search-input"
              placeholder="Search files..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          {(showNewFileInput || showNewFolderInput) && (
            <form onSubmit={handleCreateEntrySubmit} className="new-file-form">
              <div className="new-entry-label">
                {newEntryType === 'folder' ? 'Create folder path' : 'Create file path'}
              </div>
              <input
                type="text"
                className="new-file-input"
                placeholder={newEntryType === 'folder' ? 'e.g. services/auth' : 'e.g. src/services/ApiClient.java'}
                value={newEntryName}
                onChange={(e) => setNewEntryName(e.target.value)}
                autoFocus
              />
            </form>
          )}

          <div className="tree-view">
            <div className="project-root-node">
              <FolderOpen className="folder-icon root" />
              <span className="root-name">{currentProject.name}</span>
            </div>
            {renderNode(filteredTree)}
          </div>
        </>
      )}

      <ImportProjectModal isOpen={isImportModalOpen} onClose={() => setIsImportModalOpen(false)} />
    </div>
  );
};

export default FileExplorer;
