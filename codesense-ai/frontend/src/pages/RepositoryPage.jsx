import { useEffect, useState } from 'react';
import { useParams, useLocation } from 'react-router-dom';
import { repositoryApi } from '../services/api';
import ProjectSubNav from '../components/common/ProjectSubNav';

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

  const langColor = (lang) => {
    const map = {
      Java: '#b07219', Python: '#3572A5', JavaScript: '#f1e05a', TypeScript: '#2b7489',
      'C++': '#f34b7d', 'C#': '#178600', Go: '#00ADD8', Rust: '#dea584', Ruby: '#701516', XML: '#e34c26'
    };
    return map[lang] || '#94a3b8';
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
        <div style={{ display: 'grid', gridTemplateColumns: '300px 1fr', gap: '16px', minHeight: '600px' }}>
          {/* File tree */}
          <div className="card" style={{ padding: '12px', overflow: 'auto', maxHeight: '700px' }}>
            <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginBottom: '10px', fontWeight: '600', letterSpacing: '0.5px' }}>
              FILES ({files.length})
            </div>
            {files.map(f => (
              <div key={f.id || f.filePath} className="file-item" onClick={() => openFile(f)}
                style={{
                  display: 'flex', alignItems: 'center', gap: '8px', padding: '6px 8px', borderRadius: '4px',
                  cursor: 'pointer', marginBottom: '2px',
                  background: selectedFile?.filePath === f.filePath ? 'rgba(59, 130, 246, 0.15)' : 'transparent',
                  color: selectedFile?.filePath === f.filePath ? 'var(--primary-light)' : 'var(--text)'
                }}>
                <span style={{
                  width: '8px', height: '8px', borderRadius: '50%', flexShrink: 0,
                  background: langColor(f.language)
                }} />
                <span style={{ fontSize: '12px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {f.filePath}
                </span>
                {f.binary && <span className="badge badge-gray" style={{ fontSize: '9px', marginLeft: 'auto' }}>bin</span>}
              </div>
            ))}
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
                <div style={{ flex: 1, overflow: 'auto', background: '#0d1117', padding: '16px' }}>
                  <pre style={{
                    margin: 0,
                    fontSize: '13px',
                    fontFamily: 'Consolas, Monaco, "Andale Mono", "Ubuntu Mono", monospace',
                    lineHeight: '1.6',
                    color: '#e6edf3',
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-word'
                  }}>
                    {fileContent || '(empty file)'}
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
