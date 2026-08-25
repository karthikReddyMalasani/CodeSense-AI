import React, { createContext, useContext, useState } from 'react';
import { getLanguageConfig } from '../utils/languageConfig';
import { codeService } from '../services/codeService';

const EditorContext = createContext();

export const EditorProvider = ({ children }) => {
  const [activeLanguage, setActiveLanguage] = useState('java');
  const [isBottomPanelOpen, setIsBottomPanelOpen] = useState(true);
  const [activeBottomTab, setActiveBottomTab] = useState('Output'); // Problems | Output | Test Cases | Analysis | Terminal
  
  const [isRunning, setIsRunning] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [isTesting, setIsTesting] = useState(false);
  const [isDemoMode, setIsDemoMode] = useState(true);
  
  const [editorPreferences, setEditorPreferences] = useState({
    fontSize: '14',
    tabSize: '4',
    minimap: true,
    wordWrap: true,
    autoSave: false
  });

  const [output, setOutput] = useState(null);
  const [analysisResults, setAnalysisResults] = useState({
    qualityScore: 84,
    complexity: { time: 'O(n)', space: 'O(1)' },
    potentialBugsCount: 1,
    securityIssuesCount: 0,
    codeSmellsCount: 2,
    problems: [
      {
        type: 'error',
        file: 'Main.java',
        line: 15,
        title: 'NullPointerException Warning',
        message: "Variable 'user' may be null before method invocation.",
        severity: 'high'
      },
      {
        type: 'warning',
        file: 'Main.java',
        line: 27,
        title: 'Unused Variable',
        message: "Unused local variable 'count'. Consider removing it.",
        severity: 'medium'
      },
      {
        type: 'info',
        file: 'Main.java',
        line: 8,
        title: 'Complexity Alert',
        message: 'Linear search detected. Time complexity is O(n).',
        severity: 'low'
      }
    ],
    suggestions: [
      'Add null checks for input objects before invoking properties.',
      'Consider replacing imperative array loops with standard stream operations.',
      'Remove unused variable declarations to improve memory overhead.'
    ]
  });

  const [testCases, setTestCases] = useState([
    { id: 'tc1', input: '5', expected: '120', actual: '120', status: 'PASSED' },
    { id: 'tc2', input: '10', expected: '3628800', actual: '3628800', status: 'PASSED' },
    { id: 'tc3', input: '0', expected: '1', actual: '1', status: 'PASSED' },
    { id: 'tc4', input: '-1', expected: 'Error', actual: '0', status: 'FAILED' }
  ]);

  const [testSummary, setTestSummary] = useState({
    total: 4,
    passed: 3,
    failed: 1,
    passRate: 75
  });

  const handleRunCode = async (code, fileName) => {
    setIsRunning(true);
    setIsBottomPanelOpen(true);
    setActiveBottomTab('Output');

    const result = await codeService.runCode({
      code,
      fileName,
      language: activeLanguage
    });

    setOutput(result);
    setIsDemoMode(result.isDemo !== false);
    setIsRunning(false);
  };

  const handleAnalyzeCode = async (code, fileName) => {
    setIsAnalyzing(true);
    setIsBottomPanelOpen(true);
    setActiveBottomTab('Analysis');

    const result = await codeService.analyzeCode({
      code,
      fileName,
      language: activeLanguage
    });

    setAnalysisResults(result);
    setIsDemoMode(result.isDemo !== false);
    setIsAnalyzing(false);
  };

  const handleRunTests = async (code) => {
    setIsTesting(true);
    setIsBottomPanelOpen(true);
    setActiveBottomTab('Test Cases');

    const result = await codeService.runTests({
      code,
      language: activeLanguage,
      testCases
    });

    if (result.testResults) {
      setTestCases(result.testResults);
      setTestSummary({
        total: result.totalTests,
        passed: result.passCount,
        failed: result.failCount,
        passRate: result.passRate
      });
    }
    setIsDemoMode(result.isDemo !== false);
    setIsTesting(false);
  };

  const addTestCase = (input, expected) => {
    const newCase = {
      id: 'tc-' + Date.now(),
      input,
      expected,
      actual: '-',
      status: 'PENDING'
    };
    setTestCases((prev) => [...prev, newCase]);
  };

  const languageConfig = getLanguageConfig(activeLanguage);

  return (
    <EditorContext.Provider
      value={{
        activeLanguage,
        setActiveLanguage,
        languageConfig,
        isBottomPanelOpen,
        setIsBottomPanelOpen,
        activeBottomTab,
        setActiveBottomTab,
        isRunning,
        isAnalyzing,
        isTesting,
        isDemoMode,
        output,
        analysisResults,
        testCases,
        testSummary,
        handleRunCode,
        handleAnalyzeCode,
        handleRunTests,
        addTestCase,
        setTestCases,
        editorPreferences,
        setEditorPreferences
      }}
    >
      {children}
    </EditorContext.Provider>
  );
};

export const useEditor = () => useContext(EditorContext);
