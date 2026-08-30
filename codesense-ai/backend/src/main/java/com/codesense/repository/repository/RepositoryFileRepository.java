package com.codesense.repository.repository;

import com.codesense.repository.model.RepositoryFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositoryFileRepository extends JpaRepository<RepositoryFile, UUID> {
    List<RepositoryFile> findByRepositoryIdAndIgnoredFalse(UUID repositoryId);
    Page<RepositoryFile> findByRepositoryId(UUID repositoryId, Pageable pageable);
    Optional<RepositoryFile> findByIdAndRepositoryId(UUID id, UUID repositoryId);
    List<RepositoryFile> findByRepositoryIdAndLanguage(UUID repositoryId, String language);
    long countByRepositoryId(UUID repositoryId);
    @Modifying
    @Transactional
    @Query("delete from RepositoryFile file where file.repository.id = :repositoryId")
    void deleteByRepositoryId(@Param("repositoryId") UUID repositoryId);
}
