package com.swp391_be.dto;

import com.swp391_be.entity.Role;
import com.swp391_be.entity.User;

public record AdminUserResponse(Long id, String userName, String fullName, String email, String phone,
        Role role, boolean enabled, boolean emailVerified) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(user.getId(), user.getUserName(), user.getFullName(), user.getEmail(),
                user.getPhone(), user.getRole(), user.isEnabled(), user.isEmailVerified());
    }
}
