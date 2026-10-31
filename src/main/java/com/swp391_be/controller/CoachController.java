package com.swp391_be.controller;

import com.swp391_be.dto.CoachResponse;
import com.swp391_be.entity.Role;
import com.swp391_be.repository.UserRepository;
import com.swp391_be.service.CoachSubjectService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Danh sách Coach (kèm các bộ môn được dạy) để Manager chọn khi phân công lớp
@RestController
@RequestMapping("/api/coaches")
public class CoachController {

    private final UserRepository userRepository;
    private final CoachSubjectService coachSubjects;

    public CoachController(UserRepository userRepository, CoachSubjectService coachSubjects) {
        this.userRepository = userRepository;
        this.coachSubjects = coachSubjects;
    }

    @GetMapping
    public List<CoachResponse> getAll() {
        Map<Long, List<String>> subjects = coachSubjects.subjectsByCoach();
        return userRepository.findByRole(Role.COACH).stream()
                .map(user -> CoachResponse.of(user, subjects.getOrDefault(user.getId(), List.of())))
                .toList();
    }
}