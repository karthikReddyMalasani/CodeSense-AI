import { api } from './api';

export const aiService = {
  async askAI(prompt, codeContext = '', language = 'java') {
    try {
      const response = await api.post('/api/code/chat', { prompt, codeContext, language });
      return { success: true, isDemo: false, reply: response.data.reply || response.data.answer };
    } catch (err) {
      await new Promise((res) => setTimeout(res, 900));
      
      let reply = `Here is how you can address your question in **${language.toUpperCase()}**:\n\n`;
      if (prompt.toLowerCase().includes('nullpointer') || prompt.toLowerCase().includes('error')) {
        reply += `The \`NullPointerException\` occurs because a variable reference is evaluated before object instantiation.\n\n### Suggested Fix:\n\`\`\`${language}\n// Add guard clause before dereferencing\nif (object != null) {\n    object.doAction();\n}\n\`\`\`\nThis prevents runtime crashes and ensures thread-safe object access.`;
      } else if (prompt.toLowerCase().includes('optimize')) {
        reply += `To optimize this block:\n1. Replace $O(n^2)$ nested loops with a Hash Map ($O(n)$ time complexity).\n2. Pre-allocate array capacity where possible.\n3. Minimize redundant object creations inside tight loops.`;
      } else {
        reply += `CodeAssist AI analyzed your code:\n\n- **Logic Integrity**: Good control flow structure.\n- **Performance**: Execution overhead is optimal for small to medium inputs.\n- **Recommendation**: Consider adding JavaDoc/Docstring comments for public API methods.`;
      }

      return { success: true, isDemo: true, reply };
    }
  },

  async explainCode(code, language = 'java') {
    try {
      const response = await api.post('/api/code/explain', { code, language });
      return { success: true, isDemo: false, ...response.data };
    } catch (err) {
      await new Promise((res) => setTimeout(res, 1000));
      return {
        success: true,
        isDemo: true,
        overview: `This ${language.toUpperCase()} script performs array manipulation and calculates aggregated metrics efficiently.`,
        steps: [
          'Accepts an input dataset or initializes an in-memory collection.',
          'Traverses the elements using a continuous loop structure.',
          'Accumulates values to produce the total result.',
          'Returns or outputs the final formatted calculation to console.'
        ],
        complexity: {
          time: 'O(n)',
          space: 'O(1)'
        },
        suggestions: [
          'Use modern stream APIs for cleaner functional syntax.',
          'Validate non-empty input array condition before entering calculation loop.'
        ]
      };
    }
  },

  async debugCode(code, language = 'java') {
    try {
      const response = await api.post('/api/code/debug', { code, language });
      return { success: true, isDemo: false, ...response.data };
    } catch (err) {
      await new Promise((res) => setTimeout(res, 1000));
      return {
        success: true,
        isDemo: true,
        detectedIssue: {
          type: 'Runtime Error / Boundary Misalignment',
          location: 'Line 24',
          problem: 'Array index exceeds bounded length or potential null dereference.'
        },
        whyItHappens: 'The loop index upper-bound check uses `<=` instead of `<` on zero-indexed array boundaries, causing an out-of-bounds index access on the final iteration.',
        suggestedFix: `// Corrected boundary condition\nfor (int i = 0; i < arr.length; i++) {\n    // Safe access\n    System.out.println(arr[i]);\n}`,
        refactoredCode: code.replace(/<= arr\.length/g, '< arr.length')
      };
    }
  },

  async generateCode(prompt, language = 'java', options = {}) {
    try {
      const response = await api.post('/api/code/generate', { prompt, language, ...options });
      return { success: true, isDemo: false, ...response.data };
    } catch (err) {
      await new Promise((res) => setTimeout(res, 1200));
      
      let generatedCode = '';
      if (language === 'java') {
        generatedCode = `public class Solution {\n    public static int findSecondLargest(int[] arr) {\n        if (arr == null || arr.length < 2) {\n            throw new IllegalArgumentException("Array must contain at least 2 elements");\n        }\n        int largest = Integer.MIN_VALUE;\n        int secondLargest = Integer.MIN_VALUE;\n        for (int num : arr) {\n            if (num > largest) {\n                secondLargest = largest;\n                largest = num;\n            } else if (num > secondLargest && num != largest) {\n                secondLargest = num;\n            }\n        }\n        return secondLargest;\n    }\n    public static void main(String[] args) {\n        int[] numbers = {12, 35, 1, 10, 34, 1};\n        System.out.println("Second largest element: " + findSecondLargest(numbers));\n    }\n}`;
      } else if (language === 'python') {
        generatedCode = `def find_second_largest(arr):\n    if len(arr) < 2:\n        raise ValueError("Array must contain at least 2 elements")\n    unique_sorted = sorted(list(set(arr)), reverse=True)\n    return unique_sorted[1]\n\nif __name__ == "__main__":
    numbers = [12, 35, 1, 10, 34, 1]\n    print(f"Second largest element: {find_second_largest(numbers)}")`;
      } else {
        generatedCode = `// Generated ${language} code for: ${prompt}\nfunction solution(input) {\n    console.log("Processing input:", input);\n    return true;\n}\n\nsolution([10, 20, 30]);`;
      }

      return {
        success: true,
        isDemo: true,
        generatedCode,
        explanation: `Generated solution implementation based on: "${prompt}". Time Complexity is O(n), Space Complexity is O(1).`,
        testCases: [
          { input: '[12, 35, 1, 10, 34, 1]', expected: '34' },
          { input: '[10, 5]', expected: '5' }
        ]
      };
    }
  },

  async generateTests(code, language = 'java') {
    try {
      const response = await api.post('/api/code/generate-tests', { code, language });
      return { success: true, isDemo: false, ...response.data };
    } catch (err) {
      await new Promise((res) => setTimeout(res, 800));
      return {
        success: true,
        isDemo: true,
        testCases: [
          { id: 'tc-' + Date.now() + '-1', input: '5', expected: '120', actual: '120', status: 'PASSED' },
          { id: 'tc-' + Date.now() + '-2', input: '0', expected: '1', actual: '1', status: 'PASSED' },
          { id: 'tc-' + Date.now() + '-3', input: '10', expected: '3628800', actual: '3628800', status: 'PASSED' }
        ]
      };
    }
  }
};
