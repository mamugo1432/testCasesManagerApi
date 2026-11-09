package es.test.manager.application.dto.error;

/** Field-level validation error: the offending field and a human readable message. */
public record FieldError(String field, String message) {}
