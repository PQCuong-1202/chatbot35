package com.ai.chatbot.service;

import com.ai.chatbot.model.ChatMessage;
import com.ai.chatbot.model.ChatSession;
import com.ai.chatbot.model.User;
import com.ai.chatbot.model.dto.ChatResponseDto;
import com.ai.chatbot.repository.ChatMessageRepository;
import com.ai.chatbot.repository.ChatSessionRepository;
import com.ai.chatbot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RestTemplate restTemplate;
    private final UserRepository userRepository;

    public ChatService(
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            RestTemplate restTemplate,
            UserRepository userRepository
    ) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.restTemplate = restTemplate;
        this.userRepository = userRepository;
    }

    @Transactional
    public Map<String, Object> processMessageViaN8n(
            String sessionId,
            String userMessage,
            Long userId

    ) {
        User user = userRepository.findById(userId).orElse(null);

        String major = null;
        String course = null;

        if (user != null) {
            major = user.getMajor();
            course = user.getCourse();
        }
        ChatSession session = getOrCreateSession(sessionId, userId);

        // 1. Lưu message USER
        saveMessage(sessionId, userMessage, ChatMessage.MessageType.USER, userId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("message", userMessage);
        payload.put("major", major);
        payload.put("course", course);

        Map<String, Object> aiData;
        try {
            aiData = restTemplate.postForObject(
                    "https://n8n-zlq3.onrender.com/webhook/chat",
                    payload,
                    Map.class
            );
        } catch (Exception e) {
            log.error("Call n8n failed", e);
            aiData = Map.of("ai_content", "Lỗi kết nối AI");
        }
        /* ===========================
         * KIỂM TRA BE NHẬN GÌ TỪ N8N
         * =========================== */
        if (aiData == null) {
            log.warn("N8N RESPONSE = null");
        } else {
            log.warn("N8N RESPONSE CLASS = {}", aiData.getClass().getName());
            log.warn("N8N RESPONSE VALUE = {}", aiData);
            log.warn("N8N RESPONSE KEYS = {}", aiData.keySet());
        }
        // 2. Chuẩn hoá dữ liệu AI (fallback trước khi lưu)
        boolean noContent = false;

        if (aiData == null || aiData.isEmpty()) {
            noContent = true;
        } else {
            Object aiText = aiData.get("ai_content");
            Object lh = aiData.get("LH");
            noContent = (aiText == null && lh == null);
        }

        Map<String, Object> normalized = new HashMap<>();

        if (noContent) {
            normalized.put("ai_content", "AI đang gặp lỗi. Vui lòng thử lại.");
            normalized.put("LH", null);
            normalized.put("raw", aiData);
        } else {
            normalized.put("ai_content", aiData.get("ai_content"));
            normalized.put("LH", aiData.get("LH"));
            normalized.put("raw", aiData);
        }

// 3. Lưu DB bằng dữ liệu đã chuẩn hoá
        try {
            String aiJson = objectMapper.writeValueAsString(normalized);
            saveMessage(sessionId, aiJson, ChatMessage.MessageType.AI, userId);
        } catch (Exception e) {
            log.error("Serialize AI response failed", e);
            saveMessage(sessionId, "AI response serialize error", ChatMessage.MessageType.AI, userId);
        }

// 4. Update session
        session.setLastMessage(userMessage);
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.save(session);

        log.warn("FINAL response to FE = {}", normalized);
        return normalized;
    }

    @Transactional
    public ChatSession getOrCreateSession(String sessionId, Long userId) {
        return chatSessionRepository
                .findBySessionIdAndUserId(sessionId, userId)
                .orElseGet(() -> {
                    ChatSession s = new ChatSession();
                    s.setSessionId(sessionId);
                    s.setTitle("Chat - " + LocalDateTime.now());
                    s.setCreatedAt(LocalDateTime.now());
                    s.setUpdatedAt(LocalDateTime.now());
                    s.setUserId(userId);
                    return chatSessionRepository.save(s);
                });
    }

    @Transactional
    public ChatMessage saveMessage(
            String sessionId,
            String message,
            ChatMessage.MessageType type,
            Long userId
    ) {
        ChatMessage m = new ChatMessage();
        m.setSessionId(sessionId);
        m.setMessage(message);
        m.setType(type);
        m.setTimestamp(LocalDateTime.now());
        m.setUserId(userId);
        return chatMessageRepository.save(m);
    }

    @Transactional(readOnly = true)
    public List<ChatResponseDto> getChatHistory(String sessionId, Long userId) {
        List<ChatMessage> messages =
                chatMessageRepository.findBySessionIdAndUserIdOrderByTimestampAsc(sessionId, userId);

        List<ChatResponseDto> result = new ArrayList<>();
        for (ChatMessage m : messages) {
            result.add(ChatResponseDto.builder()
                    .sessionId(m.getSessionId())
                    .message(m.getMessage())
                    .sender(m.getType().name())
                    .timestamp(m.getTimestamp())
                    .userId(m.getUserId())
                    .build());
        }
        return result;
    }

    @Transactional
    public void clearChatHistory(String sessionId, Long userId) {
        chatMessageRepository.deleteBySessionIdAndUserId(sessionId, userId);
        chatSessionRepository.findBySessionIdAndUserId(sessionId, userId)
                .ifPresent(s -> {
                    s.setLastMessage(null);
                    chatSessionRepository.save(s);
                });
    }

    @Transactional(readOnly = true)
    public List<ChatSession> getUserChatSessions(Long userId) {
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }
}