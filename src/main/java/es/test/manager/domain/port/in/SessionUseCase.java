package es.test.manager.domain.port.in;

import es.test.manager.application.dto.auth.UserResponse;
import es.test.manager.domain.model.AuthenticatedUser;

/** Inbound port: return the identity carried by the current session token. */
public interface SessionUseCase {

    UserResponse getCurrentUser(AuthenticatedUser user);
}
