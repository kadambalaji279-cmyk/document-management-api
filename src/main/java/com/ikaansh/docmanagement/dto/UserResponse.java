package com.ikaansh.docmanagement.dto;

import java.time.LocalDateTime;

public class UserResponse {

    private final Long id;
    private final String username;
    private final String email;
    private final String role;
    private final LocalDateTime createdAt;

    public UserResponse(Long id, String username, String email, String role, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
