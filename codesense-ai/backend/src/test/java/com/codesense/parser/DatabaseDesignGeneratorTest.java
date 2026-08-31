package com.codesense.parser;

import com.codesense.integration.parser.dto.CodeElementDTO;
import com.codesense.integration.parser.dto.ParsedFileDTO;
import com.codesense.integration.parser.dto.ParsedRepositoryDTO;
import com.codesense.parser.model.DatabaseDesign;
import com.codesense.parser.service.DatabaseDesignGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DatabaseDesignGeneratorTest {

    @Test
    void preservesUnknownPrimaryKeyMetadataWithoutFailing() {
        ParsedRepositoryDTO parsed = ParsedRepositoryDTO.builder()
                .repositoryName("sample")
                .files(List.of(ParsedFileDTO.builder()
                        .filePath("src/main/java/example/UserEntity.java")
                        .elements(List.of(CodeElementDTO.builder()
                                .name("UserEntity")
                                .type("CLASS")
                                .annotations(List.of("@Entity"))
                                .build()))
                        .build()))
                .build();

        DatabaseDesign design = new DatabaseDesignGenerator().generate(parsed);
        List<DatabaseDesign.TableColumn> columns = design.getTables().get(0).getColumns();

        assertEquals(Boolean.TRUE, columns.get(0).getIsPrimaryKey());
        assertNull(columns.get(1).getIsPrimaryKey());
        assertEquals(Boolean.FALSE, columns.get(0).getIsForeignKey());
    }
}
