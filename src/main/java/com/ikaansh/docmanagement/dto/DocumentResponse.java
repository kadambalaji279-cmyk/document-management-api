package com.ikaansh.docmanagement.dto;

import java.time.LocalDateTime;

public class DocumentResponse {

    private final Long id;
    private final String title;
    private final String description;
    private final String fileName;
    private final String contentType;
    private final Long fileSize;
    private final Long ownerId;
    private final String ownerUsername;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public DocumentResponse(Long id,
                            String title,
                            String description,
                            String fileName,
                            String contentType,
                            Long fileSize,
                            Long ownerId,
                            String ownerUsername,
                            LocalDateTime createdAt,
                            LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.ownerId = ownerId;
        this.ownerUsername = ownerUsername;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
