package com.codesense.parser;

import com.codesense.integration.parser.dto.CodeRelationshipDTO;
import com.codesense.integration.parser.dto.ParsedFileDTO;
import com.codesense.integration.parser.dto.ParsedRepositoryDTO;
import com.codesense.parser.service.ArchitectureAnalysisService;
import com.codesense.parser.service.RepositoryParserService;
import com.codesense.repository.model.RepositoryFile;
import com.codesense.repository.repository.RepositoryFileRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArchitectureAnalysisServiceTest {

    @Test
    void buildResult_usesActualParsedRelationships_forVisuallyRenderedGraph() throws Exception {
        RepositoryParserService parserService = mock(RepositoryParserService.class);
        RepositoryFileRepository fileRepository = mock(RepositoryFileRepository.class);
        ArchitectureAnalysisService service = new ArchitectureAnalysisService(parserService, fileRepository, Runnable::run);

        ParsedRepositoryDTO parsed = ParsedRepositoryDTO.builder()
            .repositoryId(UUID.randomUUID().toString())
            .files(List.of(
                ParsedFileDTO.builder()
                    .filePath("src/main/java/com/example/FrontendController.java")
                    .language("Java")
                    .elements(List.of())
                    .relationships(List.of(
                        CodeRelationshipDTO.builder()
                            .sourceElement("FrontendController")
                            .targetElement("UserService")
                            .relationshipType("CALLS")
                            .sourceFile("FrontendController.java")
                            .targetFile("UserService.java")
                            .build()))
                    .build(),
                ParsedFileDTO.builder()
                    .filePath("src/main/java/com/example/UserService.java")
                    .language("Java")
                    .elements(List.of())
                    .relationships(List.of())
                    .build()))
            .totalRelationships(1)
            .build();

        Method buildResult = ArchitectureAnalysisService.class.getDeclaredMethod(
            "buildResult", ParsedRepositoryDTO.class, List.class, Map.class, List.class);
        buildResult.setAccessible(true);

        ArchitectureAnalysisService.Result result = (ArchitectureAnalysisService.Result) buildResult.invoke(
            service,
            parsed,
            List.of(RepositoryFile.builder().filePath("src/main/java/com/example/FrontendController.java").language("Java").ignored(false).build()),
            Map.of("Java / Maven", 1),
            List.of()
        );

        assertThat(result).isNotNull();
        assertThat(result.flows()).isNotEmpty();
        assertThat(result.mermaid()).contains("graph LR");
    }

    @Test
    void buildResult_aggregatesArchitecturalModules_andIgnoresLowLevelNoise() throws Exception {
        RepositoryParserService parserService = mock(RepositoryParserService.class);
        RepositoryFileRepository fileRepository = mock(RepositoryFileRepository.class);
        ArchitectureAnalysisService service = new ArchitectureAnalysisService(parserService, fileRepository, Runnable::run);

        ParsedRepositoryDTO parsed = ParsedRepositoryDTO.builder()
            .repositoryId(UUID.randomUUID().toString())
            .files(List.of(
                ParsedFileDTO.builder()
                    .filePath("frontend/src/pages/DashboardPage.jsx")
                    .language("JavaScript")
                    .elements(List.of())
                    .relationships(List.of())
                    .build(),
                ParsedFileDTO.builder()
                    .filePath("backend/src/main/java/com/codesense/auth/controller/AuthController.java")
                    .language("Java")
                    .elements(List.of())
                    .relationships(List.of(
                        CodeRelationshipDTO.builder()
                            .sourceElement("AuthController")
                            .targetElement("AuthService")
                            .relationshipType("CALLS")
                            .sourceFile("AuthController.java")
                            .targetFile("AuthService.java")
                            .build(),
                        CodeRelationshipDTO.builder()
                            .sourceElement("AuthService")
                            .targetElement("UUID")
                            .relationshipType("USES")
                            .sourceFile("AuthService.java")
                            .targetFile("UUID.java")
                            .build(),
                        CodeRelationshipDTO.builder()
                            .sourceElement("AuthService")
                            .targetElement("ConcurrentHashMap")
                            .relationshipType("USES")
                            .sourceFile("AuthService.java")
                            .targetFile("ConcurrentHashMap.java")
                            .build()))
                    .build(),
                ParsedFileDTO.builder()
                    .filePath("backend/src/main/java/com/codesense/project/service/ProjectService.java")
                    .language("Java")
                    .elements(List.of())
                    .relationships(List.of())
                    .build(),
                ParsedFileDTO.builder()
                    .filePath("backend/src/main/java/com/codesense/ai/service/AiService.java")
                    .language("Java")
                    .elements(List.of())
                    .relationships(List.of())
                    .build(),
                ParsedFileDTO.builder()
                    .filePath("backend/src/main/java/com/codesense/repository/repository/UserRepository.java")
                    .language("Java")
                    .elements(List.of())
                    .relationships(List.of())
                    .build()))
            .totalRelationships(3)
            .build();

        Method buildResult = ArchitectureAnalysisService.class.getDeclaredMethod(
            "buildResult", ParsedRepositoryDTO.class, List.class, Map.class, List.class);
        buildResult.setAccessible(true);

        ArchitectureAnalysisService.Result result = (ArchitectureAnalysisService.Result) buildResult.invoke(
            service,
            parsed,
            List.of(
                RepositoryFile.builder().filePath("frontend/src/pages/DashboardPage.jsx").language("JavaScript").ignored(false).build(),
                RepositoryFile.builder().filePath("backend/src/main/java/com/codesense/auth/controller/AuthController.java").language("Java").ignored(false).build(),
                RepositoryFile.builder().filePath("backend/src/main/java/com/codesense/project/service/ProjectService.java").language("Java").ignored(false).build(),
                RepositoryFile.builder().filePath("backend/src/main/java/com/codesense/ai/service/AiService.java").language("Java").ignored(false).build(),
                RepositoryFile.builder().filePath("backend/src/main/java/com/codesense/repository/repository/UserRepository.java").language("Java").ignored(false).build()
            ),
            Map.of("Java / Maven", 1, "React / Vite", 1),
            List.of()
        );

        assertThat(result.components()).extracting("name").contains("Frontend", "API", "Services", "Data");
        assertThat(result.components()).extracting("name").doesNotContain("UUID", "List", "ConcurrentHashMap");
        assertThat(result.flows()).isNotEmpty();
        assertThat(result.mermaid()).contains("graph LR");
        assertThat(result.summary().toLowerCase(Locale.ROOT)).containsAnyOf("layered", "modular");
    }
}
