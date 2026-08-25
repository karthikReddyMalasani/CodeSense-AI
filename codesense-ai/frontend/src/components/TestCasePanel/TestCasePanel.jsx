import React, { useState } from 'react';
import { useEditor } from '../../context/EditorContext';
import { useAI } from '../../context/AIContext';
import { aiService } from '../../services/aiService';
import { Plus, Play, CheckCircle2, XCircle, Clock, Sparkles } from 'lucide-react';

const TestCasePanel = () => {
  const { testCases, testSummary, handleRunTests, isTesting, addTestCase, setTestCases } = useEditor();
  const { toggleAIPanel } = useAI();

  const [showAddForm, setShowAddForm] = useState(false);
  const [inputVal, setInputVal] = useState('');
  const [expectedVal, setExpectedVal] = useState('');

  const handleAddSubmit = (e) => {
    e.preventDefault();
    if (inputVal && expectedVal) {
      addTestCase(inputVal, expectedVal);
      setInputVal('');
      setExpectedVal('');
      setShowAddForm(false);
    }
  };

  const handleGenerateTestsWithAI = async () => {
    const response = await aiService.generateTests('');
    if (response.testCases) {
      setTestCases((prev) => [...prev, ...response.testCases]);
    }
  };

  return (
    <div className="test-case-panel">
      <div className="test-toolbar">
        <div className="test-summary-badge">
          <span className="summary-item">Total: {testSummary.total}</span>
          <span className="summary-item passed">Passed: {testSummary.passed}</span>
          <span className="summary-item failed">Failed: {testSummary.failed}</span>
          <span className="summary-rate">Pass Rate: {testSummary.passRate}%</span>
        </div>

        <div className="test-actions">
          <button className="btn-test-action btn-add-tc" onClick={() => setShowAddForm(!showAddForm)}>
            <Plus className="tc-icon" />
            <span>Add Test Case</span>
          </button>

          <button className="btn-test-action btn-run-tc" onClick={() => handleRunTests()} disabled={isTesting}>
            <Play className={`tc-icon ${isTesting ? 'spin' : ''}`} />
            <span>{isTesting ? 'Running...' : 'Run All Tests'}</span>
          </button>

          <button className="btn-test-action btn-ai-tc" onClick={handleGenerateTestsWithAI}>
            <Sparkles className="tc-icon glow" />
            <span>Generate Tests with AI</span>
          </button>
        </div>
      </div>

      {showAddForm && (
        <form onSubmit={handleAddSubmit} className="add-tc-form">
          <div className="input-field-group">
            <label>Input:</label>
            <input
              type="text"
              placeholder="e.g. 5"
              value={inputVal}
              onChange={(e) => setInputVal(e.target.value)}
              required
            />
          </div>
          <div className="input-field-group">
            <label>Expected Output:</label>
            <input
              type="text"
              placeholder="e.g. 120"
              value={expectedVal}
              onChange={(e) => setExpectedVal(e.target.value)}
              required
            />
          </div>
          <button type="submit" className="btn btn-primary btn-save-tc">
            Save Test Case
          </button>
        </form>
      )}

      <div className="test-cases-grid">
        {testCases.map((tc, idx) => (
          <div key={tc.id || idx} className={`tc-card status-${tc.status.toLowerCase()}`}>
            <div className="tc-card-header">
              <span className="tc-title">Test Case #{idx + 1}</span>
              <span className={`tc-status-pill ${tc.status.toLowerCase()}`}>
                {tc.status === 'PASSED' && <CheckCircle2 className="status-icon" />}
                {tc.status === 'FAILED' && <XCircle className="status-icon" />}
                {tc.status === 'PENDING' && <Clock className="status-icon" />}
                {tc.status}
              </span>
            </div>

            <div className="tc-card-body">
              <div className="tc-row">
                <span className="tc-label">Input:</span>
                <code className="tc-code">{tc.input}</code>
              </div>
              <div className="tc-row">
                <span className="tc-label">Expected:</span>
                <code className="tc-code">{tc.expected}</code>
              </div>
              <div className="tc-row">
                <span className="tc-label">Actual:</span>
                <code className="tc-code">{tc.actual}</code>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default TestCasePanel;
