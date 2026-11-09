package es.test.manager.domain.port.in;

import es.test.manager.application.dto.auth.AuthResponse;
import es.test.manager.application.dto.auth.LoginRequest;

/** Inbound port: start a session with a username or an email plus a password. */
public interface LoginUseCase {

    AuthResponse login(LoginRequest request);
}
