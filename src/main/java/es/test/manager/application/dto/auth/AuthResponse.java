package es.test.manager.application.dto.auth;

/** Successful login response: only the access token, no session state on the server. */
public record AuthResponse(String accessToken) {}
