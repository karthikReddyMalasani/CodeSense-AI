package com.codesense.project;

import com.codesense.auth.model.User;
import com.codesense.auth.repository.UserRepository;
import com.codesense.common.exception.BadRequestException;
import com.codesense.common.exception.ResourceNotFoundException;
import com.codesense.project.dto.CreateProjectRequest;
import com.codesense.project.model.Project;
import com.codesense.project.model.ProjectStatus;
import com.codesense.project.repository.ProjectRepository;
import com.codesense.project.service.ProjectService;
import com.codesense.repository.repository.RepositoryRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock ProjectRepository projectRepository;
    @Mock UserRepository userRepository;
    @Mock RepositoryRepo repositoryRepo;

    @InjectMocks ProjectService projectService;

    private User testUser() {
        return User.builder().id(UUID.randomUUID()).email("test@example.com").name("Test").build();
    }

    @Test
    void createProject_success() {
        User user = testUser();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(projectRepository.existsByUserIdAndName(user.getId(), "My Project")).thenReturn(false);
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repositoryRepo.countByProjectId(any())).thenReturn(0L);

        CreateProjectRequest req = new CreateProjectRequest();
        req.setName("My Project");
        req.setDescription("Test description");

        var result = projectService.createProject("test@example.com", req);
        assertThat(result.getName()).isEqualTo("My Project");
    }

    @Test
    void createProject_duplicateName_throws() {
        User user = testUser();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(projectRepository.existsByUserIdAndName(user.getId(), "Dup")).thenReturn(true);

        CreateProjectRequest req = new CreateProjectRequest();
        req.setName("Dup");

        assertThatThrownBy(() -> projectService.createProject("test@example.com", req))
            .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getProject_wrongUser_throws() {
        User user = testUser();
        UUID projectId = UUID.randomUUID();
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(user));
        when(projectRepository.findByIdAndUserId(projectId, user.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProject("other@example.com", projectId))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
