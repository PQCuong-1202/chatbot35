package com.ai.chatbot.controller;

import com.ai.chatbot.model.ChatSession;
import com.ai.chatbot.model.dto.ChatResponseDto;
import com.ai.chatbot.service.ChatService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @RequestBody Map<String, Object> body,
            HttpSession session
    ) {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("ai_content", "Người dùng chưa đăng nhập"));
        }

        String sessionId = (String) body.get("sessionId");
        String message = (String) body.get("message");

        if (sessionId == null || message == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ai_content", "Thiếu dữ liệu chat"));
        }

        // GỌI SERVICE MỚI: KHÔNG AI LOCAL
        Map<String, Object> aiResult =
                chatService.processMessageViaN8n(sessionId, message, userId);

        return ResponseEntity.ok(aiResult);
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatResponseDto>> getChatHistory(@PathVariable String sessionId, HttpSession session) {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        log.info("Getting chat history for user: {}, session: {}", userId, sessionId);

        try {
            List<ChatResponseDto> history = chatService.getChatHistory(sessionId, userId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error getting chat history: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/clear/{sessionId}")
    public ResponseEntity<Void> clearChat(@PathVariable String sessionId, HttpSession session) {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        log.info("Clearing chat history for user: {}, session: {}", userId, sessionId);

        try {
            chatService.clearChatHistory(sessionId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error clearing chat history: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSession>> getChatSessions(HttpSession session) {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            List<ChatSession> sessions = chatService.getUserChatSessions(userId);
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            log.error("Error getting chat sessions: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Chat API is working!");
    }

    private Long getUserIdFromSession(HttpSession session) {
        Object user = session.getAttribute("loggedInUser");
        if (user instanceof com.ai.chatbot.model.User) {
            return ((com.ai.chatbot.model.User) user).getId();
        }
        return null;
    }
}