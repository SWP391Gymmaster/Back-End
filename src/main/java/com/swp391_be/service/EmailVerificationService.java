package com.swp391_be.service;

import com.swp391_be.entity.EmailVerificationToken;
import com.swp391_be.entity.User;
import com.swp391_be.exception.AccountOperationException;
import com.swp391_be.repository.EmailVerificationTokenRepository;
import com.swp391_be.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService {
    private final EmailVerificationTokenRepository tokens;
    private final UserRepository users;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(EmailVerificationTokenRepository tokens, UserRepository users, Clock clock) {
        this.tokens = tokens;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public VerificationDelivery issue(Long userId) {
        User user = users.findByIdForUpdate(userId).orElseThrow();
        if (!user.isEnabled() || user.isEmailVerified()) {
            throw new AccountOperationException(HttpStatus.CONFLICT, "VERIFICATION_NOT_REQUIRED",
                    "Verification email is not required");
        }
        Instant now = clock.instant();
        EmailVerificationToken saved = tokens.findByUserIdForUpdate(userId).orElse(null);
        if (saved != null) {
            if (now.isBefore(saved.getLastRequestedAt().plusSeconds(60))) {
                long retry = Duration.between(now, saved.getLastRequestedAt().plusSeconds(60)).toSeconds() + 1;
                throw new AccountOperationException(HttpStatus.TOO_MANY_REQUESTS,
                        "VERIFICATION_RATE_LIMITED", "Please wait before requesting another email", retry);
            }
            if (now.isBefore(saved.getWindowStartedAt().plusSeconds(3600)) && saved.getRequestsInWindow() >= 5) {
                long retry = Duration.between(now, saved.getWindowStartedAt().plusSeconds(3600)).toSeconds() + 1;
                throw new AccountOperationException(HttpStatus.TOO_MANY_REQUESTS,
                        "VERIFICATION_RATE_LIMITED", "Verification email limit reached", retry);
            }
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String hash = hash(raw);
        Instant expiry = now.plus(Duration.ofHours(24));
        if (saved == null) {
            tokens.save(new EmailVerificationToken(userId, hash, user.getEmail(), expiry, now));
        } else {
            saved.issue(hash, user.getEmail(), expiry, now);
        }
        return new VerificationDelivery(userId, user.getEmail(), raw, hash);
    }

    @Transactional
    public void verify(String rawToken) {
        if (rawToken == null || rawToken.length() > 100) throw invalidToken();
        EmailVerificationToken found = tokens.findByTokenHash(hash(rawToken)).orElseThrow(this::invalidToken);
        User user = users.findByIdForUpdate(found.getUserId()).orElseThrow(this::invalidToken);
        EmailVerificationToken token = tokens.findByUserIdForUpdate(user.getId()).orElseThrow(this::invalidToken);
        Instant now = clock.instant();
        if (!token.getTokenHash().equals(hash(rawToken)) || token.getConsumedAt() != null
                || !token.getEmailSnapshot().equals(user.getEmail()) || !user.isEnabled()) throw invalidToken();
        if (!now.isBefore(token.getExpiresAt())) {
            throw new AccountOperationException(HttpStatus.BAD_REQUEST, "VERIFICATION_TOKEN_EXPIRED",
                    "Verification link has expired");
        }
        token.consume(now);
        user.setEmailVerified(true);
    }

    @Transactional
    public void invalidateForUser(Long userId) {
        tokens.findByUserIdForUpdate(userId).ifPresent(token -> token.invalidate(clock.instant()));
    }

    @Transactional(readOnly = true)
    public boolean isCurrent(VerificationDelivery delivery) {
        Instant now = clock.instant();
        return tokens.findByUserId(delivery.userId()).map(token -> token.getConsumedAt() == null
                && token.getTokenHash().equals(delivery.tokenHash())
                && token.getEmailSnapshot().equals(delivery.email()) && now.isBefore(token.getExpiresAt()))
                .orElse(false);
    }

    private String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private AccountOperationException invalidToken() {
        return new AccountOperationException(HttpStatus.BAD_REQUEST,
                "INVALID_VERIFICATION_TOKEN", "Verification link is invalid");
    }

    public record VerificationDelivery(Long userId, String email, String rawToken, String tokenHash) {}
}
