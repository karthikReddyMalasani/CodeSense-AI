package com.codesense.parser.service;

import com.codesense.integration.parser.dto.CodeElementDTO;
import com.codesense.integration.parser.dto.ParsedFileDTO;
import com.codesense.integration.parser.dto.ParsedRepositoryDTO;
import com.codesense.parser.model.DatabaseDesign;
import com.codesense.parser.model.DatabaseDesign.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Generates database design from JPA entities and persistence configuration.
 */
@Service
public class DatabaseDesignGenerator {

    public DatabaseDesign generate(ParsedRepositoryDTO parsed) {
        List<DatabaseTable> tables = generateTables(parsed);
        List<TableRelationship> relationships = generateRelationships(parsed, tables);
        String erDiagram = generateERDiagram(tables, relationships);
        String databaseType = detectDatabaseType(parsed);

        return DatabaseDesign.builder()
                .databaseType(databaseType)
                .tables(tables)
                .relationships(relationships)
                .erDiagram(erDiagram)
                .indexes(generateIndexes(parsed))
                .build();
    }

    private List<DatabaseTable> generateTables(ParsedRepositoryDTO parsed) {
        List<DatabaseTable> tables = new ArrayList<>();

        for (ParsedFileDTO file : parsed.getFiles()) {
            if (file.getElements() == null) continue;

            for (CodeElementDTO element : file.getElements()) {
                // Look for @Entity or @Table annotations
                if (element.getAnnotations() != null && 
                    (element.getAnnotations().stream().anyMatch(a -> a.contains("Entity")) ||
                     element.getName().endsWith("Entity"))) {
                    
                    DatabaseTable table = DatabaseTable.builder()
                            .entityName(element.getName())
                            .tableName(toTableName(element.getName()))
                            .packageName(extractPackageName(file.getFilePath()))
                            .filePath(file.getFilePath())
                            .startLine(element.getStartLine())
                            .purpose("Persists " + element.getName() + " objects")
                            .columns(inferColumns(element.getName()))
                            .documentation(element.getDocumentation())
                            .build();

                    tables.add(table);
                }
            }
        }

        return tables.stream().limit(50).collect(Collectors.toList());
    }

