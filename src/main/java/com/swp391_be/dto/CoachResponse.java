package com.swp391_be.dto;

import com.swp391_be.entity.User;
import java.util.List;

// Giống UserSummary nhưng có thêm danh sách bộ môn coach được dạy
public record CoachResponse(Long id, String userName, String fullName, String email, String phone,
        String role, List<String> subjects) {

    public static CoachResponse of(User user, List<String> subjects) {
        return new CoachResponse(user.getId(), user.getUserName(), user.getFullName(),
                user.getEmail(), user.getPhone(), user.getRole().name(), subjects);
    }
}
