import React, { useState } from 'react';
import { useEditor } from '../context/EditorContext';
import { useProject } from '../context/ProjectContext';
import { aiService } from '../services/aiService';
import { SUPPORTED_LANGUAGES } from '../utils/languageConfig';
import {
  Search,
  Bug,
  Lightbulb,
  CheckCircle2,
  AlertTriangle,
  Zap,
  Code2,
  Copy,
  Check,
  ArrowRight
} from 'lucide-react';

const Analyzer = () => {
  const { activeLanguage } = useEditor();
  const { activeFile, updateFileContent } = useProject();

  const [codeToAnalyze, setCodeToAnalyze] = useState(
    activeFile?.content || `public class Main {
    public static void main(String[] args) {
        int[] numbers = {10, 20, 30};
        // Line 5: Potential out of bounds access
        for (int i = 0; i <= numbers.length; i++) {
            System.out.println("Element: " + numbers[i]);
        }
    }
}`
  );
  const [selectedLang, setSelectedLang] = useState(activeLanguage || 'java');
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [applied, setApplied] = useState(false);

  const [analysisResult, setAnalysisResult] = useState({
    explanation: {
      overview: 'This Java program iterates over an array of integers and prints each element to standard output.',
      steps: [
        '1. Accepts array containing 3 integer values: [10, 20, 30].',
        '2. Enters a for loop with index variable i initialized at 0.',
        '3. Evaluates array boundary condition on each loop step.',
        '4. Prints array element value to console.'
      ],
      complexity: { time: 'O(n)', space: 'O(1)' },
      suggestions: [
        'Change loop condition from i <= numbers.length to i < numbers.length to prevent ArrayIndexOutOfBoundsException.',
        'Use enhanced for-each loop for safer iteration.'
      ]
    },
    debug: {
      issueType: 'ArrayIndexOutOfBoundsException',
      location: 'Line 5',
      problem: 'Array index i reaches 3, which equals numbers.length. Maximum index is 2.',
      why: 'Zero-indexed arrays have valid indices from 0 to length - 1. Loop check <= causes index 3 access.',
      suggestedFix: `public class Main {
    public static void main(String[] args) {
        int[] numbers = {10, 20, 30};
        // Fixed: i < numbers.length
        for (int i = 0; i < numbers.length; i++) {
            System.out.println("Element: " + numbers[i]);
        }
    }
}`
    }
  });

  const handleRunAnalysis = async () => {
    setIsAnalyzing(true);
    const exp = await aiService.explainCode(codeToAnalyze, selectedLang);
    const dbg = await aiService.debugCode(codeToAnalyze, selectedLang);

    setAnalysisResult({
      explanation: {
        overview: exp.overview || 'Code analysis completed.',
        steps: exp.steps || [],
        complexity: exp.complexity || { time: 'O(n)', space: 'O(1)' },
        suggestions: exp.suggestions || []
      },
      debug: {
        issueType: dbg.detectedIssue?.type || 'Runtime Check',
        location: dbg.detectedIssue?.location || 'Line 5',
        problem: dbg.detectedIssue?.problem || 'Potential exception identified.',
        why: dbg.whyItHappens || 'Loop index out of bounds.',
        suggestedFix: dbg.refactoredCode || codeToAnalyze
      }
    });
    setIsAnalyzing(false);
  };

  const handleApplyFix = () => {
    if (activeFile) {
      updateFileContent(activeFile.id, analysisResult.debug.suggestedFix);
    }
    setApplied(true);
    setTimeout(() => setApplied(false), 2000);
  };

  return (
    <div className="analyzer-page-container">
      <div className="analyzer-header">
        <div className="title-box">
          <Search className="header-icon glow" />
          <div>
            <h2>Code Analyzer & AI Debugger</h2>
            <p>Inspect code quality, diagnose runtime errors, and review AI fix recommendations.</p>
          </div>
        </div>

        <div className="header-actions">
          <select
            className="form-select"
            value={selectedLang}
            onChange={(e) => setSelectedLang(e.target.value)}
          >
            {SUPPORTED_LANGUAGES.map((l) => (
              <option key={l.id} value={l.id}>
                {l.icon} {l.name}
              </option>
            ))}
          </select>
          <button className="btn btn-primary" onClick={handleRunAnalysis} disabled={isAnalyzing}>
            <Zap className={`btn-icon ${isAnalyzing ? 'spin' : ''}`} />
            <span>{isAnalyzing ? 'Analyzing...' : 'Run Full Analysis'}</span>
          </button>
        </div>
      </div>

      <div className="analyzer-grid">
        {/* Source Input */}
        <div className="analyzer-card input-card">
          <h3>Source Code Input</h3>
          <textarea
            className="code-input-area"
            rows="12"
            value={codeToAnalyze}
            onChange={(e) => setCodeToAnalyze(e.target.value)}
          ></textarea>
        </div>

        {/* Explanation Card */}
        <div className="analyzer-card explanation-card">
          <h3>
            <Code2 className="card-subicon" /> Overview & Logic Breakdown
          </h3>
          <p className="overview-text">{analysisResult.explanation.overview}</p>

          <h4>Step-by-Step Explanation:</h4>
          <ul className="steps-list">
            {analysisResult.explanation.steps.map((step, i) => (
              <li key={i}>{step}</li>
            ))}
          </ul>

          <div className="complexity-badge-bar">
            <span>Time Complexity: <strong>{analysisResult.explanation.complexity.time}</strong></span>
            <span>Space Complexity: <strong>{analysisResult.explanation.complexity.space}</strong></span>
          </div>
        </div>

        {/* Debug & Issue Detection Card */}
        <div className="analyzer-card debug-card">
          <h3>
            <Bug className="card-subicon bug" /> Detected Issue & Suggested Fix
          </h3>

          <div className="issue-summary-box">
            <div className="issue-row">
              <span className="label">Type:</span>
              <span className="value tag-error">{analysisResult.debug.issueType}</span>
            </div>
            <div className="issue-row">
              <span className="label">Location:</span>
              <span className="value">{analysisResult.debug.location}</span>
            </div>
            <div className="issue-row">
              <span className="label">Problem:</span>
              <span className="value">{analysisResult.debug.problem}</span>
            </div>
          </div>

          <div className="why-box">
            <h4>Why this happens:</h4>
            <p>{analysisResult.debug.why}</p>
          </div>

          <div className="fix-preview-box">
            <h4>Suggested Fix:</h4>
            <pre className="fix-code">{analysisResult.debug.suggestedFix}</pre>
            <div className="fix-actions">
              <button className="btn btn-accent" onClick={handleApplyFix}>
                {applied ? <Check className="btn-icon text-success" /> : <CheckCircle2 className="btn-icon" />}
                <span>{applied ? 'Fix Applied to Active File!' : 'Apply Fix to Workspace'}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Analyzer;
