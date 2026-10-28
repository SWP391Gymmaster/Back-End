package com.swp391_be.dto;

/**
 * Sĩ số của một lớp theo số hội viên đã đăng ký LỚP, kèm cờ hội viên hiện tại
 * đã đăng ký chưa.
 */
public record ClassStatResponse(
        Long classId,
        long registeredCount,
        long remainingSlots,
        boolean registeredByMe) {
}