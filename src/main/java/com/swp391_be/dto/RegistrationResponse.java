package com.swp391_be.dto;

public record RegistrationResponse(Long userId, String email, boolean verificationRequired,
        VerificationEmailStatus verificationEmailStatus, String message) {}
