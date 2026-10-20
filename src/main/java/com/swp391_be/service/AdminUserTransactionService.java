package com.swp391_be.service;

import tools.jackson.databind.JsonNode;
import com.swp391_be.dto.AdminCreateUserRequest;
import com.swp391_be.entity.Role;
import com.swp391_be.entity.User;
import com.swp391_be.exception.AccountOperationException;
import com.swp391_be.exception.EmailAlreadyExistsException;
import com.swp391_be.exception.ResourceNotFoundException;
import com.swp391_be.exception.UserNameAlreadyExistsException;
import com.swp391_be.repository.ClassEnrollmentRepository;
import com.swp391_be.repository.GymClassRepository;
import com.swp391_be.repository.InvoiceRepository;
import com.swp391_be.repository.MemberMembershipRepository;
import com.swp391_be.repository.PaymentRepository;
import com.swp391_be.repository.ScheduleRepository;
import com.swp391_be.repository.UserRepository;
import com.swp391_be.service.EmailVerificationService.VerificationDelivery;
import java.util.Set;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserTransactionService {
    private static final Set<String> UPDATE_FIELDS = Set.of("userName", "fullName", "email", "phone", "role");
    private final UserRepository users;
    private final AccountCreationService creation;
    private final EmailVerificationService verification;
    private final ClassEnrollmentRepository enrollments;
    private final MemberMembershipRepository memberships;
    private final PaymentRepository payments;
    private final InvoiceRepository invoices;
    private final GymClassRepository classes;
    private final ScheduleRepository schedules;

    public AdminUserTransactionService(UserRepository users, AccountCreationService creation,
            EmailVerificationService verification, ClassEnrollmentRepository enrollments,
            MemberMembershipRepository memberships, PaymentRepository payments, InvoiceRepository invoices,
            GymClassRepository classes, ScheduleRepository schedules) {
        this.users = users;
        this.creation = creation;
        this.verification = verification;
        this.enrollments = enrollments;
        this.memberships = memberships;
        this.payments = payments;
        this.invoices = invoices;
        this.classes = classes;
        this.schedules = schedules;
    }

    public record Mutation(User user, VerificationDelivery delivery, boolean emailChanged) {}

    @Transactional
    public Mutation create(AdminCreateUserRequest request) {
        if (request.role() == Role.ADMIN) throw roleDenied();
        AccountCreationService.CreatedAccount created = creation.create(request.userName(), request.fullName(),
                request.email(), request.password(), request.phone(), request.role());
        return new Mutation(created.user(), created.delivery(), true);
    }

    @Transactional
    public Mutation update(Long id, JsonNode request) {
        if (request == null || !request.isObject() || request.isEmpty()) throw invalid();
        request.properties().forEach(entry -> {
            if (!UPDATE_FIELDS.contains(entry.getKey())) throw invalid();
        });
        User user = managedUser(id);
        if (request.has("userName")) {
            String value = text(request.get("userName"), "userName", false).trim();
            if (!value.matches("[A-Za-z0-9_.-]{3,50}")) throw invalid();
            if (!value.equalsIgnoreCase(user.getUserName()) && users.existsByUserNameIgnoreCase(value)) {
                throw new UserNameAlreadyExistsException(value);
            }
            user.setUserName(value);
        }
        if (request.has("fullName")) {
            String value = text(request.get("fullName"), "fullName", false).trim();
            if (value.isEmpty() || value.length() > 100) throw invalid();
            user.setFullName(value);
        }
        if (request.has("phone")) {
            if (request.get("phone").isNull()) user.setPhone(null);
            else {
                String phone = text(request.get("phone"), "phone", false).trim();
                if (phone.length() > 30 || !phone.matches("[+0-9() -]*")) throw invalid();
                user.setPhone(phone.isEmpty() ? null : phone);
            }
        }
        if (request.has("role")) {
            Role role;
            try { role = Role.valueOf(text(request.get("role"), "role", false)); }
            catch (IllegalArgumentException error) { throw invalid(); }
            if (role == Role.ADMIN) throw roleDenied();
            ensureRoleCanChange(user, role);
            user.setRole(role);
        }
        VerificationDelivery delivery = null;
        boolean emailChanged = false;
        if (request.has("email")) {
            String email = text(request.get("email"), "email", false).trim().toLowerCase(Locale.ROOT);
            if (email.length() > 254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw invalid();
            if (!email.equals(user.getEmail())) {
                User existing = users.findByEmail(email).orElse(null);
                if (existing != null && !existing.getId().equals(user.getId())) {
                    throw new EmailAlreadyExistsException(email);
                }
                user.setEmail(email);
                user.setEmailVerified(false);
                verification.invalidateForUser(id);
                emailChanged = true;
                if (user.isEnabled()) delivery = verification.issue(id);
            }
        }
        return new Mutation(user, delivery, emailChanged);
    }

    @Transactional
    public User setEnabled(Long id, boolean enabled) {
        User user = managedUser(id);
        if (!enabled && user.isEnabled()) {
            user.setEnabled(false);
            verification.invalidateForUser(id);
        } else {
            user.setEnabled(enabled);
        }
        return user;
    }

    @Transactional
    public User getLockedManagedUser(Long id) { return managedUser(id); }

    private User managedUser(Long id) {
        User user = users.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == Role.ADMIN) throw new ResourceNotFoundException("User not found");
        return user;
    }

    private void ensureRoleCanChange(User user, Role next) {
        if (user.getRole() == next) return;
        boolean referenced = user.getRole() == Role.MEMBER
                ? enrollments.existsByMemberId(user.getId()) || memberships.existsByMemberId(user.getId())
                    || payments.existsByMemberId(user.getId()) || invoices.existsByMemberId(user.getId())
                : user.getRole() == Role.COACH
                    && (classes.existsByCoachId(user.getId()) || schedules.existsByCoachId(user.getId()));
        if (referenced) throw new AccountOperationException(HttpStatus.CONFLICT, "ROLE_CHANGE_BLOCKED",
                "Role cannot change while business records reference this user");
    }

    private String text(JsonNode node, String field, boolean allowNull) {
        if (node == null || node.isNull() && !allowNull || !node.isTextual()) throw invalid();
        return node.textValue();
    }

    private AccountOperationException invalid() {
        return new AccountOperationException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid request data");
    }

    private AccountOperationException roleDenied() {
        return new AccountOperationException(HttpStatus.BAD_REQUEST, "ADMIN_ROLE_NOT_ALLOWED",
                "ADMIN accounts cannot be managed through this endpoint");
    }
}
