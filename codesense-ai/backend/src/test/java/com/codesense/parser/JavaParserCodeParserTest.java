package com.codesense.parser;

import com.codesense.parser.core.JavaParserCodeParser;
import com.codesense.parser.model.CodeElement;
import com.codesense.parser.model.CodeMetrics;
import com.codesense.parser.model.CodeRelationship;
import com.codesense.parser.model.ParsedFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JavaParser code parser.
 * Team Member 5 (Vishnu) — Testing.
 */
class JavaParserCodeParserTest {

    private JavaParserCodeParser parser;

    @BeforeEach
    void setUp() {
        parser = new JavaParserCodeParser();
    }

    @Test
    void supports_java_language() {
        assertThat(parser.getSupportedLanguages()).contains("Java");
        assertThat(parser.supports("Java")).isTrue();
        assertThat(parser.supports("Python")).isFalse();
    }

    @Test
    void parse_simpleClass_extractsClassAndMethods() {
        String code = """
            package com.example;
            
            import java.util.List;
            
            public class UserService {
                private UserRepository repository;
                
                public UserService(UserRepository repository) {
                    this.repository = repository;
                }
                
                public List<User> findAll() {
                    return repository.findAll();
                }
                
                public User findById(Long id) {
                    return repository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Not found"));
                }
            }
            """;

        ParsedFile result = parser.parse("UserService.java", code, "Java");

        assertThat(result).isNotNull();
        assertThat(result.getFilePath()).isEqualTo("UserService.java");
        assertThat(result.getLanguage()).isEqualTo("Java");
        assertThat(result.getElements()).isNotEmpty();

        // Class should be found
        Optional<CodeElement> cls = result.getElements().stream()
            .filter(e -> e.getType() == CodeElement.ElementType.CLASS && e.getName().equals("UserService"))
            .findFirst();
        assertThat(cls).isPresent();

        // Methods should be found
        List<CodeElement> methods = result.getElements().stream()
            .filter(e -> e.getType() == CodeElement.ElementType.METHOD)
            .toList();
        assertThat(methods).extracting(CodeElement::getName)
            .contains("findAll", "findById");

        // Import relationship should be found
        List<CodeRelationship> imports = result.getRelationships().stream()
            .filter(r -> r.getType() == CodeRelationship.RelationshipType.IMPORTS)
            .toList();
        assertThat(imports).isNotEmpty();
        assertThat(imports).extracting(CodeRelationship::getTargetElement)
            .contains("java.util.List");
    }

    @Test
    void parse_interface_extractsInterface() {
        String code = """
            public interface UserRepository {
                List<User> findAll();
                Optional<User> findById(Long id);
            }
            """;

        ParsedFile result = parser.parse("UserRepository.java", code, "Java");

        Optional<CodeElement> iface = result.getElements().stream()
            .filter(e -> e.getType() == CodeElement.ElementType.INTERFACE)
            .findFirst();
        assertThat(iface).isPresent();
        assertThat(iface.get().getName()).isEqualTo("UserRepository");
    }

    @Test
    void parse_inheritance_extractsExtendsRelationship() {
        String code = """
            public class AdminUser extends User implements Serializable {
                private String adminLevel;
            }
            """;

        ParsedFile result = parser.parse("AdminUser.java", code, "Java");

        List<CodeRelationship> extends_ = result.getRelationships().stream()
            .filter(r -> r.getType() == CodeRelationship.RelationshipType.EXTENDS)
            .toList();
        assertThat(extends_).isNotEmpty();
        assertThat(extends_.get(0).getSourceElement()).isEqualTo("AdminUser");
        assertThat(extends_.get(0).getTargetElement()).isEqualTo("User");

        List<CodeRelationship> implements_ = result.getRelationships().stream()
            .filter(r -> r.getType() == CodeRelationship.RelationshipType.IMPLEMENTS)
            .toList();
        assertThat(implements_).isNotEmpty();
    }

    @Test
    void parse_emptyContent_returnsEmptyElements() {
        ParsedFile result = parser.parse("Empty.java", "", "Java");
        assertThat(result).isNotNull();
        assertThat(result.getElements()).isEmpty();
    }

    @Test
    void parse_nullContent_returnsEmptyElements() {
        ParsedFile result = parser.parse("Null.java", null, "Java");
        assertThat(result).isNotNull();
        assertThat(result.getElements()).isEmpty();
    }

    @Test
    void parse_invalidJava_returnsPartialResult() {
        String code = "this is not valid java code { {{ }";
        ParsedFile result = parser.parse("Invalid.java", code, "Java");
        assertThat(result).isNotNull();
        // Should not throw, just return what could be parsed
    }

    @Test
    void calculateMetrics_simpleClass_returnsCorrectMetrics() {
        String code = """
            // User entity
            public class User {
                private Long id;
                private String name;
                
                public Long getId() { return id; }
                public void setId(Long id) { this.id = id; }
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            }
            """;

        CodeMetrics metrics = parser.calculateMetrics("User.java", code, "Java");

        assertThat(metrics).isNotNull();
        assertThat(metrics.getTotalLines()).isGreaterThan(0);
        assertThat(metrics.getClassCount()).isEqualTo(1);
        assertThat(metrics.getMethodCount()).isGreaterThan(0);
        assertThat(metrics.getCommentLines()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void parse_enum_extractsEnum() {
        String code = """
            public enum Status {
                ACTIVE, INACTIVE, PENDING;
                
                public boolean isActive() {
                    return this == ACTIVE;
                }
            }
            """;

        ParsedFile result = parser.parse("Status.java", code, "Java");

        Optional<CodeElement> enumEl = result.getElements().stream()
            .filter(e -> e.getType() == CodeElement.ElementType.ENUM)
            .findFirst();
        assertThat(enumEl).isPresent();
        assertThat(enumEl.get().getName()).isEqualTo("Status");
    }
}
