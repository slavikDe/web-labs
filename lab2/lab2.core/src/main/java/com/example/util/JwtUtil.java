package com.example.util;

import com.example.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
public class JwtUtil {
    private static final SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final int EXPIRATION_HOURS = 24;

    public static String generateToken(User user) {
        try {
            LocalDateTime expiration = LocalDateTime.now().plusHours(EXPIRATION_HOURS);
            Date expirationDate = Date.from(expiration.atZone(ZoneId.systemDefault()).toInstant());

            return Jwts.builder()
                    .subject(user.getId())
                    .claim("username", user.getUsername())
                    .claim("email", user.getEmail())
                    .claim("role", user.getRole().toString())
                    .issuedAt(new Date())
                    .expiration(expirationDate)
                    .signWith(SECRET_KEY)
                    .compact();
        } catch (Exception e) {
            log.error("Error generating JWT token for user: {}", user.getUsername(), e);
            throw new RuntimeException("Failed to generate token", e);
        }
    }

    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(SECRET_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Error parsing JWT token", e);
            throw new RuntimeException("Invalid token", e);
        }
    }

    public static boolean isTokenValid(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            log.debug("Token validation failed", e);
            return false;
        }
    }

    public static String getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.error("Error extracting user ID from token", e);
            throw new RuntimeException("Failed to extract user ID", e);
        }
    }

    public static String getRoleFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("role", String.class);
        } catch (Exception e) {
            log.error("Error extracting role from token", e);
            throw new RuntimeException("Failed to extract role", e);
        }
    }
}