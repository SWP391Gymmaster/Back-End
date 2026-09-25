package com.swp391_be.dto;

import com.swp391_be.entity.User;

// Thông tin công khai của user (không có mật khẩu)
public record UserSummary(Long id, String userName, String fullName, String email, String phone, String role) {
    public static UserSummary from(User user) {
        return new UserSummary(user.getId(), user.getUserName(), user.getFullName(),
                user.getEmail(), user.getPhone(), user.getRole().name());
    }
}
