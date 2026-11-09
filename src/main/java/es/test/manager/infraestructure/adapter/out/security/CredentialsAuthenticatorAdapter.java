package es.test.manager.infraestructure.adapter.out.security;

import es.test.manager.domain.exception.InvalidCredentialsException;
import es.test.manager.domain.model.AuthenticatedUser;
import es.test.manager.domain.port.out.CredentialsAuthenticator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * Delegates credential verification to Spring Security's {@link AuthenticationManager}, which loads
 * the user through {@code JpaUserDetailsService} and compares the password with the configured
 * {@code PasswordEncoder}. Any authentication failure is translated to the same domain exception, so
 * unknown users and wrong passwords are indistinguishable (RF-013).
 */
@Component
@RequiredArgsConstructor
public class CredentialsAuthenticatorAdapter implements CredentialsAuthenticator {

    private final AuthenticationManager authenticationManager;

    @Override
    public AuthenticatedUser authenticate(String identifier, String rawPassword) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, rawPassword));
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException();
        }

        // The principal is checked instead of cast blindly: a missing principal or one that is not a
        // SecurityUser must fail as a normal authentication error, never as an uncaught
        // NullPointerException/ClassCastException.
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser user)) {
            throw new InvalidCredentialsException();
        }
        return user.toAuthenticatedUser();
    }
}
