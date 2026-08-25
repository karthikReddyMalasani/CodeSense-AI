package com.codesense.ai.conversation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findByUserIdAndRepositoryIdOrderByCreatedAtDesc(UUID userId, UUID repositoryId);
    Optional<Conversation> findByIdAndUserId(UUID id, UUID userId);
    Page<Conversation> findByUserId(UUID userId, Pageable pageable);
    void deleteByRepositoryId(UUID repositoryId);
}
