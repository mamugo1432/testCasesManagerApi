package es.test.manager.infraestructure.adapter.out.security;

import es.test.manager.domain.exception.InvalidTokenException;
import es.test.manager.domain.model.AuthenticatedUser;
import es.test.manager.domain.port.out.CurrentUserProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Reads the {@link AuthenticatedUser} that the JWT filter stores in the security context. */
@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    @Override
    public AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new InvalidTokenException();
        }
        return user;
    }
}
