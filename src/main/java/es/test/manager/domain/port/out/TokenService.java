package es.test.manager.domain.port.out;

import es.test.manager.domain.model.AuthenticatedUser;

/** Outbound port: issue and validate access tokens. */
public interface TokenService {

    String generateToken(AuthenticatedUser user);

    /**
     * Validates the token and returns the identity it carries.
     *
     * @throws es.test.manager.domain.exception.InvalidTokenException if it is malformed, tampered with
     *     or expired
     */
    AuthenticatedUser extractUser(String token);
}
