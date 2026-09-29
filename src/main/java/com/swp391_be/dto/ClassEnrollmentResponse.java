package com.swp391_be.dto;

import java.time.Instant;
import java.time.LocalDateTime;

public record ClassEnrollmentResponse(
        Long enrollmentId,
        Long scheduleId,
        Long classId,
        String className,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Instant enrolledAt) {
}
