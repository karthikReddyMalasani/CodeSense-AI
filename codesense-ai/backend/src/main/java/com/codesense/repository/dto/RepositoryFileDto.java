package com.codesense.repository.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RepositoryFileDto {
    private UUID id;
    private String filePath;
    private String fileName;
    private String extension;
    private String language;
    private long sizeBytes;
    private int lineCount;
    private String content;
    private boolean binary;
}
