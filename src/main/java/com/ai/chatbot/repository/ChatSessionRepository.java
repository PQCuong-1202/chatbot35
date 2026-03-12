package com.ai.chatbot.repository;

import com.ai.chatbot.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    Optional<ChatSession> findBySessionIdAndUserId(String sessionId, Long userId);
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);
}