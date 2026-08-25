package com.codesense.integration.parser;

import com.codesense.ai.ingestion.IngestionService;
import com.codesense.integration.parser.dto.ParsedFileDTO;
import com.codesense.integration.parser.dto.ParsedRepositoryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Stub/placeholder implementation of ParserIntegrationService.
 *
 * This is a NO-OP stub that:
 * 1. Logs that parser metadata was received (but does nothing with it)
 * 2. Returns isParserAvailable() = false
 *
 * Team Member 4 (Prashanthi) should REPLACE this class with their
 * JavaParser + Tree-sitter implementation.
 *
 * Team Member 4 should:
 * 1. Remove the @Primary annotation from this stub
 * 2. Create a new @Service @Primary implementation of ParserIntegrationService
 * 3. That implementation should call IngestionService.ingestFromParserMetadata()
 *
 * See: ParserIntegrationService.java for the full handoff instructions.
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class ParserIntegrationStub implements ParserIntegrationService {

    private final IngestionService ingestionService;

    @Override
    public void ingestParsedRepository(ParsedRepositoryDTO parsedRepository) {
        log.info("[PARSER STUB] Received parsed repository: {} files, {} elements",
            parsedRepository.getFiles() != null ? parsedRepository.getFiles().size() : 0,
            parsedRepository.getTotalElements());
        log.info("[PARSER STUB] Team Member 4 integration pending. " +
            "Replace ParserIntegrationStub with JavaParser+Tree-sitter implementation.");

        // When Team Member 4's implementation is ready, call:
        // ingestionService.ingestFromParserMetadata(parsedRepository);
    }

    @Override
    public void ingestParsedFile(ParsedFileDTO parsedFile) {
        log.info("[PARSER STUB] Received parsed file: {} ({})",
            parsedFile.getFilePath(), parsedFile.getLanguage());
        log.info("[PARSER STUB] Team Member 4 integration pending.");

        // When Team Member 4's implementation is ready, call:
        // ingestionService.ingestSingleParsedFile(parsedFile);
    }

    @Override
    public boolean isParserAvailable() {
        return false;
    }
}
