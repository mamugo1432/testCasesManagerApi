package es.test.manager.application.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Login payload. No trimming is applied: the identifier and the password are compared exactly as the
 * user typed them (RF-011).
 */
public record LoginRequest(
        @NotBlank(message = "El identificador es obligatorio") String identifier,
        @NotBlank(message = "La contraseña es obligatoria") String password) {}
