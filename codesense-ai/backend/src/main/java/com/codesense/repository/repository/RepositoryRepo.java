package com.codesense.repository.repository;

import com.codesense.repository.model.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositoryRepo extends JpaRepository<Repository, UUID> {
    List<Repository> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    Optional<Repository> findByIdAndProjectId(UUID id, UUID projectId);
    Optional<Repository> findByIdAndProjectUserId(UUID id, UUID userId);
    long countByProjectId(UUID projectId);
    boolean existsByProjectIdAndName(UUID projectId, String name);
}
