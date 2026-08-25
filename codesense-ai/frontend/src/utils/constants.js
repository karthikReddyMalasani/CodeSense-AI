export const APP_NAME = 'CodeAssist AI';
export const APP_TAGLINE = 'AI-Powered Code Analysis, Debugging & Assistance';

export const DEFAULT_PROJECT = {
  id: 'proj-demo-1',
  name: 'Demo Project',
  language: 'java',
  files: [
    {
      id: 'f1',
      name: 'Main.java',
      path: 'src/Main.java',
      type: 'file',
      language: 'java',
      content: `public class Main {
    public static void main(String[] args) {
        System.out.println("Welcome to CodeAssist AI!");
        
        int[] numbers = {10, 20, 30, 40, 50};
        int sum = calculateSum(numbers);
        System.out.println("Sum of array elements: " + sum);
        
        // Example array search
        int target = 30;
        int index = findElement(numbers, target);
        System.out.println("Element " + target + " found at index: " + index);
    }
    
    public static int calculateSum(int[] arr) {
        int total = 0;
        for (int num : arr) {
            total += num;
        }
        return total;
    }

    public static int findElement(int[] arr, int target) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == target) {
                return i;
            }
        }
        return -1;
    }
}`
    },
    {
      id: 'f2',
      name: 'Utils.java',
      path: 'src/Utils.java',
      type: 'file',
      language: 'java',
      content: `public class Utils {
    public static String formatMessage(String prefix, String message) {
        return "[" + prefix.toUpperCase() + "] " + message;
    }
    
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}`
    },
    {
      id: 'f3',
      name: 'CalculatorTest.java',
      path: 'tests/CalculatorTest.java',
      type: 'file',
      language: 'java',
      content: `public class CalculatorTest {
    public void testSum() {
        int[] numbers = {10, 20};
        int result = Main.calculateSum(numbers);
        assert result == 30 : "Sum test failed!";
        System.out.println("Test passed successfully.");
    }
}`
    },
    {
      id: 'f4',
      name: 'README.md',
      path: 'README.md',
      type: 'file',
      language: 'markdown',
      content: `# Demo Project

An example multi-file repository managed with **CodeAssist AI**.

## Structure
- \`src/Main.java\` - Core logic & entrypoint
- \`src/Utils.java\` - Helper utilities
- \`tests/CalculatorTest.java\` - Unit test suite
`
    },
    {
      id: 'f5',
      name: 'config.json',
      path: 'config.json',
      type: 'file',
      language: 'json',
      content: `{
  "projectName": "Demo Project",
  "version": "1.0.0",
  "environment": "development"
}`
    }
  ]
};

export const INITIAL_RECENT_PROJECTS = [
  {
    id: 'p1',
    name: 'Java Banking System',
    language: 'Java',
    fileCount: 12,
    lastModified: 'Today',
    description: 'Enterprise backend service for financial transactions'
  },
  {
    id: 'p2',
    name: 'Python RAG Pipeline',
    language: 'Python',
    fileCount: 8,
    lastModified: 'Yesterday',
    description: 'LangChain & Vector store code search engine'
  },
  {
    id: 'p3',
    name: 'React Dashboard UI',
    language: 'TypeScript',
    fileCount: 24,
    lastModified: '3 days ago',
    description: 'Modern developer tools workspace frontend'
  },
  {
    id: 'p4',
    name: 'Go Microservice API',
    language: 'Go',
    fileCount: 15,
    lastModified: '1 week ago',
    description: 'High-throughput gRPC endpoint service'
  }
];

export const KEYBOARD_SHORTCUTS = [
  { key: 'Ctrl + S', description: 'Save File' },
  { key: 'Ctrl + Enter', description: 'Run Code' },
  { key: 'Ctrl + Shift + A', description: 'Analyze Code' },
  { key: 'Ctrl + I', description: 'Toggle AI Assistant' }
];
