package com.rmap.platform.auth;

import com.rmap.platform.user.Role;

public record AuthenticatedUser(
        String userId,
        String email,
        Role role
) {}
