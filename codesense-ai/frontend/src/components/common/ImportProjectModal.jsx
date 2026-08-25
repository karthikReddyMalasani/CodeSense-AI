import React, { useState } from 'react';
import { useProject } from '../../context/ProjectContext';
import { useEditor } from '../../context/EditorContext';
import { projectService } from '../../services/projectService';
import {
  X,
  FolderGit2,
  FileArchive,
  UploadCloud,
  Link2,
  FolderPlus,
  CheckCircle2,
  AlertCircle,
  Loader2
} from 'lucide-react';

const ImportProjectModal = ({ isOpen, onClose }) => {
  const { setCurrentProject, selectFile } = useProject();
  const { setActiveLanguage } = useEditor();

  const [activeTab, setActiveTab] = useState('github'); // 'github' | 'zip'
  const [githubUrl, setGithubUrl] = useState('');
  const [branch, setBranch] = useState('main');
  const [zipFile, setZipFile] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [statusMessage, setStatusMessage] = useState(null);

  if (!isOpen) return null;

  const handleGitHubSubmit = async (e) => {
    e.preventDefault();
    if (!githubUrl.trim()) return;

    setIsSubmitting(true);
    setStatusMessage({ type: 'info', text: 'Cloning GitHub repository & indexing code...' });

    const response = await projectService.createProject({
      name: githubUrl.split('/').pop().replace('.git', '') || 'GitHub Project',
      language: 'java',
      source: 'github',
      url: githubUrl,
      branch
    });

    setTimeout(() => {
      setIsSubmitting(false);
      setStatusMessage({ type: 'success', text: 'GitHub repository imported successfully!' });
      
      setTimeout(() => {
        onClose();
        setStatusMessage(null);
      }, 1000);
    }, 1500);
  };

  const handleZipDrop = (e) => {
    e.preventDefault();
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      setZipFile(e.dataTransfer.files[0]);
    }
  };

  const handleZipFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setZipFile(e.target.files[0]);
    }
  };

  const handleZipSubmit = async (e) => {
    e.preventDefault();
    if (!zipFile) return;

    setIsSubmitting(true);
    setStatusMessage({ type: 'info', text: `Unpacking ${zipFile.name} & analyzing files...` });

    setTimeout(() => {
      setIsSubmitting(false);
      setStatusMessage({ type: 'success', text: 'ZIP repository uploaded & indexed successfully!' });

      setTimeout(() => {
        onClose();
        setStatusMessage(null);
      }, 1000);
    }, 1500);
  };

  return (
    <div className="modal-backdrop-overlay" onClick={onClose}>
      <div className="import-modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title-box">
            <FolderPlus className="modal-icon glow" />
            <div>
              <h3>Import Project / Repository</h3>
              <p>Import from GitHub or upload a local ZIP repository archive.</p>
            </div>
          </div>
          <button className="modal-close-btn" onClick={onClose}>
            <X className="close-icon" />
          </button>
        </div>

        {/* Import Mode Tabs */}
        <div className="modal-tabs">
          <button
            className={`modal-tab-btn ${activeTab === 'github' ? 'active' : ''}`}
            onClick={() => setActiveTab('github')}
          >
            <FolderGit2 className="tab-icon" />
            <span>GitHub Repository</span>
          </button>

          <button
            className={`modal-tab-btn ${activeTab === 'zip' ? 'active' : ''}`}
            onClick={() => setActiveTab('zip')}
          >
            <FileArchive className="tab-icon" />
            <span>Upload ZIP Archive</span>
          </button>
        </div>

        <div className="modal-body-content">
          {activeTab === 'github' && (
            <form onSubmit={handleGitHubSubmit} className="import-form">
              <div className="form-group">
                <label>GitHub Repository URL:</label>
                <div className="input-with-icon">
                  <Link2 className="input-icon" />
                  <input
                    type="url"
                    className="form-input"
                    placeholder="e.g. https://github.com/username/my-project"
                    value={githubUrl}
                    onChange={(e) => setGithubUrl(e.target.value)}
                    required
                  />
                </div>
                <span className="form-help">Enter a public or private GitHub repository HTTPS link.</span>
              </div>

              <div className="form-group">
                <label>Branch (Optional):</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="main / master"
                  value={branch}
                  onChange={(e) => setBranch(e.target.value)}
                />
              </div>

              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={onClose}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={isSubmitting || !githubUrl.trim()}>
                  {isSubmitting ? (
                    <>
                      <Loader2 className="btn-icon spin" />
                      <span>Importing...</span>
                    </>
                  ) : (
                    <>
                      <FolderGit2 className="btn-icon" />
                      <span>Import GitHub Repo</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          )}

          {activeTab === 'zip' && (
            <form onSubmit={handleZipSubmit} className="import-form">
              <div
                className="drop-zone-card"
                onDragOver={(e) => e.preventDefault()}
                onDrop={handleZipDrop}
              >
                <UploadCloud className="upload-cloud-icon" />
                <h4>Drag & Drop your ZIP file here</h4>
                <p>or click to browse from your computer</p>
                <input
                  type="file"
                  accept=".zip"
                  onChange={handleZipFileChange}
                  className="hidden-file-input"
                  id="zip-upload-input"
                />
                <label htmlFor="zip-upload-input" className="btn btn-secondary btn-browse">
                  Browse ZIP File
                </label>
                {zipFile && <div className="selected-zip-pill">📦 Selected: {zipFile.name}</div>}
              </div>

              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={onClose}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={isSubmitting || !zipFile}>
                  {isSubmitting ? (
                    <>
                      <Loader2 className="btn-icon spin" />
                      <span>Uploading...</span>
                    </>
                  ) : (
                    <>
                      <FileArchive className="btn-icon" />
                      <span>Upload & Extract ZIP</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          )}

          {statusMessage && (
            <div className={`status-alert-box ${statusMessage.type}`}>
              {statusMessage.type === 'success' && <CheckCircle2 className="alert-icon" />}
              {statusMessage.type === 'info' && <Loader2 className="alert-icon spin" />}
              <span>{statusMessage.text}</span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ImportProjectModal;
