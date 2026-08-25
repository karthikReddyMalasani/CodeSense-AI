package com.codesense.repository.model;

import com.codesense.project.model.Project;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "repositories")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Repository {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    @Column(name = "github_url")
    private String githubUrl;

    @Column(name = "github_owner")
    private String githubOwner;

    @Column(name = "github_repo")
    private String githubRepo;

    @Column(name = "default_branch")
    private String defaultBranch;

    @Column(name = "local_path")
    private String localPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RepositoryStatus status = RepositoryStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false)
    @Builder.Default
    private AnalysisStatus analysisStatus = AnalysisStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingestion_status", nullable = false)
    @Builder.Default
    private IngestionStatus ingestionStatus = IngestionStatus.PENDING;

    @Column(name = "total_files")
    @Builder.Default
    private int totalFiles = 0;

    @Column(name = "total_chunks")
    @Builder.Default
    private int totalChunks = 0;

    @Column(name = "languages", columnDefinition = "text")
    @Convert(converter = com.codesense.common.config.StringListConverter.class)
    @Builder.Default
    private List<String> languages = new ArrayList<>();

    @Column(name = "primary_language")
    private String primaryLanguage;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
