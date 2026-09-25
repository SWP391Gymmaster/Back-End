package com.swp391_be.controller;

import com.swp391_be.dto.UserSummary;
import com.swp391_be.entity.Role;
import com.swp391_be.repository.UserRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Danh sách Coach để Manager chọn khi phân công lớp
@RestController
@RequestMapping("/api/coaches")
public class CoachController {

    private final UserRepository userRepository;

    public CoachController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<UserSummary> getAll() {
        return userRepository.findByRole(Role.COACH).stream()
                .map(UserSummary::from)
                .toList();
    }
}
