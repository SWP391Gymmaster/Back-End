package com.swp391_be.dto;

import java.time.LocalDateTime;

public record AvailableClassSessionResponse(
        Long scheduleId,
        Long classId,
        String className,
        String subject,
        Long coachId,
        Long roomId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        int capacity,
        long registeredCount,
        long remainingSlots,
        boolean registeredByMe) {
}
