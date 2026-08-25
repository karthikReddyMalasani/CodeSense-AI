export const SUPPORTED_LANGUAGES = [
  {
    id: 'java',
    name: 'Java',
    monacoLang: 'java',
    ext: '.java',
    defaultFileName: 'Main.java',
    icon: '☕',
    starterCode: `public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, CodeAssist AI!");
        
        int[] numbers = {10, 20, 30, 40, 50};
        int sum = calculateSum(numbers);
        System.out.println("Sum of array elements: " + sum);
    }
    
    public static int calculateSum(int[] arr) {
        int total = 0;
        for (int num : arr) {
            total += num;
        }
        return total;
    }
}`
  },
  {
    id: 'python',
    name: 'Python',
    monacoLang: 'python',
    ext: '.py',
    defaultFileName: 'main.py',
    icon: '🐍',
    starterCode: `def main():
    print("Hello, CodeAssist AI!")
    
    numbers = [10, 20, 30, 40, 50]
    total = sum_array(numbers)
    print(f"Sum of array elements: {total}")

def sum_array(arr):
    return sum(arr)

if __name__ == "__main__":
    main()
`
  },
  {
    id: 'c',
    name: 'C',
    monacoLang: 'c',
    ext: '.c',
    defaultFileName: 'main.c',
    icon: '⚡',
    starterCode: `#include <stdio.stdio>
#include <stdio.h>

int calculate_sum(int arr[], int size) {
    int total = 0;
    for (int i = 0; i < size; i++) {
        total += arr[i];
    }
    return total;
}

int main() {
    printf("Hello, CodeAssist AI!\\n");
    
    int numbers[] = {10, 20, 30, 40, 50};
    int size = sizeof(numbers) / sizeof(numbers[0]);
    int sum = calculate_sum(numbers, size);
    
    printf("Sum of array elements: %d\\n", sum);
    return 0;
}
`
  },
  {
    id: 'cpp',
    name: 'C++',
    monacoLang: 'cpp',
    ext: '.cpp',
    defaultFileName: 'main.cpp',
    icon: '🔷',
    starterCode: `#include <iostream>
#include <vector>
#include <numeric>

int calculateSum(const std::vector<int>& arr) {
    return std::accumulate(arr.begin(), arr.end(), 0);
}

int main() {
    std::cout << "Hello, CodeAssist AI!" << std::endl;
    
    std::vector<int> numbers = {10, 20, 30, 40, 50};
    int sum = calculateSum(numbers);
    
    std::cout << "Sum of array elements: " << sum << std::endl;
    return 0;
}
`
  },
  {
    id: 'javascript',
    name: 'JavaScript',
    monacoLang: 'javascript',
    ext: '.js',
    defaultFileName: 'app.js',
    icon: '🟨',
    starterCode: `function main() {
  console.log("Hello, CodeAssist AI!");
  
  const numbers = [10, 20, 30, 40, 50];
  const sum = calculateSum(numbers);
  console.log(\`Sum of array elements: \${sum}\`);
}

function calculateSum(arr) {
  return arr.reduce((acc, curr) => acc + curr, 0);
}

main();
`
  },
  {
    id: 'typescript',
    name: 'TypeScript',
    monacoLang: 'typescript',
    ext: '.ts',
    defaultFileName: 'app.ts',
    icon: '🟦',
    starterCode: `interface CalculationResult {
  sum: number;
  average: number;
}

function calculateMetrics(numbers: number[]): CalculationResult {
  const sum = numbers.reduce((acc, curr) => acc + curr, 0);
  return {
    sum,
    average: numbers.length ? sum / numbers.length : 0
  };
}

const numbers: number[] = [10, 20, 30, 40, 50];
const metrics = calculateMetrics(numbers);

console.log("Hello, CodeAssist AI!");
console.log(\`Sum: \${metrics.sum}, Average: \${metrics.average}\`);
`
  },
  {
    id: 'go',
    name: 'Go',
    monacoLang: 'go',
    ext: '.go',
    defaultFileName: 'main.go',
    icon: '🐹',
    starterCode: `package main

import "fmt"

func calculateSum(numbers []int) int {
	total := 0
	for _, num := range numbers {
		total += num
	}
	return total
}

func main() {
	fmt.Println("Hello, CodeAssist AI!")
	numbers := []int{10, 20, 30, 40, 50}
	sum := calculateSum(numbers)
	fmt.Printf("Sum of array elements: %d\\n", sum)
}
`
  },
  {
    id: 'csharp',
    name: 'C#',
    monacoLang: 'csharp',
    ext: '.cs',
    defaultFileName: 'Program.cs',
    icon: '💜',
    starterCode: `using System;
using System.Linq;

class Program {
    static void Main() {
        Console.WriteLine("Hello, CodeAssist AI!");
        
        int[] numbers = {10, 20, 30, 40, 50};
        int sum = CalculateSum(numbers);
        Console.WriteLine($"Sum of array elements: {sum}");
    }
    
    static int CalculateSum(int[] arr) {
        return arr.Sum();
    }
}
`
  },
  {
    id: 'php',
    name: 'PHP',
    monacoLang: 'php',
    ext: '.php',
    defaultFileName: 'index.php',
    icon: '🐘',
    starterCode: `<?php
echo "Hello, CodeAssist AI!\n";

$numbers = [10, 20, 30, 40, 50];

function calculateSum(array $arr): int {
    return array_sum($arr);
}

$sum = calculateSum($numbers);
echo "Sum of array elements: " . $sum . "\n";
?>
`
  },
  {
    id: 'kotlin',
    name: 'Kotlin',
    monacoLang: 'kotlin',
    ext: '.kt',
    defaultFileName: 'Main.kt',
    icon: '💜',
    starterCode: `fun main() {
    println("Hello, CodeAssist AI!")
    
    val numbers = listOf(10, 20, 30, 40, 50)
    val sum = calculateSum(numbers)
    println("Sum of array elements: $sum")
}

fun calculateSum(numbers: List<Int>): Int {
    return numbers.sum()
}
`
  },
  {
    id: 'rust',
    name: 'Rust',
    monacoLang: 'rust',
    ext: '.rs',
    defaultFileName: 'main.rs',
    icon: '🦀',
    starterCode: `fn main() {
    println!("Hello, CodeAssist AI!");
    
    let numbers = vec![10, 20, 30, 40, 50];
    let sum = calculate_sum(&numbers);
    println!("Sum of array elements: {}", sum);
}

fn calculate_sum(numbers: &[i32]) -> i32 {
    numbers.iter().sum()
}
`
  }
];

export const getLanguageConfig = (langId) => {
  return (
    SUPPORTED_LANGUAGES.find(
      (l) => l.id.toLowerCase() === (langId || '').toLowerCase()
    ) || SUPPORTED_LANGUAGES[0]
  );
};
