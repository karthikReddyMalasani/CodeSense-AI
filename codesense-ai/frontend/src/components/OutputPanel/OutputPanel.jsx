import React from 'react';
import { useEditor } from '../../context/EditorContext';
import { useAI } from '../../context/AIContext';
import ProblemsPanel from '../ProblemsPanel/ProblemsPanel';
import TestCasePanel from '../TestCasePanel/TestCasePanel';
import AnalysisPanel from '../AnalysisPanel/AnalysisPanel';
import {
  Terminal as TerminalIcon,
  AlertCircle,
  CheckCircle2,
  Cpu,
  Clock,
  Sparkles,
  ChevronDown,
  ChevronUp,
  Maximize2
} from 'lucide-react';

const OutputPanel = () => {
  const {
    isBottomPanelOpen,
    setIsBottomPanelOpen,
    activeBottomTab,
    setActiveBottomTab,
    output,
    isRunning,
    analysisResults
  } = useEditor();

  const { executeQuickAction } = useAI();

  if (!isBottomPanelOpen) {
    return (
      <div className="bottom-panel-collapsed-bar" onClick={() => setIsBottomPanelOpen(true)}>
        <div className="bar-left">
          <TerminalIcon className="bar-icon" />
          <span>Output & Terminal (Click to Expand)</span>
        </div>
        <ChevronUp className="chevron-icon" />
      </div>
    );
  }

  const problemCount = (analysisResults?.problems || []).length;

  return (
    <div className="bottom-output-drawer">
      {/* Drawer Header Tabs */}
      <div className="drawer-header">
        <div className="tab-buttons">
          <button
            className={`tab-btn ${activeBottomTab === 'Problems' ? 'active' : ''}`}
            onClick={() => setActiveBottomTab('Problems')}
          >
            <span>Problems</span>
            {problemCount > 0 && <span className="tab-count">{problemCount}</span>}
          </button>

          <button
            className={`tab-btn ${activeBottomTab === 'Output' ? 'active' : ''}`}
            onClick={() => setActiveBottomTab('Output')}
          >
            <span>Output</span>
          </button>

          <button
            className={`tab-btn ${activeBottomTab === 'Test Cases' ? 'active' : ''}`}
            onClick={() => setActiveBottomTab('Test Cases')}
          >
            <span>Test Cases</span>
          </button>

          <button
            className={`tab-btn ${activeBottomTab === 'Analysis' ? 'active' : ''}`}
            onClick={() => setActiveBottomTab('Analysis')}
          >
            <span>Analysis</span>
          </button>

          <button
            className={`tab-btn ${activeBottomTab === 'Terminal' ? 'active' : ''}`}
            onClick={() => setActiveBottomTab('Terminal')}
          >
            <span>Terminal</span>
          </button>
        </div>

        <div className="drawer-controls">
          <button className="icon-control-btn" onClick={() => setIsBottomPanelOpen(false)} title="Minimize Drawer">
            <ChevronDown className="control-icon" />
          </button>
        </div>
      </div>

      {/* Drawer Content */}
      <div className="drawer-content-body">
        {activeBottomTab === 'Problems' && <ProblemsPanel />}
        {activeBottomTab === 'Test Cases' && <TestCasePanel />}
        {activeBottomTab === 'Analysis' && <AnalysisPanel />}

        {activeBottomTab === 'Output' && (
          <div className="output-tab-content">
            {isRunning ? (
              <div className="output-loading-state">
                <div className="spinner"></div>
                <p>Executing program on CodeAssist AI server environment...</p>
              </div>
            ) : output ? (
              <div className="output-result-box">
                <div className="output-meta-bar">
                  <span className={`status-tag ${output.success ? 'success' : 'failed'}`}>
                    {output.success ? <CheckCircle2 className="meta-icon" /> : <AlertCircle className="meta-icon" />}
                    {output.status || (output.success ? 'Success' : 'Failed')}
                  </span>
                  <span className="meta-info">
                    <Clock className="meta-icon" /> Time: {output.executionTimeMs || 42} ms
                  </span>
                  <span className="meta-info">
                    <Cpu className="meta-icon" /> Memory: {output.memoryMb || 18} MB
                  </span>
                </div>

                <pre className="console-output">{output.output}</pre>

                {!output.success && (
                  <div className="error-explain-banner">
                    <AlertCircle className="err-banner-icon" />
                    <span>Execution encountered runtime exception.</span>
                    <button
                      className="btn-explain-err"
                      onClick={() => executeQuickAction('debug', output.output)}
                    >
                      <Sparkles className="btn-icon glow" />
                      <span>Explain Error with AI</span>
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <div className="output-empty-state">
                <p>Click <strong>Run ▶</strong> above to execute code and view output here.</p>
              </div>
            )}
          </div>
        )}

        {activeBottomTab === 'Terminal' && (
          <div className="terminal-tab-content">
            <div className="terminal-screen">
              <p className="term-line prompt">CodeAssist AI Environment v1.0.0 (x86_64-pc-windows)</p>
              <p className="term-line prompt">Type 'help' or execute commands using top toolbar.</p>
              <p className="term-line output">$ java -version</p>
              <p className="term-line response">openjdk version "23" 2024-09-17</p>
              <p className="term-line output">$ ready _</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default OutputPanel;
