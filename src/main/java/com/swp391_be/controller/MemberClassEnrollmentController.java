package com.swp391_be.controller;

import com.swp391_be.dto.AvailableClassSessionResponse;
import com.swp391_be.dto.ClassEnrollmentResponse;
import com.swp391_be.entity.User;
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.repository.UserRepository;
import com.swp391_be.service.ClassEnrollmentService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member")
public class MemberClassEnrollmentController {

    private final ClassEnrollmentService enrollments;
    private final UserRepository users;

    public MemberClassEnrollmentController(ClassEnrollmentService enrollments, UserRepository users) {
        this.enrollments = enrollments;
        this.users = users;
    }

    @GetMapping("/class-sessions/available")
    public List<AvailableClassSessionResponse> available(Authentication authentication) {
        return enrollments.getAvailableSessions(resolveMember(authentication).getId());
    }

    @GetMapping("/class-enrollments")
    public List<ClassEnrollmentResponse> mine(Authentication authentication) {
        return enrollments.getMyEnrollments(resolveMember(authentication).getId());
    }

    @PostMapping("/class-sessions/{scheduleId}/enrollment")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassEnrollmentResponse enroll(@PathVariable Long scheduleId, Authentication authentication) {
        return enrollments.enroll(resolveMember(authentication).getId(), scheduleId);
    }

    @DeleteMapping("/class-sessions/{scheduleId}/enrollment")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long scheduleId, Authentication authentication) {
        enrollments.cancel(resolveMember(authentication).getId(), scheduleId);
    }

    private User resolveMember(Authentication authentication) {
        return users.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
    }
}
