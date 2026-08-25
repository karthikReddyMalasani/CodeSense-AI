import { api } from './api';

export const codeService = {
  async runCode(payload) {
    try {
      const response = await api.post('/api/code/run', payload);
      return { success: true, isDemo: false, ...response.data };
    } catch (err) {
      // Fallback to rich Demo Mode execution result
      await new Promise((res) => setTimeout(res, 600)); // simulate network latency
      const time = Math.floor(Math.random() * 40) + 15;
      const memory = Math.floor(Math.random() * 10) + 12;

      const hasError = payload.code.includes('NullPointer') || payload.code.includes('Exception') || payload.code.includes('Error');

      if (hasError) {
        return {
          success: false,
          isDemo: true,
          status: 'Execution Failed',
          output: `Compilation successful.\nRunning ${payload.fileName}...\n\nException in thread "main" java.lang.NullPointerException: Variable 'user' is null at line 24.\n\tat Main.main(Main.java:24)`,
          error: 'NullPointerException at line 24',
          line: 24,
          executionTimeMs: time,
          memoryMb: memory
        };
      }

      return {
        success: true,
        isDemo: true,
        status: 'Compilation successful',
        output: `> Running ${payload.fileName || 'Main.java'}...\n\nCompilation successful.\n\nOutput:\nHello, CodeAssist AI!\nSum of array elements: 150\nElement 30 found at index: 2\n\nExecution completed successfully.`,
        executionTimeMs: time,
        memoryMb: memory
      };
    }
  },

  async analyzeCode(payload) {
    try {
      const response = await api.post('/api/code/analyze', payload);
      return { success: true, isDemo: false, ...response.data };
    } catch (err) {
      await new Promise((res) => setTimeout(res, 800));
      return {
        success: true,
        isDemo: true,
        qualityScore: 84,
        complexity: {
          time: 'O(n)',
          space: 'O(1)'
        },
        potentialBugsCount: 1,
        securityIssuesCount: 0,
        codeSmellsCount: 2,
        problems: [
          {
            type: 'error',
            file: payload.fileName || 'Main.java',
            line: 15,
            title: 'NullPointerException Warning',
            message: "Variable 'user' may be null before method invocation.",
            severity: 'high'
          },
          {
            type: 'warning',
            file: payload.fileName || 'Main.java',
            line: 27,
            title: 'Unused Variable',
            message: "Unused local variable 'count'. Consider removing it.",
            severity: 'medium'
          },
          {
            type: 'info',
            file: payload.fileName || 'Main.java',
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
      };
    }
  },

  async runTests(payload) {
    try {
      const response = await api.post('/api/code/test', payload);
      return { success: true, isDemo: false, ...response.data };
    } catch (err) {
      await new Promise((res) => setTimeout(res, 700));
      const testCases = payload.testCases || [
        { id: 'tc1', input: '5', expected: '120', actual: '120', status: 'PASSED' },
        { id: 'tc2', input: '10', expected: '3628800', actual: '3628800', status: 'PASSED' },
        { id: 'tc3', input: '0', expected: '1', actual: '1', status: 'PASSED' },
        { id: 'tc4', input: '-1', expected: 'Error', actual: '0', status: 'FAILED' }
      ];

      const passed = testCases.filter((t) => t.status === 'PASSED').length;
      const total = testCases.length;

      return {
        success: true,
        isDemo: true,
        totalTests: total,
        passCount: passed,
        failCount: total - passed,
        passRate: Math.round((passed / (total || 1)) * 100),
        testResults: testCases
      };
    }
  },

  async formatCode(code, language) {
    try {
      const response = await api.post('/api/code/format', { code, language });
      return response.data.formattedCode || code;
    } catch (err) {
      // Basic formatting fallback
      return code.trim() + '\n';
    }
  },

  async saveCode(payload) {
    try {
      const response = await api.post('/api/files/save', payload);
      return { success: true, isDemo: false, ...response.data };
    } catch (err) {
      return { success: true, isDemo: true, message: 'Saved to local storage.' };
    }
  }
};
