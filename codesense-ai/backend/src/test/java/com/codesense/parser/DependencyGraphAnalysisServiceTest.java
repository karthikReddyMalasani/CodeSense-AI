package com.codesense.parser;

import com.codesense.integration.parser.dto.CodeElementDTO;
import com.codesense.integration.parser.dto.CodeRelationshipDTO;
import com.codesense.integration.parser.dto.ParsedFileDTO;
import com.codesense.integration.parser.dto.ParsedRepositoryDTO;
import com.codesense.parser.service.DependencyGraphAnalysisService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyGraphAnalysisServiceTest {

    @Test
    void duplicateNoiseNamesDoNotPreventModelConstruction() throws Exception {
        DependencyGraphAnalysisService service = new DependencyGraphAnalysisService(null, null, Runnable::run);
        Method buildModel = DependencyGraphAnalysisService.class.getDeclaredMethod("buildModel", ParsedRepositoryDTO.class, Map.class);
        buildModel.setAccessible(true);

        ParsedRepositoryDTO parsed = ParsedRepositoryDTO.builder()
                .repositoryId("repository")
                .files(List.of(ParsedFileDTO.builder()
                        .filePath("src/map.js")
                        .elements(List.of(CodeElementDTO.builder().name("map").type("FUNCTION").build()))
                        .build()))
                .build();

        Object model = buildModel.invoke(service, parsed, Map.of());
        Field nodes = model.getClass().getDeclaredField("nodes");
        nodes.setAccessible(true);

        assertThat((List<?>) nodes.get(model)).isNotEmpty();
    }

    @Test
    void sameDisplayNameInDifferentFilesGetsDistinctNodeIds() throws Exception {
        DependencyGraphAnalysisService service = new DependencyGraphAnalysisService(null, null, Runnable::run);
        Method buildModel = DependencyGraphAnalysisService.class.getDeclaredMethod("buildModel", ParsedRepositoryDTO.class, Map.class);
        buildModel.setAccessible(true);

        ParsedRepositoryDTO parsed = ParsedRepositoryDTO.builder()
                .repositoryId("repository")
                .files(List.of(
                        fileWithSymbol("frontend/mapper.js", "map"),
                        fileWithSymbol("backend/mapper.py", "map")))
                .build();

        Object model = buildModel.invoke(service, parsed, Map.of());
        Field nodes = model.getClass().getDeclaredField("nodes");
        nodes.setAccessible(true);
        List<?> modelNodes = (List<?>) nodes.get(model);
        Method name = modelNodes.get(0).getClass().getDeclaredMethod("name");
        Method id = modelNodes.get(0).getClass().getDeclaredMethod("id");

        List<String> mapIds = modelNodes.stream()
                .filter(node -> "map".equals(invoke(name, node)))
                .map(node -> (String) invoke(id, node))
                .toList();

        assertThat(mapIds).hasSize(2).doesNotHaveDuplicates();
    }

    @Test
    void repeatedLogicalRelationshipIsDeduplicated() throws Exception {
        DependencyGraphAnalysisService service = new DependencyGraphAnalysisService(null, null, Runnable::run);
        Method buildModel = DependencyGraphAnalysisService.class.getDeclaredMethod("buildModel", ParsedRepositoryDTO.class, Map.class);
        buildModel.setAccessible(true);

        ParsedFileDTO file = ParsedFileDTO.builder()
                .filePath("src/AuthController.java")
                .elements(List.of(
                        CodeElementDTO.builder().name("AuthController").type("CLASS").build(),
                        CodeElementDTO.builder().name("AuthService").type("CLASS").build()))
                .relationships(List.of(
                        relationship("AuthController", "AuthService"),
                        relationship("AuthController", "AuthService")))
                .build();

        Object model = buildModel.invoke(service, ParsedRepositoryDTO.builder()
                .repositoryId("repository").files(List.of(file)).build(), Map.of());
        Field edges = model.getClass().getDeclaredField("edges");
        edges.setAccessible(true);

        long callEdges = ((List<?>) edges.get(model)).stream()
                .filter(edge -> "CALLS".equals(invokeDeclared(edge, "type")))
                .count();
        assertThat(callEdges).isEqualTo(1);
    }

    @Test
    void semanticGraphIgnoresFilesystemContainmentArtifacts() throws Exception {
        DependencyGraphAnalysisService service = new DependencyGraphAnalysisService(null, null, Runnable::run);
        Method buildModel = DependencyGraphAnalysisService.class.getDeclaredMethod("buildModel", ParsedRepositoryDTO.class, Map.class);
        buildModel.setAccessible(true);

        ParsedFileDTO controllerFile = ParsedFileDTO.builder()
                .filePath("backend/src/main/java/com/codesense/controller/AuthController.java")
                .language("Java")
                .elements(List.of(
                        CodeElementDTO.builder().name("AuthController").type("CLASS").build(),
                        CodeElementDTO.builder().name("AuthService").type("CLASS").build()))
                .relationships(List.of(
                        relationship("AuthController", "AuthService")))
                .build();

        ParsedFileDTO metadataFile = ParsedFileDTO.builder()
                .filePath(".gitignore")
                .language("Text")
                .elements(List.of(CodeElementDTO.builder().name(".gitignore").type("MODULE").build()))
                .build();

        Object model = buildModel.invoke(service, ParsedRepositoryDTO.builder()
                .repositoryId("repository")
                .files(List.of(controllerFile, metadataFile))
                .build(), Map.of());

        Field edges = model.getClass().getDeclaredField("edges");
        edges.setAccessible(true);
        List<?> edgeList = (List<?>) edges.get(model);

        assertThat(edgeList).noneMatch(edge -> invokeDeclared(edge, "type").equals("CONTAINS"));
        assertThat(edgeList).anyMatch(edge -> String.valueOf(invokeDeclared(edge, "source")).contains("AuthController")
                && String.valueOf(invokeDeclared(edge, "target")).contains("AuthService"));
    }

    @Test
    void externalDependenciesIncludeRuntimeBackendsAndAiProviders() throws Exception {
        DependencyGraphAnalysisService service = new DependencyGraphAnalysisService(null, null, Runnable::run);
        Method detectExternal = DependencyGraphAnalysisService.class.getDeclaredMethod("detectExternal", List.class);
        detectExternal.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, DependencyGraphAnalysisService.External> externals = (Map<String, DependencyGraphAnalysisService.External>) detectExternal.invoke(service, List.of(
                new com.codesense.repository.model.RepositoryFile() {{
                    setFilePath("pom.xml");
                    setContent("<project><dependencies><dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency></dependencies></project>");
                }},
                new com.codesense.repository.model.RepositoryFile() {{
                    setFilePath("docker-compose.yml");
                    setContent("services:\n  postgres:\n    image: postgres:16\n  redis:\n    image: redis:7\n");
                }},
                new com.codesense.repository.model.RepositoryFile() {{
                    setFilePath("ai.py");
                    setContent("client = GeminiClient(api_key=os.getenv('GEMINI_API_KEY'))");
                }}
        ));

        assertThat(externals.values()).extracting(DependencyGraphAnalysisService.External::name)
                .contains("Spring Boot", "PostgreSQL", "AI Provider");
    }

    private static ParsedFileDTO fileWithSymbol(String path, String symbol) {
        return ParsedFileDTO.builder().filePath(path)
                .elements(List.of(CodeElementDTO.builder().name(symbol).type("FUNCTION").build()))
                .build();
    }

    private static CodeRelationshipDTO relationship(String source, String target) {
        return CodeRelationshipDTO.builder()
                .sourceElement(source)
                .targetElement(target)
                .relationshipType("CALLS")
                .build();
    }

    private static Object invoke(Method method, Object target) {
        try {
            return method.invoke(target);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

        private static Object invokeDeclared(Object target, String methodName) {
                try {
                        return target.getClass().getDeclaredMethod(methodName).invoke(target);
                } catch (ReflectiveOperationException error) {
                        throw new AssertionError(error);
                }
        }
}
