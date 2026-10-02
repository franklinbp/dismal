package com.dismal.distribuciones.exception;

import java.time.LocalDateTime;

/**
 * A standardized DTO for returning error details in an API response.
 * Using a Java Record for a concise, immutable data carrier.
 *
 * @param timestamp The time the error occurred.
 * @param status The HTTP status code.
 * @param error The HTTP error reason phrase.
 * @param message A developer-friendly error message.
 * @param path The path where the error occurred.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {}
