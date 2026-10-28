package com.swp391_be.controller;

import com.swp391_be.dto.ClassRegistrationResponse;
import com.swp391_be.dto.ClassStatResponse;
import com.swp391_be.entity.User;
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.repository.UserRepository;
import com.swp391_be.service.ClassRegistrationService;
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

// /api/member/** đã được SecurityConfig giới hạn cho role MEMBER, không cần sửa cấu hình bảo mật.
@RestController
@RequestMapping("/api/member/classes")
public class MemberClassRegistrationController {

    private final ClassRegistrationService registrations;
    private final UserRepository users;

    public MemberClassRegistrationController(ClassRegistrationService registrations, UserRepository users) {
        this.registrations = registrations;
        this.users = users;
    }

    @GetMapping
    public List<ClassStatResponse> stats(Authentication authentication) {
        return registrations.getClassStats(resolveMember(authentication).getId());
    }

    @PostMapping("/{classId}/registration")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassRegistrationResponse register(@PathVariable Long classId, Authentication authentication) {
        return registrations.register(resolveMember(authentication).getId(), classId);
    }

    @DeleteMapping("/{classId}/registration")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long classId, Authentication authentication) {
        registrations.cancel(resolveMember(authentication).getId(), classId);
    }

    private User resolveMember(Authentication authentication) {
        return users.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
    }
}