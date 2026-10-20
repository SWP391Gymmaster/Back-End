package com.swp391_be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "email_verification_tokens", uniqueConstraints = {
        @UniqueConstraint(name = "uk_verification_user", columnNames = "user_id"),
        @UniqueConstraint(name = "uk_verification_hash", columnNames = "token_hash")
})
public class EmailVerificationToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;
    @Column(name = "email_snapshot", nullable = false, length = 254)
    private String emailSnapshot;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "consumed_at")
    private Instant consumedAt;
    @Column(name = "last_requested_at", nullable = false)
    private Instant lastRequestedAt;
    @Column(name = "window_started_at", nullable = false)
    private Instant windowStartedAt;
    @Column(name = "requests_in_window", nullable = false)
    private int requestsInWindow;

    protected EmailVerificationToken() {}
    public EmailVerificationToken(Long userId, String tokenHash, String emailSnapshot, Instant expiresAt,
            Instant requestedAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.emailSnapshot = emailSnapshot;
        this.expiresAt = expiresAt;
        this.lastRequestedAt = requestedAt;
        this.windowStartedAt = requestedAt;
        this.requestsInWindow = 1;
    }
    public Long getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public String getEmailSnapshot() { return emailSnapshot; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public Instant getLastRequestedAt() { return lastRequestedAt; }
    public Instant getWindowStartedAt() { return windowStartedAt; }
    public int getRequestsInWindow() { return requestsInWindow; }
    public void issue(String hash, String email, Instant expiry, Instant now) {
        tokenHash = hash;
        emailSnapshot = email;
        expiresAt = expiry;
        consumedAt = null;
        lastRequestedAt = now;
        if (!now.isBefore(windowStartedAt.plusSeconds(3600))) {
            windowStartedAt = now;
            requestsInWindow = 1;
        } else {
            requestsInWindow++;
        }
    }
    public void consume(Instant now) { consumedAt = now; }
    public void invalidate(Instant now) { consumedAt = now; }
}
