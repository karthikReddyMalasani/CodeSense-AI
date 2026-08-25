package com.codesense.ai.prompt;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Reusable prompt templates for AI generation tasks.
 * Team Member 3 (Karthik) owns all prompt engineering.
 *
 * Design principles:
 * - Prevent hallucination (require source grounding)
 * - Require source references for repository-specific answers
 * - Distinguish facts from assumptions
 * - Respect project boundaries
 * - Never expose secrets or credentials
 */
@Component
public class PromptTemplates {

    // ─── Repository Chat ──────────────────────────────────────────────────────

    public String repositoryChat(String question, String context, String conversationHistory) {
        return String.format("""
            You are a code intelligence assistant for a software repository.
            Your role is to answer questions about this specific repository based ONLY on the provided context.
            
            RULES:
            1. Answer ONLY based on the provided context. Do not invent code that is not in the context.
            2. If the context does not contain enough information, say: "I could not find enough information in the repository to answer this confidently."
            3. Always reference specific files, classes, or methods when possible.
            4. Format code examples using markdown code blocks with language identifiers.
            5. Never reveal credentials, API keys, passwords, or secrets.
            
            %s
            
            REPOSITORY CONTEXT:
            ---
            %s
            ---
            
            QUESTION: %s
            
            ANSWER:
            """,
            conversationHistory != null && !conversationHistory.isBlank()
                ? "PREVIOUS CONVERSATION:\n" + conversationHistory + "\n"
                : "",
            context,
            question
        );
    }

    // ─── Code Explanation ─────────────────────────────────────────────────────

    public String codeExplanation(String code, String language, String filePath) {
        return String.format("""
            You are a senior software engineer. Analyze the following %s code and provide a comprehensive explanation.
            
            File: %s
            Language: %s
            
            CODE:
            ```%s
            %s
            ```
            
            Provide a structured explanation with the following sections:
            
            ## Summary
            A brief 2-3 sentence overview of what this code does.
            
            ## Purpose
            The specific role this code plays in the application.
            
            ## Key Components
            List the main classes, methods, functions, or blocks and what each does.
            
            ## Logic Flow
            Step-by-step explanation of the main logic/algorithm.
            
            ## Dependencies
            What external libraries, services, or modules does this code depend on?
            
            ## Potential Issues
            Any code quality concerns, potential bugs, or security considerations you notice.
            
            ## Suggestions
            Brief improvement suggestions (optional, only if clearly beneficial).
            
            Be factual. Do not invent functionality that is not present in the code.
            """,
            language != null ? language : "source",
            filePath != null ? filePath : "unknown",
            language != null ? language.toLowerCase() : "",
            language != null ? language.toLowerCase() : "",
            code
        );
    }

    // ─── README Generator ─────────────────────────────────────────────────────

    public String readmeGeneration(String repositoryName, String languages, String structure,
                                    String sampleCode, String existingReadme) {
        return String.format("""
            You are a technical documentation specialist. Generate a comprehensive README.md for this repository.
            
            Repository Name: %s
            Languages: %s
            
            Repository Structure (sample):
            %s
            
            Sample Code Context:
            %s
            
            %s
            
            Generate a complete README.md with these sections:
            
            # %s
            
            ## Overview
            [Brief description of what this project does]
            
            ## Features
            [Key features as bullet points]
            
            ## Technologies
            [Technologies and versions used]
            
            ## Project Structure
            [Key directory structure]
            
            ## Installation
            [Step-by-step setup instructions]
            
            ## Configuration
            [Environment variables and configuration]
            
            ## Usage / API
            [How to use the application]
            
            ## Architecture
            [High-level architecture description]
            
            ## How It Works
            [Technical explanation of main flows]
            
            ## Testing
            [How to run tests]
            
            ## License
            [License information if detectable, otherwise placeholder]
            
            IMPORTANT:
            - Only include information that is evident from the provided context
            - Do not invent features or APIs that are not present
            - Use "TODO" placeholders where information is not available
            - Format all code examples with appropriate language tags
            """,
            repositoryName, languages, structure, sampleCode,
            existingReadme != null && !existingReadme.isBlank()
                ? "Existing README (for reference):\n" + existingReadme : "",
            repositoryName
        );
    }

    // ─── API Documentation ────────────────────────────────────────────────────

    public String apiDocumentation(String repositoryName, String apiCode, String language) {
        return String.format("""
            You are a technical writer specializing in API documentation.
            Analyze the following API code and generate structured documentation.
            
            Repository: %s
            Language: %s
            
            API CODE:
            %s
            
            Generate API documentation in the following format for each endpoint/function found:
            
            ## [Endpoint/Function Name]
            
            **Method:** [HTTP method or function signature]
            **Path:** [URL path or function path]
            **Description:** [What it does]
            
            ### Request
            - **Headers:** [Required headers]
            - **Parameters:** [URL/query parameters]
            - **Body:** [Request body schema]
            
            ### Response
            - **Success (200/201):** [Response schema]
            - **Error responses:** [Error cases]
            
            ### Authentication
            [Authentication requirements]
            
            ### Example
            ```json
            [Example request/response]
            ```
            
            Document ALL endpoints found in the code. Do not invent endpoints that are not present.
            """,
            repositoryName, language != null ? language : "Unknown", apiCode
        );
    }

    // ─── Architecture Explanation ─────────────────────────────────────────────

    public String architectureExplanation(String repositoryName, String structure, String languages,
                                           String sampleChunks) {
        return String.format("""
            You are a software architect. Analyze this repository and explain its architecture.
            
            Repository: %s
            Languages: %s
            
            Repository Structure:
            %s
            
            Code Context:
            %s
            
            Provide:
            1. **Architecture Pattern** - What architectural pattern is used (MVC, layered, microservices, etc.)?
            2. **Main Components** - Key modules/packages and their responsibilities
            3. **Data Flow** - How data flows through the system
            4. **Key Technologies** - Main frameworks and libraries
            5. **Integration Points** - External services or APIs
            
            Base your answer ONLY on the provided context. Do not assume technologies not visible in the code.
            """,
            repositoryName, languages, structure, sampleChunks
        );
    }

    // ─── Error Explanation ────────────────────────────────────────────────────

    public String errorExplanation(String errorMessage, String stackTrace, String context) {
        return String.format("""
            You are a debugging assistant. Analyze this error and provide a clear explanation.
            
            ERROR: %s
            
            STACK TRACE:
            %s
            
            RELEVANT CODE CONTEXT:
            %s
            
            Provide:
            1. **Root Cause** - What caused this error
            2. **Explanation** - Clear explanation in plain language
            3. **Fix** - How to resolve it
            4. **Prevention** - How to prevent this in the future
            
            Be specific and reference the actual code when relevant.
            """,
            errorMessage,
            stackTrace != null ? stackTrace : "Not provided",
            context != null ? context : "Not provided"
        );
    }

    // ─── RAG Context Builder ──────────────────────────────────────────────────

    public String buildContextFromChunks(List<String> chunks) {
        if (chunks == null || chunks.isEmpty()) return "No relevant context found.";
        return chunks.stream()
            .collect(Collectors.joining("\n\n---\n\n"));
    }

    public String buildSourceList(List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) return "";
        return filePaths.stream()
            .distinct()
            .map(path -> "- " + path)
            .collect(Collectors.joining("\n"));
    }
}
