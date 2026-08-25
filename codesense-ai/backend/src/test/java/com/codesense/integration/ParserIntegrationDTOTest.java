package com.codesense.integration;

import com.codesense.integration.parser.dto.CodeElementDTO;
import com.codesense.integration.parser.dto.CodeRelationshipDTO;
import com.codesense.integration.parser.dto.ParsedFileDTO;
import com.codesense.integration.parser.dto.ParsedRepositoryDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests verifying that the Parser Integration DTOs have the correct structure
 * for Team Member 4's handoff.
 */
class ParserIntegrationDTOTest {

    @Test
    void parsedFileDTO_builderWorks() {
        ParsedFileDTO dto = ParsedFileDTO.builder()
            .repositoryId("repo-id")
            .projectId("project-id")
            .filePath("src/main/java/AuthService.java")
            .language("Java")
            .content("public class AuthService {}")
            .elements(List.of(
                CodeElementDTO.builder()
                    .name("AuthService")
                    .type("CLASS")
                    .language("Java")
                    .startLine(1)
                    .endLine(100)
                    .build()
            ))
            .relationships(List.of(
                CodeRelationshipDTO.builder()
                    .sourceElement("AuthController")
                    .targetElement("AuthService")
                    .relationshipType("CALLS")
                    .build()
            ))
            .build();

        assertThat(dto.getFilePath()).isEqualTo("src/main/java/AuthService.java");
        assertThat(dto.getLanguage()).isEqualTo("Java");
        assertThat(dto.getElements()).hasSize(1);
        assertThat(dto.getElements().get(0).getName()).isEqualTo("AuthService");
        assertThat(dto.getRelationships()).hasSize(1);
        assertThat(dto.getRelationships().get(0).getRelationshipType()).isEqualTo("CALLS");
    }

    @Test
    void parsedRepositoryDTO_builderWorks() {
        ParsedRepositoryDTO repo = ParsedRepositoryDTO.builder()
            .repositoryId("repo-id")
            .projectId("project-id")
            .repositoryName("my-project")
            .files(List.of())
            .totalElements(42)
            .totalRelationships(15)
            .languages(List.of("Java", "Python"))
            .parserVersion("1.0.0")
            .build();

        assertThat(repo.getLanguages()).containsExactly("Java", "Python");
        assertThat(repo.getTotalElements()).isEqualTo(42);
    }

    @Test
    void codeElementDTO_defaultsEmpty() {
        CodeElementDTO element = new CodeElementDTO();
        assertThat(element.getParameters()).isNull();
        assertThat(element.getAnnotations()).isNull();
    }
}
