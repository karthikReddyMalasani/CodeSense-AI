import React, { useState } from 'react';
import Sidebar from '../components/Sidebar/Sidebar';
import FileExplorer from '../components/FileExplorer/FileExplorer';
import CodeEditor from '../components/CodeEditor/CodeEditor';
import OutputPanel from '../components/OutputPanel/OutputPanel';
import AIAssistant from '../components/AIAssistant/AIAssistant';
import StatusBar from '../components/StatusBar/StatusBar';
import { useEditor } from '../context/EditorContext';
import { useProject } from '../context/ProjectContext';
import { useAI } from '../context/AIContext';
import { useEffect } from 'react';

const Workspace = () => {
  const [sidebarTab, setSidebarTab] = useState('explorer');
  const { handleRunCode, handleAnalyzeCode } = useEditor();
  const { activeFile } = useProject();
  const { setIsAIPanelOpen } = useAI();

  useEffect(() => {
    const handleKeyDown = (e) => {
      // Ctrl + Enter to Run
      if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        e.preventDefault();
        if (activeFile) handleRunCode(activeFile.content, activeFile.name);
      }
      
      // Ctrl + Shift + A to Analyze
      if ((e.ctrlKey || e.metaKey) && e.shiftKey && e.key.toLowerCase() === 'a') {
        e.preventDefault();
        if (activeFile) handleAnalyzeCode(activeFile.content, activeFile.name);
      }
      
      // Ctrl + I to Toggle AI Assistant
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'i') {
        e.preventDefault();
        setIsAIPanelOpen(prev => !prev);
      }

      // Ctrl + P to focus search
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'p') {
        e.preventDefault();
        setSidebarTab('explorer');
        const searchInput = document.querySelector('.search-input');
        if (searchInput) searchInput.focus();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [activeFile, handleRunCode, handleAnalyzeCode, setIsAIPanelOpen]);

  return (
    <div className="workspace-container">
      <div className="workspace-body">
        {/* Left Icon Rail */}
        <Sidebar activeTab={sidebarTab} setActiveTab={setSidebarTab} />

        {/* File Explorer Panel */}
        {sidebarTab === 'explorer' && <FileExplorer />}

        {/* Main Center Editor & Output Area */}
        <main className="editor-main-area">
          <CodeEditor />
          <OutputPanel />
        </main>

        {/* Right AI Drawer */}
        <AIAssistant />
      </div>

      {/* Footer Status Bar */}
      <StatusBar />
    </div>
  );
};

export default Workspace;
