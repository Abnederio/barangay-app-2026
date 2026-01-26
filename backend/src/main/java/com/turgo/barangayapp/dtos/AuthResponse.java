package com.turgo.barangayapp.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthResponse {
    private String token;
    private Long userId;
    private String email;
    private String fullName;
    @JsonProperty("isAdmin")
    private boolean admin;

    public AuthResponse() {
    }

    public AuthResponse(String token, Long userId, String email, String fullName, boolean isAdmin) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.admin = isAdmin;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
}
