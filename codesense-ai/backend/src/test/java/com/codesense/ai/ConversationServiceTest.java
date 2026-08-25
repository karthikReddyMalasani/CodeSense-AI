package com.codesense.ai;

import com.codesense.ai.conversation.*;
import com.codesense.auth.model.User;
import com.codesense.auth.repository.UserRepository;
import com.codesense.common.exception.ResourceNotFoundException;
import com.codesense.project.model.Project;
import com.codesense.repository.model.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConversationService.
 * Team Member 3 (Karthik) — conversation memory tests.
 */
@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock ConversationRepository conversationRepository;
    @Mock ConversationMessageRepository messageRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ConversationService conversationService;

    private User testUser;
    private UUID repositoryId;
    private UUID conversationId;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        repositoryId   = UUID.randomUUID();
        conversationId = UUID.randomUUID();
    }

    @Test
    void getConversations_returnsListForUser() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        Repository repo = new Repository();
        repo.setId(repositoryId);
        repo.setName("my-repo");

        Conversation conv = Conversation.builder()
            .id(conversationId)
            .user(testUser)
            .repository(repo)
            .project(new Project())
            .title("Test question")
            .build();

        when(conversationRepository.findByUserIdAndRepositoryIdOrderByCreatedAtDesc(testUser.getId(), repositoryId))
            .thenReturn(List.of(conv));
        when(messageRepository.countByConversationId(conversationId)).thenReturn(3L);

        List<ConversationSummaryDto> result = conversationService.getConversations("test@example.com", repositoryId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test question");
        assertThat(result.get(0).getMessageCount()).isEqualTo(3L);
    }

    @Test
    void getConversations_returnsEmptyListWhenNone() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(conversationRepository.findByUserIdAndRepositoryIdOrderByCreatedAtDesc(any(), any()))
            .thenReturn(List.of());

        List<ConversationSummaryDto> result =
            conversationService.getConversations("test@example.com", repositoryId);
        assertThat(result).isEmpty();
    }

    @Test
    void getConversationDetail_throwsWhenNotFound() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(conversationRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            conversationService.getConversationDetail("test@example.com", conversationId))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteConversation_throwsWhenNotFound() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(conversationRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            conversationService.deleteConversation("test@example.com", conversationId))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
