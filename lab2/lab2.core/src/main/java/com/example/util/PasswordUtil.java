package com.example.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
public class PasswordUtil {
    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public static String hashPassword(String plainPassword) {
        try {
            return passwordEncoder.encode(plainPassword);
        } catch (Exception e) {
            log.error("Error hashing password", e);
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        try {
            return passwordEncoder.matches(plainPassword, hashedPassword);
        } catch (Exception e) {
            log.error("Error verifying password", e);
            return false;
        }
    }
}