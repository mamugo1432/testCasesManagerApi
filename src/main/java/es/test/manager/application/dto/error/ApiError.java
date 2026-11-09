package es.test.manager.application.dto.error;

import java.util.List;

/** Structured error body shared by every API error response. */
public record ApiError(String message, List<FieldError> fields) {

    public ApiError(String message) {
        this(message, List.of());
    }
}
