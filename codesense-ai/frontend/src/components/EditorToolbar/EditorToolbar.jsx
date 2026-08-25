import React from 'react';
import { useEditor } from '../../context/EditorContext';
import { useProject } from '../../context/ProjectContext';
import { useAI } from '../../context/AIContext';
import { SUPPORTED_LANGUAGES } from '../../utils/languageConfig';
import { codeService } from '../../services/codeService';
import { Play, Search, AlignLeft, Save, Sparkles } from 'lucide-react';

const EditorToolbar = () => {
  const { activeLanguage, setActiveLanguage, handleRunCode, handleAnalyzeCode, isRunning, isAnalyzing } = useEditor();
  const { activeFile, updateFileContent } = useProject();
  const { toggleAIPanel } = useAI();

  const handleFormat = async () => {
    if (!activeFile) return;
    const formatted = await codeService.formatCode(activeFile.content, activeLanguage);
    updateFileContent(activeFile.id, formatted);
  };

  const handleSave = async () => {
    if (!activeFile) return;
    await codeService.saveCode({
      fileName: activeFile.name,
      content: activeFile.content,
      language: activeLanguage
    });
    alert(`File ${activeFile.name} saved successfully!`);
  };

  return (
    <div className="editor-toolbar">
      <div className="toolbar-left">
        <select
          className="toolbar-lang-select"
          value={activeLanguage}
          onChange={(e) => setActiveLanguage(e.target.value)}
        >
          {SUPPORTED_LANGUAGES.map((lang) => (
            <option key={lang.id} value={lang.id}>
              {lang.icon} {lang.name}
            </option>
          ))}
        </select>
      </div>

      <div className="toolbar-right">
        <button
          className="toolbar-btn btn-run-tool"
          onClick={() => handleRunCode(activeFile?.content || '', activeFile?.name || 'Main.java')}
          disabled={isRunning}
        >
          <Play className="toolbar-icon" />
          <span>{isRunning ? 'Running...' : 'Run ▶'}</span>
        </button>

        <button
          className="toolbar-btn btn-analyze-tool"
          onClick={() => handleAnalyzeCode(activeFile?.content || '', activeFile?.name || 'Main.java')}
          disabled={isAnalyzing}
        >
          <Search className="toolbar-icon" />
          <span>{isAnalyzing ? 'Analyzing...' : 'Analyze 🔍'}</span>
        </button>

        <button className="toolbar-btn" onClick={handleFormat} title="Format Source Code">
          <AlignLeft className="toolbar-icon" />
          <span>Format</span>
        </button>

        <button className="toolbar-btn" onClick={handleSave} title="Save (Ctrl + S)">
          <Save className="toolbar-icon" />
          <span>Save</span>
        </button>

        <button className="toolbar-btn btn-ai-tool" onClick={toggleAIPanel} title="Open AI Assistant">
          <Sparkles className="toolbar-icon glow" />
          <span>AI Assist ✨</span>
        </button>
      </div>
    </div>
  );
};

export default EditorToolbar;
