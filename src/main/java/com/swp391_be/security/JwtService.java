package com.swp391_be.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.expirationMs = expirationMs;
    }
    public String generateToken(Long userId, String role) {
        Date now = new Date();
        return Jwts.builder().subject(userId.toString()).claim("role", role).issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs)).signWith(key).compact();
    }
    public Long extractUserId(String token) { return Long.valueOf(parseClaims(token).getSubject()); }
    public String extractRole(String token) { return parseClaims(token).get("role", String.class); }
    public boolean isTokenValid(String token) {
        try { Long.parseLong(parseClaims(token).getSubject()); return true; }
        catch (JwtException | IllegalArgumentException e) { return false; }
    }
    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
