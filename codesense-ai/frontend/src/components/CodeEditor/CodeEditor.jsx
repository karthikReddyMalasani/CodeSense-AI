import React, { useRef, useEffect } from 'react';
import Editor from '@monaco-editor/react';
import { useProject } from '../../context/ProjectContext';
import { useEditor } from '../../context/EditorContext';
import { useTheme } from '../../context/ThemeContext';
import { useAI } from '../../context/AIContext';
import EditorToolbar from '../EditorToolbar/EditorToolbar';
import { X, FileCode2 } from 'lucide-react';

const CodeEditor = () => {
  const { openFiles, activeFileId, activeFile, selectFile, closeFileTab, updateFileContent, unsavedFiles, saveFile } = useProject();
  const { activeLanguage, handleRunCode, handleAnalyzeCode, editorPreferences } = useEditor();
  const { theme } = useTheme();
  const { toggleAIPanel } = useAI();
  const editorRef = useRef(null);

  const handleEditorMount = (editor) => {
    editorRef.current = editor;
  };

  // Keyboard Shortcuts Handler
  useEffect(() => {
    const handleKeyDown = (e) => {
      // Ctrl + S
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
        e.preventDefault();
        if (activeFile) {
          saveFile(activeFile.id);
        }
      }
      // Ctrl + Enter
      if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        e.preventDefault();
        handleRunCode(activeFile?.content || '', activeFile?.name || 'Main.java');
      }
      // Ctrl + Shift + A
      if ((e.ctrlKey || e.metaKey) && e.shiftKey && e.key.toLowerCase() === 'a') {
        e.preventDefault();
        handleAnalyzeCode(activeFile?.content || '', activeFile?.name || 'Main.java');
      }
      // Ctrl + I
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'i') {
        e.preventDefault();
        toggleAIPanel();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [activeFile, handleRunCode, handleAnalyzeCode, toggleAIPanel]);

  const getMonacoLanguage = (file) => {
    if (!file) return activeLanguage;
    const ext = file.name.substring(file.name.lastIndexOf('.'));
    if (ext === '.java') return 'java';
    if (ext === '.py') return 'python';
    if (ext === '.js') return 'javascript';
    if (ext === '.ts') return 'typescript';
    if (ext === '.cpp' || ext === '.c') return 'cpp';
    if (ext === '.cs') return 'csharp';
    if (ext === '.go') return 'go';
    if (ext === '.rs') return 'rust';
    if (ext === '.json') return 'json';
    if (ext === '.md') return 'markdown';
    return activeLanguage;
  };

  return (
    <div className="code-editor-container">
      {/* Editor Tabs Header */}
      <div className="editor-tabs-bar">
        <div className="tabs-list">
          {openFiles.map((file) => (
            <div
              key={file.id}
              className={`editor-tab ${activeFileId === file.id ? 'active' : ''} ${unsavedFiles.has(file.id) ? 'unsaved' : ''}`}
              onClick={() => selectFile(file)}
            >
              <FileCode2 className="tab-icon" />
              <span className="tab-title">
                {file.name} {unsavedFiles.has(file.id) && <span className="unsaved-dot">●</span>}
              </span>
              <button
                className="tab-close-btn"
                onClick={(e) => closeFileTab(file.id, e)}
                title="Close Tab"
              >
                <X className="close-icon" />
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* Editor Toolbar */}
      <EditorToolbar />

      {/* Monaco Code Editor */}
      <div className="monaco-wrapper">
        {activeFile ? (
          <Editor
            height="100%"
            language={getMonacoLanguage(activeFile)}
            value={activeFile.content}
            theme={theme === 'dark' ? 'vs-dark' : 'light'}
            onChange={(newContent) => updateFileContent(activeFile.id, newContent || '')}
            onMount={handleEditorMount}
            options={{
              fontSize: parseInt(editorPreferences.fontSize) || 14,
              fontFamily: "'Fira Code', 'Cascadia Code', Consolas, monospace",
              minimap: { enabled: editorPreferences.minimap },
              scrollBeyondLastLine: false,
              automaticLayout: true,
              tabSize: parseInt(editorPreferences.tabSize) || 4,
              wordWrap: editorPreferences.wordWrap ? 'on' : 'off',
              bracketPairColorization: { enabled: true },
              formatOnPaste: true,
              formatOnType: true,
              smoothScrolling: true,
              cursorBlinking: 'smooth',
              folding: true
            }}
          />
        ) : (
          <div className="empty-editor-state">
            <p>No file selected. Pick a file from the explorer to begin editing.</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default CodeEditor;
