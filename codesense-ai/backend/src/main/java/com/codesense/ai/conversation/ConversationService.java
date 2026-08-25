package com.codesense.ai.conversation;

import com.codesense.auth.model.User;
import com.codesense.auth.repository.UserRepository;
import com.codesense.common.exception.AccessDeniedException;
import com.codesense.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing conversation history.
 * Team Member 3 (Karthik) owns this service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final UserRepository userRepository;

    public List<ConversationSummaryDto> getConversations(String userEmail, UUID repositoryId) {
        User user = getUser(userEmail);
        List<Conversation> conversations =
            conversationRepository.findByUserIdAndRepositoryIdOrderByCreatedAtDesc(user.getId(), repositoryId);
        return conversations.stream().map(c -> toSummary(c, user)).collect(Collectors.toList());
    }

    public ConversationDetailDto getConversationDetail(String userEmail, UUID conversationId) {
        User user = getUser(userEmail);
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId.toString()));

        List<ConversationMessage> messages =
            messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        return ConversationDetailDto.builder()
            .id(conversation.getId())
            .title(conversation.getTitle())
            .status(conversation.getStatus().name())
            .repositoryId(conversation.getRepository().getId())
            .createdAt(conversation.getCreatedAt())
            .messages(messages.stream().map(this::toMessageDto).collect(Collectors.toList()))
            .build();
    }

    @Transactional
    public void deleteConversation(String userEmail, UUID conversationId) {
        User user = getUser(userEmail);
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId.toString()));
        conversationRepository.delete(conversation);
        log.info("Deleted conversation {} for user {}", conversationId, userEmail);
    }

    private ConversationSummaryDto toSummary(Conversation c, User user) {
        long msgCount = messageRepository.countByConversationId(c.getId());
        return ConversationSummaryDto.builder()
            .id(c.getId())
            .title(c.getTitle())
            .status(c.getStatus().name())
            .repositoryId(c.getRepository().getId())
            .repositoryName(c.getRepository().getName())
            .messageCount(msgCount)
            .createdAt(c.getCreatedAt())
            .updatedAt(c.getUpdatedAt())
            .build();
    }

    private ConversationDetailDto.MessageDto toMessageDto(ConversationMessage m) {
        return ConversationDetailDto.MessageDto.builder()
            .id(m.getId())
            .role(m.getRole().name())
            .content(m.getContent())
            .sources(m.getSources())
            .tokenCount(m.getTokenCount())
            .createdAt(m.getCreatedAt())
            .build();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
