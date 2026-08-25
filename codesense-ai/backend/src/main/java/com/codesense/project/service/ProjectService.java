package com.codesense.project.service;

import com.codesense.auth.model.User;
import com.codesense.auth.repository.UserRepository;
import com.codesense.common.exception.AccessDeniedException;
import com.codesense.common.exception.BadRequestException;
import com.codesense.common.exception.ResourceNotFoundException;
import com.codesense.project.dto.CreateProjectRequest;
import com.codesense.project.dto.ProjectDto;
import com.codesense.project.model.Project;
import com.codesense.project.model.ProjectStatus;
import com.codesense.project.repository.ProjectRepository;
import com.codesense.repository.repository.RepositoryRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final RepositoryRepo repositoryRepo;

    @Transactional
    public ProjectDto createProject(String email, CreateProjectRequest request) {
        User user = getUserByEmail(email);
        if (projectRepository.existsByUserIdAndName(user.getId(), request.getName())) {
            throw new BadRequestException("A project with name '" + request.getName() + "' already exists");
        }
        Project project = Project.builder()
            .user(user)
            .name(request.getName())
            .description(request.getDescription())
            .build();
        project = projectRepository.save(project);
        log.info("Project created: {} for user {}", project.getId(), email);
        return toDto(project);
    }

    public List<ProjectDto> getProjects(String email) {
        User user = getUserByEmail(email);
        return projectRepository
            .findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), ProjectStatus.ACTIVE)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public ProjectDto getProject(String email, UUID projectId) {
        Project project = getProjectForUser(email, projectId);
        return toDto(project);
    }

    @Transactional
    public void deleteProject(String email, UUID projectId) {
        Project project = getProjectForUser(email, projectId);
        project.setStatus(ProjectStatus.DELETED);
        projectRepository.save(project);
        log.info("Project soft-deleted: {} by user {}", projectId, email);
    }

    public Project getProjectForUser(String email, UUID projectId) {
        User user = getUserByEmail(email);
        return projectRepository.findByIdAndUserId(projectId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    private ProjectDto toDto(Project project) {
        long repoCount = repositoryRepo.countByProjectId(project.getId());
        return ProjectDto.builder()
            .id(project.getId())
            .name(project.getName())
            .description(project.getDescription())
            .status(project.getStatus().name())
            .repositoryCount(repoCount)
            .createdAt(project.getCreatedAt())
            .updatedAt(project.getUpdatedAt())
            .build();
    }
}
