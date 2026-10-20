package com.swp391_be.controller;

import com.swp391_be.dto.AuthResponse;
import com.swp391_be.dto.LoginRequest;
import com.swp391_be.dto.MessageResponse;
import com.swp391_be.dto.RegistrationResponse;
import com.swp391_be.dto.RegisterRequest;
import com.swp391_be.dto.ResendVerificationRequest;
import com.swp391_be.dto.VerifyEmailRequest;
import com.swp391_be.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }
    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) { return service.login(request); }
    @PostMapping("/verify-email")
    public MessageResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        service.verifyEmail(request.token());
        return new MessageResponse("Email verified. You can now log in.");
    }
    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resend(@Valid @RequestBody ResendVerificationRequest request) {
        service.resendVerification(request.email());
        return ResponseEntity.accepted().body(new MessageResponse(
                "If the account can be verified, a verification email will be sent."));
    }
}
