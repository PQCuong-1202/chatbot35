package com.ai.chatbot.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileUploadResponse {
    private boolean success;
    private String message;
    private Long fileId;
    private String fileName;
    private String summary;
    private String error;

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    // Static factory methods
    public static FileUploadResponse success(String message, Long fileId, String fileName, String summary) {
        FileUploadResponse response = new FileUploadResponse();
        response.success = true;
        response.message = message;
        response.fileId = fileId;
        response.fileName = fileName;
        response.summary = summary;
        return response;
    }

    public static FileUploadResponse error(String errorMessage) {
        FileUploadResponse response = new FileUploadResponse();
        response.success = false;
        response.error = errorMessage;
        return response;
    }

    // Builder
    public static FileUploadResponseBuilder builder() {
        return new FileUploadResponseBuilder();
    }

    public static class FileUploadResponseBuilder {
        private boolean success;
        private String message;
        private Long fileId;
        private String fileName;
        private String summary;
        private String error;

        public FileUploadResponseBuilder success(boolean success) {
            this.success = success;
            return this;
        }

        public FileUploadResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public FileUploadResponseBuilder fileId(Long fileId) {
            this.fileId = fileId;
            return this;
        }

        public FileUploadResponseBuilder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public FileUploadResponseBuilder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public FileUploadResponseBuilder error(String error) {
            this.error = error;
            return this;
        }

        public FileUploadResponse build() {
            FileUploadResponse response = new FileUploadResponse();
            response.success = success;
            response.message = message;
            response.fileId = fileId;
            response.fileName = fileName;
            response.summary = summary;
            response.error = error;
            return response;
        }
    }
}