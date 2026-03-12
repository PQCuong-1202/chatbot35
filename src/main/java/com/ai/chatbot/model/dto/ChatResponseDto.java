package com.ai.chatbot.model.dto;

import java.time.LocalDateTime;

public class ChatResponseDto {
    private String sessionId;
    private String message;
    private String sender;
    private LocalDateTime timestamp;
    private Long fileId;
    private Long userId; // Thêm userId

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    // Builder pattern
    public static ChatResponseDtoBuilder builder() {
        return new ChatResponseDtoBuilder();
    }

    public static class ChatResponseDtoBuilder {
        private String sessionId;
        private String message;
        private String sender;
        private LocalDateTime timestamp;
        private Long fileId;
        private Long userId;

        public ChatResponseDtoBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public ChatResponseDtoBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ChatResponseDtoBuilder sender(String sender) {
            this.sender = sender;
            return this;
        }

        public ChatResponseDtoBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ChatResponseDtoBuilder fileId(Long fileId) {
            this.fileId = fileId;
            return this;
        }

        public ChatResponseDtoBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public ChatResponseDto build() {
            ChatResponseDto dto = new ChatResponseDto();
            dto.sessionId = sessionId;
            dto.message = message;
            dto.sender = sender;
            dto.timestamp = timestamp;
            dto.fileId = fileId;
            dto.userId = userId;
            return dto;
        }
    }
}