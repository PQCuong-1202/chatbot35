package com.ai.chatbot.model.dto;

import java.util.List;

public class ChatRequestDto {
    private String sessionId;
    private String message;
    private List<String> contextFiles;
    private Long fileId;
    private Long userId; // Thêm userId để xác định user

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

    public List<String> getContextFiles() {
        return contextFiles;
    }

    public void setContextFiles(List<String> contextFiles) {
        this.contextFiles = contextFiles;
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
}