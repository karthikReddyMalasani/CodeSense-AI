import React from 'react';
import { useEditor } from '../../context/EditorContext';
import { ShieldCheck, Cpu, Bug, ShieldAlert, FileSearch, Lightbulb } from 'lucide-react';

const AnalysisPanel = () => {
  const { analysisResults } = useEditor();

  const {
    qualityScore = 84,
    complexity = { time: 'O(n)', space: 'O(1)' },
    potentialBugsCount = 1,
    securityIssuesCount = 0,
    codeSmellsCount = 2,
    suggestions = []
  } = analysisResults;

  return (
    <div className="analysis-panel">
      {/* Metric Cards Header */}
      <div className="analysis-metrics-grid">
        <div className="metric-card quality-card">
          <div className="metric-header">
            <ShieldCheck className="metric-icon quality" />
            <span className="metric-title">Code Quality</span>
          </div>
          <div className="metric-value-box">
            <span className="metric-number">{qualityScore}</span>
            <span className="metric-total">/ 100</span>
          </div>
        </div>

        <div className="metric-card complexity-card">
          <div className="metric-header">
            <Cpu className="metric-icon complexity" />
            <span className="metric-title">Complexity</span>
          </div>
          <div className="metric-subvalues">
            <div>
              <span className="sub-label">Time:</span>
              <span className="sub-val">{complexity.time}</span>
            </div>
            <div>
              <span className="sub-label">Space:</span>
              <span className="sub-val">{complexity.space}</span>
            </div>
          </div>
        </div>

        <div className="metric-card bug-card">
          <div className="metric-header">
            <Bug className="metric-icon bug" />
            <span className="metric-title">Potential Bugs</span>
          </div>
          <div className="metric-value-box">
            <span className="metric-number bug">{potentialBugsCount}</span>
          </div>
        </div>

        <div className="metric-card security-card">
          <div className="metric-header">
            <ShieldAlert className="metric-icon security" />
            <span className="metric-title">Security Issues</span>
          </div>
          <div className="metric-value-box">
            <span className="metric-number security">{securityIssuesCount}</span>
          </div>
        </div>

        <div className="metric-card smell-card">
          <div className="metric-header">
            <FileSearch className="metric-icon smell" />
            <span className="metric-title">Code Smells</span>
          </div>
          <div className="metric-value-box">
            <span className="metric-number smell">{codeSmellsCount}</span>
          </div>
        </div>
      </div>

      {/* Recommendations & Suggestions */}
      <div className="suggestions-section">
        <div className="section-title">
          <Lightbulb className="section-icon" />
          <span>AI Recommendations & Improvements</span>
        </div>
        <ul className="suggestions-list">
          {suggestions.map((sug, idx) => (
            <li key={idx} className="suggestion-item">
              <span className="bullet">•</span>
              <span className="sug-text">{sug}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
};

export default AnalysisPanel;
