package com.swp391_be.service;

import com.swp391_be.entity.Role;
import com.swp391_be.entity.User;
import com.swp391_be.exception.EmailAlreadyExistsException;
import com.swp391_be.exception.UserNameAlreadyExistsException;
import com.swp391_be.repository.UserRepository;
import com.swp391_be.service.EmailVerificationService.VerificationDelivery;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import com.swp391_be.exception.AccountOperationException;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountCreationService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final EmailVerificationService verification;

    public AccountCreationService(UserRepository users, PasswordEncoder encoder,
            EmailVerificationService verification) {
        this.users = users;
        this.encoder = encoder;
        this.verification = verification;
    }

    @Transactional
    public CreatedAccount create(String userName, String fullName, String email, String password, String phone,
            Role role) {
        String normalizedUserName = userName.trim();
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedName = fullName.trim();
        if (!normalizedUserName.matches("[A-Za-z0-9_.-]{3,50}") || normalizedName.isEmpty()
                || normalizedName.length() > 100 || normalizedEmail.length() > 254
                || !normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72
                || phone != null && (phone.trim().length() > 30 || !phone.trim().matches("[+0-9() -]*"))) {
            throw new AccountOperationException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid request data");
        }
        if (users.existsByEmail(normalizedEmail))
            throw new EmailAlreadyExistsException(normalizedEmail);
        if (users.existsByUserNameIgnoreCase(normalizedUserName)) {
            throw new UserNameAlreadyExistsException(normalizedUserName);
        }
        User user = users.saveAndFlush(new User(normalizedUserName, normalizedName, normalizedEmail,
                encoder.encode(password), normalizePhone(phone), role));
        VerificationDelivery delivery = verification.issue(user.getId());
        return new CreatedAccount(user, delivery);
    }

    private String normalizePhone(String phone) {
        if (phone == null)
            return null;
        String value = phone.trim();
        return value.isEmpty() ? null : value;
    }

    public record CreatedAccount(User user, VerificationDelivery delivery) {
    }
}
