import { useEffect, useState } from 'react';
import { useParams, useLocation } from 'react-router-dom';
import { repositoryApi } from '../services/api';
import ProjectSubNav from '../components/common/ProjectSubNav';
import TreeFileExplorer from '../components/FileExplorer/TreeFileExplorer';

export default function RepositoryPage() {
  const { id: projectId } = useParams();
  const location = useLocation();
  const [repositories, setRepositories] = useState([]);
  const [selectedRepo, setSelectedRepo] = useState(null);
  const [files, setFiles] = useState([]);
  const [selectedFile, setSelectedFile] = useState(null);
  const [fileContent, setFileContent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadingFile, setLoadingFile] = useState(false);

  useEffect(() => {
    repositoryApi.list(projectId).then(res => {
      const repos = res.data.data || [];
      setRepositories(repos);
      const preSelected = location.state?.repoId
        ? repos.find(r => r.id === location.state.repoId)
        : repos[0];
      if (preSelected) selectRepo(preSelected);
      else setLoading(false);
    }).catch(() => setLoading(false));
  }, [projectId]);

  const selectRepo = async (repo) => {
    setSelectedRepo(repo);
    setSelectedFile(null);
    setFileContent(null);
    setLoading(true);
    try {
      const res = await repositoryApi.files(repo.id);
      const fileList = res.data.data || res.data || [];
      setFiles(fileList);
      if (fileList.length > 0) {
        const defaultFile = fileList.find(f => !f.binary) || fileList[0];
        openFile(defaultFile, repo);
      }
    } catch (err) {
      console.error('Failed to load files for repo:', err);
      // Fallback mock files for demonstration if backend is unreachable
      const mockFiles = [
        { id: 'f-1', filePath: 'src/Main.java', fileName: 'Main.java', language: 'Java', lineCount: 18, sizeBytes: 512, content: 'package com.codesense;\n\npublic class Main {\n    public static void main(String[] args) {\n        System.out.println("Welcome to CodeSense AI Platform");\n        AnalysisEngine engine = new AnalysisEngine();\n        engine.startAnalysis();\n    }\n}' },
        { id: 'f-2', filePath: 'src/AnalysisEngine.java', fileName: 'AnalysisEngine.java', language: 'Java', lineCount: 22, sizeBytes: 768, content: 'package com.codesense;\n\npublic class AnalysisEngine {\n    public void startAnalysis() {\n        System.out.println("Indexing repository AST nodes...");\n        System.out.println("Generating vector embeddings...");\n        System.out.println("Code analysis completed successfully.");\n    }\n}' },
        { id: 'f-3', filePath: 'pom.xml', fileName: 'pom.xml', language: 'XML', lineCount: 30, sizeBytes: 1200, content: '<?xml version="1.0" encoding="UTF-8"?>\n<project xmlns="http://maven.apache.org/POM/4.0.0">\n    <modelVersion>4.0.0</modelVersion>\n    <groupId>com.codesense</groupId>\n    <artifactId>codesense-ai</artifactId>\n    <version>1.0.0</version>\n</project>' }
      ];
      setFiles(mockFiles);
      if (mockFiles.length > 0) {
        openFile(mockFiles[0], repo);
      }
    } finally {
      setLoading(false);
    }
  };

  const openFile = async (file, repoOverride) => {
    setSelectedFile(file);
    if (file.binary) {
      setFileContent('[Binary File — Cannot preview binary contents]');
      return;
    }

    // Use content if already attached to file object
    if (file.content !== undefined && file.content !== null) {
      setFileContent(file.content);
      return;
    }

    setLoadingFile(true);
    setFileContent(null);
    const targetRepoId = repoOverride?.id || selectedRepo?.id;
    try {
      const res = await repositoryApi.file(targetRepoId, file.id);
      const fetchedContent = res.data?.data?.content ?? res.data?.content ?? '';
      setFileContent(fetchedContent);
    } catch (err) {
      console.error('Failed to load file content:', err);
      setFileContent(file.content || '// Content unavailable or empty file.');
    } finally {
      setLoadingFile(false);
    }
  };

  const copyToClipboard = () => {
    if (fileContent) {
      navigator.clipboard.writeText(fileContent);
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <div className="page-title">Repository Browser</div>
          <div className="page-subtitle">
            {selectedRepo ? `${selectedRepo.name} — ${files.length} files` : 'Select a repository'}
          </div>
        </div>
      </div>

      {selectedRepo && (
        <div className="card" style={{ marginBottom: '18px', padding: '16px 18px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '10px' }}>
            <div>
              <div style={{ fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--text-muted)' }}>Repository metadata</div>
              <div style={{ fontWeight: '700', marginTop: '4px', fontSize: '18px' }}>{selectedRepo.name}</div>
            </div>
            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
              <span className="badge badge-blue">{selectedRepo.sourceType || 'ZIP'}</span>
              <span className="badge badge-gray">{selectedRepo.primaryLanguage || 'Multi-language'}</span>
              <span className="badge badge-green">{selectedRepo.status || 'READY'}</span>
            </div>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: '12px', marginTop: '14px' }}>
            {[
              ['Files', selectedRepo.totalFiles ?? files.length],
              ['Chunks', selectedRepo.totalChunks ?? 0],
              ['Ingestion', selectedRepo.ingestionStatus || 'PENDING'],
              ['Language', selectedRepo.primaryLanguage || 'Multi-language']
            ].map(([label, value]) => (
              <div key={label} style={{ background: 'var(--bg-hover)', borderRadius: '10px', padding: '10px 12px' }}>
                <div style={{ fontSize: '11px', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>{label}</div>
                <div style={{ fontWeight: '700', fontSize: '18px', marginTop: '6px' }}>{value}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      <ProjectSubNav activeTab="repository" />

      {/* Repo Selector */}
      {repositories.length > 1 && (
        <div style={{ display: 'flex', gap: '8px', marginBottom: '20px', flexWrap: 'wrap' }}>
          {repositories.map(r => (
            <button key={r.id}
              className={`btn ${selectedRepo?.id === r.id ? 'btn-primary' : 'btn-secondary'} btn-sm`}
              onClick={() => selectRepo(r)}>
              {r.name}
            </button>
          ))}
        </div>
      )}

      {loading ? (
        <div className="loading-center"><div className="spinner" /></div>
      ) : !selectedRepo ? (
        <div className="empty-state"><h3>No repositories</h3><p>Upload or import a repository first.</p></div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: '320px 1fr', gap: '16px', minHeight: '650px' }}>
          {/* VS Code Style File Tree */}
          <div className="card" style={{ padding: '0', overflow: 'hidden', height: '700px' }}>
            <TreeFileExplorer
              files={files}
              rootName={selectedRepo.name}
              selectedFile={selectedFile}
              onFileSelect={openFile}
            />
          </div>

          {/* File content */}
          <div className="card" style={{ padding: '0', overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
            {!selectedFile ? (
              <div className="empty-state" style={{ padding: '40px' }}>
                <h3>Select a file</h3>
                <p>Click a file in the tree to view its contents.</p>
              </div>
            ) : loadingFile ? (
              <div className="loading-center" style={{ height: '400px' }}><div className="spinner" /></div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', height: '100%', minHeight: '600px' }}>
                {/* Header Toolbar */}
                <div style={{
                  padding: '12px 16px', borderBottom: '1px solid var(--border)',
                  display: 'flex', alignItems: 'center', gap: '10px', background: 'var(--bg-card)'
                }}>
                  <span style={{ fontFamily: 'var(--font-mono)', fontSize: '13px', fontWeight: '600', color: 'var(--primary-light)' }}>
                    {selectedFile.filePath}
                  </span>
                  {selectedFile.language && (
                    <span className="badge badge-blue">{selectedFile.language}</span>
                  )}
                  <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                      {selectedFile.lineCount || (fileContent ? fileContent.split('\n').length : 0)} lines
                      {selectedFile.sizeBytes ? ` · ${(selectedFile.sizeBytes / 1024).toFixed(1)} KB` : ''}
                    </span>
                    <button
                      className="btn btn-secondary btn-sm"
                      onClick={copyToClipboard}
                      style={{ padding: '2px 8px', fontSize: '11px' }}
                    >
                      📋 Copy Code
                    </button>
                  </div>
                </div>

                {/* Code Viewer Container */}
                <div style={{ flex: 1, overflow: 'auto', background: 'var(--cs-input-bg)', padding: '16px' }}>
                  <pre style={{
                    margin: 0,
                    fontSize: '13px',
                    fontFamily: 'var(--font-mono)',
                    lineHeight: '1.6',
                    color: 'var(--cs-text-main) !important',
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-word',
                    backgroundColor: 'var(--cs-input-bg) !important'
                  }}>
                    <code style={{ color: 'inherit', backgroundColor: 'transparent' }}>
                      {fileContent || '(empty file)'}
                    </code>
                  </pre>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
