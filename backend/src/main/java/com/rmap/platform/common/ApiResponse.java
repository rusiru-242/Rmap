package com.rmap.platform.common;

import java.time.Instant;
import java.util.UUID;

public record ApiResponse<T>(
        boolean success,
        T data,
        Instant timestamp,
        String requestId
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, Instant.now(), UUID.randomUUID().toString());
    }
}
