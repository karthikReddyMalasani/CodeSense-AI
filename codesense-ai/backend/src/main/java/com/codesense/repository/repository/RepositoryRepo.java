package com.codesense.repository.repository;

import com.codesense.repository.model.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositoryRepo extends JpaRepository<Repository, UUID> {
    List<Repository> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    Optional<Repository> findByIdAndProjectId(UUID id, UUID projectId);
    Optional<Repository> findByIdAndProjectUserId(UUID id, UUID userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select repository from Repository repository where repository.id = :repositoryId")
    Optional<Repository> findByIdForUpdate(@Param("repositoryId") UUID repositoryId);
    long countByProjectId(UUID projectId);
    boolean existsByProjectIdAndName(UUID projectId, String name);
}
