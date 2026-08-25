package com.codesense.ai.conversation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationDetailDto {
    private UUID id;
    private String title;
    private String status;
    private UUID repositoryId;
    private Instant createdAt;
    private List<MessageDto> messages;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MessageDto {
        private UUID id;
        private String role;
        private String content;
        private String sources;
        private Integer tokenCount;
        private Instant createdAt;
    }
}
