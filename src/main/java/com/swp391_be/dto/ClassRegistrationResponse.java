package com.swp391_be.dto;

import java.time.Instant;

public record ClassRegistrationResponse(Long classId, Instant registeredAt) {
}