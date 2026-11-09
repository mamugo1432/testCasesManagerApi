package es.test.manager.infraestructure.adapter.out.security;

import es.test.manager.domain.model.AuthenticatedUser;
import es.test.manager.domain.model.Role;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Adapter between the domain user and Spring Security's {@link UserDetails}. It keeps the domain
 * model free of framework dependencies while exposing the password hash, the role and the authorities
 * that {@code DaoAuthenticationProvider} needs.
 */
@Getter
public class SecurityUser implements UserDetails {

    private static final String AUTHORITY_PREFIX = "ROLE_";

    private final Long id;
    private final String username;
    private final String email;
    private final String password;
    private final Role role;
    private final Collection<? extends GrantedAuthority> authorities;

    public SecurityUser(Long id, String username, String email, String password, Role role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role == null ? Role.USER : role;
        this.authorities = authorityOf(this.role);
    }

    /** Translates the domain role into the Spring Security authority (ROLE_ADMIN or ROLE_USER). */
    public static Collection<? extends GrantedAuthority> authorityOf(Role role) {
        Role effectiveRole = role == null ? Role.USER : role;
        return List.of(new SimpleGrantedAuthority(AUTHORITY_PREFIX + effectiveRole.name()));
    }

    /** Identity without credentials, used to issue the JWT. */
    public AuthenticatedUser toAuthenticatedUser() {
        return new AuthenticatedUser(id, username, email, role);
    }
}
