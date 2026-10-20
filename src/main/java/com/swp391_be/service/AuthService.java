package com.swp391_be.service;

import com.swp391_be.dto.AuthResponse;
import com.swp391_be.dto.LoginRequest;
import com.swp391_be.dto.RegisterRequest;
import com.swp391_be.dto.RegistrationResponse;
import com.swp391_be.dto.VerificationEmailStatus;
import com.swp391_be.entity.Role;
import com.swp391_be.entity.User;
import com.swp391_be.exception.AccountOperationException;
import com.swp391_be.repository.UserRepository;
import com.swp391_be.security.JwtService;
import com.swp391_be.service.AccountCreationService.CreatedAccount;
import com.swp391_be.service.EmailVerificationService.VerificationDelivery;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AccountCreationService accountCreation;
    private final EmailVerificationService verification;
    private final VerificationEmailService emailSender;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt,
            AccountCreationService accountCreation, EmailVerificationService verification,
            VerificationEmailService emailSender) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.accountCreation = accountCreation;
        this.verification = verification;
        this.emailSender = emailSender;
    }

    public RegistrationResponse register(RegisterRequest request) {
        CreatedAccount created = accountCreation.create(request.userName(), request.fullName(), request.email(),
                request.password(), request.phone(), Role.MEMBER);
        VerificationEmailStatus status = emailSender.send(created.delivery());
        return new RegistrationResponse(created.user().getId(), created.user().getEmail(), true,
                status, "Check your email to verify your account.");
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = users.findByEmail(email).orElse(null);
        if (user == null || !encoder.matches(request.password(), user.getPasswordHash())) {
            throw new AccountOperationException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                    "Invalid email or password");
        }
        if (!user.isEnabled()) throw new AccountOperationException(HttpStatus.FORBIDDEN,
                "ACCOUNT_DISABLED", "Account is disabled");
        if (!user.isEmailVerified()) throw new AccountOperationException(HttpStatus.FORBIDDEN,
                "EMAIL_NOT_VERIFIED", "Email address has not been verified");
        return response(user);
    }

    public void verifyEmail(String token) { verification.verify(token); }

    public void resendVerification(String address) {
        String email = address.trim().toLowerCase(Locale.ROOT);
        User user = users.findByEmail(email).orElse(null);
        if (user == null || !user.isEnabled() || user.isEmailVerified()) return;
        try {
            VerificationDelivery delivery = verification.issue(user.getId());
            emailSender.send(delivery);
        } catch (AccountOperationException exception) {
            if (!"VERIFICATION_RATE_LIMITED".equals(exception.getCode())
                    && !"VERIFICATION_NOT_REQUIRED".equals(exception.getCode())) throw exception;
        }
    }

    private AuthResponse response(User user) {
        return new AuthResponse(jwt.generateToken(user.getId(), user.getRole().name()),
                user.getEmail(), user.getFullName(), user.getRole().name(), user.getId());
    }
}
