package com.codesense.project.repository;

import com.codesense.project.model.Project;
import com.codesense.project.model.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, ProjectStatus status);
    Page<Project> findByUserIdAndStatus(UUID userId, ProjectStatus status, Pageable pageable);
    Optional<Project> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByUserIdAndName(UUID userId, String name);
    long countByUserId(UUID userId);
}
