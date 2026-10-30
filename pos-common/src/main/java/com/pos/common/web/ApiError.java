package com.pos.common.web;

import java.time.Instant;
import java.util.List;

/** Uniform error body returned by every service. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details) {
}
