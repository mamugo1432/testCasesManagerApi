package es.test.manager.domain.port.out;

import es.test.manager.domain.model.AuthenticatedUser;

/** Outbound port: verify a user's credentials against the user store. */
public interface CredentialsAuthenticator {

    /**
     * Checks the identifier (username or email) and the raw password.
     *
     * @return the identity of the authenticated user, ready to be put in a token
     * @throws es.test.manager.domain.exception.InvalidCredentialsException if the identifier does not
     *     exist or the password does not match
     */
    AuthenticatedUser authenticate(String identifier, String rawPassword);
}
