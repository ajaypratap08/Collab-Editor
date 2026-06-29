package com.collab.editor.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    // WHY: read from env var with a dev-only fallback so the app never fails to boot,
    // but production (Railway) MUST override this via JWT_SECRET env var.
    @Value("${JWT_SECRET:dev-only-secret-change-this-minimum-32-characters-long}")
    private String secretString;

    @Value("${JWT_ACCESS_EXPIRY_MS:900000}") // 15 minutes
    private long accessTokenExpiryMs;

    @Value("${JWT_REFRESH_EXPIRY_MS:604800000}") // 7 days
    private long refreshTokenExpiryMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretString.getBytes());
    }

    public String generateAccessToken(String email) {
        return buildToken(email, accessTokenExpiryMs);
    }

    public String generateRefreshToken(String email) {
        return buildToken(email, refreshTokenExpiryMs);
    }

    public long getAccessTokenExpirySeconds() {
        return accessTokenExpiryMs / 1000;
    }

    private String buildToken(String subject, long expiryMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);

        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }

    public boolean isTokenValid(String token, String expectedEmail) {
        try {
            String email = extractEmail(token);
            return email.equals(expectedEmail) && !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            return false;
        } catch (Exception e) {
            // Malformed, wrong signature, etc. — treat as invalid, never crash the filter chain
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