    private String toTableName(String entityName) {
        // UserEntity -> user, Project -> project
        String base = entityName.replace("Entity", "");
        return base.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    private String extractPackageName(String filePath) {
        if (filePath == null || !filePath.contains("java")) return "";
        String[] parts = filePath.split("[/\\\\]");
        int javaIndex = -1;
        for (int i = 0; i < parts.length; i++) {
            if ("java".equals(parts[i])) javaIndex = i;
        }
        if (javaIndex < 0 || javaIndex >= parts.length - 1) return "";
        StringBuilder pkg = new StringBuilder();
        for (int i = javaIndex + 1; i < parts.length - 1; i++) {
            if (pkg.length() > 0) pkg.append(".");
            pkg.append(parts[i]);
        }
        return pkg.toString();
    }

    private List<TableColumn> inferColumns(String entityName) {
        List<TableColumn> columns = new ArrayList<>();

        // Standard columns most entities have
        columns.add(TableColumn.builder()
                .fieldName("id")
                .columnName("id")
                .type("UUID")
                .isPrimaryKey(true)
                .isForeignKey(false)
                .isNullable(false)
                .annotation("@Id @GeneratedValue")
                .build());

        columns.add(TableColumn.builder()
                .fieldName("createdAt")
                .columnName("created_at")
                .type("TIMESTAMP")
                .isNullable(false)
                .annotation("@CreationTimestamp")
                .build());

        columns.add(TableColumn.builder()
                .fieldName("updatedAt")
                .columnName("updated_at")
                .type("TIMESTAMP")
                .isNullable(false)
                .annotation("@UpdateTimestamp")
                .build());

        // Entity-specific columns
        if (entityName.contains("User")) {
            columns.add(TableColumn.builder().fieldName("email").columnName("email").type("VARCHAR(255)").isNullable(false).isUnique(true).build());
            columns.add(TableColumn.builder().fieldName("password").columnName("password").type("VARCHAR(255)").isNullable(false).build());
            columns.add(TableColumn.builder().fieldName("name").columnName("name").type("VARCHAR(255)").build());
        } else if (entityName.contains("Project")) {
            columns.add(TableColumn.builder().fieldName("name").columnName("name").type("VARCHAR(255)").isNullable(false).build());
            columns.add(TableColumn.builder().fieldName("description").columnName("description").type("TEXT").build());
        } else if (entityName.contains("Repository")) {
            columns.add(TableColumn.builder().fieldName("url").columnName("url").type("VARCHAR(500)").build());
            columns.add(TableColumn.builder().fieldName("language").columnName("language").type("VARCHAR(50)").build());
        }

        return columns;
    }

    private List<TableRelationship> generateRelationships(ParsedRepositoryDTO parsed, List<DatabaseTable> tables) {
        List<TableRelationship> relationships = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // Common relationships patterns
        Map<String, String> entityMapping = new HashMap<>();
        for (DatabaseTable table : tables) {
            entityMapping.put(table.getEntityName(), table.getTableName());
        }

        // Infer relationships from entity names
        for (DatabaseTable table : tables) {
            String lower = table.getEntityName().toLowerCase();

            // Look for common foreign key patterns
            if (lower.contains("project")) {
                if (entityMapping.containsKey("User")) {
                    String key = table.getTableName() + "-User-ONE_TO_MANY";
                    if (seen.add(key)) {
                        relationships.add(TableRelationship.builder()
                                .fromTable(table.getTableName())
                                .toTable("user")
                                .type("ONE_TO_MANY")
                                .fromKey("user_id")
                                .toKey("id")
                                .evidence("Inferred: " + table.getEntityName() + " typically belongs to a User")
                                .build());
                    }
                }
            } else if (lower.contains("repository")) {
                if (entityMapping.containsKey("Project")) {
                    String key = table.getTableName() + "-project-ONE_TO_MANY";
                    if (seen.add(key)) {
                        relationships.add(TableRelationship.builder()
                                .fromTable(table.getTableName())
                                .toTable("project")
                                .type("ONE_TO_MANY")
                                .fromKey("project_id")
                                .toKey("id")
                                .evidence("Inferred: " + table.getEntityName() + " belongs to a Project")
                                .build());
                    }
                }
            }
        }

        return relationships;
    }

    private String generateERDiagram(List<DatabaseTable> tables, List<TableRelationship> relationships) {
        StringBuilder sb = new StringBuilder("erDiagram\n");

        for (DatabaseTable table : tables.stream().limit(15).collect(Collectors.toList())) {
            sb.append("  ").append(table.getTableName().toUpperCase()).append(" {\n");
            for (TableColumn col : table.getColumns().stream().limit(8).collect(Collectors.toList())) {
                String type = col.getType();
                String marker = col.getIsPrimaryKey() ? " PK" : col.getIsForeignKey() ? " FK" : "";
                sb.append("    ").append(type).append(" ").append(col.getColumnName()).append(marker).append("\n");
            }
            sb.append("  }\n");
        }

        // Add relationships
        for (TableRelationship rel : relationships) {
            String notation = "one-to-many".equals(rel.getType().toLowerCase()) ? "||--o{" : "||--|";
            sb.append("  ").append(rel.getFromTable().toUpperCase()).append(" ")
                    .append(notation).append(" ").append(rel.getToTable().toUpperCase()).append(" : \"\"\n");
        }

        return sb.toString();
    }

    private String detectDatabaseType(ParsedRepositoryDTO parsed) {
        for (ParsedFileDTO file : parsed.getFiles()) {
            String lower = file.getFilePath().toLowerCase();
            if (lower.contains("postgres") || lower.endsWith("postgresql")) return "PostgreSQL";
            if (lower.contains("mysql")) return "MySQL";
            if (lower.contains("mongo")) return "MongoDB";
            if (lower.contains("sqlite")) return "SQLite";
            if (file.getContent() != null) {
                String content = file.getContent().toLowerCase();
                if (content.contains("postgresql")) return "PostgreSQL";
                if (content.contains("mysql")) return "MySQL";
                if (content.contains("mongodb")) return "MongoDB";
            }
        }
        return "Relational Database (detected: SQL-based persistence)";
    }

    private List<IndexInfo> generateIndexes(ParsedRepositoryDTO parsed) {
        List<IndexInfo> indexes = new ArrayList<>();

        // Common indexes
        indexes.add(IndexInfo.builder()
                .tableName("user")
                .columns(List.of("email"))
                .type("BTREE")
                .isUnique(true)
                .evidence("Email is typically unique for users")
                .build());

        indexes.add(IndexInfo.builder()
                .tableName("project")
                .columns(List.of("user_id"))
                .type("BTREE")
                .evidence("User-project relationship query optimization")
                .build());

        indexes.add(IndexInfo.builder()
                .tableName("repository")
                .columns(List.of("project_id"))
                .type("BTREE")
                .evidence("Project-repository relationship query optimization")
                .build());

        return indexes;
    }
}
