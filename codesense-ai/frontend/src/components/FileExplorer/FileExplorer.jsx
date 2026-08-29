import React, { useState } from 'react';
import { useProject } from '../../context/ProjectContext';
import ImportProjectModal from '../common/ImportProjectModal';
import ProjectTreeExplorer from './ProjectTreeExplorer';
import {
  Folder,
  FolderOpen,
  FileCode,
  FileText,
  FileJson,
  Plus,
  ChevronDown,
  ChevronRight,
  Search,
  FolderPlus,
  FilePlus,
  FolderGit2,
  FileArchive,
  TreeIcon
} from 'lucide-react';
import './FileExplorer.css';

const FileExplorer = () => {
  const { currentProject, selectFile, activeFileId, searchQuery, setSearchQuery, createNewFile } = useProject();
  const [expandedFolders, setExpandedFolders] = useState({ src: true, tests: true });
  const [showNewFileInput, setShowNewFileInput] = useState(false);
  const [newFileName, setNewFileName] = useState('');
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [viewMode, setViewMode] = useState('current'); // 'current' or 'projects'

  const toggleFolder = (folderName) => {
    setExpandedFolders((prev) => ({ ...prev, [folderName]: !prev[folderName] }));
  };

  const handleCreateFileSubmit = (e) => {
    e.preventDefault();
    if (newFileName.trim()) {
      createNewFile(newFileName.trim(), currentProject.language || 'java');
      setNewFileName('');
      setShowNewFileInput(false);
    }
  };

  const filteredFiles = currentProject.files.filter((file) =>
    file.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const srcFiles = filteredFiles.filter((f) => f.path.startsWith('src/'));
  const testFiles = filteredFiles.filter((f) => f.path.startsWith('tests/'));
  const rootFiles = filteredFiles.filter((f) => !f.path.includes('/'));

  const getFileIcon = (fileName) => {
    if (fileName.endsWith('.java') || fileName.endsWith('.py') || fileName.endsWith('.js') || fileName.endsWith('.ts') || fileName.endsWith('.cpp')) {
      return <FileCode className="file-icon code" />;
    }
    if (fileName.endsWith('.json')) {
      return <FileJson className="file-icon json" />;
    }
    return <FileText className="file-icon doc" />;
  };

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
            <TreeIcon className="action-icon" />
          </button>
          <button className="icon-action-btn" title="New File" onClick={() => setShowNewFileInput(true)}>
            <FilePlus className="action-icon" />
          </button>
          <button className="icon-action-btn" title="Import GitHub / Upload ZIP" onClick={() => setIsImportModalOpen(true)}>
            <FolderPlus className="action-icon" />
          </button>
        </div>
      </div>

      {viewMode === 'projects' ? (
        // Projects Tree View
        <ProjectTreeExplorer onFileSelect={selectFile} />
      ) : (
        // Current Project Files View
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

          {showNewFileInput && (
            <form onSubmit={handleCreateFileSubmit} className="new-file-form">
              <input
                type="text"
                className="new-file-input"
                placeholder="e.g. Calculator.java"
                value={newFileName}
                onChange={(e) => setNewFileName(e.target.value)}
                autoFocus
              />
            </form>
          )}

          <div className="tree-view">
            <div className="project-root-node">
              <FolderOpen className="folder-icon root" />
              <span className="root-name">{currentProject.name}</span>
            </div>

            {/* src folder */}
            <div className="tree-folder">
              <div className="folder-node" onClick={() => toggleFolder('src')}>
                {expandedFolders.src ? <ChevronDown className="arrow-icon" /> : <ChevronRight className="arrow-icon" />}
                {expandedFolders.src ? <FolderOpen className="folder-icon" /> : <Folder className="folder-icon" />}
                <span className="folder-name">src</span>
              </div>

              {expandedFolders.src && (
                <div className="folder-children">
                  {srcFiles.map((file) => (
                    <div
                      key={file.id}
                      className={`file-node ${activeFileId === file.id ? 'active' : ''}`}
                      onClick={() => selectFile(file)}
                    >
                      {getFileIcon(file.name)}
                      <span className="file-name">{file.name}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* tests folder */}
            <div className="tree-folder">
              <div className="folder-node" onClick={() => toggleFolder('tests')}>
                {expandedFolders.tests ? <ChevronDown className="arrow-icon" /> : <ChevronRight className="arrow-icon" />}
                {expandedFolders.tests ? <FolderOpen className="folder-icon" /> : <Folder className="folder-icon" />}
                <span className="folder-name">tests</span>
              </div>

              {expandedFolders.tests && (
                <div className="folder-children">
                  {testFiles.map((file) => (
                    <div
                      key={file.id}
                      className={`file-node ${activeFileId === file.id ? 'active' : ''}`}
                      onClick={() => selectFile(file)}
                    >
                      {getFileIcon(file.name)}
                      <span className="file-name">{file.name}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* root files */}
            {rootFiles.map((file) => (
              <div
                key={file.id}
                className={`file-node root-file ${activeFileId === file.id ? 'active' : ''}`}
                onClick={() => selectFile(file)}
              >
                {getFileIcon(file.name)}
                <span className="file-name">{file.name}</span>
              </div>
            ))}
          </div>
        </>
      )}

      <ImportProjectModal isOpen={isImportModalOpen} onClose={() => setIsImportModalOpen(false)} />
    </div>
  );
};

export default FileExplorer;
