package es.test.manager.domain.port.in;

import es.test.manager.application.dto.auth.RegisterRequest;
import es.test.manager.application.dto.auth.UserResponse;

/** Inbound port: create a new account. */
public interface RegisterUseCase {

    UserResponse register(RegisterRequest request);
}
