import React, { useState } from 'react';
import Editor from '@monaco-editor/react';
import { useNavigate } from 'react-router-dom';
import { useTheme } from '../context/ThemeContext';
import { useEditor } from '../context/EditorContext';
import { useProject } from '../context/ProjectContext';
import { aiService } from '../services/aiService';
import { SUPPORTED_LANGUAGES } from '../utils/languageConfig';
import {
  Wand2,
  Copy,
  ArrowRight,
  Play,
  Sparkles,
  Check,
  Code2,
  FileCode
} from 'lucide-react';

const Generator = () => {
  const { theme } = useTheme();
  const { setActiveLanguage, handleRunCode } = useEditor();
  const { createNewFile } = useProject();
  const navigate = useNavigate();

  const [prompt, setPrompt] = useState('Create a program to find the second largest element in an array.');
  const [language, setLanguage] = useState('java');
  const [difficulty, setDifficulty] = useState('Medium');
  const [includeExplanation, setIncludeExplanation] = useState(true);
  const [includeTestCases, setIncludeTestCases] = useState(true);
  const [isGenerating, setIsGenerating] = useState(false);
  const [copied, setCopied] = useState(false);

  const [generatedResult, setGeneratedResult] = useState({
    code: `public class Solution {
    public static int findSecondLargest(int[] arr) {
        if (arr == null || arr.length < 2) {
            throw new IllegalArgumentException("Array must contain at least 2 elements");
        }
        int largest = Integer.MIN_VALUE;
        int secondLargest = Integer.MIN_VALUE;
        for (int num : arr) {
            if (num > largest) {
                secondLargest = largest;
                largest = num;
            } else if (num > secondLargest && num != largest) {
                secondLargest = num;
            }
        }
        return secondLargest;
    }

    public static void main(String[] args) {
        int[] numbers = {12, 35, 1, 10, 34, 1};
        System.out.println("Second largest element: " + findSecondLargest(numbers));
    }
}`,
    explanation: 'Traverses the input array in O(n) linear time while tracking the top two maximum values without extra memory allocation.',
    testCases: [
      { input: '[12, 35, 1, 10, 34, 1]', expected: '34' },
      { input: '[10, 5]', expected: '5' }
    ]
  });

  const handleGenerate = async (e) => {
    e.preventDefault();
    if (!prompt.trim()) return;

    setIsGenerating(true);
    const response = await aiService.generateCode(prompt, language, {
      difficulty,
      includeExplanation,
      includeTestCases
    });

    if (response.generatedCode) {
      setGeneratedResult({
        code: response.generatedCode,
        explanation: response.explanation || '',
        testCases: response.testCases || []
      });
    }
    setIsGenerating(false);
  };

  const handleCopy = () => {
    navigator.clipboard.writeText(generatedResult.code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleInsertIntoWorkspace = () => {
    const ext = language === 'java' ? '.java' : language === 'python' ? '.py' : '.js';
    const fileName = `GeneratedSolution${ext}`;
    createNewFile(fileName, language);
    setActiveLanguage(language);
    navigate('/workspace');
  };

  return (
    <div className="generator-page-container">
      <div className="generator-header">
        <div className="title-box">
          <Wand2 className="header-icon glow" />
          <div>
            <h2>AI Code Generator</h2>
            <p>Transform natural language descriptions into verified source code.</p>
          </div>
        </div>
      </div>

      <div className="generator-split-view">
        {/* Left Options & Prompt Form */}
        <div className="generator-controls-card">
          <form onSubmit={handleGenerate}>
            <div className="form-group">
              <label>Describe what you want to build:</label>
              <textarea
                className="prompt-textarea"
                rows="4"
                placeholder="e.g. Create a Java program to find the second largest element in an array."
                value={prompt}
                onChange={(e) => setPrompt(e.target.value)}
                required
              ></textarea>
            </div>

            <div className="options-grid">
              <div className="form-group">
                <label>Language:</label>
                <select
                  className="form-select"
                  value={language}
                  onChange={(e) => setLanguage(e.target.value)}
                >
                  {SUPPORTED_LANGUAGES.map((l) => (
                    <option key={l.id} value={l.id}>
                      {l.icon} {l.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Difficulty:</label>
                <select
                  className="form-select"
                  value={difficulty}
                  onChange={(e) => setDifficulty(e.target.value)}
                >
                  <option value="Easy">Easy</option>
                  <option value="Medium">Medium</option>
                  <option value="Hard">Hard</option>
                </select>
              </div>
            </div>

            <div className="checkboxes-group">
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={includeExplanation}
                  onChange={(e) => setIncludeExplanation(e.target.checked)}
                />
                <span>Include Explanation</span>
              </label>

              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={includeTestCases}
                  onChange={(e) => setIncludeTestCases(e.target.checked)}
                />
                <span>Include Test Cases</span>
              </label>
            </div>

            <button type="submit" className="btn btn-primary btn-generate-submit" disabled={isGenerating}>
              <Sparkles className={`btn-icon ${isGenerating ? 'spin' : 'glow'}`} />
              <span>{isGenerating ? 'Generating Code...' : 'Generate Code'}</span>
            </button>
          </form>
        </div>

        {/* Right Output Preview & Monaco Editor */}
        <div className="generator-output-card">
          <div className="output-toolbar">
            <span className="output-title">Generated Output</span>
            <div className="output-actions">
              <button className="btn-small btn-secondary" onClick={handleCopy}>
                {copied ? <Check className="btn-icon text-success" /> : <Copy className="btn-icon" />}
                <span>{copied ? 'Copied!' : 'Copy'}</span>
              </button>

              <button className="btn-small btn-primary" onClick={handleInsertIntoWorkspace}>
                <FileCode className="btn-icon" />
                <span>Insert into Workspace</span>
              </button>
            </div>
          </div>

          <div className="monaco-preview-wrapper">
            <Editor
              height="350px"
              language={language}
              value={generatedResult.code}
              theme={theme === 'dark' ? 'vs-dark' : 'light'}
              options={{
                readOnly: true,
                fontSize: 13,
                fontFamily: "'Fira Code', Consolas, monospace",
                minimap: { enabled: false }
              }}
            />
          </div>

          {includeExplanation && generatedResult.explanation && (
            <div className="generated-explanation-box">
              <h4>Overview & Logic:</h4>
              <p>{generatedResult.explanation}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Generator;
