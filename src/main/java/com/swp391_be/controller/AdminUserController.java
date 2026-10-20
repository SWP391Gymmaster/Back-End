package com.swp391_be.controller;

import tools.jackson.databind.JsonNode;
import com.swp391_be.dto.AdminCreateUserRequest;
import com.swp391_be.dto.AdminUserPageResponse;
import com.swp391_be.dto.AdminUserResponse;
import com.swp391_be.dto.AdminUserWriteResponse;
import com.swp391_be.dto.UserStatusRequest;
import com.swp391_be.dto.VerificationEmailResponse;
import com.swp391_be.entity.Role;
import com.swp391_be.service.AdminUserService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserService users;
    public AdminUserController(AdminUserService users) { this.users = users; }

    @GetMapping
    public AdminUserPageResponse list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String q,
            @RequestParam(required = false) Role role, @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean emailVerified) {
        return users.list(page, size, q, role, enabled, emailVerified);
    }

    @GetMapping("/{id}")
    public AdminUserResponse get(@PathVariable Long id) { return users.get(id); }

    @PostMapping
    public ResponseEntity<AdminUserWriteResponse> create(@Valid @RequestBody AdminCreateUserRequest request) {
        AdminUserWriteResponse created = users.create(request);
        return ResponseEntity.created(URI.create("/api/admin/users/" + created.user().id())).body(created);
    }

    @PatchMapping("/{id}")
    public AdminUserWriteResponse update(@PathVariable Long id, @RequestBody JsonNode request) {
        return users.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        users.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public AdminUserResponse status(@PathVariable Long id, @RequestBody UserStatusRequest request) {
        return users.setEnabled(id, request.enabled());
    }

    @PostMapping("/{id}/verification-email")
    public ResponseEntity<VerificationEmailResponse> resend(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(users.resend(id));
    }
}
