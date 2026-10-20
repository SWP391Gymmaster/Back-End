package com.swp391_be.service;

import tools.jackson.databind.JsonNode;
import com.swp391_be.dto.AdminCreateUserRequest;
import com.swp391_be.dto.AdminUserPageResponse;
import com.swp391_be.dto.AdminUserResponse;
import com.swp391_be.dto.AdminUserWriteResponse;
import com.swp391_be.dto.VerificationEmailResponse;
import com.swp391_be.dto.VerificationEmailStatus;
import com.swp391_be.entity.Role;
import com.swp391_be.entity.User;
import com.swp391_be.exception.AccountOperationException;
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.repository.UserRepository;
import com.swp391_be.service.AdminUserTransactionService.Mutation;
import com.swp391_be.service.EmailVerificationService.VerificationDelivery;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AdminUserService {
    private final UserRepository users;
    private final AdminUserTransactionService transactions;
    private final EmailVerificationService verification;
    private final VerificationEmailService sender;

    public AdminUserService(UserRepository users, AdminUserTransactionService transactions,
            EmailVerificationService verification, VerificationEmailService sender) {
        this.users = users;
        this.transactions = transactions;
        this.verification = verification;
        this.sender = sender;
    }

    public AdminUserPageResponse list(int page, int size, String query, Role role,
            Boolean enabled, Boolean emailVerified) {
        if (page < 0 || size < 1 || size > 100 || role == Role.ADMIN || query != null && query.length() > 100) {
            throw invalid();
        }
        String q = query == null || query.isBlank() ? null : "%" + query.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        Page<User> result = users.searchManagedUsers(q, role, enabled, emailVerified, PageRequest.of(page, size));
        return new AdminUserPageResponse(result.getContent().stream().map(AdminUserResponse::from).toList(),
                page, size, result.getTotalElements(), result.getTotalPages());
    }

    public AdminUserResponse get(Long id) { return AdminUserResponse.from(target(id)); }

    public AdminUserWriteResponse create(AdminCreateUserRequest request) {
        Mutation mutation = transactions.create(request);
        return new AdminUserWriteResponse(AdminUserResponse.from(mutation.user()), sender.send(mutation.delivery()));
    }

    public AdminUserWriteResponse update(Long id, JsonNode request) {
        Mutation mutation = transactions.update(id, request);
        VerificationEmailStatus status = !mutation.emailChanged() ? VerificationEmailStatus.NOT_REQUIRED
                : mutation.delivery() == null ? VerificationEmailStatus.NOT_REQUIRED : sender.send(mutation.delivery());
        return new AdminUserWriteResponse(AdminUserResponse.from(mutation.user()), status);
    }

    public void deactivate(Long id) { transactions.setEnabled(id, false); }

    public AdminUserResponse setEnabled(Long id, Boolean enabled) {
        if (enabled == null) throw invalid();
        return AdminUserResponse.from(transactions.setEnabled(id, enabled));
    }

    public VerificationEmailResponse resend(Long id) {
        User user = target(id);
        if (!user.isEnabled() || user.isEmailVerified()) {
            throw new AccountOperationException(HttpStatus.CONFLICT,
                    "VERIFICATION_NOT_REQUIRED", "Verification email is not required");
        }
        VerificationDelivery delivery = verification.issue(id);
        return new VerificationEmailResponse(sender.send(delivery));
    }

    private User target(Long id) {
        User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == Role.ADMIN) throw new ResourceNotFoundException("User not found");
        return user;
    }

    private AccountOperationException invalid() {
        return new AccountOperationException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid request data");
    }
}
