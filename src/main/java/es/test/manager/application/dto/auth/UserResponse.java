package es.test.manager.application.dto.auth;

import es.test.manager.domain.model.Role;

/** Public representation of a user: never exposes the password hash. */
public record UserResponse(Long id, String username, String email, Role role) {}
