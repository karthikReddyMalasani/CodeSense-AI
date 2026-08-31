package com.codesense.parser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Database design extracted from JPA entities and persistence configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatabaseDesign {

    /** Database technology detected */
    private String databaseType; // PostgreSQL, MySQL, MongoDB, etc.

    /** Database entities/tables */
    private List<DatabaseTable> tables;

    /** Entity relationships */
    private List<TableRelationship> relationships;

    /** ER diagram in Mermaid format */
    private String erDiagram;

    /** Indexes and performance notes */
    private List<IndexInfo> indexes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DatabaseTable {
        private String entityName; // Java class name
        private String tableName;
        private String packageName;
        private String filePath;
        private Integer startLine;
        private String purpose;
        private List<TableColumn> columns;
        private String documentation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TableColumn {
        private String fieldName;
        private String columnName;
        private String type;
        private Boolean isPrimaryKey;
        private Boolean isForeignKey;
        private String referencedTable;
        private String referencedColumn;
        private Boolean isNullable;
        private Boolean isUnique;
        private String defaultValue;
        private String annotation; // @Column, @Id, etc.
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TableRelationship {
        private String fromTable;
        private String toTable;
        private String type; // ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY
        private String fromKey;
        private String toKey;
        private String evidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IndexInfo {
        private String tableName;
        private List<String> columns;
        private String type; // BTREE, HASH, etc.
        private Boolean isUnique;
        private String evidence;
    }
}
