package com.codesense.integration.parser;

import com.codesense.integration.parser.dto.ParsedFileDTO;
import com.codesense.integration.parser.dto.ParsedRepositoryDTO;

import java.util.UUID;

/**
 * Integration contract for Team Member 4's Parser/Analyzer module.
 *
 * Team Member 3 (Karthik) defines this interface.
 * Team Member 4 (Prashanthi) implements this interface using JavaParser + Tree-sitter.
 *
 * ══════════════════════════════════════════════════════════
 * TEAM MEMBER 4 HANDOFF
 * ══════════════════════════════════════════════════════════
 *
 * To integrate JavaParser + Tree-sitter with the AI Engine:
 *
 * 1. Create a class implementing this interface:
 *    @Service
 *    public class JavaParserTreeSitterIntegration implements ParserIntegrationService { ... }
 *
 * 2. For each file, parse it and populate ParsedFileDTO:
 *    - Use JavaParser for .java files
 *    - Use Tree-sitter for all other languages
 *
 * 3. For Java files, extract from JavaParser:
 *    - ClassDeclaration → CodeElementDTO(type="CLASS")
 *    - MethodDeclaration → CodeElementDTO(type="METHOD")
 *    - FieldDeclaration → CodeElementDTO(type="FIELD")
 *    - Imports/dependencies → CodeRelationshipDTO(type="IMPORTS")
 *
 * 4. For Tree-sitter files, extract:
 *    - class_definition → CodeElementDTO(type="CLASS")
 *    - function_definition → CodeElementDTO(type="FUNCTION")
 *    - method_definition → CodeElementDTO(type="METHOD")
 *    - import_statement → CodeRelationshipDTO(type="IMPORTS")
 *
 * 5. Call ingestParsedRepository() or ingestParsedFile() to feed results
 *    into the AI Engine's ingestion pipeline.
 *
 * ══════════════════════════════════════════════════════════
 */
public interface ParserIntegrationService {

    /**
     * Ingest parser metadata for a complete repository.
     * This triggers re-ingestion of the repository with semantic chunks
     * based on AST elements rather than text windows.
     *
     * @param parsedRepository parsed metadata from JavaParser/Tree-sitter
     */
    void ingestParsedRepository(ParsedRepositoryDTO parsedRepository);

    /**
     * Ingest parser metadata for a single file.
     * Used for incremental updates.
     *
     * @param parsedFile parsed metadata for a single file
     */
    void ingestParsedFile(ParsedFileDTO parsedFile);

    /**
     * Check whether parser integration is available.
     * Returns false in the default (stub) implementation.
     */
    default boolean isParserAvailable() {
        return false;
    }
}
