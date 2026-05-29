package com.memoassistant.auth;

import java.time.LocalDateTime;

public record AppUser(
        Long id,
        String username,
        String passwordHash,
        String displayName,
        String role,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}

