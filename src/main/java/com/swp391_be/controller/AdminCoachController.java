package com.swp391_be.controller;

import com.swp391_be.dto.CoachResponse;
import com.swp391_be.dto.CoachSubjectsRequest;
import com.swp391_be.service.CoachSubjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Admin gán các bộ môn mà một Coach được dạy (/api/admin/** đã yêu cầu role ADMIN trong SecurityConfig)
@RestController
@RequestMapping("/api/admin/coaches")
public class AdminCoachController {

    private final CoachSubjectService service;

    public AdminCoachController(CoachSubjectService service) {
        this.service = service;
    }

    @PutMapping("/{id}/subjects")
    public CoachResponse setSubjects(@PathVariable Long id, @Valid @RequestBody CoachSubjectsRequest request) {
        return service.replaceSubjects(id, request.subjects());
    }
}
