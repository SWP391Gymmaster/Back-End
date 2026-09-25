package com.swp391_be.dto;

public record AuthResponse(String token, String email, String fullName, String role, Long userId) {}
