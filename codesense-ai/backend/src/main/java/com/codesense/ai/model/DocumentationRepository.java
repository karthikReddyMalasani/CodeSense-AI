package com.codesense.ai.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentationRepository extends JpaRepository<Documentation, UUID> {
    Optional<Documentation> findTopByRepositoryIdAndDocTypeOrderByCreatedAtDesc(
        UUID repositoryId, Documentation.DocType docType);
    boolean existsByRepositoryIdAndDocType(UUID repositoryId, Documentation.DocType docType);
}
