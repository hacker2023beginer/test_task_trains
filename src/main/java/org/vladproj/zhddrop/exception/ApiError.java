package org.vladproj.zhddrop.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public record ApiError(Instant timestamp, int status, String code, String message, List<String> details) {

    public static ApiError of(HttpStatus status, String code, String message) {
        return of(status, code, message, List.of());
    }

    public static ApiError of(HttpStatus status, String code, String message, List<String> details) {
        return new ApiError(
            Instant.now(),
            status.value(),
            code,
            message,
            details == null ? List.of() : List.copyOf(details));
    }
}
