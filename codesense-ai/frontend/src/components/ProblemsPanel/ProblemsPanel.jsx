import React from 'react';
import { useEditor } from '../../context/EditorContext';
import { useAI } from '../../context/AIContext';
import { AlertCircle, AlertTriangle, Info, Sparkles, ExternalLink } from 'lucide-react';

const ProblemsPanel = () => {
  const { analysisResults } = useEditor();
  const { executeQuickAction } = useAI();

  const problems = analysisResults.problems || [];

  return (
    <div className="problems-panel">
      {problems.length === 0 ? (
        <div className="empty-problems-state">
          <p>✓ No syntax or logical problems detected in your workspace.</p>
        </div>
      ) : (
        <div className="problems-list">
          {problems.map((prob, idx) => (
            <div key={idx} className={`problem-card severity-${prob.type}`}>
              <div className="problem-header">
                <div className="problem-badge">
                  {prob.type === 'error' && <AlertCircle className="prob-icon error" />}
                  {prob.type === 'warning' && <AlertTriangle className="prob-icon warning" />}
                  {prob.type === 'info' && <Info className="prob-icon info" />}
                  <span className="prob-type">{prob.type.toUpperCase()}</span>
                </div>
                <span className="prob-location">
                  {prob.file}:{prob.line}
                </span>
              </div>

              <div className="problem-body">
                <h4 className="problem-title">{prob.title}</h4>
                <p className="problem-message">{prob.message}</p>
              </div>

              <div className="problem-actions">
                <button className="btn-problem btn-view-code">
                  <ExternalLink className="prob-action-icon" />
                  <span>View Code</span>
                </button>
                <button
                  className="btn-problem btn-ask-ai"
                  onClick={() => executeQuickAction('debug', `Problem at ${prob.file}:${prob.line}: ${prob.message}`)}
                >
                  <Sparkles className="prob-action-icon glow" />
                  <span>Ask AI</span>
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default ProblemsPanel;
