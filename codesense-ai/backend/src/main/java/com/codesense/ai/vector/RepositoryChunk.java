package com.codesense.ai.vector;

import com.codesense.project.model.Project;
import com.codesense.repository.model.Repository;
import com.codesense.repository.model.RepositoryFile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * A chunk of repository source code with its vector embedding.
 * Stored in PostgreSQL with PGVector for semantic search.
 *
 * SECURITY: Always filter by project_id AND repository_id. Never allow cross-project access.
 */
@Entity
@Table(name = "repository_chunks")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class RepositoryChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private RepositoryFile file;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "language")
    private String language;

    @Column(name = "symbol_name")
    private String symbolName;

    @Column(name = "symbol_type")
    private String symbolType;

    @Enumerated(EnumType.STRING)
    @Column(name = "chunk_type", nullable = false)
    @Builder.Default
    private ChunkType chunkType = ChunkType.TEXT;

    @Column(name = "chunk_index")
    @Builder.Default
    private int chunkIndex = 0;

    @Column(name = "start_line")
    private Integer startLine;

    @Column(name = "end_line")
    private Integer endLine;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_hash")
    private String contentHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    /**
     * The embedding vector stored as native vector(768) in PGVector.
     * Dimensions must match EMBEDDING_DIMENSION configuration.
     */
    @Column(name = "embedding", columnDefinition = "vector(768)")
    private float[] embedding;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public enum ChunkType {
        TEXT,           // Plain text fallback chunk
        CLASS,          // Java/Python/etc. class
        METHOD,         // Method or function
        FUNCTION,       // Standalone function
        MODULE,         // Module or package summary
        INTERFACE,      // Interface definition
        ENUM,           // Enum type
        DOCUMENTATION,  // Comment blocks, JSDoc, JavaDoc
        CONFIGURATION   // Config files (YAML, JSON, etc.)
    }
}
